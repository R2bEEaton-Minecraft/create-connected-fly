package com.hlysine.create_connected.registries;

import com.hlysine.create_connected.CreateConnected;
import com.hlysine.create_connected.config.FeatureToggle;
import com.hlysine.create_connected.content.kineticbattery.KineticBatteryBlockEntity;
import com.zurrtum.create.AllCreativeModeTabs;
import com.zurrtum.create.catnip.registry.RegisteredObjectsHelper;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

// Original was DeferredRegister<CreativeModeTab> + BuildCreativeModeTabContentsEvent (NeoForge).
// Converted to direct registration + Fabric API's ItemGroupEvents (see PORTING_NOTES.md).
public class CCCreativeTabs {
    public static final List<ItemLike> ITEMS = new ArrayList<>();

    static {
        ITEMS.addAll(List.of(
                CCBlocks.ENCASED_CHAIN_COGWHEEL,
                CCBlocks.CRANK_WHEEL,
                CCBlocks.LARGE_CRANK_WHEEL,
                CCBlocks.INVERTED_CLUTCH,
                CCBlocks.INVERTED_GEARSHIFT,
                CCBlocks.PARALLEL_GEARBOX,
                CCItems.VERTICAL_PARALLEL_GEARBOX,
                CCBlocks.SIX_WAY_GEARBOX,
                CCItems.VERTICAL_SIX_WAY_GEARBOX,
                CCBlocks.BRASS_GEARBOX,
                CCItems.VERTICAL_BRASS_GEARBOX,
                CCBlocks.CROSS_CONNECTOR,
                CCBlocks.SHEAR_PIN,
                CCBlocks.OVERSTRESS_CLUTCH,
                CCBlocks.CENTRIFUGAL_CLUTCH,
                CCBlocks.FREEWHEEL_CLUTCH,
                CCBlocks.BRAKE,
                CCBlocks.KINETIC_BRIDGE,
                CCBlocks.KINETIC_BATTERY,
                CCBlocks.ITEM_SILO,
                CCBlocks.FLUID_VESSEL,
                CCBlocks.CREATIVE_FLUID_VESSEL,
                CCBlocks.INVENTORY_ACCESS_PORT,
                CCBlocks.INVENTORY_BRIDGE,
                CCBlocks.BRASS_CHUTE,
                CCBlocks.DASHBOARD,
                CCBlocks.SEQUENCED_PULSE_GENERATOR,
                CCItems.LINKED_TRANSMITTER,
                CCItems.REDSTONE_LINK_WILDCARD,
                CCBlocks.EMPTY_FAN_CATALYST,
                CCBlocks.FAN_BLASTING_CATALYST,
                CCBlocks.FAN_SMOKING_CATALYST,
                CCBlocks.FAN_SPLASHING_CATALYST,
                CCBlocks.FAN_HAUNTING_CATALYST,
                CCBlocks.FAN_FREEZING_CATALYST,
                CCBlocks.FAN_SEETHING_CATALYST,
                CCBlocks.FAN_SANDING_CATALYST,
                CCBlocks.FAN_ENRICHED_CATALYST,
                CCBlocks.FAN_ENDING_CATALYST_DRAGONS_BREATH,
                CCBlocks.FAN_ENDING_CATALYST_DRAGON_HEAD,
                CCBlocks.FAN_WITHERING_CATALYST,
                CCBlocks.FAN_CHOCOLATE_COATING_CATALYST,
                CCBlocks.FAN_HONEY_COATING_CATALYST,
                CCBlocks.FAN_EXPLODING_CATALYST,
                CCBlocks.FAN_RESONANCE_CATALYST,
                CCBlocks.FAN_SCULKING_CATALYST,
                CCBlocks.FAN_PURIFYING_CATALYST,
                CCBlocks.FAN_TRANSMUTATION_CATALYST,
                CCBlocks.FAN_GLOOMING_CATALYST,
                CCBlocks.FAN_SOUL_STRIPPING_CATALYST
        ));
        CCBlocks.FAN_DYEING_CATALYSTS.forEach((color, block) -> ITEMS.add(block));
        ITEMS.addAll(List.of(
                CCBlocks.COPYCAT_BLOCK,
                CCBlocks.COPYCAT_SLAB,
                CCBlocks.COPYCAT_BEAM,
                CCBlocks.COPYCAT_VERTICAL_STEP,
                CCBlocks.COPYCAT_STAIRS,
                CCBlocks.COPYCAT_FENCE,
                CCBlocks.COPYCAT_FENCE_GATE,
                CCBlocks.COPYCAT_WALL,
                CCBlocks.COPYCAT_BOARD,
                CCItems.COPYCAT_BOX,
                CCItems.COPYCAT_CATWALK,
                CCItems.CONTROL_CHIP,
                CCItems.MUSIC_DISC_ELEVATOR,
                CCItems.MUSIC_DISC_INTERLUDE
        ));
    }

    public static final ResourceKey<CreativeModeTab> MAIN_KEY = ResourceKey.create(Registries.CREATIVE_MODE_TAB, CreateConnected.asResource("main"));

    public static final CreativeModeTab MAIN = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, MAIN_KEY, CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.create_connected.main"))
            .withTabsBefore(AllCreativeModeTabs.PALETTES_GROUP)
            .icon(CCBlocks.BRASS_GEARBOX::asItem)
            .displayItems(new DisplayItemsGenerator())
            .build());

    private static boolean isEnabled(ItemLike item) {
        return FeatureToggle.isEnabled(id(item));
    }

    private static Identifier id(ItemLike item) {
        return RegisteredObjectsHelper.getKeyOrThrow(item.asItem());
    }

    public static void register() {
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.SEARCH).register(entries -> {
            entries.getDisplayStacks().removeIf(stack -> ITEMS.stream().anyMatch(item -> item.asItem() == stack.getItem()) && !isEnabled(stack.getItem()));
            entries.getSearchTabStacks().removeIf(stack -> ITEMS.stream().anyMatch(item -> item.asItem() == stack.getItem()) && !isEnabled(stack.getItem()));
        });
    }

    private static class DisplayItemsGenerator implements CreativeModeTab.DisplayItemsGenerator {
        @Override
        public void accept(@NotNull CreativeModeTab.ItemDisplayParameters params, @NotNull CreativeModeTab.Output output) {
            for (ItemLike item : ITEMS) {
                if (isEnabled(item)) {
                    if (item.asItem() == CCBlocks.KINETIC_BATTERY.asItem()) {
                        ItemStack stack = new ItemStack(item.asItem());
                        stack.set(CCDataComponents.KINETIC_BATTERY_CHARGE, KineticBatteryBlockEntity.getMaxBatteryLevel());
                        output.accept(stack);
                    } else {
                        output.accept(item);
                    }
                }
            }
        }
    }
}
