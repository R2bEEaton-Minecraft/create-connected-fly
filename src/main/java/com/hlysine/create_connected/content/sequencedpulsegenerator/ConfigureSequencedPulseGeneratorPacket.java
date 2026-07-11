package com.hlysine.create_connected.content.sequencedpulsegenerator;

import com.hlysine.create_connected.CreateConnected;
import com.hlysine.create_connected.content.sequencedpulsegenerator.instructions.Instruction;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;

// Real vanilla CustomPacketPayload (C2S) replacing Create Fly's now-nonexistent
// foundation.networking.BlockEntityConfigurationPacket base class - see PORTING_NOTES.md. The
// distance/block-entity-type check that base class used to provide is inlined into handle().
public record ConfigureSequencedPulseGeneratorPacket(BlockPos pos, Tag instructions) implements CustomPacketPayload {
    public static final Type<ConfigureSequencedPulseGeneratorPacket> TYPE = new Type<>(CreateConnected.asResource("configure_sequencer"));
    private static final int MAX_RANGE = 16;

    public static final StreamCodec<RegistryFriendlyByteBuf, ConfigureSequencedPulseGeneratorPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, ConfigureSequencedPulseGeneratorPacket::pos,
            ByteBufCodecs.TAG, ConfigureSequencedPulseGeneratorPacket::instructions,
            ConfigureSequencedPulseGeneratorPacket::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(ServerPlayer player) {
        if (player == null) return;
        if (!player.level().isLoaded(pos)) return;
        if (player.position().distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > (double) (MAX_RANGE * MAX_RANGE))
            return;
        BlockEntity be = player.level().getBlockEntity(pos);
        if (!(be instanceof SequencedPulseGeneratorBlockEntity generatorBE))
            return;
        generatorBE.currentInstruction = -1;
        generatorBE.instructions = Instruction.deserializeAll((ListTag) instructions);
        generatorBE.reset();
    }
}
