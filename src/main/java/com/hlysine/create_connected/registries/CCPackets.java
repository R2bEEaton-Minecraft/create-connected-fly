package com.hlysine.create_connected.registries;

import com.hlysine.create_connected.content.contraption.jukebox.PlayContraptionJukeboxPacket;
import com.hlysine.create_connected.content.sequencedpulsegenerator.ConfigureSequencedPulseGeneratorPacket;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

// Create Fly's own com.zurrtum.create.catnip.net.base.* networking base classes
// (BasePacketPayload/CatnipPacketRegistry/ClientboundPacketPayload/BlockEntityConfigurationPacket)
// don't exist at all in this version (verified: absent from the sources jar entirely, not just
// relocated) - this mod's packets are now plain vanilla CustomPacketPayload records registered
// directly via Fabric's PayloadTypeRegistry, matching the pattern config/SyncConfigBase.java used
// in session 1. The server-bound ConfigureSequencedPulseGeneratorPacket's receiver is registered
// here (main, both sides need the payload type registered); the client-bound
// PlayContraptionJukeboxPacket's receiver is registered client-side (see
// content/contraption/jukebox/PlayContraptionJukeboxPacketClient.java).
public class CCPackets {
    public static void register() {
        PayloadTypeRegistry.playS2C().register(PlayContraptionJukeboxPacket.TYPE, PlayContraptionJukeboxPacket.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(ConfigureSequencedPulseGeneratorPacket.TYPE, ConfigureSequencedPulseGeneratorPacket.STREAM_CODEC);

        ServerPlayNetworking.registerGlobalReceiver(ConfigureSequencedPulseGeneratorPacket.TYPE, (payload, context) ->
                payload.handle(context.player()));
    }
}
