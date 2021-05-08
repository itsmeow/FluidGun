package dev.itsmeow.fluidgun.network;

import dev.itsmeow.fluidgun.FluidGunConfigMain;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class ConfigurationPacket {

    public String itemName;

    public int capacity;
    public float range;

    public ConfigurationPacket(String itemName, int capacity, float range) {
        this.itemName = itemName;
        this.capacity = capacity;
        this.range = range;
    }

    public static ConfigurationPacket decode(PacketBuffer buf) {
        String itemName = buf.readString(32);
        int capacity = buf.readInt();
        float range = buf.readFloat();
        return new ConfigurationPacket(itemName, capacity, range);
    }

    public static void encode(ConfigurationPacket pkt, PacketBuffer buf) {
        buf.writeString(pkt.itemName, 32);
        buf.writeInt(pkt.capacity);
        buf.writeFloat(pkt.range);
    }

    public static class Handler {

        public static void handle(ConfigurationPacket msg, Supplier<NetworkEvent.Context> ctx) {
            if(ctx.get().getDirection() != NetworkDirection.PLAY_TO_CLIENT) {
                ctx.get().setPacketHandled(false);
                return;
            }
            ctx.get().enqueueWork(() -> {
                FluidGunConfigMain.GunConfig.COUNT.put(msg.itemName, msg.capacity);
                FluidGunConfigMain.GunConfig.RANGE.put(msg.itemName, msg.range);
            });
            ctx.get().setPacketHandled(true);
        }

    }

}