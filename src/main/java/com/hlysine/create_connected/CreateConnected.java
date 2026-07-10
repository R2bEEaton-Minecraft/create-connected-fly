package com.hlysine.create_connected;

import com.hlysine.create_connected.compat.AdditionalPlacementsCompat;
import com.hlysine.create_connected.compat.CopycatsManager;
import com.hlysine.create_connected.compat.Mods;
import com.hlysine.create_connected.config.CCConfigs;
import com.hlysine.create_connected.registries.CCArmInteractionPointTypes;
import com.hlysine.create_connected.registries.CCBlockEntityTypes;
import com.hlysine.create_connected.registries.CCBlocks;
import com.hlysine.create_connected.registries.CCCreativeTabs;
import com.hlysine.create_connected.registries.CCDataComponents;
import com.hlysine.create_connected.registries.CCDisplaySources;
import com.hlysine.create_connected.registries.CCDisplayTargets;
import com.hlysine.create_connected.registries.CCInteractionBehaviours;
import com.hlysine.create_connected.registries.CCItemAttributes;
import com.hlysine.create_connected.registries.CCItems;
import com.hlysine.create_connected.registries.CCMountedStorageTypes;
import com.hlysine.create_connected.registries.CCMovementBehaviours;
import com.hlysine.create_connected.registries.CCPackets;
import com.hlysine.create_connected.registries.CCSoundEvents;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;

// Original was a NeoForge @Mod constructor wired to IEventBus (RegisterEvent, FMLCommonSetupEvent,
// GatherDataEvent, etc). Fabric registration is eager/direct - see PORTING_NOTES.md. Everything that
// was previously gated behind RegisterEvent/FMLCommonSetupEvent now just runs in declaration order
// here; static initializers in each CCXxx registries class do the actual Registry.register() calls
// the moment that class is first referenced, so call order below only matters for behavior that
// explicitly reads another registry's contents (e.g. CCCreativeTabs reading CCBlocks/CCItems).
public class CreateConnected implements ModInitializer {
    public static final String MODID = "create_connected";
    public static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public void onInitialize() {
        CCDataComponents.register();
        CCBlocks.register();
        CCItems.register();
        CCBlockEntityTypes.register();
        CCPackets.register();
        CCArmInteractionPointTypes.register();
        CCItemAttributes.register();
        CCMountedStorageTypes.register();
        CCDisplaySources.register();
        CCDisplayTargets.register();
        CCInteractionBehaviours.register();
        CCMovementBehaviours.register();
        CCCreativeTabs.register();
        CCSoundEvents.register();

        CCConfigs.register();

        if (Mods.COPYCATS.isLoaded())
            CopycatsManager.registerTickListener();
        Mods.ADDITIONAL_PLACEMENTS.executeIfInstalled(() -> AdditionalPlacementsCompat::register);

        // TODO (see PORTING_NOTES.md): CCAdvancements/CCTriggers custom trigger types and
        // CCCraftingConditions' Fabric replacement (recipe-level FeatureToggle gating) still
        // need wiring once mixin/ and datagen/advancements are converted.
    }

    public static Identifier asResource(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }
}
