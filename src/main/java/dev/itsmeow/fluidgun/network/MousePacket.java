package dev.itsmeow.fluidgun.network;

import dev.itsmeow.fluidgun.content.ItemBaseFluidGun;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.Hand;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class MousePacket {

    private Hand hand;
    private boolean forward;

    public MousePacket(Hand hand, boolean forward) {
        this.hand = hand;
        this.forward = forward;
    }

    public static void encode(MousePacket pkt, PacketBuffer buffer) {
        buffer.writeBoolean(pkt.hand == Hand.MAIN_HAND);
        buffer.writeBoolean(pkt.forward);
    }

    public static MousePacket decode(PacketBuffer buffer) {
        Hand hand = buffer.readBoolean() ? Hand.MAIN_HAND : Hand.OFF_HAND;
        boolean forward = buffer.readBoolean();
        return new MousePacket(hand, forward);
    }
    
    public static class Handler {
        
        public static void handle(MousePacket p, Supplier<NetworkEvent.Context> ctx) {
            if(ctx.get().getDirection() != NetworkDirection.PLAY_TO_SERVER) {
                ctx.get().setPacketHandled(false);
                return;
            }

            ctx.get().enqueueWork(() -> {
                ServerPlayerEntity player = ctx.get().getSender();

                ItemStack stack = player.getHeldItem(p.hand);
                if(stack.getItem() instanceof ItemBaseFluidGun) {
                    ItemBaseFluidGun gun = (ItemBaseFluidGun) stack.getItem();
                    gun.handleMouseWheelAction(stack, player, p.forward);
                }
            });
            ctx.get().setPacketHandled(true);
        }
        
    }
    
}