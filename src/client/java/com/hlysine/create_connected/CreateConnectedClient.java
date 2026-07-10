package com.hlysine.create_connected;

import com.hlysine.create_connected.config.CCConfigsClient;
import com.hlysine.create_connected.content.kineticbridge.KineticBridgeBlockItemClient;
import com.hlysine.create_connected.content.overstressclutch.OverstressClutchBlockEntityClient;
import net.fabricmc.api.ClientModInitializer;

// registries.CCPartialModels/CCPonderPlugin wiring deferred until the mixin/ and content/
// conversion passes land (see PORTING_NOTES.md) - those still reference unconverted classes.
public class CreateConnectedClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        CCConfigsClient.register();
        KineticBridgeBlockItemClient.register();
        OverstressClutchBlockEntityClient.register();
    }
}
