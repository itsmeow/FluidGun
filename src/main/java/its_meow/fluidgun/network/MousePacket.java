package its_meow.fluidgun.network;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

public class MousePacket implements IMessage {

    int slot;
    boolean forward;

    public MousePacket() {
    }

    public MousePacket(int slot, boolean forward) {
        this.slot = slot;
        this.forward = forward;
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeInt(slot);
        buffer.writeBoolean(forward);
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        slot = buffer.readInt();
        forward = buffer.readBoolean();
    }
}