package its_meow.fluidgun.network;

import its_meow.fluidgun.client.ClientEvents;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;

public class GunFiredPacketHandler implements IMessageHandler<GunFiredPacket, IMessage> {

    @Override
    public IMessage onMessage(GunFiredPacket message, MessageContext ctx) {
        if(ctx.side == Side.SERVER) {
            return null;
        }
        
        return ClientEvents.onGunFired(message);
    }

}
