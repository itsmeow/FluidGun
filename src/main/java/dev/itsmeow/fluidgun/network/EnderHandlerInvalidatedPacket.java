package dev.itsmeow.fluidgun.network;

import dev.itsmeow.fluidgun.client.ClientEvents;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class EnderHandlerInvalidatedPacket {

    public int slot;

    public EnderHandlerInvalidatedPacket(int slot) {
        this.slot = slot;
    }

    public static void encode(EnderHandlerInvalidatedPacket pkt, PacketBuffer buf) {
        buf.writeInt(pkt.slot);
    }

    public static EnderHandlerInvalidatedPacket decode(PacketBuffer buf) {
        return new EnderHandlerInvalidatedPacket(buf.readInt());
    }

    public static class Handler {

        public static void handle(EnderHandlerInvalidatedPacket msg, Supplier<NetworkEvent.Context> ctx) {
            if(ctx.get().getDirection() != NetworkDirection.PLAY_TO_CLIENT) {
                ctx.get().setPacketHandled(false);
                return;
            }
            ctx.get().enqueueWork(() -> {
                ClientEvents.onHandlerInvalidated(msg);
            });
            ctx.get().setPacketHandled(true);
        }

    }

}
