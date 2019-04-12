package its_meow.fluidgun.client;

import its_meow.fluidgun.BaseMod;
import its_meow.fluidgun.Ref;
import its_meow.fluidgun.content.ItemBaseFluidGun;
import its_meow.fluidgun.content.ItemFluidGun;
import its_meow.fluidgun.network.GunFiredPacket;
import its_meow.fluidgun.network.MousePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@Mod.EventBusSubscriber(value = Side.CLIENT)
public class ClientEvents {

	@SubscribeEvent
	public static void modelRegister(ModelRegistryEvent event) {
		for(ItemBaseFluidGun gun : BaseMod.guns) {
			ModelLoader.setCustomModelResourceLocation(gun, 0, new ModelResourceLocation(gun.getRegistryName(), "inventory"));
		}
		ModelLoader.setCustomModelResourceLocation(BaseMod.TAB_HOLDER, 0, new ModelResourceLocation(BaseMod.TAB_HOLDER.getRegistryName(), "inventory"));
	}

	@SubscribeEvent
	public static void mouseEvent(MouseEvent e) {
		EntityPlayer player = Minecraft.getMinecraft().player;

		for(EnumHand hand : EnumHand.values()) {
			ItemStack stack = player.getHeldItem(hand);
			if(stack.getItem() instanceof ItemBaseFluidGun) {
				if(GuiScreen.isAltKeyDown() && e.getDwheel() != 0) {
					BaseMod.NETWORK_INSTANCE.sendToServer(new MousePacket(player.inventory.currentItem, e.getDwheel() > 0)); 
					e.setCanceled(true);
				}
				break; // If player is holding two only scroll for one
			}
		}
	}

	public static IMessage onGunFired(GunFiredPacket message) {
		String gunName = message.gunName;
		EnumHand hand = message.hand;
		int max = message.max;
		int count = message.count;
		
		Item i = ForgeRegistries.ITEMS.getValue(new ResourceLocation(Ref.MODID, gunName));
		if(i != null && i instanceof ItemBaseFluidGun) {
			ItemBaseFluidGun gun = (ItemBaseFluidGun) i;
			if(gun instanceof ItemFluidGun) {
				int maxC = BaseMod.FluidGunConfig.COUNT.get(gunName);
				if(maxC == 0) return null;
				BaseMod.FluidGunConfig.COUNT.put(gunName, max);
			}
			EntityPlayerSP player = Minecraft.getMinecraft().player;
			ItemStack stack = player.getHeldItem(hand);
			if(stack != null && !stack.isEmpty() && stack.getItem() == gun) {
				ClientEvents.gunFiredClientSide(stack, count, max);
			}
		}
		return null;
	}

	@SideOnly(Side.CLIENT)
	public static void gunFiredClientSide(ItemStack stack, int count, int max) {
		if(stack.getItem() instanceof ItemBaseFluidGun) {
			if(Minecraft.getMinecraft().player != null) {
				EntityPlayerSP player = Minecraft.getMinecraft().player;
				player.sendStatusMessage(new TextComponentString(TextFormatting.DARK_GRAY + I18n.format("item.fluidgun.contents") + ": " + TextFormatting.GRAY + count + "/" + max), true);
			}
		}
	}

}
