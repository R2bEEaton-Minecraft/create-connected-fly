package com.hlysine.create_connected.content.contraption.noteblock;

import com.zurrtum.create.content.contraptions.AbstractContraptionEntity;
import com.zurrtum.create.content.contraptions.Contraption;
import com.zurrtum.create.content.contraptions.behaviour.SimpleBlockMovingInteraction;
import net.minecraft.core.BlockPos;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;

import static net.minecraft.world.level.block.NoteBlock.INSTRUMENT;
import static net.minecraft.world.level.block.state.properties.BlockStateProperties.NOTE;

// NeoForge's CommonHooks.onNoteChange (a NeoForge-only event hook letting other mods
// veto/modify note-block pitch changes) has no Fabric equivalent - simplified to the direct
// vanilla cycle behavior. Only loses other-mod interception of this specific interaction, not
// this mod's own note-cycling functionality.
public class NoteBlockInteractionBehaviour extends SimpleBlockMovingInteraction {

    @Override
    protected BlockState handle(Player player, Contraption contraption, BlockPos contraptionPos, BlockState currentState) {
        AbstractContraptionEntity contraptionEntity = contraption.entity;
        Level contraptionWorld = contraption.getContraptionWorld();
        Level realWorld = player.level();
        BlockPos realPos = BlockPos.containing(contraptionEntity.toGlobalVector(Vec3.atCenterOf(contraptionPos), 1));
        currentState = currentState.cycle(NOTE);

        if (currentState.getValue(INSTRUMENT).worksAboveNoteBlock() || contraptionWorld.getBlockState(contraptionPos.above()).isAir()) {
            currentState.triggerEvent(realWorld, realPos, 0, 0);
            realWorld.gameEvent(player, GameEvent.NOTE_BLOCK_PLAY, realPos);
        }

        player.awardStat(Stats.TUNE_NOTEBLOCK);
        return currentState;
    }
}
