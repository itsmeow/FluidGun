package its_meow.fluidgun.network;

import its_meow.fluidgun.content.ItemFluidGun;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class MouseHandler implements IMessageHandler<MousePacket, IMessage> {

    @Override
    public IMessage onMessage(MousePacket p, MessageContext ctx) {

        EntityPlayerMP player = ctx.getServerHandler().player;

        ItemStack stack = player.inventory.getStackInSlot(p.slot);
        if(stack != null && stack.getItem() instanceof ItemFluidGun) {
            ItemFluidGun gun = (ItemFluidGun) stack.getItem();
            gun.handleMouseWheelAction(stack, player, false, p.forward);
        }

        return null;
    }
}