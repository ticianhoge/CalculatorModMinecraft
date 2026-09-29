package net.calculatormod.item;

import net.calculatormod.CalculatorMod;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public class ModItems {

    public static final Item CALCULATOR = registerItem(
            "calculator",
            new CalculatorItem(
                    new Item.Settings().registryKey(
                            RegistryKey.of(
                                    RegistryKeys.ITEM,
                                    Identifier.of(CalculatorMod.MOD_ID, "calculator")
                            )
                    )
            )
    );

    private static Item registerItem(String name, Item item) {
        return Registry.register(
                Registries.ITEM,
                Identifier.of(CalculatorMod.MOD_ID, name),
                item
        );
    }

    public static void registerModItems() {
        CalculatorMod.LOGGER.info("Registering Mod Items for " + CalculatorMod.MOD_ID);

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> {
            entries.add(CALCULATOR);
        });
    }
}