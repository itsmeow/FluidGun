package its_meow.fluidgun;

import java.util.HashMap;

import org.apache.logging.log4j.Logger;

import its_meow.fluidgun.content.ItemBaseFluidGun;
import its_meow.fluidgun.content.ItemEnderFluidGun;
import its_meow.fluidgun.content.ItemFluidGun;
import its_meow.fluidgun.network.ConfigurationPacket;
import its_meow.fluidgun.network.ConfigurationPacketHandler;
import its_meow.fluidgun.network.GunFiredPacket;
import its_meow.fluidgun.network.GunFiredPacketHandler;
import its_meow.fluidgun.network.MouseHandler;
import its_meow.fluidgun.network.MousePacket;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber // how to: subscribe to pewdiepie?
@Mod(modid = Ref.MODID, name = Ref.NAME, version = Ref.VERSION)
public class BaseMod {

    public static Logger LOGGER = null;
    public static final SimpleNetworkWrapper NETWORK_INSTANCE = NetworkRegistry.INSTANCE.newSimpleChannel(Ref.MODID);
    public static final ItemFluidGun FLUID_GUN = new ItemFluidGun("fluid_gun", 5, 30F);
    public static final ItemFluidGun LARGE_FLUID_GUN = new ItemFluidGun("large_fluid_gun", 10, 50F);
    public static final ItemFluidGun GIANT_FLUID_GUN = new ItemFluidGun("giant_fluid_gun", 25, 80F);
    public static final ItemFluidGun CREATIVE_FLUID_GUN = new ItemFluidGun("creative_fluid_gun", 10000, 1000F);
    public static final ItemEnderFluidGun ENDER_FLUID_GUN = new ItemEnderFluidGun("ender_fluid_gun", 50F);
    public static final Item TAB_HOLDER = new Item().setRegistryName("tab_item");
    public static ItemBaseFluidGun[] guns = {FLUID_GUN, LARGE_FLUID_GUN, GIANT_FLUID_GUN, ENDER_FLUID_GUN, CREATIVE_FLUID_GUN};
    
    public static CreativeTabs tab = new CreativeTabs("fluid_gun") {

        @Override
        public ItemStack createIcon() {
            return new ItemStack(TAB_HOLDER);
        }
        
    };

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOGGER = event.getModLog();
        int packets = 0;
        NETWORK_INSTANCE.registerMessage(MouseHandler.class, MousePacket.class, packets++, Side.SERVER);
        NETWORK_INSTANCE.registerMessage(ConfigurationPacketHandler.class, ConfigurationPacket.class, packets++, Side.CLIENT);
        NETWORK_INSTANCE.registerMessage(GunFiredPacketHandler.class, GunFiredPacket.class, packets++, Side.CLIENT);
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {

    }

    @EventHandler
    public void postInit(FMLPostInitializationEvent event) {

    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        for(int i = 0; i < guns.length; i++) {
            String name = guns[i].getRegistryName().getPath();
            if(guns[i] instanceof ItemFluidGun) {
            	((ItemFluidGun)guns[i]).setCapacity(FluidGunConfig.COUNT.get(name));
            }
        }
        event.getRegistry().registerAll(guns);
        event.getRegistry().register(TAB_HOLDER);
    }

    @Config(modid = Ref.MODID)
    public static class FluidGunConfig {

        @Config.RequiresMcRestart
        @Config.Comment("Amount of buckets that can be held")
        public static HashMap<String, Integer> COUNT = new HashMap<String, Integer>();
        @Config.Comment("Range a gun can place or take at")
        public static HashMap<String, Float> RANGE = new HashMap<String, Float>();

    }

    @SubscribeEvent
    public static void onPlayerJoin(PlayerLoggedInEvent e) {
        if(e.player instanceof EntityPlayerMP) {
            for(String gun : FluidGunConfig.COUNT.keySet()) {
                int count = FluidGunConfig.COUNT.get(gun);
                float range = FluidGunConfig.RANGE.get(gun);
                NETWORK_INSTANCE.sendTo(new ConfigurationPacket(gun, count, range), (EntityPlayerMP) e.player);
            }
        }
    }

    @SubscribeEvent
    public static void onConfigChanged(ConfigChangedEvent event) {
        if(event.getModID().equals(Ref.MODID)) {
            ConfigManager.sync(event.getModID(), Config.Type.INSTANCE);

        }
    }

}