package its_meow.fluidgun;

import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fluids.IFluidBlock;

public class ItemFluidGun extends Item {
	
	public ItemFluidGun() {
		super();
		this.setTranslationKey(Ref.MODID + "." + this.getRegistryName().getPath());
		this.setCreativeTab(CreativeTabs.TOOLS);
	}
	
	
	
	@Override
	public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
		RayTraceResult ray = player.rayTrace(50F, 0F);
		if(ray.entityHit == null) {
			if(ray.typeOfHit == RayTraceResult.Type.BLOCK) {
				BlockPos pos = ray.getBlockPos();
				IBlockState state = world.getBlockState(pos);
				if(state.getBlock() instanceof IFluidBlock) {
					
				}
			}
		}
		return super.onItemRightClick(world, player, hand);
	}



	@Override
	public ICapabilityProvider initCapabilities(ItemStack stack, NBTTagCompound nbt) {
		return new FluidCapabilityProvider(stack, nbt);
	}

}
