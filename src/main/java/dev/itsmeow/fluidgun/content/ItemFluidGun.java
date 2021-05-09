package dev.itsmeow.fluidgun.content;

import dev.itsmeow.fluidgun.FluidGunConfigMain;
import dev.itsmeow.fluidgun.FluidGunMod;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class ItemFluidGun extends ItemBaseFluidGun {

    protected int capacity;

    public ItemFluidGun(String name, int capacity, float range) {
        super(name, range, new Item.Properties().group(FluidGunMod.GROUP));
        this.capacity = capacity * 1000;
        FluidGunConfigMain.GunConfig.COUNT.put(name, capacity);
    }
    
    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }
    
    @Override
    public ICapabilityProvider initCapabilities(@Nonnull ItemStack stack, @Nullable CompoundNBT nbt) {
        return new FluidHandlerItemStackBuckets(stack, capacity);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, PlayerEntity player, Hand hand) {
        if(!world.isRemote()) {
            this.onFired((ServerPlayerEntity) player, world, player.getHeldItem(hand), hand);
        }
        return ActionResult.resultPass(player.getHeldItem(hand));
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void addInformationMain(ItemStack stack, World worldIn, List<ITextComponent> tooltip) {
        tooltip.add(new TranslationTextComponent("item.fluidgun.max_capacity", wrapString(this.getMaxCapacityBuckets(this.getFluidHandler(stack)), TextFormatting.YELLOW)));
    }

    public IFluidHandlerItem getFluidHandler(ItemStack stack) {
        return stack.getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null).orElseGet(null);
    }

}