package its_meow.fluidgun;

import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidBlock;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.fluids.capability.ItemFluidContainer;

public class ItemFluidGun extends ItemFluidContainer {
	
	public ItemFluidGun(String name, int capacity) {
		super(capacity);
		this.setRegistryName(name);
		this.setTranslationKey(Ref.MODID + "." + this.getRegistryName().getPath());
		this.setCreativeTab(CreativeTabs.TOOLS);
	}
	
	
	
	@Override
	public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
		RayTraceResult ray = player.rayTrace(50F, 0F);
		ItemStack stack = player.getHeldItem(hand);
		if(ray.entityHit == null) {
			if(ray.typeOfHit == RayTraceResult.Type.BLOCK) {
				BlockPos pos = ray.getBlockPos();
				IBlockState state = world.getBlockState(pos);
				if(state.getBlock() instanceof IFluidBlock) {
					IFluidHandlerItem handler = this.getFluidCapability(stack);
					if(world.isBlockLoaded(pos)) {
						if(!world.isRemote) {
							int breakE = ForgeHooks.onBlockBreakEvent(world, ((EntityPlayerMP) player).interactionManager.getGameType(), (EntityPlayerMP) player, pos); // fire break on pos to ensure permission
							if(breakE != -1) {
								FluidStack fstack = new FluidStack(FluidRegistry.lookupFluidForBlock(state.getBlock()), 1);
								if(handler.fill(fstack, false) > 0) {
									handler.fill(fstack, true);
									world.setBlockToAir(pos);
								}
							}
						}
					}
				}
			}
		}
		return super.onItemRightClick(world, player, hand);
	}
	
	public IFluidHandlerItem getFluidCapability(ItemStack stack) {
		return stack.getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null);
	}

}
