package com.hlysine.create_connected.content.itemsilo;

import com.zurrtum.create.client.AllSpriteShifts;
import com.zurrtum.create.api.connectivity.ConnectivityHandler;
import com.zurrtum.create.client.foundation.block.connected.CTSpriteShiftEntry;
import com.zurrtum.create.client.foundation.block.connected.ConnectedTextureBehaviour;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class ItemSiloCTBehaviour extends ConnectedTextureBehaviour.Base {

    @Override
    public CTSpriteShiftEntry getShift(BlockState state, Direction direction, @Nullable TextureAtlasSprite sprite) {
        Axis vaultBlockAxis = ItemSiloBlock.getVaultBlockAxis(state);
        boolean small = !ItemSiloBlock.isLarge(state);
        if (vaultBlockAxis == null)
            return null;

        if (direction.getAxis() == vaultBlockAxis)
            return AllSpriteShifts.VAULT_FRONT.get(small);
        if (direction == Direction.UP)
            return AllSpriteShifts.VAULT_TOP.get(small);
        if (direction == Direction.DOWN)
            return AllSpriteShifts.VAULT_BOTTOM.get(small);

        // The silo model deliberately maps vault_top_small onto its vertical sides. Use the matching
        // shift entry; returning VAULT_SIDE here cannot transform those quads because its source
        // sprite is vault_side_small.
        return AllSpriteShifts.VAULT_TOP.get(small);
    }

    public boolean buildContextForOccludedDirections() {
        return true;
    }

    @Override
    protected Direction getUpDirection(BlockAndTintGetter reader, BlockPos pos, BlockState state, Direction face) {
        // The silo is a vault whose multiblock axis is vertical. On its four side faces, texture-grid
        // vertical must therefore follow world Y so stacked layers participate in the CT context.
        if (face.getAxis().isHorizontal())
            return Direction.UP;
        return super.getUpDirection(reader, pos, state, face);
    }

    @Override
    public boolean connectsTo(BlockState state, BlockState other, BlockAndTintGetter reader, BlockPos pos,
                              BlockPos otherPos, Direction face) {
        return state.getBlock() == other.getBlock() && ConnectivityHandler.isConnected(reader, pos, otherPos);
    }

}

