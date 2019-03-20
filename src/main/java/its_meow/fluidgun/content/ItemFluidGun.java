package its_meow.fluidgun.content;

import javax.annotation.Nullable;

import its_meow.fluidgun.BaseMod;
import its_meow.fluidgun.FluidGunItemConfig;
import its_meow.fluidgun.Ref;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.BlockStaticLiquid;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
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

	private FluidGunItemConfig gunConfig = null;

	public ItemFluidGun(String name, int capacity, float range) {
		super(capacity);
		gunConfig = new FluidGunItemConfig(capacity, range);
		BaseMod.FluidGunConfig.CONFIG.put(name, gunConfig);
		this.setRegistryName(name);
		this.setTranslationKey(Ref.MODID + "." + this.getRegistryName().getPath());
		this.setCreativeTab(CreativeTabs.TOOLS);
	}

	public void updateConfig(FluidGunItemConfig cfg) {
		this.gunConfig = cfg;
	}

	@Override
	public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
		RayTraceResult ray = ItemFluidGun.rayTrace(player, this.gunConfig.range, 1F);
		ItemStack stack = player.getHeldItem(hand);
		if(ray.entityHit == null) {
			if(ray.typeOfHit == RayTraceResult.Type.BLOCK) {
				BlockPos pos = ray.getBlockPos();
				IBlockState state = world.getBlockState(pos);
				if(world.isBlockLoaded(pos)) {
					FluidHandlerItemStack handler = (FluidHandlerItemStack) this.getFluidCapability(stack);
					if(state.getBlock() instanceof IFluidBlock || state.getBlock() instanceof BlockStaticLiquid || state.getBlock() instanceof BlockLiquid) {
						int breakE = -1;
						if(!world.isRemote && player instanceof EntityPlayerMP) breakE = ForgeHooks.onBlockBreakEvent(world, ((EntityPlayerMP)player).interactionManager.getGameType(), (EntityPlayerMP) player, pos); // fire break on pos to ensure permission
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
					} else if(handler.drain(handler.getFluid(), false) != null){
						FluidStack fstack = handler.drain(1, false);
						Block fluid = fstack.getFluid().getBlock();
						if(fluid.canPlaceBlockAt(world, pos.offset(ray.sideHit))) {
							BlockEvent.PlaceEvent event = new BlockEvent.PlaceEvent(BlockSnapshot.getBlockSnapshot(world, pos.offset(ray.sideHit)), state, player, hand);
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

	@Nullable
	@SideOnly(Side.CLIENT)
	public static RayTraceResult rayTrace(EntityPlayer player, double blockReachDistance, float partialTicks) {
		Vec3d vec3d = player.getPositionEyes(partialTicks);
		Vec3d vec3d1 = player.getLook(partialTicks);
		Vec3d vec3d2 = vec3d.add(vec3d1.x * blockReachDistance, vec3d1.y * blockReachDistance, vec3d1.z * blockReachDistance);
		return player.world.rayTraceBlocks(vec3d, vec3d2, true, false, true);
	}



	public IFluidHandlerItem getFluidCapability(ItemStack stack) {
		return stack.getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null);
	}

}
