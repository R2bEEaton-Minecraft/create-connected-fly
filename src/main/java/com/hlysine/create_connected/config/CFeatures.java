package com.hlysine.create_connected.config;

import com.google.gson.JsonObject;
import com.zurrtum.create.catnip.config.Builder;
import com.zurrtum.create.catnip.config.ConfigValue;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;

public class CFeatures extends SyncConfigBase {

    @Override
    public String getName() {
        return "features";
    }

    final Map<Identifier, ConfigValue<Boolean>> toggles = new HashMap<>();

    Map<Identifier, Boolean> synchronizedToggles;

    @Override
    public void registerAll(Builder builder) {
        FeatureToggle.TOGGLEABLE_FEATURES.forEach((r) -> toggles.put(r, builder.define(r.toString(), true)));
    }

    public boolean hasToggle(Identifier key) {
        return (synchronizedToggles != null && synchronizedToggles.containsKey(key)) || toggles.containsKey(key);
    }

    public boolean isEnabled(Identifier key) {
        if (this.synchronizedToggles != null) {
            Boolean synced = synchronizedToggles.get(key);
            if (synced != null) return synced;
        }
        ConfigValue<Boolean> value = toggles.get(key);
        if (value != null)
            return value.get();
        return true;
    }

    @Override
    protected void readSyncConfig(JsonObject json) {
        synchronizedToggles = new HashMap<>();
        for (String key : json.keySet()) {
            Identifier location = Identifier.parse(key);
            synchronizedToggles.put(location, json.get(key).getAsBoolean());
        }
        FeatureToggle.refreshItemVisibility();
    }

    @Override
    protected void writeSyncConfig(JsonObject json) {
        toggles.forEach((key, value) -> json.addProperty(key.toString(), value.get()));
    }
}
