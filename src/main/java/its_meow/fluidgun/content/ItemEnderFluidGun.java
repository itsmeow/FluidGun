package its_meow.fluidgun.content;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ItemEnderFluidGun extends ItemBaseFluidGun {

	public ItemEnderFluidGun(String name, float range) {
		super(name, range);
	}

	@Override
	public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
		ItemStack stack = player.getHeldItem(hand);
		if(world.isRemote) {
			return new ActionResult<ItemStack>(EnumActionResult.PASS, player.getHeldItem(hand));
		}
		if(player.isSneaking()) {
			RayTraceResult ray = this.rayTrace(world, player, false);
			if(ray != null && ray.typeOfHit == RayTraceResult.Type.BLOCK) {
				BlockPos pos = ray.getBlockPos();
				if(this.isValidHandler(player, player.world, pos, true)) {
					this.writeHandlerPosition(stack, pos);
					this.writeHandlerDimension(stack, world.provider.getDimension());
				}
			}
		} else {
			if(this.hasHandlerPositionTag(stack) && this.hasHandlerDimensionTag(stack)) {
				if(this.getFluidHandler(stack) != null) {
					this.onFired(player, world, stack, hand);
				} else {
					player.sendStatusMessage(new TextComponentTranslation("item.enderfluidgun.handler_invalidated"), false);
				}
			} else {
				player.sendStatusMessage(new TextComponentTranslation("item.enderfluidgun.set_handler"), false);
			}
		}
		return new ActionResult<ItemStack>(EnumActionResult.PASS, player.getHeldItem(hand));
	}

	private void writeHandlerDimension(ItemStack stack, int dimension) {
		NBTTagCompound tag = this.getCheckedTag(stack);
		tag.setInteger("handlerDim", dimension);
	}

	@Nonnull
	public NBTTagCompound getCheckedTag(@Nonnull ItemStack stack) {
		if(!stack.hasTagCompound()) {
			stack.setTagCompound(new NBTTagCompound());
		}
		return stack.getTagCompound();
	}

	public void writeHandlerPosition(@Nonnull ItemStack stack, @Nonnull BlockPos pos) {
		NBTTagCompound tag = this.getCheckedTag(stack);
		tag.setInteger("handlerX", pos.getX());
		tag.setInteger("handlerY", pos.getY());
		tag.setInteger("handlerZ", pos.getZ());
	}

	@Nullable
	public BlockPos getHandlerPosition(@Nonnull ItemStack stack) {
		NBTTagCompound tag = this.getCheckedTag(stack);
		if(!tag.hasKey("handlerX") || !tag.hasKey("handlerY") || !tag.hasKey("handlerZ")) {
			return null;
		}
		int x = tag.getInteger("handlerX");
		int y = tag.getInteger("handlerY");
		int z = tag.getInteger("handlerZ");
		return new BlockPos(x, y, z);
	}

	public boolean hasHandlerPositionTag(@Nonnull ItemStack stack) {
		return getHandlerPosition(stack) != null;
	}

	public boolean isValidHandler(EntityPlayer player, World world, BlockPos pos, boolean statusMessage) {
		return getFluidHandler(player, world, pos, statusMessage) != null;
	}

	public IFluidHandler getFluidHandler(EntityPlayer player, World world, BlockPos pos, boolean statusMessage) {
		TileEntity te = player.world.getTileEntity(pos);
		if(te != null) {
			IFluidHandler cap = te.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, null);
			if(cap != null) {
				if(statusMessage)
					player.sendStatusMessage(new TextComponentTranslation("item.enderfluidgun.valid_handler"), false);
				return cap;
			} else if(statusMessage) {
				player.sendStatusMessage(new TextComponentTranslation("item.enderfluidgun.invalid_handler"), false);
			}
		} else if(statusMessage) {
			player.sendStatusMessage(new TextComponentTranslation("item.enderfluidgun.invalid_handler"), false);
		}
		return null;
	}

	@Override
	public IFluidHandler getFluidHandler(ItemStack stack) {
		if(this.getHandlerPosition(stack) != null) {
			if(this.hasHandlerDimensionTag(stack)) {
				World world = DimensionManager.getWorld(this.getHandlerDimension(stack));
				if(world != null) {
					TileEntity te = world.getTileEntity(this.getHandlerPosition(stack));
					if(te != null) {
						IFluidHandler cap = te.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, null);
						if(cap != null) {
							return cap;
						}
					}
				}
			}
		}
		return null;
	}

	public int getHandlerDimension(ItemStack stack) {
		NBTTagCompound tag = this.getCheckedTag(stack);
		return tag.getInteger("handlerDim");
	}

	public boolean hasHandlerDimensionTag(ItemStack stack) {
		NBTTagCompound tag = this.getCheckedTag(stack);
		return tag.hasKey("handlerDim");
	}
	
    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        super.addInformation(stack, worldIn, tooltip, flagIn);
        if(this.getContentsBuckets(stack) > 0) {
            IFluidHandler handler = this.getFluidHandler(stack);
            for(FluidStack fs : this.getFluidStacks(stack, handler)) {
                String fluid = this.localizeFluid(stack, handler, fs);
                tooltip.add((this.getFluidUnlocalizedName(stack, handler, fs).equals("fluid.tile.lava") ? TextFormatting.GOLD : TextFormatting.BLUE) + fluid);
            }
        }
        tooltip.add(TextFormatting.DARK_GRAY + I18n.format("item.fluidgun.contents") + ": " + TextFormatting.GRAY + this.getContentsBuckets(stack) + "/" + this.getMaxCapacityBuckets(true, stack, this.getFluidHandler(stack)));
        tooltip.add(I18n.format("item.fluidgun.mode." + this.getMode(stack).name().toLowerCase() + ".info"));
        if(GuiScreen.isShiftKeyDown()) {
            tooltip.add(TextFormatting.GOLD + I18n.format("item.fluidgun.range") + ": " + TextFormatting.YELLOW
                    + this.getRange());

            tooltip.add(I18n.format("item.fluidgun.info"));
            tooltip.add(I18n.format("item.fluidgun.wheel.info"));
            tooltip.add(I18n.format("item.enderfluidgun.info"));
            if(this.getFluidHandler(stack) != null && this.hasHandlerPositionTag(stack) && this.hasHandlerDimensionTag(stack)) {
            	tooltip.add(I18n.format("item.enderfluidgun.linked"));
            	BlockPos pos = this.getHandlerPosition(stack);
            	tooltip.add("X: " + pos.getX());
            	tooltip.add("Y: " + pos.getY());
            	tooltip.add("Z: " + pos.getZ());
            	tooltip.add("DIM: " + this.getHandlerDimension(stack));
            }
        } else {
            tooltip.add(I18n.format("item.fluidgun.more.info"));
        }
    }

}
