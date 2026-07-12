package com.hlysine.create_connected.content.contraption.jukebox;

import com.zurrtum.create.content.contraptions.AbstractContraptionEntity;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.JukeboxSong;

import java.util.Optional;

public class PlayContraptionJukeboxPacketClient {
    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(PlayContraptionJukeboxPacket.TYPE, (payload, context) ->
                context.client().execute(() -> handle(payload, context.client())));
    }

    private static void handle(PlayContraptionJukeboxPacket payload, net.minecraft.client.Minecraft minecraft) {
        ClientLevel world = minecraft.level;
        if (world == null || !world.dimension().identifier().equals(payload.level()))
            return;
        if (!world.isLoaded(payload.worldPos()))
            return;
        Entity entity = world.getEntity(payload.contraptionId());
        if (!(entity instanceof AbstractContraptionEntity contraptionEntity))
            return;
        if (payload.play()) {
            Optional<JukeboxSong> song = world.registryAccess()
                    .registryOrThrow(Registries.JUKEBOX_SONG)
                    .getHolder(payload.recordId())
                    .map(Holder.Reference::value);
            if (song.isEmpty())
                return;
            ContraptionMusicManager.playContraptionMusic(
                    song.get(),
                    contraptionEntity,
                    payload.contraptionPos(),
                    payload.worldPos(),
                    payload.silent()
            );
        } else {
            ContraptionMusicManager.playContraptionMusic(
                    null,
                    contraptionEntity,
                    payload.contraptionPos(),
                    payload.worldPos(),
                    payload.silent()
            );
        }
    }
}
