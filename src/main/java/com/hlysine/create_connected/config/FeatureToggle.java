package com.hlysine.create_connected.config;

import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public class FeatureToggle {
    public static final Set<Identifier> TOGGLEABLE_FEATURES = new HashSet<>();
    public static final Map<Identifier, Identifier> DEPENDENT_FEATURES = new HashMap<>();
    public static final Map<Identifier, Set<FeatureCategory>> FEATURE_CATEGORIES = new HashMap<>();
    public static final Map<Identifier, Supplier<Boolean>> FEATURE_CONDITIONS = new HashMap<>();

    public static void register(Identifier key) {
        TOGGLEABLE_FEATURES.add(key);
    }

    public static void register(Identifier key, FeatureCategory... categories) {
        register(key);
        FEATURE_CATEGORIES.put(key, Set.of(categories));
    }

    public static void registerDependent(Identifier key, Identifier dependency) {
        DEPENDENT_FEATURES.put(key, dependency);
    }

    public static void registerDependent(Identifier key, Identifier dependency, FeatureCategory... categories) {
        registerDependent(key, dependency);
        FEATURE_CATEGORIES.put(key, Set.of(categories));
    }

    public static void addCondition(Identifier key, Supplier<Boolean> condition) {
        FEATURE_CONDITIONS.put(key, condition);
    }

    private static CFeatures getToggles() {
        return CCConfigs.common().toggle;
    }

    private static CFeatureCategories getCategories() {
        return CCConfigs.common().categories;
    }

    /**
     * Check whether a feature is enabled.
     * If the provided {@link Identifier} is not registered with this feature toggle, it is assumed to be enabled.
     *
     * @param key The {@link Identifier} of the feature.
     * @return Whether the feature is enabled.
     */
    public static boolean isEnabled(Identifier key) {
        if (FEATURE_CATEGORIES.containsKey(key)) {
            Set<FeatureCategory> categories = FEATURE_CATEGORIES.get(key);
            for (FeatureCategory category : categories) {
                if (!getCategories().isEnabled(category)) return false;
            }
        }
        if (FEATURE_CONDITIONS.containsKey(key)) {
            if (!FEATURE_CONDITIONS.get(key).get()) return false;
        }
        if (getToggles().hasToggle(key)) {
            return getToggles().isEnabled(key);
        } else {
            Identifier dependency = DEPENDENT_FEATURES.get(key);
            if (dependency != null) return isEnabled(dependency);
        }
        return true;
    }

    // populated by CreateConnectedClient on the client physical side only; the config
    // sync handler and CFeatures/CFeatureCategories live in the common source set, but
    // rebuilding creative tab contents and refreshing JEI's item list are client-only
    public static Runnable clientRefreshHook = () -> {
    };

    static void refreshItemVisibility() {
        clientRefreshHook.run();
    }
}
