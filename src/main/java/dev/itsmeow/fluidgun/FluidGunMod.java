package dev.itsmeow.fluidgun;

import com.google.common.collect.ImmutableList;
import dev.itsmeow.fluidgun.content.ItemEnderFluidGun;
import dev.itsmeow.fluidgun.network.*;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.network.NetworkManager;
import net.minecraft.util.Hand;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.fml.network.simple.SimpleChannel;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

@Mod.EventBusSubscriber(modid = FluidGunMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
@Mod(value = FluidGunMod.MODID)
public class FluidGunMod {

    public static final String MODID = "fluidgun";
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel HANDLER = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation(FluidGunMod.MODID, "main_channel"))
            .clientAcceptedVersions(PROTOCOL_VERSION::equals)
            .serverAcceptedVersions(PROTOCOL_VERSION::equals)
            .networkProtocolVersion(() -> PROTOCOL_VERSION)
            .simpleChannel();

    public static final ItemGroup GROUP = new ItemGroup("fluid_gun") {
        @Override
        public ItemStack createIcon() {
            return new ItemStack(ModItems.TAB_HOLDER.get());
        }
    };

    public FluidGunMod() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        ModItems.subscribe(FMLJavaModLoadingContext.get().getModEventBus());

        FluidGunConfigMain.setupConfig();

        // Make sure to do this after containers are loaded
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, FluidGunConfigMain.SERVER_CONFIG);
    }

    private void setup(final FMLCommonSetupEvent event) {
        int packets = 0;
        HANDLER.registerMessage(packets++, MousePacket.class, MousePacket::encode, MousePacket::decode, MousePacket.Handler::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        HANDLER.registerMessage(packets++, ConfigurationPacket.class, ConfigurationPacket::encode, ConfigurationPacket::decode, ConfigurationPacket.Handler::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        HANDLER.registerMessage(packets++, GunFiredPacket.class, GunFiredPacket::encode, GunFiredPacket::decode, GunFiredPacket.Handler::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        HANDLER.registerMessage(packets++, EnderUpdateClientPacket.class, EnderUpdateClientPacket::encode, EnderUpdateClientPacket::decode, EnderUpdateClientPacket.Handler::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        HANDLER.registerMessage(packets++, EnderHandlerInvalidatedPacket.class, EnderHandlerInvalidatedPacket::encode, EnderHandlerInvalidatedPacket::decode, EnderHandlerInvalidatedPacket.Handler::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getPlayer() instanceof ServerPlayerEntity) {
            updateAllGunsConfig(false, ((ServerPlayerEntity) e.getPlayer()).connection.getNetworkManager());
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        // every 15s update client fluid guns
        if (event.side == LogicalSide.SERVER && event.player.ticksExisted % 300 == 0) {
            ServerPlayerEntity player = (ServerPlayerEntity) event.player;
            List[] invs = new List[] { player.inventory.mainInventory, player.inventory.armorInventory, player.inventory.offHandInventory };
            int i = 0;
            for (List<ItemStack> inventory : invs) {
                for (ItemStack stack : inventory) {
                    updateStack(player, i, stack);
                    i++;
                }
            }
        }
    }

    @SubscribeEvent
    public static void onHandChange(LivingEquipmentChangeEvent event) {
        // always update when hand switches to gun
        if (event.getEntityLiving() instanceof ServerPlayerEntity && (event.getSlot() == EquipmentSlotType.MAINHAND || event.getSlot() == EquipmentSlotType.OFFHAND)) {
            ServerPlayerEntity player = (ServerPlayerEntity) event.getEntityLiving();
            Hand hand = event.getSlot() == EquipmentSlotType.MAINHAND ? Hand.MAIN_HAND : Hand.OFF_HAND;
            updateStack(player, ItemEnderFluidGun.handToSlot(player, hand), event.getTo());
        }
    }

    public static void updateStack(ServerPlayerEntity player, int slot, ItemStack stack) {
        if (stack.getItem() instanceof ItemEnderFluidGun) {
            ItemEnderFluidGun item = (ItemEnderFluidGun) stack.getItem();
            if(item.getFluidHandler(stack) != null) {
                FluidGunMod.HANDLER.send(PacketDistributor.PLAYER.with(() -> player), new EnderUpdateClientPacket(false, slot, item.getContentsBuckets(stack), item.getMaxCapacityBuckets(stack), item.getFluidHandler(stack)));
            } else if(item.hasHandlerDimension(stack) && item.hasHandlerPositionTag(stack) && !item.getCheckedTag(stack).contains("link_error")) {
                FluidGunMod.HANDLER.send(PacketDistributor.PLAYER.with(() -> player), new EnderHandlerInvalidatedPacket(slot));
            }
        }
    }

    public static void updateAllGunsConfig(boolean all, @Nullable NetworkManager mgr) {
        for (String gun : FluidGunConfigMain.GunConfig.RANGE.keySet()) {
            int count = 0;
            if (FluidGunConfigMain.GunConfig.COUNT.containsKey(gun)) {
                count = FluidGunConfigMain.GunConfig.COUNT.get(gun);
            }
            float range = FluidGunConfigMain.GunConfig.RANGE.get(gun);
            ConfigurationPacket pkt = new ConfigurationPacket(gun, count, range);
            if (all) {
                HANDLER.send(PacketDistributor.ALL.noArg(), pkt);
            } else {
                HANDLER.sendTo(pkt, mgr, NetworkDirection.PLAY_TO_CLIENT);
            }
        }
    }

}