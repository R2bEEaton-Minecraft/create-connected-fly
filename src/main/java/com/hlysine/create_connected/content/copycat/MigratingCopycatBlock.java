package com.hlysine.create_connected.content.copycat;

import com.hlysine.create_connected.registries.CCBlockEntityTypes;
import com.hlysine.create_connected.compat.CopycatsManager;
import com.hlysine.create_connected.compat.Mods;
import com.hlysine.create_connected.config.CCConfigs;
import com.zurrtum.create.content.decoration.copycat.CopycatBlock;
import com.zurrtum.create.content.decoration.copycat.CopycatBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public abstract class MigratingCopycatBlock extends CopycatBlock {

    public MigratingCopycatBlock(Properties pProperties) {
        super(pProperties);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(@NotNull BlockPlaceContext pContext) {
        BlockState state = super.getStateForPlacement(pContext);
        assert state != null;
        return migrate(state);
    }

    @Override
    public @NotNull BlockState updateShape(@NotNull BlockState pState, @NotNull LevelReader pLevel, @NotNull ScheduledTickAccess pScheduledTickAccess, @NotNull BlockPos pCurrentPos, @NotNull Direction pDirection, @NotNull BlockPos pNeighborPos, @NotNull BlockState pNeighborState, @NotNull RandomSource pRandom) {
        return migrateOnUpdate(pLevel.isClientSide(), super.updateShape(pState, pLevel, pScheduledTickAccess, pCurrentPos, pDirection, pNeighborPos, pNeighborState, pRandom));
    }

    protected static BlockState migrateOnUpdate(boolean isClient, BlockState state) {
        if (!isClient && CCConfigs.common().migrateCopycatsOnBlockUpdate.get())
            return migrate(state);
        return state;
    }

    protected static BlockState migrate(BlockState state) {
        return Mods.COPYCATS.runIfInstalled(() -> () -> CopycatsManager.convertIfEnabled(state)).orElse(state);
    }

    protected boolean isSelfState(BlockState state) {
        if (state.is(this)) return true;
        return Mods.COPYCATS.runIfInstalled(() -> () -> state.is(CopycatsManager.convertIfEnabled(this))).orElse(false);
    }

    // BlockBehaviour.onRemove(BlockState, Level, BlockPos, BlockState newState, boolean isMoving) was
    // replaced by affectNeighborsAfterRemoval(BlockState, ServerLevel, BlockPos, boolean movedByPiston)
    // (confirmed via javap - neither vanilla's Block/BlockBehaviour nor real Create Fly's own CopycatBlock
    // override either method under any name anymore). Note the new signature dropped the `newState`
    // param entirely, so the original "skip cleanup when old and new states are equivalent Copycats+
    // conversions" check can no longer be expressed here. This is not a real feature loss in practice:
    // Copycats+ has no Fabric 1.21.11 build at all (confirmed elsewhere in this port, see
    // CopycatBlockMixin/CopycatsManager), so Mods.COPYCATS.runIfInstalled(...) always evaluates to
    // "not installed" on this port and the skipped branch was already permanently dead code.
    @Override
    protected void affectNeighborsAfterRemoval(BlockState pState, net.minecraft.server.level.ServerLevel pLevel, BlockPos pPos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(pState, pLevel, pPos, movedByPiston);
    }

    @Override
    public @NotNull List<ItemStack> getDrops(@NotNull BlockState pState, LootParams.@NotNull Builder pParams) {
        List<ItemStack> drops = super.getDrops(pState, pParams);
        return Mods.COPYCATS.runIfInstalled(() -> () -> {
            for (int i = 0; i < drops.size(); i++) {
                ItemStack drop = drops.get(i);
                Item converted = CopycatsManager.convert(drop.getItem());
                if (!converted.equals(drop.getItem())) {
                    drops.set(i, new ItemStack(converted, drop.getCount()));
                }
            }
            return drops;
        }).orElse(drops);
    }

    @Override
    public BlockEntityType<? extends CopycatBlockEntity> getBlockEntityType() {
        return CCBlockEntityTypes.COPYCAT;
    }
}
