package com.hlysine.create_connected.compat;

// Create Aeronautics / "Simulated" has no Fabric build for MC 1.21.11 (verified via Modrinth),
// so there is no dev.simulated_team.simulated.* artifact to compile this integration against.
// Mods.SIMULATED.isLoaded() will always be false on this platform+version, so this being a
// no-op matches actual runtime behavior. content/linkedtransmitter/LinkedThrottleLeverBlock(Entity)
// (Renderer) already exist and are otherwise self-contained - re-add the direct-registration
// equivalent of the original REGISTRATE.block("linked_throttle_lever", ...) chain here (see
// registries/CCBlocks.java's LINKED_LEVER/LINKED_ANALOG_LEVER for the pattern) once Simulated
// ships a compatible build and its real SimBlocks.THROTTLE_LEVER type can be referenced again.
public class SimCompatRegistry {
    public static void register() {
    }
}
