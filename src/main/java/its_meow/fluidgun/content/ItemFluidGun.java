package its_meow.fluidgun.content;

import java.util.List;

import javax.annotation.Nullable;

import its_meow.fluidgun.BaseMod;
import its_meow.fluidgun.Ref;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.BlockStaticLiquid;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidBlock;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.fluids.capability.ItemFluidContainer;
import net.minecraftforge.fluids.capability.templates.FluidHandlerItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ItemFluidGun extends ItemFluidContainer {

    public static final String NBT_MODE = "toolMode";

    public ItemFluidGun(String name, int capacity, float range) {
        super(capacity);
        BaseMod.FluidGunConfig.COUNT.put(name, capacity);
        BaseMod.FluidGunConfig.RANGE.put(name, range);
        this.setRegistryName(name);
        this.setTranslationKey(Ref.MODID + "." + this.getRegistryName().getPath());
        this.setCreativeTab(CreativeTabs.TOOLS);
        this.setMaxStackSize(1);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        RayTraceResult ray = ItemFluidGun.rayTrace(player, this.getRange(), 1F);
        ItemStack stack = player.getHeldItem(hand);
        if(ray.entityHit == null) {
            if(ray.typeOfHit == RayTraceResult.Type.BLOCK) {
                BlockPos pos = ray.getBlockPos();
                IBlockState state = world.getBlockState(pos);
                if(world.isBlockLoaded(pos)) {
                    FluidHandlerItemStack handler = (FluidHandlerItemStack) this.getFluidCapability(stack);
                    if(this.shouldIntake(stack) && state.getBlock() instanceof IFluidBlock
                            || state.getBlock() instanceof BlockStaticLiquid
                            || state.getBlock() instanceof BlockLiquid) {
                        int breakE = -1;
                        if(!world.isRemote && player instanceof EntityPlayerMP)
                            breakE = ForgeHooks.onBlockBreakEvent(world,
                                    ((EntityPlayerMP) player).interactionManager.getGameType(), (EntityPlayerMP) player,
                                    pos); // fire break on pos to ensure permission
                        if(breakE != -1) {
                            FluidStack fstack = new FluidStack(FluidRegistry.lookupFluidForBlock(state.getBlock()), 1);
                            if(handler.fill(fstack, false) > 0) {
                                handler.fill(fstack, true);
                                world.setBlockToAir(pos);
                                world.scheduleBlockUpdate(pos, Blocks.AIR, 50, 1);
                                world.notifyBlockUpdate(pos, state, Blocks.AIR.getDefaultState(), 2);
                                world.notifyNeighborsOfStateChange(pos, Blocks.AIR, true);
                            }
                        }
                    } else if(this.shouldPlace(stack) && handler.drain(handler.getFluid(), false) != null) {
                        FluidStack fstack = handler.drain(1, false);
                        Block fluid = fstack.getFluid().getBlock();
                        if(fluid.canPlaceBlockAt(world, pos.offset(ray.sideHit))) {
                            BlockEvent.PlaceEvent event = new BlockEvent.PlaceEvent(
                                    BlockSnapshot.getBlockSnapshot(world, pos.offset(ray.sideHit)), state, player,
                                    hand);
                            MinecraftForge.EVENT_BUS.post(event);
                            if(!event.isCanceled()) {
                                handler.drain(1, true);
                                world.setBlockState(pos.offset(ray.sideHit), fluid.getDefaultState(), 1);
                                world.scheduleBlockUpdate(pos.offset(ray.sideHit), fluid, 50, 1);
                                world.notifyBlockUpdate(pos, Blocks.AIR.getDefaultState(), fluid.getDefaultState(), 2);
                                world.notifyNeighborsOfStateChange(pos, fluid, true);
                            }
                        }
                    }

                    if(!world.isRemote) {
                        world.scheduleBlockUpdate(pos.offset(ray.sideHit), world.getBlockState(pos).getBlock(), 1, 100);
                        world.notifyBlockUpdate(pos, Blocks.AIR.getDefaultState(), world.getBlockState(pos), 2);
                    }
                }
            }
        }
        return super.onItemRightClick(world, player, hand);
    }

    public float getRange() {
        return BaseMod.FluidGunConfig.RANGE.get(this.getRegistryName().getPath());
    }

    public int getMaxCapacity() {
        return BaseMod.FluidGunConfig.COUNT.get(this.getRegistryName().getPath());
    }

    public boolean shouldPlace(ItemStack stack) {
        ScrollMode mode = this.getMode(stack);
        return mode == ScrollMode.BOTH || mode == ScrollMode.OUT;
    }

    public boolean shouldIntake(ItemStack stack) {
        ScrollMode mode = this.getMode(stack);
        return mode == ScrollMode.BOTH || mode == ScrollMode.IN;
    }

    @Override
    public void addInformation(ItemStack stack, World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        super.addInformation(stack, worldIn, tooltip, flagIn);
        tooltip.add(TextFormatting.GOLD + I18n.format("item.fluidgun.capacity") + ": " + TextFormatting.YELLOW
                + this.getMaxCapacity());
        tooltip.add(TextFormatting.GOLD + I18n.format("item.fluidgun.range") + ": " + TextFormatting.YELLOW
                + this.getRange());
        tooltip.add(I18n.format("item.fluidgun.mode." + this.getMode(stack).name().toLowerCase() + ".info"));
        if(GuiScreen.isShiftKeyDown()) {
            tooltip.add(I18n.format("item.fluidgun.info"));
            tooltip.add(I18n.format("item.fluidgun.use.info"));
            tooltip.add(I18n.format("item.fluidgun.wheel.info"));
        } else {
            tooltip.add(I18n.format("item.fluidgun.more.info"));
        }
    }

    @Nullable
    @SideOnly(Side.CLIENT)
    public static RayTraceResult rayTrace(EntityPlayer player, double blockReachDistance, float partialTicks) {
        Vec3d vec3d = player.getPositionEyes(partialTicks);
        Vec3d vec3d1 = player.getLook(partialTicks);
        Vec3d vec3d2 = vec3d.add(vec3d1.x * blockReachDistance, vec3d1.y * blockReachDistance,
                vec3d1.z * blockReachDistance);
        return player.world.rayTraceBlocks(vec3d, vec3d2, true, false, true);
    }

    public IFluidHandlerItem getFluidCapability(ItemStack stack) {
        return stack.getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null);
    }

    public void handleMouseWheelAction(ItemStack stack, EntityPlayerMP player, boolean b, boolean forward) {
        this.toggleMode(stack, forward);
        player.inventoryContainer.detectAndSendChanges();
        player.sendStatusMessage(new TextComponentTranslation(
                "item.fluidgun.mode." + this.getMode(stack).name().toLowerCase() + ".info"), true);
    }

    public void toggleMode(ItemStack stack, boolean forward) {
        this.setMode(stack, forward ? this.getMode(stack).next() : this.getMode(stack).prev());
    }

    public ScrollMode getMode(ItemStack stack) {
        if(stack.getTagCompound() != null) {
            return ScrollMode.get(stack.getTagCompound().getInteger(NBT_MODE));
        }
        return ScrollMode.BOTH;
    }

    public void setMode(ItemStack stack, ScrollMode mode) {
        if(stack.getTagCompound() == null) {
            stack.setTagCompound(new NBTTagCompound());
        }
        stack.getTagCompound().setInteger(NBT_MODE, mode.ordinal());
    }

}