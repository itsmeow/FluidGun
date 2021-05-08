package dev.itsmeow.fluidgun.content;

import net.minecraft.fluid.Fluid;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.templates.FluidHandlerItemStack;

public class FluidHandlerItemStackBuckets extends FluidHandlerItemStack {

    public FluidHandlerItemStackBuckets(ItemStack container, int capacity) {
        super(container, capacity);
    }

    @Override
    public int fill(FluidStack resource, FluidAction doFill) {
        if (container.getCount() != 1 || resource == null || resource.getAmount() <= 0 || !canFillFluidType(resource)) {
            return 0;
        }
        
        if(resource.getAmount() < 1000) {
        	return 0;
        }

        FluidStack contained = getFluid();
        if (contained.isEmpty()) {
            int fillAmount = Math.min(capacity, resource.getAmount());

            if (doFill == FluidAction.EXECUTE) {
                FluidStack filled = resource.copy();
                filled.setAmount(fillAmount - (fillAmount % 1000));
                setFluid(filled);
            }

            return fillAmount - (fillAmount % 1000);
        } else {
            if (contained.isFluidEqual(resource)) {
                int fillAmount = Math.min(capacity - contained.getAmount(), resource.getAmount());

                if (doFill == FluidAction.EXECUTE && fillAmount > 0) {
                    contained.setAmount(contained.getAmount() + (fillAmount - (fillAmount % 1000)));
                    setFluid(contained);
                }

                return fillAmount - (fillAmount % 1000);
            }

            return 0;
        }
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction doDrain) {
        if (container.getCount() != 1 || maxDrain <= 0) {
            return FluidStack.EMPTY;
        }

        FluidStack contained = getFluid();
        if (contained.isEmpty() || contained.getAmount() <= 0 || !canDrainFluidType(contained)) {
            return FluidStack.EMPTY;
        }
        
        if(maxDrain < 1000) {
        	return FluidStack.EMPTY;
        }

        final int drainAmount = Math.min(contained.getAmount(), maxDrain);

        FluidStack drained = contained.copy();
        drained.setAmount(drainAmount - (drainAmount % 1000));

        if (doDrain == FluidAction.EXECUTE) {
            contained.setAmount(contained.getAmount() - drainAmount);
            if (contained.getAmount() == 0)
            {
                setContainerToEmpty();
            } else {
                setFluid(contained);
            }
        }

        return drained;
    }

}
