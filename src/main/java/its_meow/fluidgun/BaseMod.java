package its_meow.fluidgun;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import its_meow.fluidgun.content.ItemEnderFluidGun;
import its_meow.fluidgun.content.ItemFluidGun;
import its_meow.fluidgun.network.ConfigurationPacket;
import its_meow.fluidgun.network.GunFiredPacket;
import its_meow.fluidgun.network.MousePacket;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;

@Mod.EventBusSubscriber(modid = Ref.MODID, bus = Mod.EventBusSubscriber.Bus.MOD) // how to: subscribe to pewdiepie?
@Mod(value = Ref.MODID)
public class BaseMod {

    public static Logger LOGGER = LogManager.getLogger();
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel HANDLER = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation(Ref.MODID, "main_channel"))
            .clientAcceptedVersions(PROTOCOL_VERSION::equals)
            .serverAcceptedVersions(PROTOCOL_VERSION::equals)
            .networkProtocolVersion(() -> PROTOCOL_VERSION)
            .simpleChannel();
    public static final ItemFluidGun FLUID_GUN = new ItemFluidGun("fluid_gun", 5, 30F);
    public static final ItemFluidGun LARGE_FLUID_GUN = new ItemFluidGun("large_fluid_gun", 10, 50F);
    public static final ItemFluidGun GIANT_FLUID_GUN = new ItemFluidGun("giant_fluid_gun", 25, 80F);
    public static final ItemFluidGun CREATIVE_FLUID_GUN = new ItemFluidGun("creative_fluid_gun", 10000, 1000F);
    public static final ItemEnderFluidGun ENDER_FLUID_GUN = new ItemEnderFluidGun("ender_fluid_gun", 50F);
    public static final Item TAB_HOLDER = new Item(new Item.Properties()).setRegistryName("tab_item");
    public static ItemFluidGun[] guns = {FLUID_GUN, LARGE_FLUID_GUN, GIANT_FLUID_GUN, CREATIVE_FLUID_GUN};
    
    public static ItemGroup tab = new ItemGroup("fluid_gun") {

        @Override
        public ItemStack createIcon() {
            return new ItemStack(TAB_HOLDER);
        }
        
    };
    
    public BaseMod() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);

        FluidGunConfigMain.setupConfig();

        // Make sure to do this after containers are loaded
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, FluidGunConfigMain.SERVER_CONFIG);
    }

    private void setup(final FMLCommonSetupEvent event) {
        int packets = 0;
        HANDLER.registerMessage(packets++, MousePacket.class, MousePacket::encode, MousePacket::decode, MousePacket.Handler::handle);
        HANDLER.registerMessage(packets++, ConfigurationPacket.class, ConfigurationPacket::encode, ConfigurationPacket::decode, ConfigurationPacket.Handler::handle);
        HANDLER.registerMessage(packets++, GunFiredPacket.class, GunFiredPacket::encode, GunFiredPacket::decode, GunFiredPacket.Handler::handle);
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        for(int i = 0; i < guns.length; i++) {
            String name = guns[i].getRegistryName().getPath();
            guns[i] = new ItemFluidGun(name, FluidGunConfigMain.GunConfig.COUNT.get(name), FluidGunConfigMain.GunConfig.RANGE.get(name));
        }
        event.getRegistry().registerAll(guns);
        String name = ENDER_FLUID_GUN.getRegistryName().getPath();
        event.getRegistry().registerAll(TAB_HOLDER, new ItemEnderFluidGun(name, FluidGunConfigMain.GunConfig.RANGE.get(name)));
    }

    @SubscribeEvent
    public static void onPlayerJoin(PlayerLoggedInEvent e) {
        if(e.getPlayer() instanceof EntityPlayerMP) {
            for(String gun : FluidGunConfigMain.GunConfig.COUNT.keySet()) {
                int count = FluidGunConfigMain.GunConfig.COUNT.get(gun);
                float range = FluidGunConfigMain.GunConfig.RANGE.get(gun);
                HANDLER.sendTo(new ConfigurationPacket(gun, count, range), ((EntityPlayerMP) e.getPlayer()).connection.netManager, NetworkDirection.PLAY_TO_CLIENT);
            }
        }
    }

}