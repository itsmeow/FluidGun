package its_meow.fluidgun;

import java.util.HashMap;

import org.apache.commons.lang3.tuple.Pair;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.config.ModConfig;

public class FluidGunConfigMain {
    
    private static GunConfig ENTITY_CONFIG = null;

    public static ForgeConfigSpec SERVER_CONFIG = null;

    public static void setupConfig() {
        final Pair<GunConfig, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(GunConfig::new);
        SERVER_CONFIG = specPair.getRight();
        ENTITY_CONFIG = specPair.getLeft();
    }
    
    @SubscribeEvent
    public static void onLoad(final ModConfig.Loading configEvent) {
        if (configEvent.getConfig().getSpec() == SERVER_CONFIG) {
            ENTITY_CONFIG.onLoad();
        }
    }
    
    public static class GunConfig {
        
        public static HashMap<String, Integer> COUNT = new HashMap<String, Integer>();
        public static HashMap<String, Float> RANGE = new HashMap<String, Float>();
        
        public static HashMap<String, ConfigValue<Integer>> COUNTC = new HashMap<String, ConfigValue<Integer>>();
        public static HashMap<String, ConfigValue<Float>> RANGEC = new HashMap<String, ConfigValue<Float>>();
        
        public GunConfig(ForgeConfigSpec.Builder builder) {
            builder.push("count");
            COUNT.forEach((gun, count) -> {
                COUNTC.put(gun, builder.comment("Capacity of gun").worldRestart().define(gun, count));
            });
            builder.pop();
            builder.push("range");
            RANGE.forEach((gun, range) -> {
                RANGEC.put(gun, builder.comment("Range of gun").worldRestart().define(gun, range));
            });
            builder.pop();
        }
        
        public void onLoad() {
            COUNTC.forEach((gun, countV) -> {
                COUNT.put(gun, countV.get());
            });
            RANGEC.forEach((gun, rangeV) -> {
                RANGE.put(gun, rangeV.get());
            });
        }
        
    }
    
}
