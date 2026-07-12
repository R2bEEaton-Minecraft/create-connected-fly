package com.hlysine.create_connected.content.copycat.block;

import com.hlysine.create_connected.content.copycat.ICopycatWithWrappedBlock;
import com.hlysine.create_connected.content.copycat.MigratingCopycatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

public class CopycatBlockBlock extends MigratingCopycatBlock implements ICopycatWithWrappedBlock {

    public CopycatBlockBlock(Properties properties) {
        super(properties);
    }

    @Override
    public Block getWrappedBlock() {
        return Blocks.STONE;
    }

    @Override
    public boolean canConnectTexturesToward(BlockAndTintGetter reader, BlockPos fromPos, BlockPos toPos, BlockState state) {
        return true;
    }

    @Override
    public boolean canFaceBeOccluded(BlockState state, Direction face) {
        return true;
    }

    @SuppressWarnings("deprecation")
    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState pState, @NotNull BlockGetter pLevel, @NotNull BlockPos pPos, @NotNull CollisionContext pContext) {
        return Shapes.block();
    }

    // hidesNeighborFace(BlockGetter, BlockPos, BlockState, BlockState, Direction) does not exist anywhere
    // in this Fabric API surface (confirmed via javap; see CopycatWallBlock.java for the full writeup) -
    // a NeoForge-only IBlockExtension face-culling hook with no Fabric replacement. Feature reduction:
    // adjacent copycat blocks sharing the same material will render their shared internal faces instead
    // of culling them (a fill-rate optimization loss only).
}

