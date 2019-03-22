package its_meow.fluidgun.client;

import its_meow.fluidgun.BaseMod;
import its_meow.fluidgun.content.ItemFluidGun;
import its_meow.fluidgun.network.MousePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(value = Side.CLIENT)
public class ClientEvents {

    @SubscribeEvent
    public static void modelRegister(ModelRegistryEvent event) {
        for(ItemFluidGun gun : BaseMod.guns) {
            ModelLoader.setCustomModelResourceLocation(gun, 0, new ModelResourceLocation(gun.getRegistryName(), "inventory"));
        }
    }

    @SubscribeEvent
    public static void mouseEvent(MouseEvent e) {
        EntityPlayer player = Minecraft.getMinecraft().player;

        for(EnumHand hand : EnumHand.values()) {
            ItemStack stack = player.getHeldItem(hand);
            if(stack.getItem() instanceof ItemFluidGun) {
                if(player.isSneaking() && e.getDwheel() != 0) {
                    BaseMod.NETWORK_INSTANCE.sendToServer(new MousePacket(player.inventory.currentItem, e.getDwheel() > 0)); 
                    e.setCanceled(true);
                }
                break; // If player is holding two only scroll for one
            }
        }
    }

}
