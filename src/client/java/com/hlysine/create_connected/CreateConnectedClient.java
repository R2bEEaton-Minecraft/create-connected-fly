package com.hlysine.create_connected;

import com.hlysine.create_connected.config.CCConfigsClient;
import com.hlysine.create_connected.content.dashboard.ClientPlayerAccess;
import com.hlysine.create_connected.content.dashboard.DashboardBlockEntity;
import com.hlysine.create_connected.content.kineticbattery.KineticBatteryDisplaySourceRender;
import com.hlysine.create_connected.content.kineticbattery.KineticBatteryTooltipBehaviour;
import com.hlysine.create_connected.content.overstressclutch.OverstressClutchBlockEntityClient;
import com.hlysine.create_connected.content.overstressclutch.OverstressClutchTooltipBehaviour;
import com.hlysine.create_connected.content.sequencedpulsegenerator.instructions.Instruction;
import com.hlysine.create_connected.registries.CCBlockEntityTypes;
import com.hlysine.create_connected.registries.CCBlocks;
import com.hlysine.create_connected.registries.CCColorHandlers;
import com.hlysine.create_connected.registries.CCDisplaySources;
import com.hlysine.create_connected.registries.CCItemTooltips;
import com.hlysine.create_connected.registries.CCMvpBlockEntityRenders;
import com.hlysine.create_connected.registries.CCModels;
import com.hlysine.create_connected.registries.CCPartialModels;
import com.zurrtum.create.api.behaviour.BlockEntityBehaviour;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.minecraft.client.resources.language.I18n;

public class CreateConnectedClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        CCConfigsClient.register();
        OverstressClutchBlockEntityClient.register();
        // Replaces the removed direct client-only-class reference DashboardBlockEntity used to make
        // from common code - see DashboardBlockEntity.localPlayerHook for the full writeup.
        DashboardBlockEntity.localPlayerHook = ClientPlayerAccess::getPlayer;
        Instruction.i18nExistsHook = I18n::exists;
        CCDisplaySources.KINETIC_BATTERY.attachRender = new KineticBatteryDisplaySourceRender();
        CCPartialModels.register();
        CCModels.register();
        CCMvpBlockEntityRenders.register();
        CCItemTooltips.register();
        ColorProviderRegistry.BLOCK.register(CCColorHandlers.waterBlockTint(), CCBlocks.FAN_SPLASHING_CATALYST);
        // MVP no-ops: custom block-entity rendering, kinetic-battery item predicates,
        // the sequenced-pulse-generator screen and Ponder scenes
        // are excluded in build.gradle until their 1.21.11 client API ports are complete.
        BlockEntityBehaviour.addClient(CCBlockEntityTypes.KINETIC_BATTERY, KineticBatteryTooltipBehaviour::new);
        BlockEntityBehaviour.addClient(CCBlockEntityTypes.OVERSTRESS_CLUTCH, OverstressClutchTooltipBehaviour::new);
    }
}
