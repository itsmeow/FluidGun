package its_meow.fluidgun.network;

import its_meow.fluidgun.BaseMod;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;

public class ConfigurationPacketHandler implements IMessageHandler<ConfigurationPacket, IMessage> {

    @Override
    public IMessage onMessage(ConfigurationPacket message, MessageContext ctx) {
        if(ctx.side == Side.SERVER) {
            return null;
        }
        
        BaseMod.FluidGunConfig.COUNT.put(message.itemName, message.capacity);
        BaseMod.FluidGunConfig.RANGE.put(message.itemName, message.range);
        
        return null;
    }

}