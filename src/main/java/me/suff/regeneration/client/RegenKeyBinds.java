package me.suff.regeneration.client;

import me.suff.regeneration.RegenerationMod;
import me.suff.regeneration.common.capability.CapabilityRegeneration;
import me.suff.regeneration.network.MessageTriggerRegeneration;
import me.suff.regeneration.network.NetworkHandler;
import me.suff.regeneration.util.EnumCompatModids;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.util.InputMappings;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputUpdateEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import org.lwjgl.glfw.GLFW;

/**
 * Created by Sub
 * on 17/09/2018.
 */
@EventBusSubscriber(Dist.CLIENT)
public class RegenKeyBinds {
	private static KeyBinding REGEN_NOW;
	
	public static void init() {
		if (!EnumCompatModids.LCCORE.isLoaded()) {
			REGEN_NOW= new KeyBinding("regeneration.keybinds.regenerate", KeyConflictContext.IN_GAME, KeyModifier.NONE, InputMappings.getInputByCode(GLFW.GLFW_KEY_O, 0), RegenerationMod.NAME);
			ClientRegistry.registerKeyBinding(REGEN_NOW);
		}
	}
	
	
	@SubscribeEvent
	public static void keyInput(InputUpdateEvent e) {
		EntityPlayer player = Minecraft.getInstance().player;
		if (player == null || EnumCompatModids.LCCORE.isLoaded())
			return;
		if (REGEN_NOW.isPressed() && CapabilityRegeneration.get(player).getState().isGraceful()) {
			NetworkHandler.sendToServer(new MessageTriggerRegeneration(player.getUniqueID(), player.dimension.getId()));
		}
	}
	
	/**
	 * Handles LCCore compatibility
	 */
	public static String getRegenerateNowDisplayName() {
		return REGEN_NOW.getTranslationKey();
	}
	
}
