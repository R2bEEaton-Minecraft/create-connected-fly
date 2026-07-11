package com.hlysine.create_connected.content.inventorybridge;

import com.hlysine.create_connected.registries.CCBlockEntityTypes;
import com.zurrtum.create.content.equipment.wrench.IWrenchable;
import com.zurrtum.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import org.jetbrains.annotations.NotNull;

public class InventoryBridgeBlock extends Block implements IBE<InventoryBridgeBlockEntity>, IWrenchable {

    public static BooleanProperty ATTACHED_POSITIVE = BooleanProperty.create("attached_positive");
    public static BooleanProperty ATTACHED_NEGATIVE = BooleanProperty.create("attached_negative");
    public static EnumProperty<Axis> AXIS = BlockStateProperties.AXIS;

    public InventoryBridgeBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(ATTACHED_POSITIVE, false)
                .setValue(ATTACHED_NEGATIVE, false)
                .setValue(AXIS, Axis.X)
        );
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        super.createBlockStateDefinition(pBuilder.add(ATTACHED_POSITIVE, ATTACHED_NEGATIVE, AXIS));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState();

        // NeoForge's BlockCapability/Capabilities.ItemHandler.BLOCK lookup is gone - Fabric's
        // ItemStorage.SIDED.find(level, pos, side) is the direct replacement (side=null here,
        // matching the original's side-agnostic probe intent).
        Direction preferredFacing = null;
        for (Direction face : context.getNearestLookingDirections()) {
            BlockPos neighborPos = context.getClickedPos().relative(face);
            if (ItemStorage.SIDED.find(context.getLevel(), neighborPos, null) != null) {
                preferredFacing = face;
                break;
            }
        }

        if (preferredFacing == null) {
            preferredFacing = context.getNearestLookingDirection();
        }

        return state.setValue(AXIS, preferredFacing.getAxis());
    }

    @Override
    public void onPlace(@NotNull BlockState state, @NotNull Level worldIn, @NotNull BlockPos pos, @NotNull BlockState oldState, boolean isMoving) {
        withBlockEntityDo(worldIn, pos, InventoryBridgeBlockEntity::updateConnectedInventory);
    }

    // neighborChanged(state, level, pos, block, fromPos, isMoving) is gone - vanilla now passes an
    // Orientation instead of the raw fromPos, with getFront() giving the equivalent "which direction
    // did the change come from" info directly (no more manual delta->Direction.fromDelta() dance).
    @Override
    public void neighborChanged(
            @NotNull BlockState pState, @NotNull Level pLevel, @NotNull BlockPos pPos,
            @NotNull Block pBlock, @NotNull Orientation orientation, boolean pIsMoving
    ) {
        withBlockEntityDo(pLevel, pPos, InventoryBridgeBlockEntity::updateConnectedInventory);
        super.neighborChanged(pState, pLevel, pPos, pBlock, orientation, pIsMoving);
        Direction fromSide = orientation.getFront();
        if (fromSide == null)
            pLevel.updateNeighborsAt(pPos, this, orientation);
        else
            pLevel.updateNeighborsAtExceptFromFacing(pPos, this, fromSide, orientation);
    }

    public static Direction getNegativeTarget(BlockState state) {
        return Direction.fromAxisAndDirection(state.getValue(AXIS), Direction.AxisDirection.NEGATIVE);
    }

    public static Direction getPositiveTarget(BlockState state) {
        return Direction.fromAxisAndDirection(state.getValue(AXIS), Direction.AxisDirection.POSITIVE);
    }

    @Override
    public boolean hasAnalogOutputSignal(@NotNull BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(@NotNull BlockState blockState, @NotNull Level worldIn, @NotNull BlockPos pos, @NotNull Direction direction) {
        BlockPos pos1 = pos.relative(getNegativeTarget(blockState));
        BlockPos pos2 = pos.relative(getPositiveTarget(blockState));
        BlockState target1 = worldIn.getBlockState(pos1);
        BlockState target2 = worldIn.getBlockState(pos2);
        int total = 0;
        if (blockState.getValue(ATTACHED_NEGATIVE) && !target1.is(this) && target1.hasAnalogOutputSignal())
            total += target1.getAnalogOutputSignal(worldIn, pos1, direction);
        if (blockState.getValue(ATTACHED_POSITIVE) && !target2.is(this) && target2.hasAnalogOutputSignal())
            total += target2.getAnalogOutputSignal(worldIn, pos2, direction);
        return total / 2;
    }

    @Override
    public Class<InventoryBridgeBlockEntity> getBlockEntityClass() {
        return InventoryBridgeBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends InventoryBridgeBlockEntity> getBlockEntityType() {
        return CCBlockEntityTypes.INVENTORY_BRIDGE;
    }

}

