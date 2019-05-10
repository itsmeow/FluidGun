package its_meow.fluidgun.content;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

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
            if(ray != null && ray.type == RayTraceResult.Type.BLOCK) {
                BlockPos pos = ray.getBlockPos();
                if(this.isValidHandler(player, player.world, pos, true)) {
                    this.writeHandlerPosition(stack, pos);
                    this.writeHandlerDimension(stack, world.getDimension().getType().getId());
                }
            }
        } else {
            if(this.hasHandlerPositionTag(stack) && this.hasHandlerDimensionTag(stack)) {
                if(this.getFluidHandler(stack) != null) {
                    this.onFired(player, world, stack, hand);
                } else {
                    player.sendStatusMessage(new TextComponentTranslation("item.enderfluidgun.handler_invalidated"), false);
                }
            } else {
                player.sendStatusMessage(new TextComponentTranslation("item.enderfluidgun.set_handler"), false);
            }
        }
        return new ActionResult<ItemStack>(EnumActionResult.PASS, player.getHeldItem(hand));
        /*ItemStack stack = player.getHeldItem(hand);
        if(world.isRemote) {
            return new ActionResult<ItemStack>(EnumActionResult.PASS, player.getHeldItem(hand));
        }
        if(player.isSneaking()) {
            RayTraceResult ray = this.rayTrace(world, player, false);
            if(ray != null && ray.type == RayTraceResult.Type.BLOCK) {
                BlockPos pos = ray.getBlockPos();
                if(this.isValidHandler(player, pos, true)) {
                    this.writeHandlerPosition(stack, pos);
                    this.writeHandlerDimension(stack, world.getDimension().getType().getId());
                }
            }
        } else {
            boolean e = true;
            int c = 0;

            do {
                RayTraceResult ray = (c == 0 ? ItemFluidGun.rayTrace(player, this.getRange(), 1F, RayTraceFluidMode.SOURCE_ONLY) : ItemFluidGun.rayTrace(player, this.getRange(), 1F, RayTraceFluidMode.NEVER));
                if(ray != null && ray.entity == null) {
                    if(ray.type == RayTraceResult.Type.BLOCK) {
                        BlockPos pos = ray.getBlockPos();
                        IBlockState state = world.getBlockState(pos);
                        EnumFacing side = ray.sideHit;
                        ItemUseContext ctx = new ItemUseContext(player, stack, pos, side, (float) ray.hitVec.x, (float) ray.hitVec.y, (float) ray.hitVec.z);
                        BlockItemUseContext bctx = new BlockItemUseContext(ctx);
                        if(world.isBlockLoaded(pos) && pos.getY() >= 0 && pos.getY() < world.getHeight() && pos.offset(side).getY() >= 0 && pos.offset(side).getY() < world.getHeight()) {
                            IFluidHandler handler = this.getFluidHandler(stack);
                            if(c == 0 && state.getBlock() instanceof IFluidBlock) {
                                if(this.shouldIntake(stack)) {
                                    int breakE = ForgeHooks.onBlockBreakEvent(world, ((EntityPlayerMP) player).interactionManager.getGameType(), (EntityPlayerMP) player, pos); // fire break on pos to ensure permission
                                    if(breakE != -1) {
                                        this.takeAndFill(handler, state, side, world, player, pos, hand, stack, ctx, bctx);
                                        e = false;
                                    }
                                }
                            } else if(c != 0 && this.shouldPlace(stack) && !(state.getBlock() instanceof IFluidBlock) && (state.getBlock() == Blocks.SNOW || state.isReplaceable(bctx))) {
                                if(state.getBlock() == Blocks.SNOW || state.isReplaceable(bctx)) pos = pos.offset(side.getOpposite());
                                this.placeAndDrain(handler, state, side, world, player, pos, hand, stack, ctx, bctx);
                            }
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
        return new ActionResult<ItemStack>(EnumActionResult.PASS, player.getHeldItem(hand));*/
    }

    private void writeHandlerDimension(ItemStack stack, int dimension) {
        NBTTagCompound tag = this.getCheckedTag(stack);
        tag.putInt("handlerDim", dimension);
    }

    @Nonnull
    public NBTTagCompound getCheckedTag(@Nonnull ItemStack stack) {
        if(!stack.hasTag()) {
            stack.setTag(new NBTTagCompound());
        }
        return stack.getTag();
    }

    public void writeHandlerPosition(@Nonnull ItemStack stack, @Nonnull BlockPos pos) {
        NBTTagCompound tag = this.getCheckedTag(stack);
        tag.putInt("handlerX", pos.getX());
        tag.putInt("handlerY", pos.getX());
        tag.putInt("handlerZ", pos.getX());
    }

    @Nullable
    public BlockPos getHandlerPosition(@Nonnull ItemStack stack) {
        NBTTagCompound tag = this.getCheckedTag(stack);
        if(!tag.hasUniqueId("handlerX") || !tag.hasUniqueId("handlerY") || !tag.hasUniqueId("handlerZ")) {
            return null;
        }
        int x = tag.getInt("handlerX");
        int y = tag.getInt("handlerY");
        int z = tag.getInt("handlerZ");
        return new BlockPos(x, y, z);
    }

    public boolean hasHandlerPositionTag(@Nonnull ItemStack stack) {
        return getHandlerPosition(stack) != null;
    }

    public boolean isValidHandler(EntityPlayer player, World world, BlockPos pos, boolean statusMessage) {
        return getFluidHandler(player, world, pos, statusMessage) != null;
    }

    public IFluidHandler getFluidHandler(EntityPlayer player, BlockPos pos, boolean statusMessage) {
        TileEntity te = player.world.getTileEntity(pos);
        if(te != null) {
            IFluidHandler cap = te.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, null).orElse(null);
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
                World world = DimensionManager.getWorld(ServerLifecycleHooks.getCurrentServer(), DimensionType.getById(this.getHandlerDimension(stack)), false, false);
                if(world != null) {
                    TileEntity te = world.getTileEntity(this.getHandlerPosition(stack));
                    if(te != null) {
                        IFluidHandler cap = te.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, null).orElse(null);
                        if(cap != null) {
                            return cap;
                        }
                    }
                }
            }
        }
        return null;
    }

    public boolean hasHandlerDimension(ItemStack stack) {
        NBTTagCompound tag = this.getCheckedTag(stack);
        return tag.hasUniqueId("handlerDim");
    }

    public IFluidHandler getFluidHandler(EntityPlayer player, World world, BlockPos pos, boolean statusMessage) {
        TileEntity te = player.world.getTileEntity(pos);
        if(te != null) {
            IFluidHandler cap = te.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, null).orElse(null);
            if(cap != null) {
                if(statusMessage)
                    player.sendStatusMessage(new TextComponentTranslation("item.enderfluidgun.valid_handler"), false);
                return cap;
            } else if(statusMessage) {
                player.sendStatusMessage(new TextComponentTranslation("item.enderfluidgun.invalid_handler"), false);
            }
        } else if(statusMessage) {
            player.sendStatusMessage(new TextComponentTranslation("item.enderfluidgun.invalid_handler"), false);
        }
        return null;
    }

    public int getHandlerDimension(ItemStack stack) {
        NBTTagCompound tag = this.getCheckedTag(stack);
        return tag.getInt("handlerDim");
    }

    public boolean hasHandlerDimensionTag(ItemStack stack) {
        NBTTagCompound tag = this.getCheckedTag(stack);
        return tag.hasUniqueId("handlerDim");
    }
    
    @OnlyIn(Dist.CLIENT)
    public void addInformation(ItemStack stack, World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {
        super.addInformation(stack, worldIn, tooltip, flagIn);
        if(this.getContentsBuckets(stack) > 0) {
            IFluidHandler handler = this.getFluidHandler(stack);
            for(FluidStack fs : this.getFluidStacks(stack, handler)) {
                String fluid = this.localizeFluid(stack, handler, fs);
                boolean isLava = this.getFluidUnlocalizedName(stack, handler, fs).equals("fluid.tile.lava");
                tooltip.add(new TextComponentString((isLava ? TextFormatting.GOLD : TextFormatting.BLUE) + fluid));
            }
        }
        tooltip.add(new TextComponentTranslation("item.fluidgun.contents", this.getContentsBuckets(stack) + "/" + this.getMaxCapacityBuckets(true, stack, this.getFluidHandler(stack))));
        tooltip.add(new TextComponentTranslation("item.fluidgun.mode." + this.getMode(stack).name().toLowerCase() + ".info"));
        if(GuiScreen.isShiftKeyDown()) {
            tooltip.add(new TextComponentTranslation("item.fluidgun.range", this.getRange()));

            tooltip.add(new TextComponentTranslation("item.fluidgun.info"));
            tooltip.add(new TextComponentTranslation("item.fluidgun.wheel.info"));
            tooltip.add(new TextComponentTranslation("item.enderfluidgun.info"));
            if(this.getFluidHandler(stack) != null && this.hasHandlerPositionTag(stack) && this.hasHandlerDimensionTag(stack)) {
                tooltip.add(new TextComponentTranslation("item.enderfluidgun.linked"));
                BlockPos pos = this.getHandlerPosition(stack);
                tooltip.add(new TextComponentString("X: " + pos.getX()));
                tooltip.add(new TextComponentString("Y: " + pos.getY()));
                tooltip.add(new TextComponentString("Z: " + pos.getZ()));
                tooltip.add(new TextComponentString("DIM: " + this.getHandlerDimension(stack)));
            }
        } else {
            tooltip.add(new TextComponentTranslation("item.fluidgun.more.info"));
        }
    }
    
}
