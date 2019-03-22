package its_meow.fluidgun.network;

import com.google.common.base.Charsets;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

public class ConfigurationPacket implements IMessage {

    public String itemName;

    public int capacity;
    public float range;
    
    public ConfigurationPacket() {}

    public ConfigurationPacket(String itemName, int capacity, float range) {
        this.itemName = itemName;
        this.capacity = capacity;
        this.range = range;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        int len = buf.readInt();
        this.itemName = buf.readCharSequence(len, Charsets.UTF_8).toString();
        this.capacity = buf.readInt();
        this.range = buf.readFloat();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(itemName.length());
        buf.writeCharSequence(itemName, Charsets.UTF_8);
        buf.writeInt(this.capacity);
        buf.writeFloat(this.range);
    }

}