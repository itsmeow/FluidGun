package its_meow.fluidgun.content;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.block.BlockStaticLiquid;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.fluids.IFluidBlock;
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
            boolean e = true;
            int c = 0;

            do {
                RayTraceResult ray = (c == 0 ? ItemFluidGun.rayTrace(player, this.getRange(), 1F, true) : ItemFluidGun.rayTrace(player, this.getRange(), 1F, false));
                if(ray != null && ray.entityHit == null) {
                    if(ray.typeOfHit == RayTraceResult.Type.BLOCK) {
                        BlockPos pos = ray.getBlockPos();
                        IBlockState state = world.getBlockState(pos);
                        EnumFacing side = ray.sideHit;
                        if(world.isBlockLoaded(pos) && pos.getY() >= 0 && pos.getY() < world.provider.getHeight() && pos.offset(side).getY() >= 0 && pos.offset(side).getY() < world.provider.getHeight()) {
                            IFluidHandler handler = this.getFluidHandler(stack);
                            if(c == 0 && state.getBlock() instanceof IFluidBlock || state.getBlock() instanceof BlockStaticLiquid) {
                                if(this.shouldIntake(stack)) {
                                    int breakE = ForgeHooks.onBlockBreakEvent(world, ((EntityPlayerMP) player).interactionManager.getGameType(), (EntityPlayerMP) player, pos); // fire break on pos to ensure permission
                                    if(breakE != -1) {
                                        this.takeAndFill(handler, state, side, world, player, pos, hand, stack);
                                        e = false;
                                    }
                                }
                            } else if(c != 0 && this.shouldPlace(stack) && !(state.getBlock() instanceof IFluidBlock || state.getBlock() instanceof BlockStaticLiquid) && (state.getBlock() == Blocks.SNOW_LAYER || state.isSideSolid(world, pos, side) || state.getBlock().isReplaceable(world, pos.offset(side)))) {
                                if(state.getBlock() == Blocks.SNOW_LAYER || state.getBlock().isReplaceable(world, pos)) pos = pos.offset(side.getOpposite());
                                this.placeAndDrain(handler, state, side, world, player, pos, hand, stack);
                            }
                            world.scheduleBlockUpdate(pos.offset(side), world.getBlockState(pos).getBlock(), 1, 100);
                            world.notifyBlockUpdate(pos, Blocks.AIR.getDefaultState(), world.getBlockState(pos), 2);
                        }
                    }
                }
                if(c != 0) {
                    e = false;
                }
                c++;
            } while(e);
        }
        //FluidHandlerItemStackBuckets handler = (FluidHandlerItemStackBuckets) this.getFluidCapability(stack);
        //if(handler != null) {
          //  BaseMod.NETWORK_INSTANCE.sendTo(new GunFiredPacket(this.getRegistryName().getPath().toString(), handler.getFluid() == null || handler.getFluid().getFluid() == null ? "" : handler.getFluid().getFluid().getName(), hand, this.getContentsBuckets(handler), this.getMaxCapacityBuckets(false, stack)), (EntityPlayerMP) player);
        //}
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
        tag.setInteger("handlerY", pos.getX());
        tag.setInteger("handlerZ", pos.getX());
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
