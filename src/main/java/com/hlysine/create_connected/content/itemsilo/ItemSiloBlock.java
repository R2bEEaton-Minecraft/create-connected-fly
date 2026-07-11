package com.hlysine.create_connected.content.itemsilo;

import com.hlysine.create_connected.registries.CCBlockEntityTypes;
import com.hlysine.create_connected.registries.CCBlocks;
import com.zurrtum.create.api.connectivity.ConnectivityHandler;
import com.zurrtum.create.content.equipment.wrench.IWrenchable;
import com.zurrtum.create.content.logistics.vault.ItemVaultBlock;
import com.zurrtum.create.foundation.block.IBE;
import com.zurrtum.create.foundation.item.ItemHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.neoforged.neoforge.common.util.DeferredSoundType;

import org.jetbrains.annotations.Nullable;

public class ItemSiloBlock extends Block implements IWrenchable, IBE<ItemSiloBlockEntity> {
    public static final BooleanProperty LARGE = ItemVaultBlock.LARGE;

    public ItemSiloBlock(Properties p_i48440_1_) {
        super(p_i48440_1_);
        registerDefaultState(defaultBlockState().setValue(LARGE, false));
    }

    @Override
    protected void createBlockStateDefinition(Builder<Block, BlockState> pBuilder) {
        pBuilder.add(LARGE);
        super.createBlockStateDefinition(pBuilder);
    }

    @Override
    public void onPlace(BlockState pState, Level pLevel, BlockPos pPos, BlockState pOldState, boolean pIsMoving) {
        if (pOldState.getBlock() == pState.getBlock())
            return;
        if (pIsMoving)
            return;
        withBlockEntityDo(pLevel, pPos, ItemSiloBlockEntity::updateConnectivity);
    }

    @Override
    public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean pIsMoving) {
        if (state.hasBlockEntity() && (state.getBlock() != newState.getBlock() || !newState.hasBlockEntity())) {
            BlockEntity be = world.getBlockEntity(pos);
            if (!(be instanceof ItemSiloBlockEntity vaultBE))
                return;
            ItemHelper.dropContents(world, pos, vaultBE.inventory);
            world.removeBlockEntity(pos);
            ConnectivityHandler.splitMulti(vaultBE);
        }
    }

    public static boolean isVault(BlockState state) {
        return CCBlocks.ITEM_SILO.has(state);
    }

    @Nullable
    public static Direction.Axis getVaultBlockAxis(BlockState state) {
        if (!isVault(state))
            return null;
        return Direction.Axis.Y;
    }

    public static boolean isLarge(BlockState state) {
        if (!isVault(state))
            return false;
        return state.getValue(LARGE);
    }

    // Vaults are less noisy when placed in batch
    public static final SoundType SILENCED_METAL =
            new DeferredSoundType(0.1F, 1.5F, () -> SoundEvents.NETHERITE_BLOCK_BREAK, () -> SoundEvents.NETHERITE_BLOCK_STEP,
                    () -> SoundEvents.NETHERITE_BLOCK_PLACE, () -> SoundEvents.NETHERITE_BLOCK_HIT,
                    () -> SoundEvents.NETHERITE_BLOCK_FALL);

    // Real feature reduction, disclosed: vanilla's getSoundType(BlockState) no longer receives an
    // Entity/LevelReader/BlockPos context (see PORTING_NOTES.md), so the "quieter when placed in
    // batch" per-placing-entity silencing via a "SilenceVaultSound" persistent-data flag can no
    // longer be conditionally applied at this override point - always uses the normal sound now.
    @Override
    public SoundType getSoundType(BlockState state) {
        return super.getSoundType(state);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState pState, Level pLevel, BlockPos pPos, Direction direction) {
        return ItemHelper.calcRedstoneFromBlockEntity(this, pLevel, pPos);
    }

    @Override
    public BlockEntityType<? extends ItemSiloBlockEntity> getBlockEntityType() {
        return CCBlockEntityTypes.ITEM_SILO;
    }

    @Override
    public Class<ItemSiloBlockEntity> getBlockEntityClass() {
        return ItemSiloBlockEntity.class;
    }
}

