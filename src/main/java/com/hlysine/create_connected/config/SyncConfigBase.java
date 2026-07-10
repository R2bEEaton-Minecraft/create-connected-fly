package com.hlysine.create_connected.config;

import com.google.gson.JsonObject;
import com.hlysine.create_connected.CreateConnected;
import com.zurrtum.create.catnip.config.ConfigBase;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

// Create Fly's own catnip config is plain JSON with no built-in sync/reload-event hooks
// (unlike the NeoForge ModConfigSpec this was originally written against), so sync is
// implemented here directly on top of Fabric's networking API instead.
public abstract class SyncConfigBase extends ConfigBase {
    public static void registerPayloadType() {
        PayloadTypeRegistry.playS2C().register(SyncConfigPayload.TYPE, SyncConfigPayload.STREAM_CODEC);
    }

    public final JsonObject getSyncConfig() {
        JsonObject json = new JsonObject();
        writeSyncConfig(json);
        if (children != null)
            for (ConfigBase child : children) {
                if (child instanceof SyncConfigBase syncChild) {
                    if (json.has(child.getName()))
                        throw new RuntimeException("A sync config key starts with " + child.getName() + " but does not belong to the child");
                    json.add(child.getName(), syncChild.getSyncConfig());
                }
            }
        return json;
    }

    protected void writeSyncConfig(JsonObject json) {
    }

    public final void setSyncConfig(JsonObject config) {
        if (children != null)
            for (ConfigBase child : children) {
                if (child instanceof SyncConfigBase syncChild) {
                    JsonObject json = config.getAsJsonObject(child.getName());
                    syncChild.readSyncConfig(json == null ? new JsonObject() : json);
                }
            }
        readSyncConfig(config);
    }

    protected void readSyncConfig(JsonObject json) {
    }

    public void syncToPlayer(ServerPlayer player) {
        if (player == null) return;
        CreateConnected.LOGGER.debug("Sync Config: Sending server config to {}", player.getScoreboardName());
        ServerPlayNetworking.send(player, new SyncConfigPayload(getSyncConfig().toString()));
    }

    public record SyncConfigPayload(String json) implements CustomPacketPayload {
        public static final Type<SyncConfigPayload> TYPE = new Type<>(CreateConnected.asResource("sync_config"));

        public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, SyncConfigPayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8,
                SyncConfigPayload::json,
                SyncConfigPayload::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
