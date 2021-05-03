package dev.itsmeow.fluidgun.network;

import dev.itsmeow.fluidgun.content.ItemBaseFluidGun;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.Hand;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class MousePacket {

    private boolean offHand;
    private boolean forward;

    public MousePacket(boolean offHand, boolean forward) {
        this.offHand = offHand;
        this.forward = forward;
    }

    public static void encode(MousePacket pkt, PacketBuffer buffer) {
        buffer.writeBoolean(pkt.offHand);
        buffer.writeBoolean(pkt.forward);
    }

    public static MousePacket decode(PacketBuffer buffer) {
        boolean offHand = buffer.readBoolean();
        boolean forward = buffer.readBoolean();
        return new MousePacket(offHand, forward);
    }
    
    public static class Handler {
        
        public static void handle(MousePacket p, Supplier<NetworkEvent.Context> ctx) {
            ServerPlayerEntity player = ctx.get().getSender();

            ItemStack stack = player.getHeldItem(p.offHand ? Hand.OFF_HAND : Hand.MAIN_HAND);
            if(stack.getItem() instanceof ItemBaseFluidGun) {
                ItemBaseFluidGun gun = (ItemBaseFluidGun) stack.getItem();
                gun.handleMouseWheelAction(stack, player, p.forward);
            }
        }
        
    }
    
}