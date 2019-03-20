package its_meow.fluidgun;

import org.apache.logging.log4j.Logger;

import net.minecraft.item.Item;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

@Mod.EventBusSubscriber
@Mod(modid = Ref.MODID, name = Ref.NAME, version = Ref.VERSION)
public class BaseMod {
	
	public static Logger LOGGER = null;
	public static final ItemFluidGun FLUID_GUN = new ItemFluidGun("fluid_gun", 5);
	
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
    
    public static void registerItems(RegistryEvent.Register<Item> event) {
    	event.getRegistry().register(FLUID_GUN);
    }
	
}