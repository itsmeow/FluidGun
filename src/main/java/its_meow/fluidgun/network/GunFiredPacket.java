package its_meow.fluidgun.network;

import java.util.function.Supplier;

import com.google.common.base.Charsets;

import its_meow.fluidgun.client.ClientEvents;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumHand;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkEvent;

public class GunFiredPacket {

    public String gunName = "";
    public EnumHand hand = EnumHand.MAIN_HAND;
    public int count = 0;
    public int max = 1;

    public GunFiredPacket() {}

    public GunFiredPacket(String gunName, EnumHand hand, int count, int max) {
        this.gunName = gunName;
        this.hand = hand;
        this.count = count;
        this.max = max;
    }

    public static void encode(GunFiredPacket pkt, PacketBuffer buf) {
        buf.writeInt(pkt.gunName.length());
        buf.writeCharSequence(pkt.gunName, Charsets.UTF_8);
        buf.writeInt(pkt.hand.ordinal());
        buf.writeInt(pkt.count);
        buf.writeInt(pkt.max);
    }

    public static GunFiredPacket decode(PacketBuffer buf) {
        int len1 = buf.readInt();
        String gunName = buf.readCharSequence(len1, Charsets.UTF_8).toString();
        EnumHand hand = EnumHand.values()[buf.readInt()];
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
