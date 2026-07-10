package com.hlysine.create_connected.registries;

import com.hlysine.create_connected.CreateConnected;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.JukeboxSong;

// Jukebox songs are a datapack registry (JSON-driven, not a Java BuiltInRegistries entry) - the
// original NeoForge JsonCodecProvider datagen output is already committed as static JSON at
// src/generated/resources/data/create_connected/jukebox_song/*.json, so no registration code is
// needed here, just the ResourceKey constants referenced elsewhere (e.g. CCItems' jukeboxPlayable).
public class CCJukeboxSongs {
    public static final ResourceKey<JukeboxSong> INTERLUDE = ResourceKey.create(Registries.JUKEBOX_SONG, CreateConnected.asResource("interlude"));
    public static final ResourceKey<JukeboxSong> ELEVATOR = ResourceKey.create(Registries.JUKEBOX_SONG, CreateConnected.asResource("elevator"));
}
