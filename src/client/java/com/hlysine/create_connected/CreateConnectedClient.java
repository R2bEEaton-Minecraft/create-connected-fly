package com.hlysine.create_connected;

import com.hlysine.create_connected.config.CCConfigsClient;
import com.hlysine.create_connected.content.contraption.jukebox.PlayContraptionJukeboxPacketClient;
import com.hlysine.create_connected.content.dashboard.ClientPlayerAccess;
import com.hlysine.create_connected.content.dashboard.DashboardBlockEntity;
import com.hlysine.create_connected.content.fluidvessel.FluidVesselTooltipBehaviour;
import com.hlysine.create_connected.content.kineticbattery.KineticBatteryDisplaySourceRender;
import com.hlysine.create_connected.content.kineticbattery.KineticBatteryOverrides;
import com.hlysine.create_connected.content.kineticbattery.KineticBatteryTooltipBehaviour;
import com.hlysine.create_connected.content.kineticbattery.KineticBatteryValueBox;
import com.hlysine.create_connected.content.kineticbridge.KineticBridgeBlockItemClient;
import com.hlysine.create_connected.content.overstressclutch.OverstressClutchBlockEntityClient;
import com.hlysine.create_connected.content.overstressclutch.OverstressClutchTooltipBehaviour;
import com.hlysine.create_connected.content.sequencedpulsegenerator.SequencedPulseGeneratorBlock;
import com.hlysine.create_connected.content.sequencedpulsegenerator.SequencedPulseGeneratorBlockClient;
import com.hlysine.create_connected.content.sequencedpulsegenerator.instructions.Instruction;
import com.hlysine.create_connected.registries.CCBlockEntityRenders;
import com.hlysine.create_connected.registries.CCBlockEntityTypes;
import com.hlysine.create_connected.registries.CCBlocks;
import com.hlysine.create_connected.registries.CCDisplaySources;
import com.hlysine.create_connected.registries.CCModels;
import com.hlysine.create_connected.registries.CCPonderPlugin;
import com.zurrtum.create.api.behaviour.BlockEntityBehaviour;
import com.zurrtum.create.client.foundation.blockEntity.behaviour.scrollValue.RotationDirectionScrollBehaviour;
import com.zurrtum.create.client.ponder.foundation.PonderIndex;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

// registries.CCPartialModels doesn't need an explicit register() call beyond its own static field
// init (PartialModel.of(...) instances get discovered the same way Create Fly's own do - no
// separate registry step) - see PORTING_NOTES.md for the ponder plugin wiring this session added.
public class CreateConnectedClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        CCConfigsClient.register();
        KineticBridgeBlockItemClient.register();
        OverstressClutchBlockEntityClient.register();
        // Replaces the removed direct client-only-class reference DashboardBlockEntity used to make
        // from common code - see DashboardBlockEntity.localPlayerHook for the full writeup.
        DashboardBlockEntity.localPlayerHook = ClientPlayerAccess::getPlayer;
        PlayContraptionJukeboxPacketClient.register();
        Instruction.i18nExistsHook = I18n::exists;
        CCDisplaySources.KINETIC_BATTERY.attachRender = new KineticBatteryDisplaySourceRender();
        CCBlockEntityRenders.register();
        KineticBatteryOverrides.registerModelOverridesClient(CCBlocks.KINETIC_BATTERY_ITEM);
        SequencedPulseGeneratorBlock.displayScreenHook = SequencedPulseGeneratorBlockClient::displayScreen;
        CCModels.register();
        PonderIndex.addPlugin(new CCPonderPlugin());
        BlockEntityBehaviour.addClient(CCBlockEntityTypes.FLUID_VESSEL, FluidVesselTooltipBehaviour::new);
        // Reuses Create Fly's own RotationDirectionScrollBehaviour directly (it's a generic
        // ScrollOptionBehaviour<WindmillBearingBlockEntity.RotationDirection> that only needs a
        // SmartBlockEntity/label/ValueBoxTransform, matching this mod's own KineticBatteryBlockEntity
        // exactly - no need to write a duplicate icon-mapping enum, see PORTING_NOTES.md).
        BlockEntityBehaviour.addClient(CCBlockEntityTypes.KINETIC_BATTERY, be -> new RotationDirectionScrollBehaviour(
                be,
                Component.translatable(CreateConnected.MODID + ".battery.rotation_direction"),
                new KineticBatteryValueBox(3)
        ));
        BlockEntityBehaviour.addClient(CCBlockEntityTypes.KINETIC_BATTERY, KineticBatteryTooltipBehaviour::new);
        BlockEntityBehaviour.addClient(CCBlockEntityTypes.OVERSTRESS_CLUTCH, OverstressClutchTooltipBehaviour::new);
    }
}
