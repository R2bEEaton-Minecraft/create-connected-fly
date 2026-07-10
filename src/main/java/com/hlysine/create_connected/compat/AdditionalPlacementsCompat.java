package com.hlysine.create_connected.compat;

// Additional Placements Fabric has no build for MC 1.21.11 (verified: Modrinth tops out at
// 1.21.3-1.21.5-fabric as of this port), so there is no artifact to compile this integration
// against. Mods.ADDITIONAL_PLACEMENTS.isLoaded() will always be false on this platform+version,
// so this being a no-op matches actual runtime behavior; re-implement register() against
// com.firemerald.additionalplacements.generation.* once a compatible build exists.
public class AdditionalPlacementsCompat {
    public static void register() {
    }
}
