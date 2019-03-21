package its_meow.fluidgun.client;

import java.util.HashMap;

import its_meow.fluidgun.BaseMod;
import its_meow.fluidgun.content.ItemFluidGun;
import its_meow.fluidgun.network.MousePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelBakery;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(value = Side.CLIENT)
public class ClientEvents {

    public static HashMap<ItemFluidGun, ModelResourceLocation> mrls_2D = new HashMap<ItemFluidGun, ModelResourceLocation>();
    public static HashMap<ItemFluidGun, ModelResourceLocation> mrls_3D = new HashMap<ItemFluidGun, ModelResourceLocation>();

    public static HashMap<ItemFluidGun, IBakedModel> models_2D = new HashMap<ItemFluidGun, IBakedModel>();
    public static HashMap<ItemFluidGun, IBakedModel> models_3D = new HashMap<ItemFluidGun, IBakedModel>();

    @SubscribeEvent
    public static void modelRegister(ModelRegistryEvent event) {
        for(ItemFluidGun gun : BaseMod.guns) {
            ModelResourceLocation d2 = new ModelResourceLocation(gun.getRegistryName(), "inventory");
            mrls_2D.put(gun, d2);
            ModelResourceLocation d3 = new ModelResourceLocation(gun.getRegistryName() + "_3D", "inventory");
            mrls_3D.put(gun, d3);
            gun.setTileEntityItemStackRenderer(new GunTEISR());
            ModelLoader.setCustomModelResourceLocation(gun, 0, d2);
            ModelBakery.registerItemVariants(gun, d3);
        }
    }

    @SubscribeEvent
    public static void onModelBake(ModelBakeEvent event) {
        for(ItemFluidGun gun : BaseMod.guns) {
            models_2D.put(gun, event.getModelRegistry().getObject(mrls_2D.get(gun)));
            event.getModelRegistry().putObject(mrls_2D.get(gun), new BaseModel(models_2D.get(gun), gun));
            models_3D.put(gun, event.getModelRegistry().getObject(mrls_3D.get(gun)));
        }
    }

    @SubscribeEvent
    public static void mouseEvent(MouseEvent e) {
        EntityPlayer player = Minecraft.getMinecraft().player;

        for(EnumHand hand : EnumHand.values()) {
            ItemStack stack = player.getHeldItem(hand);
            if(stack.getItem() instanceof ItemFluidGun) {
                if(player.isSneaking() && e.getDwheel() != 0) {
                    BaseMod.NETWORK_INSTANCE
                            .sendToServer(new MousePacket(player.inventory.currentItem, e.getDwheel() > 0));
                    e.setCanceled(true);
                }
                break; // If player is holding two only scroll for one
            }
        }
    }

}
