package its_meow.fluidgun.client;

import java.util.List;

import javax.vecmath.Matrix4f;

import org.apache.commons.lang3.tuple.Pair;

import its_meow.fluidgun.content.ItemFluidGun;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.EnumFacing;

public class BaseModel implements IBakedModel {

    private final IBakedModel oldModel;
    private final ItemFluidGun gun;

    public BaseModel(IBakedModel internal, ItemFluidGun gun) {
        this.oldModel = internal;
        this.gun = gun;
    }

    @Override
    public List<BakedQuad> getQuads(IBlockState state, EnumFacing side, long rand) {
        return oldModel.getQuads(state, side, rand);
    }

    @Override
    public boolean isAmbientOcclusion() {
        return oldModel.isAmbientOcclusion();
    }

    @Override
    public boolean isGui3d() {
        return oldModel.isGui3d();
    }

    public IBakedModel getInternal() {
        return oldModel;
    }

    @Override
    public boolean isBuiltInRenderer() {
        return true;
    }

    @Override
    public TextureAtlasSprite getParticleTexture() {
        return oldModel.getParticleTexture();
    }

    @Override
    public ItemOverrideList getOverrides() {
        return ItemOverrideList.NONE;
    }

    @Override
    public Pair<? extends IBakedModel, Matrix4f> handlePerspective(TransformType type) {
        ((GunTEISR) gun.getTileEntityItemStackRenderer()).transform = type;
        return Pair.of(this, oldModel.handlePerspective(type).getRight());
    }

}