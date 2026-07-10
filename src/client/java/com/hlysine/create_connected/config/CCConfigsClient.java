package com.hlysine.create_connected.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class CCConfigsClient {
    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(SyncConfigBase.SyncConfigPayload.TYPE, (payload, context) -> {
            JsonObject object = JsonParser.parseString(payload.json()).getAsJsonObject();
            context.client().execute(() -> CCConfigs.common().setSyncConfig(object));
        });
    }
}
