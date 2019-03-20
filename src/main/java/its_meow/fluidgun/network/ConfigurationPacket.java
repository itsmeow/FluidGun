package its_meow.fluidgun.network;

import com.google.common.base.Charsets;

import io.netty.buffer.ByteBuf;
import its_meow.fluidgun.FluidGunItemConfig;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

public class ConfigurationPacket implements IMessage {
	
	public String itemName;
	
	public FluidGunItemConfig config;
	
	public ConfigurationPacket(String itemName, FluidGunItemConfig config) {
		this.itemName = itemName;
		this.config = config;
	}
	
	@Override
	public void fromBytes(ByteBuf buf) {
		buf.writeInt(itemName.length());
		buf.writeCharSequence(itemName, Charsets.UTF_8);
		this.config = new FluidGunItemConfig(buf);
	}

	@Override
	public void toBytes(ByteBuf buf) {
		int len = buf.readInt();
		this.itemName = buf.readCharSequence(len, Charsets.UTF_8).toString();
		config.toBytes(buf);
	}

}
