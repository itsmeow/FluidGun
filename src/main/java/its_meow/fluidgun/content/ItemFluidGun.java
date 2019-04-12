package its_meow.fluidgun.content;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import its_meow.fluidgun.BaseMod;
import net.minecraft.block.BlockStaticLiquid;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidBlock;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ItemFluidGun extends ItemBaseFluidGun {

    protected int capacity;

    public ItemFluidGun(String name, int capacity, float range) {
        super(name, range);
        this.capacity = capacity * 1000;
        BaseMod.FluidGunConfig.COUNT.put(name, capacity);
    }

    @Override
    public ICapabilityProvider initCapabilities(@Nonnull ItemStack stack, @Nullable NBTTagCompound nbt) {
        return new FluidHandlerItemStackBuckets(stack, capacity);
    }

    @Override
    public EnumActionResult onItemUseFirst(EntityPlayer player, World world, BlockPos pos, EnumFacing side, float hitX,
            float hitY, float hitZ, EnumHand hand) {

        return EnumActionResult.FAIL;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        if(world.isRemote) {
            return super.onItemRightClick(world, player, hand);
        }

        boolean e = true;
        int c = 0;

        do {
            RayTraceResult ray = (c == 0 ? ItemFluidGun.rayTrace(player, this.getRange(), 1F, true) : ItemFluidGun.rayTrace(player, this.getRange(), 1F, false));
            ItemStack stack = player.getHeldItem(hand);
            if(ray != null && ray.entityHit == null) {
                if(ray.typeOfHit == RayTraceResult.Type.BLOCK) {
                    BlockPos pos = ray.getBlockPos();
                    IBlockState state = world.getBlockState(pos);
                    EnumFacing side = ray.sideHit;
                    if(world.isBlockLoaded(pos) && pos.getY() >= 0 && pos.getY() < world.provider.getHeight() && pos.offset(side).getY() >= 0 && pos.offset(side).getY() < world.provider.getHeight()) {
                        FluidHandlerItemStackBuckets handler = (FluidHandlerItemStackBuckets) this.getFluidHandler(stack);
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
        return super.onItemRightClick(world, player, hand);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        super.addInformation(stack, worldIn, tooltip, flagIn);
        if(this.getContentsBuckets(stack) > 0) {
            IFluidHandler handler = this.getFluidHandler(stack);
            for(FluidStack fs : this.getFluidStacks(stack, handler)) {
                String fluid = this.localizeFluid(stack, handler, fs);
                tooltip.add((this.getFluidUnlocalizedName(stack, handler, fs).equals("fluid.tile.lava") ? TextFormatting.GOLD : TextFormatting.BLUE) + fluid);
            }
        }
        tooltip.add(TextFormatting.DARK_GRAY + I18n.format("item.fluidgun.contents") + ": " + TextFormatting.GRAY + this.getContentsBuckets(stack) + "/" + this.getMaxCapacityBuckets(true, stack, this.getFluidHandler(stack)));
        tooltip.add(I18n.format("item.fluidgun.mode." + this.getMode(stack).name().toLowerCase() + ".info"));
        if(GuiScreen.isShiftKeyDown()) {

            tooltip.add(TextFormatting.GOLD + I18n.format("item.fluidgun.max_capacity") + ": " + TextFormatting.YELLOW
                    + this.getMaxCapacityBuckets(true, stack, this.getFluidHandler(stack)));
            tooltip.add(TextFormatting.GOLD + I18n.format("item.fluidgun.range") + ": " + TextFormatting.YELLOW
                    + this.getRange());

            tooltip.add(I18n.format("item.fluidgun.info"));
            tooltip.add(I18n.format("item.fluidgun.wheel.info"));
        } else {
            tooltip.add(I18n.format("item.fluidgun.more.info"));
        }
    }

    public IFluidHandlerItem getFluidHandler(ItemStack stack) {
        return stack.getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null);
    }

}