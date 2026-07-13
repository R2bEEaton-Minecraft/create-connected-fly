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
import com.hlysine.create_connected.registries.CCDisplaySources;
import com.zurrtum.create.api.behaviour.BlockEntityBehaviour;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.resources.language.I18n;

// registries.CCPartialModels doesn't need an explicit register() call beyond its own static field
// init (PartialModel.of(...) instances get discovered the same way Create Fly's own do - no
// separate registry step) - see PORTING_NOTES.md for the ponder plugin wiring this session added.
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
        // MVP no-ops: custom block-entity rendering, kinetic-battery item predicates,
        // the sequenced-pulse-generator screen, copycat model wrappers, and Ponder scenes
        // are excluded in build.gradle until their 1.21.11 client API ports are complete.
        BlockEntityBehaviour.addClient(CCBlockEntityTypes.KINETIC_BATTERY, KineticBatteryTooltipBehaviour::new);
        BlockEntityBehaviour.addClient(CCBlockEntityTypes.OVERSTRESS_CLUTCH, OverstressClutchTooltipBehaviour::new);
    }
}
