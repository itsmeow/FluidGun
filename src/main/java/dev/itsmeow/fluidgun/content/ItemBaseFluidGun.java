package dev.itsmeow.fluidgun.content;

import dev.itsmeow.fluidgun.FluidGunConfigMain;
import dev.itsmeow.fluidgun.FluidGunMod;
import dev.itsmeow.fluidgun.network.GunFiredPacket;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ILiquidContainer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.particles.BlockParticleData;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.state.Property;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.*;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.wrappers.BlockWrapper;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.PacketDistributor;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public abstract class ItemBaseFluidGun extends Item {

    public static final String NBT_MODE = "toolMode";

    public ItemBaseFluidGun(String name, float range, Item.Properties props) {
        super(props.maxStackSize(1));
        FluidGunConfigMain.GunConfig.RANGE.put(name, range);
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return false;
    }

    public static BlockRayTraceResult rayTrace(PlayerEntity player, double blockReachDistance, RayTraceContext.FluidMode liquid) {
        Vector3d vec3d = player.getEyePosition(1F);
        Vector3d vec3d1 = player.getLook(1F);
        Vector3d vec3d2 = vec3d.add(vec3d1.x * blockReachDistance, vec3d1.y * blockReachDistance, vec3d1.z * blockReachDistance);
        return player.world.rayTraceBlocks(new RayTraceContext(vec3d, vec3d2, RayTraceContext.BlockMode.VISUAL, liquid, null));
    }

    public void handleMouseWheelAction(ItemStack stack, ServerPlayerEntity player, boolean forward) {
        this.toggleMode(stack, forward);
        player.container.detectAndSendChanges();
        player.sendStatusMessage(new TranslationTextComponent("item.fluidgun.mode." + this.getMode(stack).name().toLowerCase() + ".info"), true);
    }

    public void toggleMode(ItemStack stack, boolean forward) {
        this.setMode(stack, forward ? this.getMode(stack).next() : this.getMode(stack).prev());
    }

    public ScrollMode getMode(ItemStack stack) {
        if(stack.getTag() != null) {
            return ScrollMode.get(stack.getTag().getInt(NBT_MODE));
        }
        return ScrollMode.BOTH;
    }

    public void setMode(ItemStack stack, ScrollMode mode) {
        if(stack.getTag() == null) {
            stack.setTag(new CompoundNBT());
        }
        stack.getTag().putInt(NBT_MODE, mode.ordinal());
    }

    protected Set<Fluid> getFluids(IFluidHandler handler) {
        HashSet<Fluid> set = new HashSet<>();
        for(FluidStack p : getFluidStacks(handler)) {
            set.add(p.getFluid());
        }
        return set;
    }

    protected Set<FluidStack> getFluidStacks(IFluidHandler handler) {
        HashSet<FluidStack> set = new HashSet<>();
        int tanks = handler.getTanks();
        for(int i = 0; i < tanks; i++) {
            set.add(handler.getFluidInTank(i));
        }
        return set;
    }

    public int getStoredAmount(IFluidHandler handler) {
        int amount = 0;
        for(FluidStack p : getFluidStacks(handler)) {
            amount += p.getAmount();
        }
        return amount;
    }

    public int getContentsBuckets(ItemStack stack) {
        return this.getContentsBuckets(this.getFluidHandler(stack));
    }

    public int getContentsBuckets(IFluidHandler handler) {
        return (handler == null) ? 0 : this.getStoredAmount(handler) / 1000;
    }

    protected String getFluidUnlocalizedName(IFluidHandler handler, FluidStack fs) {
        if(handler == null || fs == null) {
            return "";
        }
        return fs.getTranslationKey();
    }

    public TranslationTextComponent localizeFluid(IFluidHandler handler, FluidStack fs) {
        if(handler == null || fs == null) {
            return null;
        }
        return new TranslationTextComponent(this.getFluidUnlocalizedName(handler, fs));
    }

    public int getMaxCapacityMilliBuckets(IFluidHandler handler) {
        int amount = 0;
        int tanks = handler.getTanks();
        for(int i = 0; i < tanks; i++) {
            amount += handler.getTankCapacity(i);
        }
        return amount;
    }

    public int getMaxCapacityBuckets(ItemStack stack) {
        return this.getMaxCapacityBuckets(this.getFluidHandler(stack));
    }

    public int getMaxCapacityBuckets(IFluidHandler handler) {
        return this.getMaxCapacityMilliBuckets(handler) / 1000;
    }

    public abstract IFluidHandler getFluidHandler(ItemStack stack);

    public boolean shouldPlace(ItemStack stack) {
        ScrollMode mode = this.getMode(stack);
        return mode == ScrollMode.BOTH || mode == ScrollMode.OUT;
    }

    public boolean shouldIntake(ItemStack stack) {
        ScrollMode mode = this.getMode(stack);
        return mode == ScrollMode.BOTH || mode == ScrollMode.IN;
    }

    public float getRange() {
        return FluidGunConfigMain.GunConfig.RANGE.get(this.getRegistryName().getPath());
    }

    protected boolean takeAndFill(IFluidHandler handler, BlockState state, Direction side, World world, ServerPlayerEntity player, BlockPos pos, Hand hand, ItemStack stack, ItemUseContext ctx, BlockItemUseContext bctx) {
        FluidStack fstack = new FluidStack(state.getFluidState().getFluid(), 1000);
        if (handler.fill(fstack, IFluidHandler.FluidAction.SIMULATE) > 0) {
            int breakE = ForgeHooks.onBlockBreakEvent(world, player.interactionManager.getGameType(), player, pos);
            if (breakE != -1) {
                handler.fill(fstack, IFluidHandler.FluidAction.EXECUTE);
                Optional<Property<?>> waterProp = state.getProperties().stream().filter(prop -> prop.getName().equals("waterlogged")).findFirst();
                BlockState destState = waterProp.isPresent() ? state.with((Property) waterProp.get(), state.getBlock().getDefaultState().get(waterProp.get())) : Blocks.AIR.getDefaultState();
                world.setBlockState(pos, destState);
                world.notifyBlockUpdate(pos, state, destState, 2);
                world.notifyNeighborsOfStateChange(pos, destState.getBlock());
                sendFiredPacket(player, hand, handler);
                this.spawnPathBetweenReversed(world, player.getEyePosition(1F), Vector3d.copyCentered(pos.offset(side)), state);
                world.playSound(null, player.getPosX(), player.getPosY(), player.getPosZ(), fstack.getFluid().getAttributes().getEmptySound(), SoundCategory.PLAYERS, 1.0F, 1.0F);
                return true;
            }
        }
        return false;
    }

    protected boolean placeAndDrain(IFluidHandler handler, BlockState state, Direction side, World world, ServerPlayerEntity player, BlockPos pos, Hand hand, ItemStack stack, ItemUseContext ctx, BlockItemUseContext bctx) {
        FluidStack fstack = handler.drain(1000, IFluidHandler.FluidAction.SIMULATE);
        if(fstack.getAmount() > 0) {
            Fluid fluidF = fstack.getFluid();
            if(fluidF != null) {
                BlockState fluid = fluidF.getDefaultState().getBlockState();
                boolean willVaporize = fluidF.getAttributes().doesVaporize(world, pos, fstack) && world.getDimensionType().isUltrawarm();
                if(fluidF.getAttributes().canBePlacedInWorld(world, pos, fluidF.getDefaultState()) && !willVaporize && (state.isReplaceable(bctx) || state.getBlock() == Blocks.SNOW)) {
                    BlockEvent.EntityPlaceEvent event = new BlockEvent.EntityPlaceEvent(BlockSnapshot.create(world.getDimensionKey(), world, pos.offset(side)), state, player);
                    MinecraftForge.EVENT_BUS.post(event);
                    if(!event.isCanceled() && player.canPlayerEdit(pos, side, stack)) {
                        //handler.drain(1000, IFluidHandler.FluidAction.EXECUTE);
                        IFluidHandler tempHandler;
                        if (state.getBlock() instanceof ILiquidContainer && ((ILiquidContainer) state.getBlock()).canContainFluid(world, pos, state, fluidF)) {
                            tempHandler = new BlockWrapper.LiquidContainerBlockWrapper((ILiquidContainer) state.getBlock(), world, pos);
                        } else {
                            tempHandler = new BlockWrapper(fluidF.getAttributes().getBlock(world, pos, fluidF.getDefaultState()), world, pos);
                        }
                        FluidStack result = FluidUtil.tryFluidTransfer(tempHandler, handler, fstack, true);
                        if (!result.isEmpty()) {
                            sendFiredPacket(player, hand, handler);
                            this.spawnPathBetween(world, player.getEyePosition(1F), Vector3d.copyCentered(pos), fluid);
                            world.playSound(null, player.getPosX(), player.getPosY(), player.getPosZ(), fluidF.getAttributes().getEmptySound(), SoundCategory.PLAYERS, 1.0F, 1.0F);
                            return true;
                        }
                        //world.setBlockState(pos, fluid, 2);
                        //world.notifyBlockUpdate(pos, state, fluid, 2);
                        //world.notifyNeighborsOfStateChange(pos, fluid.getBlock());
                    }
                }
            }
        }
        return false;
    }

    public void sendFiredPacket(ServerPlayerEntity player, Hand hand, IFluidHandler handler) {
        FluidGunMod.HANDLER.send(PacketDistributor.PLAYER.with(() -> player), new GunFiredPacket(this.getRegistryName().getPath(), hand, this.getContentsBuckets(handler), this.getMaxCapacityBuckets(handler)));
    }

    protected String[] getFluidStrings(IFluidHandler handler) {
        Set<String> st = new HashSet<>();
        for(FluidStack fluid : this.getFluidStacks(handler)) {
            st.add(fluid.getTranslationKey());
        }
        return st.toArray(new String[0]);
    }

    protected void spawnPathBetween(World world, Vector3d start, Vector3d dest, BlockState state) {
        final double stops = 100;
        double dirX = (dest.getX() - start.getX()) / stops;
        double dirY = (dest.getY() - start.getY()) / stops;
        double dirZ = (dest.getZ() - start.getZ()) / stops;
        Vector3d dir = new Vector3d(dirX, dirY, dirZ);
        for(double i = 1; i <= stops; i++) {
            Vector3d posOff = dir.scale(i);
            this.spawnParticle(world, posOff.add(start.getX(), start.getY(), start.getZ()), dir, stops, state);
        }
    }

    protected void spawnPathBetweenReversed(World world, Vector3d start, Vector3d dest, BlockState state) {
        final double stops = 15;
        double dirX = (dest.getX() - start.getX()) / stops;
        double dirY = (dest.getY() - start.getY()) / stops;
        double dirZ = (dest.getZ() - start.getZ()) / stops;
        Vector3d dir = new Vector3d(dirX, dirY, dirZ);
        for(double i = stops; i >= 1; i--) {
            Vector3d posOff = dir.scale(i);
            this.spawnParticle(world, posOff.add(start.getX(), start.getY(), start.getZ()), dir, stops, state);
        }
    }

    protected void spawnParticle(World world, Vector3d pos, Vector3d dir, double stops, BlockState state) {
        if(!world.isRemote() && world instanceof ServerWorld) {
            ServerWorld worldS = (ServerWorld) world;
            worldS.spawnParticle(new BlockParticleData(ParticleTypes.BLOCK, state.getFluidState().getBlockState()), pos.x, pos.y, pos.z, 1, dir.x / 5, dir.y / 5, dir.z / 5, 1.0D);
        }
    }

    protected boolean onFired(ServerPlayerEntity player, World world, ItemStack stack, Hand hand) {
        return doRayPass(false, player, world, stack, hand) || doRayPass(true, player, world, stack, hand);
    }

    protected boolean doRayPass(boolean doPlace, ServerPlayerEntity player, World world, ItemStack stack, Hand hand) {
        BlockRayTraceResult ray = ItemFluidGun.rayTrace(player, this.getRange(), !doPlace ? RayTraceContext.FluidMode.SOURCE_ONLY : RayTraceContext.FluidMode.NONE);
        return ray.getType() == RayTraceResult.Type.BLOCK && doFire(doPlace, player, world, stack, hand, ray);
    }

    protected boolean doFire(boolean doPlace, ServerPlayerEntity player, World world, ItemStack stack, Hand hand, BlockRayTraceResult ray) {
        BlockPos pos = ray.getPos();
        BlockState state = world.getBlockState(pos);
        Direction side = ray.getFace();
        BlockPos posOffset = pos.offset(side);
        BlockState stateOffset = world.getBlockState(posOffset);
        ItemUseContext ctx = new ItemUseContext(player, hand, ray);
        BlockItemUseContext bctx = new BlockItemUseContext(ctx);
        if (!doPlace) {
            if (this.shouldIntake(stack) && world.isBlockPresent(pos) && state.getFluidState().isSource()) {
                if (!this.takeAndFill(this.getFluidHandler(stack), state, side, world, player, pos, hand, stack, ctx, bctx)) {
                    // inform client it failed
                    world.notifyBlockUpdate(pos, state, state, 2);
                    return false;
                }
                return true;
            }
        } else if (this.shouldPlace(stack)) {
            if (world.isBlockPresent(posOffset) && !stateOffset.getFluidState().isSource() && (stateOffset.isReplaceable(bctx) || state.getBlock() == Blocks.SNOW)) {
                if (state.getBlock() == Blocks.SNOW && world.isBlockPresent(pos)) {
                    this.placeAndDrain(this.getFluidHandler(stack), state, side.getOpposite(), world, player, pos, hand, stack, ctx, bctx);
                } else {
                    this.placeAndDrain(this.getFluidHandler(stack), stateOffset, side, world, player, posOffset, hand, stack, ctx, bctx);
                }
                return true;
            }
        }
        return false;
    }

    @OnlyIn(Dist.CLIENT)
    public static TranslationTextComponent contentsText(int contents, int max) {
        return new TranslationTextComponent("item.fluidgun.contents", wrapString(contents, TextFormatting.GRAY), wrapString(max, TextFormatting.GRAY));
    }

    @OnlyIn(Dist.CLIENT)
    public void addInformation(ItemStack stack, World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {
        if(worldIn != null) {
            addHandlerInfo(stack, worldIn, tooltip);
            tooltip.add(new TranslationTextComponent("item.fluidgun.mode." + this.getMode(stack).name().toLowerCase() + ".info").mergeStyle(TextFormatting.GRAY));
            addInformationMain(stack, worldIn, tooltip);
            if (Screen.hasShiftDown()) {
                addInformationShift(true, stack, worldIn, tooltip);
                tooltip.add(new TranslationTextComponent("item.fluidgun.range", wrapString(this.getRange(), TextFormatting.YELLOW)));

                tooltip.add(new TranslationTextComponent("item.fluidgun.info"));
                tooltip.add(new TranslationTextComponent("item.fluidgun.wheel.info"));
                addInformationShift(false, stack, worldIn, tooltip);
            } else {
                tooltip.add(new TranslationTextComponent("item.fluidgun.more.info"));
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    public void addHandlerInfo(ItemStack stack, World worldIn, List<ITextComponent> tooltip) {
        if (this.getContentsBuckets(stack) > 0) {
            IFluidHandler handler = this.getFluidHandler(stack);
            for (FluidStack fs : this.getFluidStacks(handler)) {
                TranslationTextComponent fluid = this.localizeFluid(handler, fs);
                if(fluid != null) {
                    boolean isHot = fs.getFluid().getAttributes().getTemperature() > 500;
                    tooltip.add(fluid.mergeStyle(isHot ? TextFormatting.GOLD : TextFormatting.BLUE));
                }
            }
        }
        tooltip.add(contentsText(this.getContentsBuckets(stack), this.getMaxCapacityBuckets(this.getFluidHandler(stack))));
    }

    @OnlyIn(Dist.CLIENT)
    public void addInformationMain(ItemStack stack, World worldIn, List<ITextComponent> tooltip) {
    }

    @OnlyIn(Dist.CLIENT)
    public void addInformationShift(boolean top, ItemStack stack, World worldIn, List<ITextComponent> tooltip) {
    }

    @OnlyIn(Dist.CLIENT)
    protected static IFormattableTextComponent wrapString(int value, TextFormatting format) {
        return new StringTextComponent(String.valueOf(value)).mergeStyle(format);
    }

    @OnlyIn(Dist.CLIENT)
    protected static IFormattableTextComponent wrapString(float value, TextFormatting format) {
        return new StringTextComponent(String.valueOf(value)).mergeStyle(format);
    }

    @OnlyIn(Dist.CLIENT)
    protected static IFormattableTextComponent wrapString(String value, TextFormatting format) {
        return new StringTextComponent(value).mergeStyle(format);
    }
}
