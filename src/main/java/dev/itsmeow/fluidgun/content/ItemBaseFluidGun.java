package dev.itsmeow.fluidgun.content;

import dev.itsmeow.fluidgun.FluidGunConfigMain;
import dev.itsmeow.fluidgun.FluidGunMod;
import dev.itsmeow.fluidgun.network.GunFiredPacket;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.resources.I18n;
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
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidBlock;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fml.network.NetworkDirection;

import java.util.HashSet;
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

    @Override
    public ActionResultType onItemUse(ItemUseContext context) {
        return ActionResultType.FAIL;
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

    protected int getStoredAmount(IFluidHandler handler) {
        int amount = 0;
        for(FluidStack p : getFluidStacks(handler)) {
            amount += p.getAmount();
        }
        return amount;
    }

    protected int getContentsBuckets(ItemStack stack) {
        return this.getContentsBuckets(this.getFluidHandler(stack));
    }

    protected int getContentsBuckets(IFluidHandler handler) {
        return (handler == null) ? 0 : this.getStoredAmount(handler) / 1000;
    }

    protected String getFluidUnlocalizedName(IFluidHandler handler, FluidStack fs) {
        if(handler == null || fs == null) {
            return "";
        }
        return fs.getTranslationKey();
    }

    protected String localizeFluid(IFluidHandler handler, FluidStack fs) {
        if(handler == null || fs == null) {
            return "";
        }
        return I18n.format(this.getFluidUnlocalizedName(handler, fs));
    }

    public int getMaxCapacityMilliBuckets(IFluidHandler handler) {
        int amount = 0;
        int tanks = handler.getTanks();
        for(int i = 0; i < tanks; i++) {
            amount += handler.getTankCapacity(i);
        }
        return amount;
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
                world.setBlockState(pos, Blocks.AIR.getDefaultState());
                world.notifyBlockUpdate(pos, state, Blocks.AIR.getDefaultState(), 2);
                world.notifyNeighborsOfStateChange(pos, Blocks.AIR);
                FluidGunMod.HANDLER.sendTo(new GunFiredPacket(this.getRegistryName().getPath(), hand, this.getContentsBuckets(handler), this.getMaxCapacityBuckets(handler)), player.connection.netManager, NetworkDirection.PLAY_TO_CLIENT);
                this.spawnPathBetweenReversed(world, player.getEyePosition(1F), Vector3d.copyCentered(pos.offset(side)), state);
                getFluids(handler).stream().findFirst().ifPresent(s -> world.playSound(null, player.getPosX(), player.getPosY(), player.getPosZ(), s.getAttributes().getFillSound(), SoundCategory.PLAYERS, 1.0F, 1.0F));
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
                if(state.isReplaceable(bctx) || state.getBlock() == Blocks.SNOW) {
                    BlockEvent.EntityPlaceEvent event = new BlockEvent.EntityPlaceEvent(BlockSnapshot.create(world.getDimensionKey(), world, pos.offset(side)), state, player);
                    MinecraftForge.EVENT_BUS.post(event);
                    if(!event.isCanceled() && player.canPlayerEdit(pos, side, stack)) {
                        handler.drain(1000, IFluidHandler.FluidAction.EXECUTE);
                        world.setBlockState(pos, fluid, 2);
                        world.notifyBlockUpdate(pos, state, fluid, 2);
                        world.notifyNeighborsOfStateChange(pos, fluid.getBlock());
                        FluidGunMod.HANDLER.sendTo(new GunFiredPacket(this.getRegistryName().getPath(), hand, this.getContentsBuckets(handler), this.getMaxCapacityBuckets(handler)), player.connection.netManager, NetworkDirection.PLAY_TO_CLIENT);
                        this.spawnPathBetween(world, player.getEyePosition(1F), Vector3d.copyCentered(pos), fluid);
                        world.playSound(null, player.getPosX(), player.getPosY(), player.getPosZ(), fluidF.getAttributes().getEmptySound(), SoundCategory.PLAYERS, 1.0F, 1.0F);
                        return true;
                    }
                }
            }
        }
        return false;
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
        if(!world.isRemote && world instanceof ServerWorld) {
            ServerWorld worldS = (ServerWorld) world;
            worldS.spawnParticle(new BlockParticleData(ParticleTypes.BLOCK, state), pos.x, pos.y, pos.z, 1, dir.x / 5, dir.y / 5, dir.z / 5, 1.0D);
        }
    }

    protected void onFired(ServerPlayerEntity player, World world, ItemStack stack, Hand hand) {
        for (boolean doPlacePass = false; true; doPlacePass = true) {
            BlockRayTraceResult ray = ItemFluidGun.rayTrace(player, this.getRange(), !doPlacePass ? RayTraceContext.FluidMode.SOURCE_ONLY : RayTraceContext.FluidMode.NONE);
            if (ray.getType() == RayTraceResult.Type.BLOCK) {
                BlockPos pos = ray.getPos();
                BlockState state = world.getBlockState(pos);
                Direction side = ray.getFace();
                BlockPos posOffset = pos.offset(side);
                BlockState stateOffset = world.getBlockState(posOffset);
                ItemUseContext ctx = new ItemUseContext(player, hand, ray);
                BlockItemUseContext bctx = new BlockItemUseContext(ctx);
                if (!doPlacePass) {
                    if (this.shouldIntake(stack) && world.isBlockPresent(pos) && state.getFluidState().isSource()) {
                        if (!this.takeAndFill(this.getFluidHandler(stack), state, side, world, player, pos, hand, stack, ctx, bctx)) {
                            // inform client it failed
                            world.notifyBlockUpdate(pos, state, state, 2);
                        }
                        break;
                    }
                } else if (this.shouldPlace(stack)) {
                    if (world.isBlockPresent(posOffset) && !stateOffset.getFluidState().isSource() && (stateOffset.isReplaceable(bctx) || state.getBlock() == Blocks.SNOW)) {
                        if (state.getBlock() == Blocks.SNOW && world.isBlockPresent(pos)) {
                            this.placeAndDrain(this.getFluidHandler(stack), state, side.getOpposite(), world, player, pos, hand, stack, ctx, bctx);
                        } else {
                            this.placeAndDrain(this.getFluidHandler(stack), stateOffset, side, world, player, posOffset, hand, stack, ctx, bctx);
                        }
                        break;
                    }
                }
                //world.notifyBlockUpdate(pos, Blocks.AIR.getDefaultState(), world.getBlockState(pos), 2);
            }
            if (doPlacePass) {
                break;
            }
        }
    }

}
