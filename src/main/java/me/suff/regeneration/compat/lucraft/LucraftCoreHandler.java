package me.suff.regeneration.compat.lucraft;

import lucraft.mods.lucraftcore.materials.potions.PotionRadiation;
import lucraft.mods.lucraftcore.sizechanging.capabilities.CapabilitySizeChanging;
import lucraft.mods.lucraftcore.sizechanging.capabilities.ISizeChanging;
import lucraft.mods.lucraftcore.superpowers.SuperpowerHandler;
import lucraft.mods.lucraftcore.util.abilitybar.AbilityBarHandler;
import lucraft.mods.lucraftcore.util.abilitybar.AbilityBarKeys;
import me.suff.regeneration.RegenConfig;
import me.suff.regeneration.common.capability.CapabilityRegeneration;
import me.suff.regeneration.common.capability.IRegeneration;
import me.suff.regeneration.handlers.IActingHandler;
import me.suff.regeneration.util.ClientUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.InputUpdateEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import static me.suff.regeneration.util.RegenUtil.randFloat;

public class LucraftCoreHandler implements IActingHandler {

	public static void registerEntry() {
		AbilityBarHandler.registerProvider(new LCCoreBarEntry());
	}
	
	public static void registerEventBus() {
		MinecraftForge.EVENT_BUS.register(new LucraftCoreHandler());
	}
	
	public static String getKeyBindDisplayName() {
		for (int i = 0; i < AbilityBarHandler.ENTRY_SHOW_AMOUNT; i++) {
			if (AbilityBarHandler.getEntryFromKey(i) instanceof LCCoreBarEntry) {
				return AbilityBarKeys.KEYS.get(i).getDisplayName();
			}
		}
		return "???";
	}
	
	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public void onGui(InputUpdateEvent tickEvent) {
		Minecraft minecraft = Minecraft.getMinecraft();
		if (minecraft.currentScreen == null && minecraft.player != null) {
			ClientUtil.keyBind = getKeyBindDisplayName();
		}
	}
	
	@Override
	public void onRegenTick(IRegeneration cap) {

	}
	
	@Override
	public void onEnterGrace(IRegeneration cap) {
		
	}
	
	@Override
	public void onHandsStartGlowing(IRegeneration cap) {
		
	}
	
	@Override
	public void onRegenFinish(IRegeneration cap) {
		
	}
	
	@Override
	public void onRegenTrigger(IRegeneration cap) {
		if (RegenConfig.modIntegrations.lucraftcore.lucraftcoreSizeChanging) {
			EntityPlayer player = cap.getPlayer();
			ISizeChanging sizeCap = player.getCapability(CapabilitySizeChanging.SIZE_CHANGING_CAP, null);
			if (sizeCap != null) {
				sizeCap.setSize(randFloat(RegenConfig.modIntegrations.lucraftcore.sizeChangingMin, RegenConfig.modIntegrations.lucraftcore.sizeChangingMax));
			}
		}
	}
	
	@Override
	public void onGoCritical(IRegeneration cap) {
		
	}
	
	@SubscribeEvent
	public void onHurt(LivingHurtEvent e) {
		if (e.getEntityLiving() instanceof EntityPlayer) {
			EntityPlayer player = (EntityPlayer) e.getEntityLiving();
			IRegeneration data = CapabilityRegeneration.getForPlayer(player);
			boolean flag = data.canRegenerate() && e.getSource() == PotionRadiation.RADIATION && RegenConfig.modIntegrations.lucraftcore.immuneToRadiation;
			e.setCanceled(flag);
		}
	}

	@SubscribeEvent
	public void onCanRegen(PlayerCanRegenEvent e) {
		if(RegenConfig.modIntegrations.lucraftcore.superpowerDisable && SuperpowerHandler.hasSuperpower(e.getEntityPlayer()))
			e.setCanceled(true);
	}
}
