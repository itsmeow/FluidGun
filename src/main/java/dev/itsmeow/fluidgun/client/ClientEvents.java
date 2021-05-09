package dev.itsmeow.fluidgun.client;

import dev.itsmeow.fluidgun.FluidGunConfigMain;
import dev.itsmeow.fluidgun.FluidGunMod;
import dev.itsmeow.fluidgun.content.ItemBaseFluidGun;
import dev.itsmeow.fluidgun.content.ItemEnderFluidGun;
import dev.itsmeow.fluidgun.content.ItemFluidGun;
import dev.itsmeow.fluidgun.network.EnderHandlerInvalidatedPacket;
import dev.itsmeow.fluidgun.network.EnderUpdateClientPacket;
import dev.itsmeow.fluidgun.network.GunFiredPacket;
import dev.itsmeow.fluidgun.network.MousePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.Hand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TranslationTextComponent;
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
                    FluidGunMod.HANDLER.sendToServer(new MousePacket(hand, e.getScrollDelta() > 0));
                    e.setCanceled(true);
                }
                break; // If player is holding two only scroll for main hand
            }
        }
    }

    public static void onEnderFired(EnderUpdateClientPacket message) {
        int slot = message.slot;
        int max = message.max;
        int count = message.count;
        EnderUpdateClientPacket.TankNetwork[] tanks = message.tanks;
        ClientPlayerEntity player = Minecraft.getInstance().player;
        ItemStack stack = player.inventory.getStackInSlot(slot);
        if (stack != null && !stack.isEmpty() && stack.getItem() instanceof ItemEnderFluidGun) {
            if (message.notify) {
                showStatus(player, count, max);
            }
            CompoundNBT tag = ((ItemEnderFluidGun) stack.getItem()).getCheckedTag(stack);
            CompoundNBT info = new CompoundNBT();
            info.putInt("contents", count);
            info.putInt("max", max);
            ListNBT list = new ListNBT();
            for(EnderUpdateClientPacket.TankNetwork tank : tanks) {
                CompoundNBT tankC = new CompoundNBT();
                tankC.putInt("temp", tank.temp);
                tankC.putString("key", tank.fluidTranslationKey);
                tankC.putInt("contained", tank.contained / 1000);
                tankC.putInt("max", tank.max / 1000);
                list.add(tankC);
            }
            info.put("tanks", list);
            tag.put("client_handler_info", info);
            if(tag.contains("link_error")) {
                tag.remove("link_error");
            }
        }
    }

    public static void onGunFired(GunFiredPacket message) {
        String gunName = message.gunName;
        Hand hand = message.hand;
        int max = message.max;
        int count = message.count;
        Item i = ForgeRegistries.ITEMS.getValue(new ResourceLocation(FluidGunMod.MODID, gunName));
        if(i != null && i instanceof ItemBaseFluidGun) {
            ItemBaseFluidGun gun = (ItemBaseFluidGun) i;
            if(gun instanceof ItemFluidGun) {
                FluidGunConfigMain.GunConfig.COUNT.put(gunName, max);
            }
            ClientPlayerEntity player = Minecraft.getInstance().player;
            ItemStack stack = player.getHeldItem(hand);
            if(stack != null && !stack.isEmpty() && stack.getItem() == gun) {
                showStatus(player, count, max);
            }
        }
    }

    public static void showStatus(ClientPlayerEntity player, int count, int max) {
        player.sendStatusMessage(ItemBaseFluidGun.contentsText(count, max), true);
    }

    public static void onHandlerInvalidated(EnderHandlerInvalidatedPacket msg) {
        ClientPlayerEntity player = Minecraft.getInstance().player;
        ItemStack stack = player.inventory.getStackInSlot(msg.slot);
        if(stack != null && !stack.isEmpty() && stack.getItem() instanceof ItemEnderFluidGun) {
            CompoundNBT tag = ((ItemEnderFluidGun) stack.getItem()).getCheckedTag(stack);
            if (!tag.contains("link_error"))
                player.sendStatusMessage(new TranslationTextComponent("item.enderfluidgun.handler_invalidated"), false);
            tag.putBoolean("link_error", true);
        }
    }
}
