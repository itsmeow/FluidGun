package its_meow.fluidgun.network;

import java.util.function.Supplier;

import com.google.common.base.Charsets;

import its_meow.fluidgun.FluidGunConfigMain;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkEvent;

public class ConfigurationPacket {

    public String itemName;

    public int capacity;
    public float range;

    public ConfigurationPacket() {}

    public ConfigurationPacket(String itemName, int capacity, float range) {
        this.itemName = itemName;
        this.capacity = capacity;
        this.range = range;
    }

    public static ConfigurationPacket decode(PacketBuffer buf) {
        int len = buf.readInt();
        String itemName = buf.readCharSequence(len, Charsets.UTF_8).toString();
        int capacity = buf.readInt();
        float range = buf.readFloat();
        return new ConfigurationPacket(itemName, capacity, range);
    }

    public static void encode(ConfigurationPacket pkt, PacketBuffer buf) {
        buf.writeInt(pkt.itemName.length());
        buf.writeCharSequence(pkt.itemName, Charsets.UTF_8);
        buf.writeInt(pkt.capacity);
        buf.writeFloat(pkt.range);
    }

    public static class Handler {

        public static void handle(ConfigurationPacket msg, Supplier<NetworkEvent.Context> ctx) {
            if(ctx.get().getDirection() != NetworkDirection.PLAY_TO_CLIENT) {
                return;
            }

            FluidGunConfigMain.GunConfig.COUNT.put(msg.itemName, msg.capacity);
            FluidGunConfigMain.GunConfig.RANGE.put(msg.itemName, msg.range);
        }

    }

}