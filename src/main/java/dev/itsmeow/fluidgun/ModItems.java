package dev.itsmeow.fluidgun;

import dev.itsmeow.fluidgun.content.ItemEnderFluidGun;
import dev.itsmeow.fluidgun.content.ItemFluidGun;
import net.minecraft.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Collection;
import java.util.function.Function;
import java.util.function.Supplier;

public class ModItems {

    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, FluidGunMod.MODID);

    public static final RegistryObject<ItemFluidGun> FLUID_GUN = r("fluid_gun", s -> new ItemFluidGun(s, 5, 30F));
    public static final RegistryObject<ItemFluidGun> LARGE_FLUID_GUN = r("large_fluid_gun", s -> new ItemFluidGun(s, 10, 50F));
    public static final RegistryObject<ItemFluidGun> GIANT_FLUID_GUN = r("giant_fluid_gun", s -> new ItemFluidGun(s, 25, 80F));
    public static final RegistryObject<ItemFluidGun> CREATIVE_FLUID_GUN = r("creative_fluid_gun", s -> new ItemFluidGun(s, 10000, 1000F));
    public static final RegistryObject<ItemEnderFluidGun> ENDER_FLUID_GUN = r("ender_fluid_gun", s -> new ItemEnderFluidGun(s, 50F));
    public static final RegistryObject<Item> TAB_HOLDER = rH("tab_item");

    private static RegistryObject<Item> r(String name) {
        return ITEMS.register(name, () -> new Item(new Item.Properties().group(FluidGunMod.GROUP)));
    }

    private static RegistryObject<Item> rH(String name) {
        return ITEMS.register(name, () -> new Item(new Item.Properties()));
    }

    private static <T extends Item> RegistryObject<T> r(String name, Supplier<T> b) {
        return ITEMS.register(name, b);
    }

    private static <T extends Item> RegistryObject<T> r(String name, Function<String, T> b) {
        return ITEMS.register(name, () -> b.apply(name));
    }

    public static void subscribe(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }

    public static Collection<RegistryObject<Item>> getItems() {
        return ITEMS.getEntries();
    }
}
