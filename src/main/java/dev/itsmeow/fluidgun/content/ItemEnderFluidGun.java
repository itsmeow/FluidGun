package dev.itsmeow.fluidgun.content;

import dev.itsmeow.fluidgun.FluidGunMod;
import dev.itsmeow.fluidgun.network.EnderUpdateClientPacket;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.text.*;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

public class ItemEnderFluidGun extends ItemBaseFluidGun {

    public ItemEnderFluidGun(String name, float range) {
        super(name, range, new Item.Properties().group(FluidGunMod.GROUP));
    }

    @Override
    public ActionResultType onItemUse(ItemUseContext context) {
        if(!context.getWorld().isRemote() && context.getPlayer().isSneaking()) {
            boolean sidedValid = this.isValidHandler(context.getWorld(), context.getPos(), context.getFace());
            boolean anyValid = sidedValid || this.isValidHandler(context.getWorld(), context.getPos(), null);
            sendHandlerStatus(context.getPlayer(), anyValid);
            if (anyValid) {
                this.writeHandlerPosition(context.getItem(), context.getPos());
                this.writeHandlerDimension(context.getItem(), context.getWorld().getDimensionKey());
                this.writeHandlerSide(context.getItem(), sidedValid ? context.getFace() : null);
                FluidGunMod.updateStack((ServerPlayerEntity) context.getPlayer(), ItemEnderFluidGun.handToSlot(context.getPlayer(), context.getHand()), context.getItem());
            }
            return ActionResultType.FAIL;
        }
        return ActionResultType.PASS;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (!world.isRemote() && !player.isSneaking()) {
            if (this.hasHandlerPositionTag(stack) && this.hasHandlerDimensionTag(stack)) {
                if (this.getFluidHandler(stack) != null) {
                    this.onFired((ServerPlayerEntity) player, world, stack, hand);
                    if(this.getCheckedTag(stack).contains("link_error")) {
                        this.getCheckedTag(stack).remove("link_error");
                    }
                } else {
                    player.sendStatusMessage(new TranslationTextComponent("item.enderfluidgun.handler_invalidated"), false);
                    this.getCheckedTag(stack).putBoolean("link_error", true);
                }
            } else {
                player.sendStatusMessage(new TranslationTextComponent("item.enderfluidgun.set_handler"), false);
            }
        }
        return ActionResult.resultPass(player.getHeldItem(hand));
    }

    /*
     * Handler getters
     */

    @Override
    public IFluidHandler getFluidHandler(ItemStack stack) {
        if(this.getHandlerPosition(stack) != null) {
            if(this.hasHandlerDimension(stack)) {
                return getFluidHandler(ServerLifecycleHooks.getCurrentServer().getWorld(this.getHandlerDimension(stack)), this.getHandlerPosition(stack), this.getHandlerSide(stack));
            }
        }
        return null;
    }

    public IFluidHandler getFluidHandler(World world, BlockPos pos, Direction side) {
        if(world != null && pos != null) {
            TileEntity te = world.getTileEntity(pos);
            return getFluidHandler(te, side);
        }
        return null;
    }

