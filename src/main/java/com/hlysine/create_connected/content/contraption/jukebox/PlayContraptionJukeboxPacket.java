package com.hlysine.create_connected.content.contraption.jukebox;

import com.hlysine.create_connected.CreateConnected;
import com.mojang.datafixers.util.Function7;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

// Real vanilla CustomPacketPayload (S2C) replacing Create Fly's now-nonexistent
// catnip.net.base.ClientboundPacketPayload - see PORTING_NOTES.md. The client-side handler lives
// in src/client/java's PlayContraptionJukeboxPacketClient, registered directly via
// ClientPlayNetworking (CreateConnectedClient.onInitializeClient()).
public record PlayContraptionJukeboxPacket(Identifier level, int contraptionId, BlockPos contraptionPos, BlockPos worldPos,
                                            int recordId, boolean play, boolean silent) implements CustomPacketPayload {
    public static final Type<PlayContraptionJukeboxPacket> TYPE = new Type<>(CreateConnected.asResource("play_contraption_jukebox"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PlayContraptionJukeboxPacket> STREAM_CODEC = composite(
            Identifier.STREAM_CODEC, PlayContraptionJukeboxPacket::level,
            ByteBufCodecs.VAR_INT, PlayContraptionJukeboxPacket::contraptionId,
            BlockPos.STREAM_CODEC, PlayContraptionJukeboxPacket::contraptionPos,
            BlockPos.STREAM_CODEC, PlayContraptionJukeboxPacket::worldPos,
            ByteBufCodecs.VAR_INT, PlayContraptionJukeboxPacket::recordId,
            ByteBufCodecs.BOOL, PlayContraptionJukeboxPacket::play,
            ByteBufCodecs.BOOL, PlayContraptionJukeboxPacket::silent,
            PlayContraptionJukeboxPacket::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static <B, C, T1, T2, T3, T4, T5, T6, T7> StreamCodec<B, C> composite(
            final StreamCodec<? super B, T1> codec1,
            final Function<C, T1> getter1,
            final StreamCodec<? super B, T2> codec2,
            final Function<C, T2> getter2,
            final StreamCodec<? super B, T3> codec3,
            final Function<C, T3> getter3,
            final StreamCodec<? super B, T4> codec4,
            final Function<C, T4> getter4,
            final StreamCodec<? super B, T5> codec5,
            final Function<C, T5> getter5,
            final StreamCodec<? super B, T6> codec6,
            final Function<C, T6> getter6,
            final StreamCodec<? super B, T7> codec7,
            final Function<C, T7> getter7,
            final Function7<T1, T2, T3, T4, T5, T6, T7, C> factory
    ) {
        return new StreamCodec<B, C>() {
            @SuppressWarnings("unchecked")
            public @NotNull C decode(@NotNull B from) {
                T1 t1 = (T1) codec1.decode(from);
                T2 t2 = (T2) codec2.decode(from);
                T3 t3 = (T3) codec3.decode(from);
                T4 t4 = (T4) codec4.decode(from);
                T5 t5 = (T5) codec5.decode(from);
                T6 t6 = (T6) codec6.decode(from);
                T7 t7 = (T7) codec7.decode(from);
                return factory.apply(t1, t2, t3, t4, t5, t6, t7);
            }

            public void encode(@NotNull B from, @NotNull C to) {
                codec1.encode(from, getter1.apply(to));
                codec2.encode(from, getter2.apply(to));
                codec3.encode(from, getter3.apply(to));
                codec4.encode(from, getter4.apply(to));
                codec5.encode(from, getter5.apply(to));
                codec6.encode(from, getter6.apply(to));
                codec7.encode(from, getter7.apply(to));
            }
        };
    }
}
