package its_meow.fluidgun.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.tileentity.TileEntityItemStackRenderer;
import net.minecraft.item.ItemStack;

public class GunTEISR extends TileEntityItemStackRenderer {

    // This is set properly in the model class
    public TransformType transform = TransformType.GUI;

    @Override
    public void renderByItem(ItemStack itemStack) {
        super.renderByItem(itemStack);
        IBakedModel model = ClientEvents.models_2D.get(itemStack.getItem());
        if(this.transform != TransformType.GUI) {
            model = ClientEvents.models_3D.get(itemStack.getItem());
        }
        GlStateManager.pushMatrix();
        GlStateManager.translate(0.5F, 0.5F, 0.5F);
        Minecraft.getMinecraft().getRenderItem().renderItem(itemStack, model);
        GlStateManager.popMatrix();
    }

}
