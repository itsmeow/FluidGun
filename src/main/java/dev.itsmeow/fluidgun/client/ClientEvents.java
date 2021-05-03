package dev.itsmeow.fluidgun.client;

import dev.itsmeow.fluidgun.FluidGunConfigMain;
import dev.itsmeow.fluidgun.FluidGunMod;
import dev.itsmeow.fluidgun.content.ItemBaseFluidGun;
import dev.itsmeow.fluidgun.network.GunFiredPacket;
import dev.itsmeow.fluidgun.network.MousePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent.MouseScrollEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class ClientEvents {

    @SubscribeEvent
    public static void mouseEvent(MouseScrollEvent e) {
        PlayerEntity player = Minecraft.getInstance().player;

        for(Hand hand : Hand.values()) {
            ItemStack stack = player.getHeldItem(hand);
            if(stack.getItem() instanceof ItemBaseFluidGun) {
                if(Screen.hasAltDown() && e.getScrollDelta() != 0) {
                    FluidGunMod.HANDLER.sendToServer(new MousePacket(hand == Hand.OFF_HAND, e.getScrollDelta() > 0));
                    e.setCanceled(true);
                }
                break; // If player is holding two only scroll for main hand
            }
        }
    }

    public static void onGunFired(GunFiredPacket message) {
        String gunName = message.gunName;
        Hand hand = message.hand;
        int max = message.max;
        int count = message.count;

        int maxC = FluidGunConfigMain.GunConfig.COUNT.get(gunName);
        if(maxC == 0) return;
        FluidGunConfigMain.GunConfig.COUNT.put(gunName, max);
        Item i = ForgeRegistries.ITEMS.getValue(new ResourceLocation(FluidGunMod.MODID, gunName));
        if(i != null && i instanceof ItemBaseFluidGun) {
            ItemBaseFluidGun gun = (ItemBaseFluidGun) i;
            ClientPlayerEntity player = Minecraft.getInstance().player;
            ItemStack stack = player.getHeldItem(hand);
            if(stack != null && !stack.isEmpty() && stack.getItem() == gun) {
                ClientEvents.gunFiredClientSide(stack, count, maxC);
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static void gunFiredClientSide(ItemStack stack, int count, int max) {
        if(stack.getItem() instanceof ItemBaseFluidGun) {
            if(Minecraft.getInstance().player != null) {
                ClientPlayerEntity player = Minecraft.getInstance().player;
                player.sendStatusMessage(new StringTextComponent(TextFormatting.DARK_GRAY + I18n.format("item.fluidgun.contents", count, max)), true);
            }
        }
    }

}
