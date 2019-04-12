package its_meow.fluidgun.content;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.templates.FluidHandlerItemStack;

public class FluidHandlerItemStackBuckets extends FluidHandlerItemStack {

    public FluidHandlerItemStackBuckets(ItemStack container, int capacity) {
        super(container, capacity);
    }

    @Override
    public int fill(FluidStack resource, boolean doFill)
    {
        if (container.getCount() != 1 || resource == null || resource.amount <= 0 || !canFillFluidType(resource))
        {
            return 0;
        }
        
        if(resource.amount < 1000) {
        	return 0;
        }

        FluidStack contained = getFluid();
        if (contained == null)
        {
            int fillAmount = Math.min(capacity, resource.amount);

            if (doFill)
            {
                FluidStack filled = resource.copy();
                filled.amount = fillAmount - (fillAmount % 1000);
                setFluid(filled);
            }

            return fillAmount - (fillAmount % 1000);
        }
        else
        {
            if (contained.isFluidEqual(resource))
            {
                int fillAmount = Math.min(capacity - contained.amount, resource.amount);

                if (doFill && fillAmount > 0) {
                    contained.amount += fillAmount - (fillAmount % 1000);
                    setFluid(contained);
                }

                return fillAmount - (fillAmount % 1000);
            }

            return 0;
        }
    }

    @Override
    public FluidStack drain(int maxDrain, boolean doDrain)
    {
        if (container.getCount() != 1 || maxDrain <= 0)
        {
            return null;
        }

        FluidStack contained = getFluid();
        if (contained == null || contained.amount <= 0 || !canDrainFluidType(contained))
        {
            return null;
        }
        
        if(maxDrain < 1000) {
        	return null;
        }

        final int drainAmount = Math.min(contained.amount, maxDrain);

        FluidStack drained = contained.copy();
        drained.amount = drainAmount - (drainAmount % 1000);

        if (doDrain)
        {
            contained.amount -= drainAmount;
            if (contained.amount == 0)
            {
                setContainerToEmpty();
            }
            else
            {
                setFluid(contained);
            }
        }

        return drained;
    }

    public void forceFluid(Fluid fluid, int count, int max) {
        this.capacity = max;
        if(count == 0) {
            this.setContainerToEmpty();
            return;
        }
        if(this.getFluid() == null) this.setFluid(new FluidStack(fluid, count));
        if(this.getFluid().amount != count) this.getFluid().amount = count;
    }

    public void setEmpty() {
        this.setContainerToEmpty();
    }

    public final int getCapacity() {
        return this.capacity;
    }

}