    public IFluidHandler getFluidHandler(TileEntity te, Direction side) {
        if(te != null) {
            Optional<IFluidHandler> handlerOpt = te.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, side).resolve();
            if(handlerOpt.isPresent()) {
                return handlerOpt.get();
            }
        }
        return null;
    }

    public void sendHandlerStatus(PlayerEntity player, boolean valid) {
        player.sendStatusMessage(new TranslationTextComponent("item.enderfluidgun." + (valid ? "" : "in") + "valid_handler"), false);
    }

    /*
     * Handler Tag Methods
     */

    public RegistryKey<World> getHandlerDimension(ItemStack stack) {
        CompoundNBT tag = this.getCheckedTag(stack);
        return RegistryKey.getOrCreateKey(Registry.WORLD_KEY, new ResourceLocation(tag.getString("handlerDim")));
    }

    public boolean hasHandlerDimension(ItemStack stack) {
        CompoundNBT tag = this.getCheckedTag(stack);
        return tag.contains("handlerDim");
    }

    public boolean hasHandlerDimensionTag(ItemStack stack) {
        CompoundNBT tag = this.getCheckedTag(stack);
        return tag.contains("handlerDim");
    }

    private void writeHandlerDimension(ItemStack stack, RegistryKey<World> dimension) {
        CompoundNBT tag = this.getCheckedTag(stack);
        tag.putString("handlerDim", dimension.getLocation().toString());
    }

    @Nonnull
    public CompoundNBT getCheckedTag(@Nonnull ItemStack stack) {
        if(!stack.hasTag()) {
            stack.setTag(new CompoundNBT());
        }
        return stack.getTag();
    }

    public void writeHandlerPosition(@Nonnull ItemStack stack, @Nonnull BlockPos pos) {
        CompoundNBT tag = this.getCheckedTag(stack);
        tag.putInt("handlerX", pos.getX());
        tag.putInt("handlerY", pos.getY());
        tag.putInt("handlerZ", pos.getZ());
    }

    public void writeHandlerSide(@Nonnull ItemStack stack, @Nullable Direction side) {
        CompoundNBT tag = this.getCheckedTag(stack);
        if(side == null && tag.contains("handlerSide")) {
            tag.remove("handlerSide");
        } else {
            tag.putString("handlerSide", side.getName2());
        }
    }

    @Nullable
    public BlockPos getHandlerPosition(@Nonnull ItemStack stack) {
        CompoundNBT tag = this.getCheckedTag(stack);
        if(!tag.contains("handlerX") || !tag.contains("handlerY") || !tag.contains("handlerZ")) {
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

    @Nullable
    public Direction getHandlerSide(@Nonnull ItemStack stack) {
        CompoundNBT tag = this.getCheckedTag(stack);
        if(!tag.contains("handlerSide")) {
            return null;
        }
        String name = tag.getString("handlerSide");
        return Direction.byName(name);
    }

    public boolean hasHandlerSide(@Nonnull ItemStack stack) {
        return getHandlerSide(stack) != null;
    }

    public boolean isValidHandler(World world, BlockPos pos, Direction side) {
        return getFluidHandler(world, pos, side) != null;
    }

    @Override
    public void sendFiredPacket(ServerPlayerEntity player, Hand hand, IFluidHandler handler) {
        FluidGunMod.HANDLER.send(PacketDistributor.PLAYER.with(() -> player), new EnderUpdateClientPacket(true, handToSlot(player, hand), this.getContentsBuckets(handler), this.getMaxCapacityBuckets(handler), handler));
    }

    public static int handToSlot(PlayerEntity player, Hand hand) {
        return hand == Hand.MAIN_HAND ? player.inventory.getSlotFor(player.getHeldItem(hand)) : player.inventory.mainInventory.size() + player.inventory.armorInventory.size();
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void addHandlerInfo(ItemStack stack, World worldIn, List<ITextComponent> tooltip) {
        if(!this.getCheckedTag(stack).contains("link_error") && this.getCheckedTag(stack).contains("client_handler_info")) {
            CompoundNBT info = this.getCheckedTag(stack).getCompound("client_handler_info");
            if (info.getInt("contents") > 0) {
                ListNBT list = info.getList("tanks", Constants.NBT.TAG_COMPOUND);
                CompoundNBT[] compounds = list.toArray(new CompoundNBT[0]);
                if(compounds.length > 1) {
                    tooltip.add(new TranslationTextComponent("item.enderfluidgun.tanks"));
                }
                for (CompoundNBT tank : compounds) {
                    TranslationTextComponent fluid = new TranslationTextComponent(tank.getString("key"));
                    if(fluid != null && !fluid.getKey().isEmpty()) {
                        boolean isHot = tank.getInt("temp") > 500;
                        IFormattableTextComponent fluidText = fluid.mergeStyle(isHot ? TextFormatting.GOLD : TextFormatting.BLUE);
                        if(compounds.length > 1) {
                            tooltip.add(new TranslationTextComponent("item.enderfluidgun.seperator.colon", fluidText, new TranslationTextComponent("item.enderfluidgun.seperator.slash", wrapString(tank.getInt("contained"), TextFormatting.GRAY), wrapString(tank.getInt("max"), TextFormatting.GRAY)).mergeStyle(TextFormatting.GRAY)).mergeStyle(TextFormatting.GRAY));
                        } else {
                            tooltip.add(fluidText);
                        }
                    } else if(compounds.length > 1) {
                        tooltip.add(new TranslationTextComponent("item.enderfluidgun.empty", wrapString(tank.getInt("max"), TextFormatting.GRAY)));
                    }
                }
            }
            tooltip.add(contentsText(info.getInt("contents"), info.getInt("max")));
        }
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void addInformationShift(boolean top, ItemStack stack, World worldIn, List<ITextComponent> tooltip) {
        if(!top) {
            tooltip.add(new TranslationTextComponent("item.enderfluidgun.info"));
            if (!this.getCheckedTag(stack).contains("link_error") && this.hasHandlerPositionTag(stack) && this.hasHandlerDimensionTag(stack)) {
                tooltip.add(new TranslationTextComponent("item.enderfluidgun.linked"));
                BlockPos pos = this.getHandlerPosition(stack);
                tooltip.add(new TranslationTextComponent("item.enderfluidgun.x", wrapString(pos.getX(), TextFormatting.GRAY)));
                tooltip.add(new TranslationTextComponent("item.enderfluidgun.y", wrapString(pos.getY(), TextFormatting.GRAY)));
                tooltip.add(new TranslationTextComponent("item.enderfluidgun.z", wrapString(pos.getZ(), TextFormatting.GRAY)));
                tooltip.add(new TranslationTextComponent("item.enderfluidgun.dim", wrapString(this.getHandlerDimension(stack).getLocation().toString(), TextFormatting.GRAY)));
                if(this.hasHandlerSide(stack)) {
                    String sideName = this.getHandlerSide(stack).getName2();
                    // capitalize
                    sideName = sideName.substring(0, 1).toUpperCase() + sideName.substring(1);
                    tooltip.add(new TranslationTextComponent("item.enderfluidgun.side", wrapString(sideName, TextFormatting.GRAY)));
                }
            }
        }
    }
}
