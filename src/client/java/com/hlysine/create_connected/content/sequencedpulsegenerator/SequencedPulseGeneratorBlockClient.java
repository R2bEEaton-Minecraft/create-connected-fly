package com.hlysine.create_connected.content.sequencedpulsegenerator;

import com.zurrtum.create.client.catnip.gui.ScreenOpener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;

// Client-only counterpart of SequencedPulseGeneratorBlock's screen-opening logic. Registered via
// SequencedPulseGeneratorBlock.displayScreenHook = SequencedPulseGeneratorBlockClient::displayScreen
// in CreateConnectedClient.onInitializeClient().
public class SequencedPulseGeneratorBlockClient {
    public static void displayScreen(SequencedPulseGeneratorBlockEntity be, Player player) {
        if (player instanceof LocalPlayer)
            ScreenOpener.open(new SequencedPulseGeneratorScreen(be));
    }
}
