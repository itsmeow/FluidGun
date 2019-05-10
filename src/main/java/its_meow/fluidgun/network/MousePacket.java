package its_meow.fluidgun.network;

import java.util.function.Supplier;

import its_meow.fluidgun.content.ItemBaseFluidGun;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

public class MousePacket {

    int slot;
    boolean forward;

    public MousePacket() {
    }

    public MousePacket(int slot, boolean forward) {
        this.slot = slot;
        this.forward = forward;
    }

    public static void encode(MousePacket pkt, PacketBuffer buffer) {
        buffer.writeInt(pkt.slot);
        buffer.writeBoolean(pkt.forward);
    }

    public static MousePacket decode(PacketBuffer buffer) {
        int slot = buffer.readInt();
        boolean forward = buffer.readBoolean();
        return new MousePacket(slot, forward);
    }
    
    public static class Handler {
        
        public static void handle(MousePacket p, Supplier<NetworkEvent.Context> ctx) {
            EntityPlayerMP player = ctx.get().getSender();

            ItemStack stack = player.inventory.getStackInSlot(p.slot);
            if(stack != null && stack.getItem() instanceof ItemBaseFluidGun) {
                ItemBaseFluidGun gun = (ItemBaseFluidGun) stack.getItem();
                gun.handleMouseWheelAction(stack, player, false, p.forward);
            }
        }
        
    }
    
}