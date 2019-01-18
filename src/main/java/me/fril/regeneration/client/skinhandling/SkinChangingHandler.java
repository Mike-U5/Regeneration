package me.fril.regeneration.client.skinhandling;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mojang.authlib.minecraft.MinecraftProfileTexture;

import me.fril.regeneration.RegenConfig;
import me.fril.regeneration.common.capability.CapabilityRegeneration;
import me.fril.regeneration.common.capability.IRegeneration;
import me.fril.regeneration.network.MessageUpdateSkin;
import me.fril.regeneration.network.NetworkHandler;
import me.fril.regeneration.util.ClientUtil;
import me.fril.regeneration.util.RegenState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StringUtils;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class SkinChangingHandler {
	
	public static final File SKIN_DIRECTORY = new File("./mods/regeneration/skins/");
	public static final Map<UUID, SkinInfo> PLAYER_SKINS = new HashMap<>();
	private static String CURRENT_SKIN = "banana.png";
	
	private static final File SKIN_CACHE_DIRECTORY = new File("./mods/regeneration/skincache/" + Minecraft.getMinecraft().getSession().getProfile().getId() + "/skins");
	private static final File SKIN_DIRECTORY_STEVE = new File(SKIN_DIRECTORY, "/steve");
	private static final File SKIN_DIRECTORY_ALEX = new File(SKIN_DIRECTORY, "/alex");
	
	private static FilenameFilter IMAGE_FILTER = (dir, name)->name.endsWith(".png") && !name.equals(CURRENT_SKIN);
	private static final Logger SKIN_LOG = LogManager.getLogger(SkinChangingHandler.class); //TODO move to debugger
	private static final Random RAND = new Random();
	
	private static final ModelBase STEVE_MODEL = new ModelPlayer(0.1F, false);
	private static final ModelBase ALEX_MODEL = new ModelPlayer(0.1F, true);
	
	public static void registerResources() {
		
		if (!SKIN_CACHE_DIRECTORY.exists()) {
			SKIN_CACHE_DIRECTORY.mkdirs();
		}
		
		if (!SKIN_DIRECTORY.exists()) {
			SKIN_DIRECTORY.mkdirs();
		}
		
		SKIN_DIRECTORY_ALEX.mkdirs();
		SKIN_DIRECTORY_STEVE.mkdirs();
		
		if (Objects.requireNonNull(SKIN_DIRECTORY_ALEX.listFiles()).length < 1 || Objects.requireNonNull(SKIN_DIRECTORY_STEVE.listFiles()).length < 1) {
			try {
				createDefaultImages();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	// Convert buffered image to Pixel data
	private static byte[] encodeToPixelData(BufferedImage bufferedImage) {
		WritableRaster raster = bufferedImage.getRaster();
		DataBufferByte buffer = (DataBufferByte) raster.getDataBuffer();
		return buffer.getData();
	}
	
	// Convert Pixel data to BufferedImage
	private static BufferedImage toImage(EntityPlayer player, byte[] imageData) throws IOException {
		BufferedImage img = new BufferedImage(64, 64, BufferedImage.TYPE_4BYTE_ABGR);
		img.setData(Raster.createRaster(img.getSampleModel(), new DataBufferByte(imageData, imageData.length), new Point()));
		File file = new File(SKIN_CACHE_DIRECTORY, "cache-" + player.getUniqueID() + ".png");
		
		if (!file.getParentFile().exists()) {
			file.getParentFile().mkdirs();
		}
		if (file.exists()) {
			file.delete();
		}
		
		ImageIO.write(img, "png", file);
		
		return img;
	}
	
	public static void skinChangeRandom(Random random, EntityPlayer player) throws IOException {
		if (Minecraft.getMinecraft().player.getUniqueID() != player.getUniqueID())
			return;
		if (RegenConfig.changeMySkin) {
			
			boolean isAlex = RegenConfig.prefferedModel.isAlex();
			
			File skin = SkinChangingHandler.getRandomSkinFile(random, isAlex);
			BufferedImage image = ImageIO.read(skin);
			CURRENT_SKIN = skin.getName();
			IMAGE_FILTER = (dir, name)->name.endsWith(".png") && !name.equals(CURRENT_SKIN);
			byte[] pixelData = SkinChangingHandler.encodeToPixelData(image);
			CapabilityRegeneration.getForPlayer(player).setEncodedSkin(pixelData);
			if (pixelData.length >= 32767) {
				NetworkHandler.INSTANCE.sendToServer(new MessageUpdateSkin(new byte[0], isAlex));
			} else {
				NetworkHandler.INSTANCE.sendToServer(new MessageUpdateSkin(pixelData, isAlex));
			}
		} else {
			ClientUtil.sendResetPacket();
		}
	}
	
	private static SkinInfo getSkin(EntityPlayer pl, IRegeneration data) throws IOException {
		AbstractClientPlayer player = (AbstractClientPlayer) pl;
		byte[] encodedSkin = CapabilityRegeneration.getForPlayer(pl).getEncodedSkin();
		ResourceLocation resourceLocation = null;
		SkinInfo.SkinType skinType = null;
		
		if (Arrays.equals(data.getEncodedSkin(), new byte[0]) || encodedSkin.length < 16383) {
			resourceLocation = getSkinFromMojang(player);
			
			if (player.getSkinType().equals("slim")) {
				skinType = SkinInfo.SkinType.ALEX;
			} else {
				skinType = SkinInfo.SkinType.STEVE;
			}
		} else {
			BufferedImage bufferedImage = toImage(pl, encodedSkin);
			resourceLocation = Minecraft.getMinecraft().getTextureManager().getDynamicTextureLocation(pl.getName() + "_skin", new DynamicTexture(bufferedImage));
			skinType = CapabilityRegeneration.getForPlayer(player).getSkinType();
		}
		
		return new SkinInfo(resourceLocation, skinType);
	}
	
	private static ResourceLocation getSkinFromMojang(AbstractClientPlayer player) throws IOException {
		setPlayerTexture(player, null);
		Minecraft minecraft = Minecraft.getMinecraft();
		URL url = new URL(String.format(RegenConfig.downloadUrl, StringUtils.stripControlCodes(player.getUniqueID().toString())));
		BufferedImage img = ImageIO.read(url);
		SKIN_LOG.info("Downloading Skin from: " + url.toString());
		ImageIO.write(img, "png", new File(SKIN_CACHE_DIRECTORY, "cache-" + player.getUniqueID() + ".png"));
		return minecraft.getTextureManager().getDynamicTextureLocation(player.getName() + "_skin", new DynamicTexture(img));
	}
	
	private static void downloadImages(URL url, File file, String filename) throws IOException {
		SKIN_LOG.info("Downloading Skin from: " + url.toString());
		BufferedImage img = ImageIO.read(url);
		ImageIO.write(img, "png", new File(file, filename + ".png"));
	}
	
	private static File getRandomSkinFile(Random rand, boolean isAlex) {
		File skins = null;
		if (isAlex) {
			skins = SKIN_DIRECTORY_ALEX;
		} else {
			skins = SKIN_DIRECTORY_STEVE;
		}
		File[] files = skins.listFiles(IMAGE_FILTER);
		
		if (files.length <= 1) {
			files = skins.listFiles();
		}
		
		File file = files[rand.nextInt(files.length)];
		return file;
	}
	
	private static void createDefaultImages() throws IOException {
		for (DefaultSkins value : DefaultSkins.values()) {
			File dummy;
			if (value.isAlexDir()) {
				dummy = SKIN_DIRECTORY_ALEX;
			} else {
				dummy = SKIN_DIRECTORY_STEVE;
			}
			
			downloadImages(new URL(value.getURL()), dummy, value.name().toLowerCase());
		}
	}
	
	private static void setPlayerTexture(AbstractClientPlayer player, ResourceLocation texture) {
		NetworkPlayerInfo playerInfo = ObfuscationReflectionHelper.getPrivateValue(AbstractClientPlayer.class, player, 0);
		if (playerInfo == null)
			return;
		Map<MinecraftProfileTexture.Type, ResourceLocation> playerTextures = ObfuscationReflectionHelper.getPrivateValue(NetworkPlayerInfo.class, playerInfo, 1);
		playerTextures.put(MinecraftProfileTexture.Type.SKIN, texture);
		if (texture == null)
			ObfuscationReflectionHelper.setPrivateValue(NetworkPlayerInfo.class, playerInfo, false, 4);
	}
	
	private static void setPlayerModel(RenderPlayer renderer, ModelBase model) {
		renderer.mainModel = model;
	}
	
	@SubscribeEvent
	public void onRenderPlayer(RenderPlayerEvent.Pre e) {
		AbstractClientPlayer player = (AbstractClientPlayer) e.getEntityPlayer();
		IRegeneration cap = CapabilityRegeneration.getForPlayer(player);
		
		if (cap.getState() == RegenState.REGENERATING) {
			cap.getType().getRenderer().onRenderRegeneratingPlayerPre(cap.getType(), e, cap); //SUB where was this before?
		} else if (cap.getState() == RegenState.ALIVE && !PLAYER_SKINS.containsKey(player.getUniqueID())) {
			setSkinFromData(player, cap, e.getRenderer());
		}
	}
	
	private void setSkinFromData(AbstractClientPlayer player, IRegeneration cap, RenderPlayer renderPlayer) {
		SkinInfo skinInfo = null;
		try {
			skinInfo = SkinChangingHandler.getSkin(player, cap);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		SkinChangingHandler.setPlayerTexture(player, skinInfo.getTextureLocation());
		
		if (skinInfo.getSkintype() == SkinInfo.SkinType.ALEX) {
			SkinChangingHandler.setPlayerModel(renderPlayer, ALEX_MODEL);
		} else {
			SkinChangingHandler.setPlayerModel(renderPlayer, STEVE_MODEL);
		}
		PLAYER_SKINS.put(player.getGameProfile().getId(), skinInfo);
	}
	
	public enum EnumChoices {
		ALEX(true), STEVE(false), EITHER(true);
		
		private boolean isAlex;
		
		EnumChoices(boolean b) {
			this.isAlex = b;
		}
		
		public boolean isAlex() {
			if (this == EITHER) {
				return RAND.nextBoolean();
			}
			return isAlex;
		}
	}
	
}