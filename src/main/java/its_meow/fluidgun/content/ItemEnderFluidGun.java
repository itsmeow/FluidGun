package its_meow.fluidgun.content;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;

public class ItemEnderFluidGun extends ItemBaseFluidGun {

    public ItemEnderFluidGun(String name, float range) {
        super(name, range);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if(world.isRemote) {
            return new ActionResult<ItemStack>(EnumActionResult.PASS, player.getHeldItem(hand));
        }
        if(player.isSneaking()) {
            RayTraceResult ray = this.rayTrace(world, player, false);
            if(ray != null && ray.typeOfHit == RayTraceResult.Type.BLOCK) {
                BlockPos pos = ray.getBlockPos();
                if(this.isValidHandler(player, pos, true)) {
                    this.writeHandlerPosition(stack, pos);
                    this.writeHandlerDimension(stack, world.provider.getDimension());
                }
            }
        } else {
        	this.onFired(player, world, stack, hand);
        }
        return new ActionResult<ItemStack>(EnumActionResult.PASS, player.getHeldItem(hand));
    }

    private void writeHandlerDimension(ItemStack stack, int dimension) {
        NBTTagCompound tag = this.getCheckedTag(stack);
        tag.setInteger("handlerDim", dimension);
    }

    @Nonnull
    public NBTTagCompound getCheckedTag(@Nonnull ItemStack stack) {
        if(!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        return stack.getTagCompound();
    }

    public void writeHandlerPosition(@Nonnull ItemStack stack, @Nonnull BlockPos pos) {
        NBTTagCompound tag = this.getCheckedTag(stack);
        tag.setInteger("handlerX", pos.getX());
        tag.setInteger("handlerY", pos.getY());
        tag.setInteger("handlerZ", pos.getZ());
    }

    @Nullable
    public BlockPos getHandlerPosition(@Nonnull ItemStack stack) {
        NBTTagCompound tag = this.getCheckedTag(stack);
        if(!tag.hasKey("handlerX") || !tag.hasKey("handlerY") || !tag.hasKey("handlerZ")) {
            return null;
        }
        int x = tag.getInteger("handlerX");
        int y = tag.getInteger("handlerY");
        int z = tag.getInteger("handlerZ");
        return new BlockPos(x, y, z);
    }

    public boolean hasHandlerTag(@Nonnull ItemStack stack) {
        return getHandlerPosition(stack) != null;
    }

    public boolean isValidHandler(EntityPlayer player, BlockPos pos, boolean statusMessage) {
        return getFluidHandler(player, pos, statusMessage) != null;
    }

    public IFluidHandler getFluidHandler(EntityPlayer player, BlockPos pos, boolean statusMessage) {
        TileEntity te = player.world.getTileEntity(pos);
        if(te != null) {
            IFluidHandler cap = te.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, null);
            if(cap != null) {
                if(statusMessage)
                    player.sendStatusMessage(new TextComponentTranslation("fluidgun.valid_handler"), false);
                return cap;
            } else if(statusMessage) {
                player.sendStatusMessage(new TextComponentTranslation("fluidgun.invalid_handler"), false);
            }
        } else if(statusMessage) {
            player.sendStatusMessage(new TextComponentTranslation("fluidgun.invalid_handler"), false);
        }
        return null;
    }

    @Override
    public IFluidHandler getFluidHandler(ItemStack stack) {
        if(this.getHandlerPosition(stack) != null) {
            if(this.hasHandlerDimension(stack)) {
                World world = DimensionManager.getWorld(this.getHandlerDimension(stack));
                if(world != null) {
                    TileEntity te = world.getTileEntity(this.getHandlerPosition(stack));
                    if(te != null) {
                        IFluidHandler cap = te.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, null);
                        if(cap != null) {
                            return cap;
                        }
                    }
                }
            }
        }
        return null;
    }

    public int getHandlerDimension(ItemStack stack) {
        NBTTagCompound tag = this.getCheckedTag(stack);
        return tag.getInteger("handlerDim");
    }

    public boolean hasHandlerDimension(ItemStack stack) {
        NBTTagCompound tag = this.getCheckedTag(stack);
        return tag.hasKey("handlerDim");
    }

}
