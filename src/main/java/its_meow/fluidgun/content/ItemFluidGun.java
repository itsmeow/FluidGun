package its_meow.fluidgun.content;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import its_meow.fluidgun.FluidGunConfigMain;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceFluidMode;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidBlock;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;

public class ItemFluidGun extends ItemBaseFluidGun {

    protected int capacity;

    public ItemFluidGun(String name, int capacity, float range) {
        super(name, range);
        this.capacity = capacity * 1000;
        FluidGunConfigMain.GunConfig.COUNT.put(name, capacity);
    }

    @Override
    public ICapabilityProvider initCapabilities(@Nonnull ItemStack stack, @Nullable NBTTagCompound nbt) {
        return new FluidHandlerItemStackBuckets(stack, capacity);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        if(world.isRemote) {
            return super.onItemRightClick(world, player, hand);
        }

        boolean e = true;
        int c = 0;

        do {
            RayTraceResult ray = (c == 0 ? ItemFluidGun.rayTrace(player, this.getRange(), 1F, RayTraceFluidMode.SOURCE_ONLY) : ItemFluidGun.rayTrace(player, this.getRange(), 1F, RayTraceFluidMode.NEVER));
            ItemStack stack = player.getHeldItem(hand);
            if(ray != null && ray.entity == null) {
                if(ray.type == RayTraceResult.Type.BLOCK) {
                    BlockPos pos = ray.getBlockPos();
                    IBlockState state = world.getBlockState(pos);
                    EnumFacing side = ray.sideHit;
                    ItemUseContext ctx = new ItemUseContext(player, stack, pos, side, (float) ray.hitVec.x, (float) ray.hitVec.y, (float) ray.hitVec.z);
                    BlockItemUseContext bctx = new BlockItemUseContext(ctx);
                    if(world.isBlockLoaded(pos) && pos.getY() >= 0 && pos.getY() < world.getHeight() && pos.offset(side).getY() >= 0 && pos.offset(side).getY() < world.getHeight()) {
                        FluidHandlerItemStackBuckets handler = (FluidHandlerItemStackBuckets) this.getFluidHandler(stack);
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
        return super.onItemRightClick(world, player, hand);
    }

    @Override
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

            tooltip.add(new TextComponentTranslation("item.fluidgun.max_capacity", this.getMaxCapacityBuckets(true, stack, this.getFluidHandler(stack))));
            tooltip.add(new TextComponentTranslation("item.fluidgun.range", this.getRange()));

            tooltip.add(new TextComponentTranslation("item.fluidgun.info"));
            tooltip.add(new TextComponentTranslation("item.fluidgun.wheel.info"));
        } else {
            tooltip.add(new TextComponentTranslation("item.fluidgun.more.info"));
        }
    }

    public IFluidHandlerItem getFluidHandler(ItemStack stack) {
        return stack.getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null).orElseGet(null);
    }

}