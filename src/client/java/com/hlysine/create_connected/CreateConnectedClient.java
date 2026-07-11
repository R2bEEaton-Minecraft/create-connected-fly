package com.hlysine.create_connected;

import com.hlysine.create_connected.config.CCConfigsClient;
import com.hlysine.create_connected.content.contraption.jukebox.PlayContraptionJukeboxPacketClient;
import com.hlysine.create_connected.content.kineticbattery.KineticBatteryDisplaySourceRender;
import com.hlysine.create_connected.content.kineticbattery.KineticBatteryOverrides;
import com.hlysine.create_connected.content.kineticbridge.KineticBridgeBlockItemClient;
import com.hlysine.create_connected.content.overstressclutch.OverstressClutchBlockEntityClient;
import com.hlysine.create_connected.content.sequencedpulsegenerator.SequencedPulseGeneratorBlock;
import com.hlysine.create_connected.content.sequencedpulsegenerator.SequencedPulseGeneratorBlockClient;
import com.hlysine.create_connected.content.sequencedpulsegenerator.instructions.Instruction;
import com.hlysine.create_connected.registries.CCBlockEntityRenders;
import com.hlysine.create_connected.registries.CCBlocks;
import com.hlysine.create_connected.registries.CCDisplaySources;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.resources.language.I18n;

// registries.CCPartialModels/CCPonderPlugin wiring deferred until the mixin/ and content/
// conversion passes land (see PORTING_NOTES.md) - those still reference unconverted classes.
public class CreateConnectedClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        CCConfigsClient.register();
        KineticBridgeBlockItemClient.register();
        OverstressClutchBlockEntityClient.register();
        PlayContraptionJukeboxPacketClient.register();
        Instruction.i18nExistsHook = I18n::exists;
        CCDisplaySources.KINETIC_BATTERY.attachRender = new KineticBatteryDisplaySourceRender();
        CCBlockEntityRenders.register();
        KineticBatteryOverrides.registerModelOverridesClient(CCBlocks.KINETIC_BATTERY_ITEM);
        SequencedPulseGeneratorBlock.displayScreenHook = SequencedPulseGeneratorBlockClient::displayScreen;
    }
}
