package dev.itsmeow.fluidgun.network;

import dev.itsmeow.fluidgun.client.ClientEvents;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.Hand;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkEvent;

import java.nio.charset.StandardCharsets;
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
        buf.writeInt(pkt.gunName.length());
        buf.writeCharSequence(pkt.gunName, StandardCharsets.UTF_8);
        buf.writeInt(pkt.hand.ordinal());
        buf.writeInt(pkt.count);
        buf.writeInt(pkt.max);
    }

    public static GunFiredPacket decode(PacketBuffer buf) {
        int len1 = buf.readInt();
        String gunName = buf.readCharSequence(len1, StandardCharsets.UTF_8).toString();
        Hand hand = Hand.values()[buf.readInt()];
        int count = buf.readInt();
        int max = buf.readInt();
        return new GunFiredPacket(gunName, hand, count, max);
    }

    public static class Handler {

        public static void handle(GunFiredPacket msg, Supplier<NetworkEvent.Context> ctx) {
            if(ctx.get().getDirection() != NetworkDirection.PLAY_TO_CLIENT) {
                return;
            }

            ClientEvents.onGunFired(msg);
        }

    }

}
