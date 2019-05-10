package its_meow.fluidgun.content;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import its_meow.fluidgun.FluidGunConfigMain;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fluids.FluidStack;
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