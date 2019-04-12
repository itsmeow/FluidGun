package its_meow.fluidgun.content;

import its_meow.fluidgun.BaseMod;
import its_meow.fluidgun.network.GunFiredPacket;
import net.minecraft.block.BlockStaticLiquid;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.fluids.IFluidBlock;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;

public class ItemEnderFluidGun extends ItemFluidGun {

	public ItemEnderFluidGun(String name, int capacity, float range) {
		super(name, capacity, range);
	}

	@Override
	public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
		ItemStack stack = player.getHeldItem(hand);
		if(world.isRemote) {
			return super.onItemRightClick(world, player, hand);
		}
		if(player.isSneaking()) {
			RayTraceResult ray = this.rayTrace(world, player, false);
			if(ray != null && ray.typeOfHit == RayTraceResult.Type.BLOCK) {
				BlockPos pos = ray.getBlockPos();
				TileEntity te = world.getTileEntity(pos);
				if(te != null) {
					IFluidHandler cap = te.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, null);
					if(cap != null) {
						player.sendStatusMessage(new TextComponentTranslation("fluidgun.valid_handler"), false);
					} else {
						player.sendStatusMessage(new TextComponentTranslation("fluidgun.invalid_handler"), false);
					}
				} else {
					player.sendStatusMessage(new TextComponentTranslation("fluidgun.invalid_handler"), false);
				}
			}
		} else {
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
		}
		FluidHandlerItemStackBuckets handler = (FluidHandlerItemStackBuckets) this.getFluidCapability(stack);
		if(handler != null) {
			BaseMod.NETWORK_INSTANCE.sendTo(new GunFiredPacket(this.getRegistryName().getPath().toString(), handler.getFluid() == null || handler.getFluid().getFluid() == null ? "" : handler.getFluid().getFluid().getName(), hand, this.getContentsBuckets(handler), this.getMaxCapacityBuckets(false, stack)), (EntityPlayerMP) player);
		}
		return super.onItemRightClick(world, player, hand);
	}

}
