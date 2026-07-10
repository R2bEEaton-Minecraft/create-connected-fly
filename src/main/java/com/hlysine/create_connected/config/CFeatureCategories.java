package com.hlysine.create_connected.config;

import com.google.gson.JsonObject;
import com.zurrtum.create.catnip.config.Builder;
import com.zurrtum.create.catnip.config.ConfigValue;
import org.jetbrains.annotations.ApiStatus;

import java.util.HashMap;
import java.util.Map;

/**
 * Configures all feature categories.
 * Values in this class should NOT be accessed directly. Please access via {@link FeatureToggle} instead.
 */
public class CFeatureCategories extends SyncConfigBase {

    @Override
    public String getName() {
        return "feature_categories";
    }

    final Map<FeatureCategory, ConfigValue<Boolean>> toggles = new HashMap<>();

    Map<FeatureCategory, Boolean> synchronizedToggles;

    @Override
    public void registerAll(Builder builder) {
        for (FeatureCategory r : FeatureCategory.values()) {
            builder.comment(r.getDescription());
            toggles.put(r, builder.define(r.getSerializedName(), true));
        }
    }

    @ApiStatus.Internal
    public boolean isEnabled(FeatureCategory category) {
        if (this.synchronizedToggles != null) {
            Boolean synced = synchronizedToggles.get(category);
            if (synced != null) return synced;
        }
        ConfigValue<Boolean> value = toggles.get(category);
        if (value != null)
            return value.get();
        return true;
    }

    @Override
    protected void readSyncConfig(JsonObject json) {
        synchronizedToggles = new HashMap<>();
        for (String key : json.keySet()) {
            FeatureCategory category = FeatureCategory.byName(key);
            synchronizedToggles.put(category, json.get(key).getAsBoolean());
        }
        FeatureToggle.refreshItemVisibility();
    }

    @Override
    protected void writeSyncConfig(JsonObject json) {
        toggles.forEach((key, value) -> json.addProperty(key.getSerializedName(), value.get()));
    }
}
