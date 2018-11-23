package me.fril.regeneration;

import me.fril.regeneration.common.capability.CapabilityRegeneration;
import me.fril.regeneration.common.capability.IRegeneration;
import me.fril.regeneration.common.capability.RegenerationProvider;
import me.fril.regeneration.util.RegenConfig;
import me.fril.regeneration.util.RegenObjects;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.SoundCategory;
import net.minecraft.world.storage.loot.LootEntry;
import net.minecraft.world.storage.loot.LootEntryTable;
import net.minecraft.world.storage.loot.LootPool;
import net.minecraft.world.storage.loot.RandomValueRange;
import net.minecraft.world.storage.loot.conditions.LootCondition;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerChangedDimensionEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerRespawnEvent;

/**
 * Created by Sub
 * on 16/09/2018.
 */
@Mod.EventBusSubscriber(modid = RegenerationMod.MODID)
public class RegenerationEventHandler {
	
	//=========== CAPABILITY HANDLING =============
	@SubscribeEvent
	public static void onPlayerUpdate(LivingEvent.LivingUpdateEvent event) {
		if (event.getEntityLiving() instanceof EntityPlayer) {
			CapabilityRegeneration.getForPlayer((EntityPlayer) event.getEntityLiving()).update();
		}
	}
	
	@SubscribeEvent
	public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
		if (event.getObject() instanceof EntityPlayer) {
			event.addCapability(CapabilityRegeneration.CAP_REGEN_ID, new RegenerationProvider(new CapabilityRegeneration((EntityPlayer) event.getObject())));
		}
	}
	
	@SubscribeEvent
	public static void onPlayerClone(PlayerEvent.Clone event) {
		NBTTagCompound nbt = (NBTTagCompound) CapabilityRegeneration.CAPABILITY.getStorage().writeNBT(CapabilityRegeneration.CAPABILITY, CapabilityRegeneration.getForPlayer(event.getEntityPlayer()), null);
		CapabilityRegeneration.CAPABILITY.getStorage().readNBT(CapabilityRegeneration.CAPABILITY, CapabilityRegeneration.getForPlayer(event.getEntityPlayer()), null, nbt);
	}
	
	@SubscribeEvent
	public static void playerTracking(PlayerEvent.StartTracking event) {
		CapabilityRegeneration.getForPlayer(event.getEntityPlayer()).sync();
	}
	
	@SubscribeEvent
	public static void onPlayerRespawn(PlayerRespawnEvent event) {
		CapabilityRegeneration.getForPlayer(event.player).sync();
	}
	
	@SubscribeEvent
	public static void onPlayerChangedDimension(PlayerChangedDimensionEvent event) {
		CapabilityRegeneration.getForPlayer(event.player).sync();
	}
	
	@SubscribeEvent
	public static void onPlayerLoggedIn(PlayerLoggedInEvent event) {
		CapabilityRegeneration.getForPlayer(event.player).sync();
	}
	
	
	
	
	//============ USER EVENTS ==========
	@SubscribeEvent
	public static void breakBlock(PlayerInteractEvent.LeftClickBlock event) {
		EntityPlayer player = event.getEntityPlayer();
		IRegeneration cap = CapabilityRegeneration.getForPlayer(player);
		boolean inGracePeriod = cap.isInGracePeriod() && cap.isGlowing();
		
		if (inGracePeriod) {
			cap.setGlowing(false);
			cap.setTicksGlowing(0);
			cap.sync();
		}
	}
	
	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void onHurt(LivingHurtEvent event) {
		if (!(event.getEntity() instanceof EntityPlayer))
			return;
		
		EntityPlayer player = (EntityPlayer) event.getEntity();
		IRegeneration cap = CapabilityRegeneration.getForPlayer(player);
		if (player.getHealth() + player.getAbsorptionAmount() - event.getAmount() > 0 ||
				!cap.isCapable() || cap.isRegenerating()) {
			
			if (cap.isRegenerating()) {
				//FIXME correctly handle death mid-regen
				cap.reset();
				
				//XXX does not work, I remember something about onHurt only firing on server, but how do I fix it?
				if (player.world.isRemote && player.getEntityId() == Minecraft.getMinecraft().player.getEntityId()) {
					//FIXME perspective not resetting when killed mid-regen
					Minecraft.getMinecraft().gameSettings.thirdPersonView = 0;
				}
			}
			return;
		}
		
		event.setCanceled(true);
		cap.setRegenerating(true);
		
		player.clearActivePotions();
		player.extinguish();
		player.setArrowCountInEntity(0);
		
		if (cap.isRegenerating() && cap.isInGracePeriod()) {
			player.world.playSound(null, player.posX, player.posY, player.posZ, RegenObjects.Sounds.HAND_GLOW, SoundCategory.PLAYERS, 1.0F, 1.0F);
			cap.setInGracePeriod(false);
			cap.setSolaceTicks(199);
		}
	}
	
	
	
	
	
	//================ OTHER ==============
	@SubscribeEvent
	public static void onLogin(PlayerLoggedInEvent event) {
		if (!RegenConfig.startAsTimelord || event.player.world.isRemote)
			return;
		
		NBTTagCompound nbt = event.player.getEntityData();
		boolean loggedInBefore = nbt.getBoolean("loggedInBefore");
		if (!loggedInBefore) {
			event.player.inventory.addItemStackToInventory(new ItemStack(RegenObjects.Items.FOB_WATCH));
			nbt.setBoolean("loggedInBefore", true);
		}
	}
	
	@SubscribeEvent
	public static void registerLoot(LootTableLoadEvent event) {
		if (!event.getName().toString().toLowerCase().matches(RegenConfig.Loot.lootRegex) || RegenConfig.Loot.disableLoot)
			return;
		
		//TODO configurable chances? Maybe by doing a simple loottable turtorial?
		LootEntryTable entry = new LootEntryTable(RegenerationMod.LOOT_FILE, 1, 0, new LootCondition[0], "regeneration_inject_entry");
		LootPool pool = new LootPool(new LootEntry[] { entry }, new LootCondition[0], new RandomValueRange(1), new RandomValueRange(1), "regeneration_inject_pool");
		event.getTable().addPool(pool);
	}
	
}
