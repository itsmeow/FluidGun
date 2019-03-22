package its_meow.fluidgun.network;

import com.google.common.base.Charsets;

import io.netty.buffer.ByteBuf;
import net.minecraft.util.EnumHand;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

public class GunFiredPacket implements IMessage {
    
    public String gunName = "";
    public String fluidName = "";
    public EnumHand hand = EnumHand.MAIN_HAND;
    public int count = 0;
    public int max = 1;
    
    public GunFiredPacket() {}
    
    public GunFiredPacket(String gunName, String fluidName, EnumHand hand, int count, int max) {
        this.gunName = gunName;
        this.fluidName = fluidName;
        this.hand = hand;
        this.count = count;
        this.max = max;
    }
    
    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(gunName.length());
        buf.writeCharSequence(gunName, Charsets.UTF_8);
        buf.writeInt(fluidName.length());
        buf.writeCharSequence(fluidName, Charsets.UTF_8);
        buf.writeInt(hand.ordinal());
        buf.writeInt(count);
        buf.writeInt(max);
    }
    
    @Override
    public void fromBytes(ByteBuf buf) {
        int len1 = buf.readInt();
        this.gunName = buf.readCharSequence(len1, Charsets.UTF_8).toString();
        int len2 = buf.readInt();
        this.fluidName = buf.readCharSequence(len2, Charsets.UTF_8).toString();
        this.hand = EnumHand.values()[buf.readInt()];
        this.count = buf.readInt();
        this.max = buf.readInt();
    }

}
