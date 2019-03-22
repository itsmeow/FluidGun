package its_meow.fluidgun.content;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import its_meow.fluidgun.BaseMod;
import its_meow.fluidgun.Ref;
import its_meow.fluidgun.network.GunFiredPacket;
import net.minecraft.block.Block;
import net.minecraft.block.BlockStaticLiquid;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidBlock;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.fluids.capability.ItemFluidContainer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ItemFluidGun extends ItemFluidContainer {

    public static final String NBT_MODE = "toolMode";

    public ItemFluidGun(String name, int capacity, float range) {
        super(capacity * 1000);
        BaseMod.FluidGunConfig.COUNT.put(name, capacity);
        BaseMod.FluidGunConfig.RANGE.put(name, range);
        this.setRegistryName(name);
        this.setTranslationKey(Ref.MODID + "." + this.getRegistryName().getPath());
        this.setCreativeTab(BaseMod.tab);
        this.setMaxStackSize(1);
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return false;
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
            RayTraceResult ray = (c == 0 ? ItemFluidGun.rayTrace(player, this.getRange(), 1F, true) : ItemFluidGun.rayTrace(player, this.getRange(), 1F, false));
            ItemStack stack = player.getHeldItem(hand);
            if(ray != null && ray.entityHit == null) {
                if(ray.typeOfHit == RayTraceResult.Type.BLOCK) {
                    BlockPos pos = ray.getBlockPos();
                    IBlockState state = world.getBlockState(pos);
                    EnumFacing side = ray.sideHit;
                    if(world.isBlockLoaded(pos) && pos.getY() >= 0 && pos.getY() < world.provider.getHeight() && pos.offset(side).getY() >= 0 && pos.offset(side).getY() < world.provider.getHeight()) {
                        FluidHandlerItemStackBuckets handler = (FluidHandlerItemStackBuckets) this.getFluidCapability(stack);
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

    protected void takeAndFill(FluidHandlerItemStackBuckets handler, IBlockState state, EnumFacing side, World world, EntityPlayer player, BlockPos pos, EnumHand hand, ItemStack stack) {
        FluidStack fstack = new FluidStack(FluidRegistry.lookupFluidForBlock(state.getBlock()), 1000);
        if(handler.fill(fstack, false) > 0) {
            handler.fill(fstack, true);
            world.setBlockToAir(pos);
            world.scheduleBlockUpdate(pos, Blocks.AIR, 50, 1);
            world.notifyBlockUpdate(pos, state, Blocks.AIR.getDefaultState(), 2);
            world.notifyNeighborsOfStateChange(pos, Blocks.AIR, true);
            BaseMod.NETWORK_INSTANCE.sendTo(new GunFiredPacket(this.getRegistryName().getPath().toString(), handler.getFluid() == null || handler.getFluid().getFluid() == null ? "" : handler.getFluid().getFluid().getName(), hand, this.getContentsBuckets(handler), this.getMaxCapacityBuckets(false, stack)), (EntityPlayerMP) player);
            this.spawnPathBetweenReversed(world, player.getPosition().add(0, player.getEyeHeight(), 0), pos.offset(side), state);
            boolean isWater = handler.getFluid().getFluid().getBlock() == Blocks.WATER;
            world.playSound(player.posX, player.posY, player.posZ, isWater ? SoundEvents.ITEM_BUCKET_FILL : SoundEvents.ITEM_BUCKET_FILL_LAVA, SoundCategory.PLAYERS, 1.0F, 1.0F, false);
        }
    }

    protected void placeAndDrain(FluidHandlerItemStackBuckets handler, IBlockState state, EnumFacing side, World world, EntityPlayer player, BlockPos pos, EnumHand hand, ItemStack stack) {
        FluidStack fstack = handler.drain(1000, false);
        if(fstack != null  && fstack.amount > 0) {
            Fluid fluidF = fstack.getFluid();
            if(fluidF != null) {
                Block fluid = fluidF.getBlock();
                if(fluid != null) {
                    if(fluid.canPlaceBlockAt(world, pos.offset(side)) && world.isSideSolid(pos, side) || world.getBlockState(pos.offset(side)).getBlock().isReplaceable(world, pos.offset(side))) {
                        BlockEvent.PlaceEvent event = new BlockEvent.PlaceEvent(
                                BlockSnapshot.getBlockSnapshot(world, pos.offset(side)), state, player,
                                hand);
                        MinecraftForge.EVENT_BUS.post(event);
                        if(!event.isCanceled() && world.mayPlace(fluid, pos.offset(side), true, side, player)) {
                            handler.drain(1000, true);
                            world.setBlockState(pos.offset(side), fluid.getDefaultState(), 1);
                            world.scheduleBlockUpdate(pos.offset(side), fluid, 50, 1);
                            world.notifyBlockUpdate(pos, Blocks.AIR.getDefaultState(), fluid.getDefaultState(), 2);
                            world.notifyNeighborsOfStateChange(pos, fluid, true);
                            BaseMod.NETWORK_INSTANCE.sendTo(new GunFiredPacket(this.getRegistryName().getPath().toString(), handler.getFluid() == null || handler.getFluid().getFluid() == null ? "" : handler.getFluid().getFluid().getName(), hand, this.getContentsBuckets(handler), this.getMaxCapacityBuckets(false, stack)), (EntityPlayerMP) player);
                            this.spawnPathBetween(world, player.getPosition().add(0, player.getEyeHeight(), 0), pos.offset(side), fluid.getDefaultState());
                            world.playSound(player.posX, player.posY, player.posZ, fluid == Blocks.WATER ? SoundEvents.ITEM_BUCKET_EMPTY : SoundEvents.ITEM_BUCKET_EMPTY_LAVA, SoundCategory.PLAYERS, 1.0F, 1.0F, false);
                        }
                    }
                }
            }
        }
    }

    protected void spawnPathBetween(World world, BlockPos start, BlockPos dest, IBlockState state) {
        final double stops = 100;
        double dirX = (dest.getX() - start.getX()) / stops;
        double dirY = (dest.getY() - start.getY()) / stops;
        double dirZ = (dest.getZ() - start.getZ()) / stops;
        Vec3d dir = new Vec3d(dirX, dirY, dirZ);
        for(double i = 1; i <= stops; i++) {
            Vec3d posOff = dir.scale(i);
            this.spawnParticle(world, posOff.add(start.getX(), start.getY(), start.getZ()), dir, stops, state);
        }
    }

    protected void spawnPathBetweenReversed(World world, BlockPos start, BlockPos dest, IBlockState state) {
        final double stops = 15;
        double dirX = (dest.getX() - start.getX()) / stops;
        double dirY = (dest.getY() - start.getY()) / stops;
        double dirZ = (dest.getZ() - start.getZ()) / stops;
        Vec3d dir = new Vec3d(dirX, dirY, dirZ);
        for(double i = stops; i >= 1; i--) {
            Vec3d posOff = dir.scale(i);
            this.spawnParticle(world, posOff.add(start.getX(), start.getY(), start.getZ()), dir, stops, state);
        }
    }

    protected void spawnParticle(World world, Vec3d pos, Vec3d dir, double stops, IBlockState state) {
        if(!world.isRemote && world instanceof WorldServer) {
            ((WorldServer)world).spawnParticle(EnumParticleTypes.BLOCK_CRACK, false, pos.x, pos.y, pos.z, 1, dir.x / 5, dir.y / 5, dir.z / 5, 1.0D, Block.getStateId(state));
        }
    }

    public float getRange() {
        return BaseMod.FluidGunConfig.RANGE.get(this.getRegistryName().getPath());
    }

    public int getMaxCapacityMilliBuckets(boolean isClient, ItemStack stack) {
        return this.getMaxCapacityBuckets(isClient, stack) * 1000;
    }

    public int getMaxCapacityBuckets(boolean isClient, ItemStack stack) {
        int def = BaseMod.FluidGunConfig.COUNT.get(this.getRegistryName().getPath());
        if(isClient) return def;
        if(this.getFluidCapability(stack) instanceof FluidHandlerItemStackBuckets) {
            FluidHandlerItemStackBuckets handler = (FluidHandlerItemStackBuckets) this.getFluidCapability(stack);
            if(handler != null) {
                return handler.getCapacity() / 1000;
            }
        }
        return def;
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
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        super.addInformation(stack, worldIn, tooltip, flagIn);
        if(this.getContentsBuckets(stack) > 0) {
            String fluid = this.localizeFluid(stack);
            tooltip.add((this.getFluidUnlocalizedName(stack).equals("fluid.tile.lava") ? TextFormatting.GOLD : TextFormatting.BLUE) + fluid);
        }
        tooltip.add(TextFormatting.DARK_GRAY + I18n.format("item.fluidgun.contents") + ": " + TextFormatting.GRAY + this.getContentsBuckets(stack) + "/" + this.getMaxCapacityBuckets(true, stack));
        tooltip.add(I18n.format("item.fluidgun.mode." + this.getMode(stack).name().toLowerCase() + ".info"));
        if(GuiScreen.isShiftKeyDown()) {

            tooltip.add(TextFormatting.GOLD + I18n.format("item.fluidgun.max_capacity") + ": " + TextFormatting.YELLOW
                    + this.getMaxCapacityBuckets(true, stack));
            tooltip.add(TextFormatting.GOLD + I18n.format("item.fluidgun.range") + ": " + TextFormatting.YELLOW
                    + this.getRange());

            tooltip.add(I18n.format("item.fluidgun.info"));
            tooltip.add(I18n.format("item.fluidgun.wheel.info"));
        } else {
            tooltip.add(I18n.format("item.fluidgun.more.info"));
        }
    }

    private String getFluidUnlocalizedName(ItemStack stack) {
        FluidHandlerItemStackBuckets handler = ((FluidHandlerItemStackBuckets) this.getFluidCapability(stack));
        if(handler == null || handler.getFluid() == null) {
            return "";
        }
        return handler.getFluid().getUnlocalizedName();
    }

    private String localizeFluid(ItemStack stack) {
        FluidHandlerItemStackBuckets handler = ((FluidHandlerItemStackBuckets) this.getFluidCapability(stack));
        if(handler == null || handler.getFluid() == null) {
            return "";
        }
        String res = I18n.format(this.getFluidUnlocalizedName(stack));
        if(res.startsWith("fluid.") || res.startsWith("tile.")) {
            res = I18n.format(handler.getFluid().getFluid().getUnlocalizedName());
        }
        if(res.startsWith("fluid.") || res.startsWith("tile.")) {
            res = I18n.format(handler.getFluid().getFluid().getBlock().getTranslationKey());
        }
        if(res.startsWith("fluid.") || res.startsWith("tile.")) {
            res = I18n.format(handler.getFluid().getFluid().getBlock().getLocalizedName());
        }
        return res;
    }

    private int getContentsBuckets(ItemStack stack) {
        return this.getContentsBuckets((FluidHandlerItemStackBuckets) this.getFluidCapability(stack));
    }

    private int getContentsBuckets(FluidHandlerItemStackBuckets handler) {
        return (handler == null || handler.getFluid() == null) ? 0 : handler.getFluid().amount / 1000;
    }

    @Nullable
    public static RayTraceResult rayTrace(EntityPlayer player, double blockReachDistance, float partialTicks, boolean liquid) {
        Vec3d vec3d = player.getPositionEyes(partialTicks);
        Vec3d vec3d1 = player.getLook(partialTicks);
        Vec3d vec3d2 = vec3d.add(vec3d1.x * blockReachDistance, vec3d1.y * blockReachDistance, vec3d1.z * blockReachDistance);
        return player.world.rayTraceBlocks(vec3d, vec3d2, liquid, false, true);
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