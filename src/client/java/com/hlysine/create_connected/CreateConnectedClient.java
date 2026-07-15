package com.hlysine.create_connected;

import com.hlysine.create_connected.config.CCConfigsClient;
import com.hlysine.create_connected.content.ClutchValueBox;
import com.hlysine.create_connected.content.dashboard.ClientPlayerAccess;
import com.hlysine.create_connected.content.centrifugalclutch.CentrifugalClutchScrollValueBehaviour;
import com.hlysine.create_connected.content.contraption.jukebox.PlayContraptionJukeboxPacketClient;
import com.hlysine.create_connected.content.dashboard.DashboardBlockEntity;
import com.hlysine.create_connected.content.fluidvessel.FluidVesselTooltipBehaviour;
import com.hlysine.create_connected.content.kineticbattery.KineticBatteryDisplaySourceRender;
import com.hlysine.create_connected.content.kineticbattery.KineticBatteryTooltipBehaviour;
import com.hlysine.create_connected.content.kineticbridge.KineticBridgeBlockItemClient;
import com.hlysine.create_connected.content.kineticbridge.StressImpactScrollValueBehaviour;
import com.hlysine.create_connected.content.overstressclutch.OverstressClutchBlockEntityClient;
import com.hlysine.create_connected.content.overstressclutch.OverstressClutchTooltipBehaviour;
import com.hlysine.create_connected.content.overstressclutch.OverstressClutchScrollValueBehaviour;
import com.hlysine.create_connected.content.sequencedpulsegenerator.SequencedPulseGeneratorBlock;
import com.hlysine.create_connected.content.sequencedpulsegenerator.SequencedPulseGeneratorBlockClient;
import com.hlysine.create_connected.content.sequencedpulsegenerator.instructions.Instruction;
import com.hlysine.create_connected.registries.CCBlockEntityTypes;
import com.hlysine.create_connected.registries.CCBlocks;
import com.hlysine.create_connected.registries.CCColorHandlers;
import com.hlysine.create_connected.registries.CCDisplaySources;
import com.hlysine.create_connected.registries.CCItemTooltips;
import com.hlysine.create_connected.registries.CCBlockEntityRenders;
import com.hlysine.create_connected.registries.CCModels;
import com.hlysine.create_connected.registries.CCPartialModels;
import com.hlysine.create_connected.registries.CCPonderPlugin;
import com.zurrtum.create.api.behaviour.BlockEntityBehaviour;
import com.zurrtum.create.AllBlockEntityTypes;
import com.zurrtum.create.client.content.redstone.link.ConnectedAnalogLeverLinkBehaviour;
import com.zurrtum.create.client.content.redstone.link.ConnectedLinkBehaviour;
import com.zurrtum.create.client.foundation.blockEntity.behaviour.scrollValue.RotationDirectionScrollBehaviour;
import com.zurrtum.create.client.foundation.blockEntity.behaviour.tooltip.AnalogLeverTooltipBehaviour;
import com.zurrtum.create.client.foundation.blockEntity.behaviour.tooltip.ChuteTooltipBehaviour;
import com.zurrtum.create.client.ponder.foundation.PonderIndex;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

public class CreateConnectedClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        CCConfigsClient.register();
        OverstressClutchBlockEntityClient.register();
        // Replaces the removed direct client-only-class reference DashboardBlockEntity used to make
        // from common code - see DashboardBlockEntity.localPlayerHook for the full writeup.
        DashboardBlockEntity.localPlayerHook = ClientPlayerAccess::getPlayer;
        SequencedPulseGeneratorBlock.displayScreenHook = SequencedPulseGeneratorBlockClient::displayScreen;
        Instruction.i18nExistsHook = I18n::exists;
        KineticBridgeBlockItemClient.register();
        PlayContraptionJukeboxPacketClient.register();
        CCDisplaySources.KINETIC_BATTERY.attachRender = new KineticBatteryDisplaySourceRender();
        CCPartialModels.register();
        CCModels.register();
        CCBlockEntityRenders.register();
        CCItemTooltips.register();
        PonderIndex.addPlugin(new CCPonderPlugin());
        ColorProviderRegistry.BLOCK.register(CCColorHandlers.waterBlockTint(), CCBlocks.FAN_SPLASHING_CATALYST);
        BlockEntityBehaviour.addClient(CCBlockEntityTypes.KINETIC_BATTERY, KineticBatteryTooltipBehaviour::new);
        BlockEntityBehaviour.addClient(CCBlockEntityTypes.BRASS_CHUTE, ChuteTooltipBehaviour::new);
        BlockEntityBehaviour.addClient(CCBlockEntityTypes.FLUID_VESSEL, FluidVesselTooltipBehaviour::new);
        BlockEntityBehaviour.addClient(CCBlockEntityTypes.CREATIVE_FLUID_VESSEL, FluidVesselTooltipBehaviour::new);
        BlockEntityBehaviour.addClient(CCBlockEntityTypes.KINETIC_BRIDGE, StressImpactScrollValueBehaviour::new);
        BlockEntityBehaviour.addClient(CCBlockEntityTypes.OVERSTRESS_CLUTCH, OverstressClutchTooltipBehaviour::new);
        BlockEntityBehaviour.addClient(CCBlockEntityTypes.LINKED_TRANSMITTER, ConnectedLinkBehaviour::new);
        // LinkedAnalogLeverBlockEntity retags itself to LINKED_ANALOG_LEVER (see its constructor),
        // so these registrations key correctly; Create Fly's own ANALOG_LEVER registrations no
        // longer reach linked levers, hence the tooltip behaviour is re-added here.
        BlockEntityBehaviour.addClient(CCBlockEntityTypes.LINKED_ANALOG_LEVER, ConnectedAnalogLeverLinkBehaviour::new);
        BlockEntityBehaviour.addClient(CCBlockEntityTypes.LINKED_ANALOG_LEVER, AnalogLeverTooltipBehaviour::new);
        BlockEntityBehaviour.addClient(CCBlockEntityTypes.CENTRIFUGAL_CLUTCH,
                CentrifugalClutchScrollValueBehaviour::new);
        BlockEntityBehaviour.addClient(CCBlockEntityTypes.FREEWHEEL_CLUTCH,
                be -> new RotationDirectionScrollBehaviour(
                        be,
                        Component.translatable("create.contraptions.windmill.rotation_direction"),
                        new ClutchValueBox()));
        BlockEntityBehaviour.addClient(CCBlockEntityTypes.OVERSTRESS_CLUTCH,
                OverstressClutchScrollValueBehaviour::new);
    }
}
