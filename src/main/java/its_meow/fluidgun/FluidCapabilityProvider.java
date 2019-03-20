package its_meow.fluidgun;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidHandlerItemStack;

public class FluidCapabilityProvider implements ICapabilitySerializable<NBTTagCompound> {
	
	private FluidHandlerItemStack handler;
	private ItemStack stack;

	public FluidCapabilityProvider(ItemStack stack, NBTTagCompound compound) {
		this.handler = new FluidHandlerItemStack(stack, 5);
		this.stack = stack;
	}

	@Override
	public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
		return capability == CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY && !stack.isEmpty();
	}

	@Override
	public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
		if (hasCapability(capability, facing)) {
			return CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY.cast(handler);
		}

		return null;
	}

	@Override
	public NBTTagCompound serializeNBT() {
		return handler.getContainer().serializeNBT();
	}

	@Override
	public void deserializeNBT(NBTTagCompound nbt) {
		if(nbt != null) {
			handler.getContainer().deserializeNBT(nbt);
		}
	}
	
}
