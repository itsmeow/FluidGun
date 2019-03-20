package its_meow.fluidgun;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

public class FluidGunItemConfig implements IMessage {
	
	public int bucketCount = 5;
	public float range = 50F;
	
	public FluidGunItemConfig(int bucketCount, float range) {
		this.bucketCount = bucketCount;
		this.range = range;
	}

	public FluidGunItemConfig(ByteBuf buf) {
		this.fromBytes(buf);
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		this.bucketCount = buf.readInt();
		this.range = buf.readFloat();
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeInt(bucketCount);
		buf.writeFloat(range);
	}
	
}
