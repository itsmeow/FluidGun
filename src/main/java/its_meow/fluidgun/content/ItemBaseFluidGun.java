package its_meow.fluidgun.content;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nullable;

import its_meow.fluidgun.BaseMod;
import its_meow.fluidgun.Ref;
import its_meow.fluidgun.network.GunFiredPacket;
import net.minecraft.block.Block;
import net.minecraft.block.BlockStaticLiquid;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidBlock;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;

public abstract class ItemBaseFluidGun extends Item {

    public static final String NBT_MODE = "toolMode";

    public ItemBaseFluidGun(String name, float range) {
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
    public EnumActionResult onItemUseFirst(EntityPlayer player, World world, BlockPos pos, EnumFacing side, float hitX,
            float hitY, float hitZ, EnumHand hand) {

        return EnumActionResult.FAIL;
    }

    @Nullable
    public static RayTraceResult rayTrace(EntityPlayer player, double blockReachDistance, float partialTicks, boolean liquid) {
        Vec3d vec3d = player.getPositionEyes(partialTicks);
        Vec3d vec3d1 = player.getLook(partialTicks);
        Vec3d vec3d2 = vec3d.add(vec3d1.x * blockReachDistance, vec3d1.y * blockReachDistance, vec3d1.z * blockReachDistance);
        return player.world.rayTraceBlocks(vec3d, vec3d2, liquid, false, true);
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

    protected Set<Fluid> getFluids(ItemStack stack, IFluidHandler handler) {
        HashSet<Fluid> set = new HashSet<Fluid>();
        for(IFluidTankProperties p : handler.getTankProperties()) {
            if(p.getContents() != null) {
                set.add(p.getContents().getFluid());
            }
        }
        return set;
    }

    protected Set<FluidStack> getFluidStacks(ItemStack stack, IFluidHandler handler) {
        HashSet<FluidStack> set = new HashSet<FluidStack>();
        for(IFluidTankProperties p : handler.getTankProperties()) {
            if(p.getContents() != null) {
                set.add(p.getContents());
            }
        }
        return set;
    }

    protected int getStoredAmount(ItemStack stack, IFluidHandler handler) {
        int amount = 0;
        for(IFluidTankProperties p : handler.getTankProperties()) {
            if(p.getContents() != null) {
                amount += p.getContents().amount;
            }
        }
        return amount;
    }

    protected int getContentsBuckets(ItemStack stack) {
        return this.getContentsBuckets(stack, this.getFluidHandler(stack));
    }

    protected int getContentsBuckets(ItemStack stack, IFluidHandler handler) {
        return (handler == null) ? 0 : this.getStoredAmount(stack, handler) / 1000;
    }

    protected String getFluidUnlocalizedName(ItemStack stack, IFluidHandler handler, FluidStack fs) {
        if(handler == null || fs == null) {
            return "";
        }
        return fs.getUnlocalizedName();
    }

    protected String localizeFluid(ItemStack stack, IFluidHandler handler, FluidStack fs) {
        if(handler == null || fs == null) {
            return "";
        }
        String res = I18n.format(this.getFluidUnlocalizedName(stack, handler, fs));
        if(res.startsWith("fluid.") || res.startsWith("tile.")) {
            res = I18n.format(fs.getFluid().getUnlocalizedName());
        }
        if(res.startsWith("fluid.") || res.startsWith("tile.")) {
            res = I18n.format(fs.getFluid().getBlock().getTranslationKey());
        }
        if(res.startsWith("fluid.") || res.startsWith("tile.")) {
            res = I18n.format(fs.getFluid().getBlock().getLocalizedName());
        }
        return res;
    }

    public int getMaxCapacityMilliBuckets(boolean isClient, ItemStack stack, IFluidHandler handler) {
        int amount = 0;
        for(IFluidTankProperties p : handler.getTankProperties()) {
            amount += p.getCapacity();
        }
        return amount;
    }

    public int getMaxCapacityBuckets(boolean isClient, ItemStack stack, IFluidHandler handler) {
        return this.getMaxCapacityMilliBuckets(isClient, stack, handler) / 1000;
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
        return BaseMod.FluidGunConfig.RANGE.get(this.getRegistryName().getPath());
    }

    protected void takeAndFill(IFluidHandler handler, IBlockState state, EnumFacing side, World world, EntityPlayer player, BlockPos pos, EnumHand hand, ItemStack stack) {
        FluidStack fstack = new FluidStack(FluidRegistry.lookupFluidForBlock(state.getBlock()), 1000);
        int amt = handler.fill(fstack, false);
        if(amt >= 1000) {
            handler.fill(fstack, true);
            world.setBlockToAir(pos);
            world.scheduleBlockUpdate(pos, Blocks.AIR, 50, 1);
            world.notifyBlockUpdate(pos, state, Blocks.AIR.getDefaultState(), 2);
            world.notifyNeighborsOfStateChange(pos, Blocks.AIR, true);
            BaseMod.NETWORK_INSTANCE.sendTo(new GunFiredPacket(this.getRegistryName().getPath().toString(), hand, this.getContentsBuckets(stack, handler), this.getMaxCapacityBuckets(false, stack, handler)), (EntityPlayerMP) player);
            this.spawnPathBetweenReversed(world, player.getPosition().add(0, player.getEyeHeight(), 0), pos.offset(side), state);
            boolean isWater = !this.getFluids(stack, handler).contains(FluidRegistry.LAVA);
            world.playSound(player.posX, player.posY, player.posZ, isWater ? SoundEvents.ITEM_BUCKET_FILL : SoundEvents.ITEM_BUCKET_FILL_LAVA, SoundCategory.PLAYERS, 1.0F, 1.0F, false);
        }
    }

    protected void placeAndDrain(IFluidHandler handler, IBlockState state, EnumFacing side, World world, EntityPlayer player, BlockPos pos, EnumHand hand, ItemStack stack) {
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
                            BaseMod.NETWORK_INSTANCE.sendTo(new GunFiredPacket(this.getRegistryName().getPath().toString(), hand, this.getContentsBuckets(stack, handler), this.getMaxCapacityBuckets(false, stack, handler)), (EntityPlayerMP) player);
                            this.spawnPathBetween(world, player.getPosition().add(0, player.getEyeHeight(), 0), pos.offset(side), fluid.getDefaultState());
                            world.playSound(player.posX, player.posY, player.posZ, fluid == Blocks.WATER ? SoundEvents.ITEM_BUCKET_EMPTY : SoundEvents.ITEM_BUCKET_EMPTY_LAVA, SoundCategory.PLAYERS, 1.0F, 1.0F, false);
                        }
                    }
                }
            }
        }
    }

    protected String[] getFluidStrings(ItemStack stack, IFluidHandler handler) {
        Set<String> st = new HashSet<String>();
        for(Fluid fluid : this.getFluids(stack, handler)) {
            st.add(fluid.getUnlocalizedName());
        }
        return st.toArray(new String[st.size()]);
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
    
    protected void onFired(EntityPlayer player, World world, ItemStack stack, EnumHand hand) {
        boolean e = true;
        int c = 0;

        do {
            RayTraceResult ray = (c == 0 ? ItemFluidGun.rayTrace(player, this.getRange(), 1F, true) : ItemFluidGun.rayTrace(player, this.getRange(), 1F, false));
            if(ray != null && ray.entityHit == null) {
                if(ray.typeOfHit == RayTraceResult.Type.BLOCK) {
                    BlockPos pos = ray.getBlockPos();
                    IBlockState state = world.getBlockState(pos);
                    EnumFacing side = ray.sideHit;
                    if(world.isBlockLoaded(pos) && pos.getY() >= 0 && pos.getY() < world.provider.getHeight() && pos.offset(side).getY() >= 0 && pos.offset(side).getY() < world.provider.getHeight()) {
                        IFluidHandler handler = this.getFluidHandler(stack);
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
    }
    
}
