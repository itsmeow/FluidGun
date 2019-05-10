package its_meow.fluidgun.client;

import its_meow.fluidgun.BaseMod;
import its_meow.fluidgun.FluidGunConfigMain;
import its_meow.fluidgun.Ref;
import its_meow.fluidgun.content.ItemBaseFluidGun;
import its_meow.fluidgun.network.GunFiredPacket;
import its_meow.fluidgun.network.MousePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.GuiScreenEvent.MouseScrollEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class ClientEvents {

    @SubscribeEvent
    public static void mouseEvent(MouseScrollEvent.Pre e) {
        EntityPlayer player = Minecraft.getInstance().player;

        for(EnumHand hand : EnumHand.values()) {
            ItemStack stack = player.getHeldItem(hand);
            if(stack.getItem() instanceof ItemBaseFluidGun) {
                if(GuiScreen.isAltKeyDown() && e.getScrollDelta() != 0) {
                    BaseMod.HANDLER.sendToServer(new MousePacket(player.inventory.currentItem, e.getScrollDelta() > 0)); 
                    e.setCanceled(true);
                }
                break; // If player is holding two only scroll for one
            }
        }
    }

    public static void onGunFired(GunFiredPacket message) {
        String gunName = message.gunName;
        EnumHand hand = message.hand;
        int max = message.max;
        int count = message.count;

        int maxC = FluidGunConfigMain.GunConfig.COUNT.get(gunName);
        if(maxC == 0) return;
        FluidGunConfigMain.GunConfig.COUNT.put(gunName, max);
        Item i = ForgeRegistries.ITEMS.getValue(new ResourceLocation(Ref.MODID, gunName));
        if(i != null && i instanceof ItemBaseFluidGun) {
            ItemBaseFluidGun gun = (ItemBaseFluidGun) i;
            EntityPlayerSP player = Minecraft.getInstance().player;
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
                EntityPlayerSP player = Minecraft.getInstance().player;
                player.sendStatusMessage(new TextComponentString(TextFormatting.DARK_GRAY + I18n.format("item.fluidgun.contents") + ": " + TextFormatting.GRAY + count + "/" + max), true);
            }
        }
    }

}
