package me.suff.regeneration.network;

import me.suff.regeneration.common.capability.CapabilityRegeneration;
import me.suff.regeneration.common.capability.IRegeneration;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class MessageUpdateModel {
	
	private String preferred;
	
	
	public MessageUpdateModel() {
	}
	
	public MessageUpdateModel(String preferred) {
		this.preferred = preferred;
	}
	
	public static void encode(MessageUpdateModel model, PacketBuffer buf) {
		buf.writeString(model.preferred);
	}
	
	public static MessageUpdateModel decode(PacketBuffer buffer){
		return new MessageUpdateModel(buffer.readString(10));
	}
	
	public static class Handler {
		public static void handle(MessageUpdateModel message, Supplier<NetworkEvent.Context> ctx) {
			ctx.get().getSender().getServerWorld().addScheduledTask(() -> {
				IRegeneration data = CapabilityRegeneration.get(ctx.get().getSender());
				data.setPreferredModel(message.preferred);
				data.synchronise();
			});
			ctx.get().setPacketHandled(true);
		}
	}
}
