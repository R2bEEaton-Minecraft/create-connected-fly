package com.hlysine.create_connected.compat;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * For compatibility with and without another mod present, we have to define load conditions of the specific code
 */
public enum Mods {
    JEI("jei"),
    COPYCATS("copycats"),
    DIAGONAL_FENCES("diagonalfences"),
    DREAMS_DESIRES("dndesires"),
    NUCLEAR("createnuclear"),
    ADDITIONAL_PLACEMENTS("additionalplacements"),
    GARNISHED("garnished"),
    DRAGONS_PLUS("create_dragons_plus"),
    STEAM_N_RAILS("railways"),
    MORE_CATALYSTS("create_more_catalysts"),
    SHIMMER("create_shimmer"),
    NETHER_INDUSTRY("createnetherindustry"),
    TWILIGHT_FOREST("twilightforest"),
    SIMULATED("simulated"),
    DYE_DEPOT("dye_depot");

    private final String id;

    Mods(String id) {
        this.id = id;
    }

    /**
     * @return the mod id
     */
    public String id() {
        return id;
    }

    public Identifier rl(String path) {
        return Identifier.fromNamespaceAndPath(id, path);
    }

    // Registry.get(Identifier) now returns Optional<Holder.Reference<T>> instead of a plain T
    // (confirmed via javap) - unwrapped with .map(Holder.Reference::value), falling back to AIR to
    // preserve the old behavior of returning a non-null sentinel for unregistered ids.
    public Item getItem(String id) {
        return BuiltInRegistries.ITEM.get(rl(id)).map(net.minecraft.core.Holder.Reference::value).orElse(Items.AIR);
    }

    public Item getItem(Identifier id) {
        return BuiltInRegistries.ITEM.get(id).map(net.minecraft.core.Holder.Reference::value).orElse(Items.AIR);
    }

    /**
     * @return a boolean of whether the mod is loaded or not based on mod id
     */
    public boolean isLoaded() {
        return FabricLoader.getInstance().isModLoaded(id);
    }

    /**
     * Simple hook to run code if a mod is installed
     *
     * @param toRun will be run only if the mod is loaded
     * @return Optional.empty() if the mod is not loaded, otherwise an Optional of the return value of the given supplier
     */
    public <T> Optional<T> runIfInstalled(Supplier<Supplier<T>> toRun) {
        if (isLoaded())
            return Optional.of(toRun.get().get());
        return Optional.empty();
    }

    /**
     * Simple hook to execute code if a mod is installed
     *
     * @param toExecute will be executed only if the mod is loaded
     */
    public void executeIfInstalled(Supplier<Runnable> toExecute) {
        if (isLoaded()) {
            toExecute.get().run();
        }
    }
}

