package dev.itsmeow.fluidgun;

import dev.itsmeow.fluidgun.network.ConfigurationPacket;
import dev.itsmeow.fluidgun.network.GunFiredPacket;
import dev.itsmeow.fluidgun.network.MousePacket;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.network.NetworkManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
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
    }

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getPlayer() instanceof ServerPlayerEntity) {
            updateAllGunsConfig(false, ((ServerPlayerEntity) e.getPlayer()).connection.getNetworkManager());
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