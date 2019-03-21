package its_meow.fluidgun;

import java.util.HashMap;

import org.apache.logging.log4j.Logger;

import its_meow.fluidgun.content.ItemFluidGun;
import net.minecraft.item.Item;
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

@Mod.EventBusSubscriber // how to: subscribe to pewdiepie
@Mod(modid = Ref.MODID, name = Ref.NAME, version = Ref.VERSION)
public class BaseMod {

    public static Logger LOGGER = null;
    public static final ItemFluidGun FLUID_GUN = new ItemFluidGun("fluid_gun", 5, 30F);
    public static final ItemFluidGun LARGE_FLUID_GUN = new ItemFluidGun("large_fluid_gun", 10, 50F);
    public static final ItemFluidGun GIANT_FLUID_GUN = new ItemFluidGun("giant_fluid_gun", 25, 80F);

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOGGER = event.getModLog();
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {

    }

    @EventHandler
    public void postInit(FMLPostInitializationEvent event) {

    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        ItemFluidGun[] guns = { FLUID_GUN, LARGE_FLUID_GUN, GIANT_FLUID_GUN };
        for(int i = 0; i < guns.length; i++) {
            String name = guns[i].getRegistryName().getPath();
            guns[i] = new ItemFluidGun(name, FluidGunConfig.COUNT.get(name), FluidGunConfig.RANGE.get(name));
        }
        event.getRegistry().registerAll(guns);
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
    public static void onConfigChanged(ConfigChangedEvent event) {
        if(event.getModID().equals(Ref.MODID)) {
            ConfigManager.sync(event.getModID(), Config.Type.INSTANCE);

        }
    }

}