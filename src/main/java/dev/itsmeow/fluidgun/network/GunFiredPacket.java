package dev.itsmeow.fluidgun.network;

import dev.itsmeow.fluidgun.client.ClientEvents;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.Hand;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class GunFiredPacket {

    public String gunName;
    public Hand hand;
    public int count;
    public int max;

    public GunFiredPacket(String gunName, Hand hand, int count, int max) {
        this.gunName = gunName;
        this.hand = hand;
        this.count = count;
        this.max = max;
    }

    public static void encode(GunFiredPacket pkt, PacketBuffer buf) {
        buf.writeString(pkt.gunName, 32);
        buf.writeBoolean(pkt.hand == Hand.MAIN_HAND);
        buf.writeInt(pkt.count);
        buf.writeInt(pkt.max);
    }

    public static GunFiredPacket decode(PacketBuffer buf) {
        String gunName = buf.readString(32);
        Hand hand = buf.readBoolean() ? Hand.MAIN_HAND : Hand.OFF_HAND;
        int count = buf.readInt();
        int max = buf.readInt();
        return new GunFiredPacket(gunName, hand, count, max);
    }

    public static class Handler {

        public static void handle(GunFiredPacket msg, Supplier<NetworkEvent.Context> ctx) {
            if(ctx.get().getDirection() != NetworkDirection.PLAY_TO_CLIENT) {
                ctx.get().setPacketHandled(false);
                return;
            }
            ctx.get().enqueueWork(() -> ClientEvents.onGunFired(msg));
            ctx.get().setPacketHandled(true);
        }

    }

}
