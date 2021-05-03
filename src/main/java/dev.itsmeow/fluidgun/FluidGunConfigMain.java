package dev.itsmeow.fluidgun;

import java.util.HashMap;

import dev.itsmeow.fluidgun.content.ItemBaseFluidGun;
import dev.itsmeow.fluidgun.content.ItemFluidGun;
import net.minecraft.item.Item;
import net.minecraftforge.fml.RegistryObject;
import org.apache.commons.lang3.tuple.Pair;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.config.ModConfig;

public class FluidGunConfigMain {

    private static GunConfig GUN_CONFIG = null;

    public static ForgeConfigSpec SERVER_CONFIG = null;

    public static void setupConfig() {
        final Pair<GunConfig, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(GunConfig::new);
        SERVER_CONFIG = specPair.getRight();
        GUN_CONFIG = specPair.getLeft();
    }

    @SubscribeEvent
    public static void onLoad(final ModConfig.Loading configEvent) {
        if (configEvent.getConfig().getSpec() == SERVER_CONFIG) {
            GUN_CONFIG.onLoad();
        }
    }

    public static class GunConfig {

        public static HashMap<String, Integer> COUNT = new HashMap<>();
        public static HashMap<String, Float> RANGE = new HashMap<>();

        public static HashMap<String, ConfigValue<Integer>> COUNTC = new HashMap<>();
        public static HashMap<String, ConfigValue<Float>> RANGEC = new HashMap<>();

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
            for(RegistryObject<Item> i : ModItems.getItems()) {
                if(i.get() instanceof ItemBaseFluidGun) {
                    ItemBaseFluidGun gun = (ItemBaseFluidGun) i.get();
                    String name = gun.getRegistryName().getPath();
                    if (gun instanceof ItemFluidGun) {
                        ((ItemFluidGun) gun).setCapacity(FluidGunConfigMain.GunConfig.COUNT.get(name) * 1000);
                    }
                }
            }
        }

    }

}
