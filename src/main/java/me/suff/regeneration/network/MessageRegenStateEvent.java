package me.suff.regeneration.network;

import me.suff.regeneration.common.capability.CapabilityRegeneration;
import me.suff.regeneration.handlers.ActingForwarder;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

public class MessageRegenStateEvent {
	
	private UUID player;
	private String event;
	
	public MessageRegenStateEvent() {
	}
	
	public MessageRegenStateEvent(EntityPlayer player, String event) {
		this.player = player.getUniqueID();
		this.event = event;
	}
	
	public static void encode(MessageRegenStateEvent event, PacketBuffer packetBuffer) {
		packetBuffer.writeUniqueId(event.player);
		packetBuffer.writeString(event.event);
	}
	
	public static MessageRegenStateEvent decode(PacketBuffer buffer) {
		if (Minecraft.getInstance().player == null)
			return null;
		return new MessageRegenStateEvent(Objects.requireNonNull(Minecraft.getInstance().player.world.getPlayerEntityByUUID(buffer.readUniqueId())), buffer.readString(600));
	}
	
	
	public static class Handler {
		private static EntityPlayer player;
		
		public static void handle(MessageRegenStateEvent message, Supplier<NetworkEvent.Context> ctx) {
			Minecraft.getInstance().addScheduledTask(() ->
					player = Minecraft.getInstance().world.getPlayerEntityByUUID(message.player));
			ActingForwarder.onClient(message.event, CapabilityRegeneration.get(player)
			);
			ctx.get().setPacketHandled(true);
		}
	}
	
}
