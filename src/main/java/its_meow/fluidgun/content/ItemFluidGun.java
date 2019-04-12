package its_meow.fluidgun.content;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import its_meow.fluidgun.BaseMod;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fluids.FluidStack;
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
    
    public void setCapacity(int capacity) {
    	this.capacity = capacity;
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

        this.onFired(player, world, player.getHeldItem(hand), hand);
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