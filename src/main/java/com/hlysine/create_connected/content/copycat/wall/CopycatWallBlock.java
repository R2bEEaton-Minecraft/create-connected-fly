package com.hlysine.create_connected.content.copycat.wall;

import com.hlysine.create_connected.content.DirectionHelper;
import com.hlysine.create_connected.content.copycat.ICopycatWithWrappedBlock;
import com.hlysine.create_connected.content.copycat.WaterloggedCopycatWrappedBlock;
import com.zurrtum.create.content.decoration.copycat.CopycatBlock;
import com.zurrtum.create.catnip.data.Iterate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.WallSide;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

import static net.minecraft.core.Direction.Axis;
import static net.minecraft.world.level.block.WallBlock.*;

public class CopycatWallBlock extends WaterloggedCopycatWrappedBlock {

    public static WallBlock wall;

    public CopycatWallBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(UP, true)
                .setValue(NORTH, WallSide.NONE)
                .setValue(SOUTH, WallSide.NONE)
                .setValue(EAST, WallSide.NONE)
                .setValue(WEST, WallSide.NONE)
        );
    }

    @Override
    public Block getWrappedBlock() {
        return wall;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.@NotNull Builder<Block, BlockState> pBuilder) {
        super.createBlockStateDefinition(pBuilder.add(UP, NORTH, SOUTH, EAST, WEST));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(@NotNull BlockPlaceContext pContext) {
        BlockState state = wall.getStateForPlacement(pContext);
        if (state == null) return super.getStateForPlacement(pContext);
        return ICopycatWithWrappedBlock.copyState(state, super.getStateForPlacement(pContext), false);
    }

    // collisionExtendsVertically(BlockState, BlockGetter, BlockPos, Entity) does not exist anywhere in
    // this Fabric API surface (confirmed via javap on Block/BlockBehaviour/FabricBlock/FabricBlockState;
    // also absent from real Create Fly's own compiled CopycatBlock.class and CopycatWallBlock analog).
    // This was a NeoForge-only IBlockExtension hook with no Fabric replacement - real Create Fly's own
    // MetalLadderBlock has an analogous hook (supportsExternalFaceHiding) left commented out with a
    // "//TODO" for the same reason, confirming this is a genuine, currently-unported API gap rather
    // than a mistake on this mod's part. Feature reduction: entity collision may no longer extend
    // through the full vertical height of a tall wall segment the way vanilla WallBlock's own
    // (also-removed) equivalent used to; the wall's actual getCollisionShape/getShape still render and
    // collide correctly per-voxel, this only affected a specific vertical-extension optimization.

    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState pState, @NotNull BlockGetter pLevel, @NotNull BlockPos pPos, @NotNull CollisionContext pContext) {
        return ICopycatWithWrappedBlock.wrappedState(wall, pState).getShape(pLevel, pPos, pContext);
    }

    @Override
    public @NotNull VoxelShape getCollisionShape(@NotNull BlockState pState, @NotNull BlockGetter pLevel, @NotNull BlockPos pPos, @NotNull CollisionContext pContext) {
        return ICopycatWithWrappedBlock.wrappedState(wall, pState).getCollisionShape(pLevel, pPos, pContext);
    }

    @Override
    protected boolean isPathfindable(@NotNull BlockState pState, @NotNull PathComputationType pPathComputationType) {
        return ICopycatWithWrappedBlock.wrappedState(wall, pState).isPathfindable(pPathComputationType);
    }

    @Override
    public @NotNull BlockState updateShape(@NotNull BlockState pState, @NotNull LevelReader pLevel, @NotNull ScheduledTickAccess pScheduledTickAccess, @NotNull BlockPos pCurrentPos, @NotNull Direction pDirection, @NotNull BlockPos pNeighborPos, @NotNull BlockState pNeighborState, @NotNull RandomSource pRandom) {
        return migrateOnUpdate(pLevel.isClientSide(), ICopycatWithWrappedBlock.unwrapForOperation(wall, pState, state -> state.updateShape(pLevel, pScheduledTickAccess, pCurrentPos, pDirection, pNeighborPos, pNeighborState, pRandom)));
    }

    @Override
    public boolean propagatesSkylightDown(@NotNull BlockState pState) {
        return ICopycatWithWrappedBlock.wrappedState(wall, pState).propagatesSkylightDown();
    }

    @Override
    public @NotNull BlockState rotate(@NotNull BlockState pState, @NotNull Rotation pRotation) {
        return ICopycatWithWrappedBlock.unwrapForOperation(wall, pState, state -> state.rotate(pRotation));
    }

    @Override
    public @NotNull BlockState mirror(@NotNull BlockState pState, @NotNull Mirror pMirror) {
        return ICopycatWithWrappedBlock.unwrapForOperation(wall, pState, state -> state.mirror(pMirror));
    }

    @Override
    public boolean isIgnoredConnectivitySide(BlockAndTintGetter reader, BlockState state, Direction face,
                                             @Nullable BlockPos fromPos, @Nullable BlockPos toPos) {
        if (fromPos == null || toPos == null)
            return true;

        BlockState toState = reader.getBlockState(toPos);
        if (!toState.is(this) || !state.is(this)) return true;

        boolean isCross = true;
        for (Direction direction : Iterate.horizontalDirections) {
            if (toState.getValue(byDirection(direction)) == WallSide.NONE) {
                isCross = false;
                break;
            }
        }
        return isCross;
    }

    @Override
    public boolean canConnectTexturesToward(BlockAndTintGetter reader, BlockPos fromPos, BlockPos toPos, BlockState state) {
        BlockState toState = reader.getBlockState(toPos);
        if (!toState.is(this)) return false;

        long sideCount = Arrays.stream(Iterate.horizontalDirections).filter(s -> state.getValue(byDirection(s)) != WallSide.NONE).count();
        if (sideCount > 2)
            return false;
        if (sideCount == 2 && (state.getValue(NORTH) != state.getValue(SOUTH) || state.getValue(EAST) != state.getValue(WEST))) {
            return false;
        }

        BlockPos diff = toPos.subtract(fromPos);
        if (diff.equals(Vec3i.ZERO)) {
            return true;
        }
        Direction face = DirectionHelper.fromDelta(diff.getX(), diff.getY(), diff.getZ());
        if (face == null) {
            if (diff.distManhattan(Vec3i.ZERO) > 2) return false;
            if (diff.getY() == 0) return false;
            Direction horizontalDiff = Direction.fromAxisAndDirection(diff.getX() == 0 ? Axis.Z : Axis.X,
                    (diff.getX() + diff.getZ() > 0) ? Direction.AxisDirection.POSITIVE : Direction.AxisDirection.NEGATIVE);
            if (diff.getY() > 0) {
                if (state.getValue(byDirection(horizontalDiff)) != WallSide.TALL) return false;
                if (toState.getValue(byDirection(horizontalDiff.getOpposite())) == WallSide.NONE) return false;
            } else {
                if (state.getValue(byDirection(horizontalDiff)) == WallSide.NONE) return false;
                if (toState.getValue(byDirection(horizontalDiff.getOpposite())) != WallSide.TALL) return false;
            }
            return true;
        } else if (face == Direction.DOWN || face == Direction.UP) {
            return canConnectVertically(state) && canConnectVertically(toState);
        } else {
            if (state.getValue(WallBlock.UP)) return false;
            if (state.getValue(byDirection(face)) == WallSide.NONE) return false;
            return true;
        }
    }

    private boolean canConnectVertically(BlockState state) {
        if (!state.getValue(WallBlock.UP)) return false;
        for (Direction direction : Iterate.horizontalDirections) {
            WallSide side = state.getValue(byDirection(direction));
            if (side != WallSide.NONE) return false;
        }
        return true;
    }

    @Override
    public boolean canFaceBeOccluded(BlockState state, Direction face) {
        if (face.getAxis().isHorizontal()) {
            WallSide side = state.getValue(byDirection(face));
            return side != WallSide.NONE &&
                    !state.getValue(UP) &&
                    side == state.getValue(byDirection(face.getOpposite())) &&
                    state.getValue(byDirection(face.getClockWise())) == WallSide.NONE &&
                    state.getValue(byDirection(face.getCounterClockWise())) == WallSide.NONE;
        }
        return false;
    }

    @Override
    public boolean shouldFaceAlwaysRender(BlockState state, Direction face) {
        return !canFaceBeOccluded(state, face);
    }

    // supportsExternalFaceHiding(BlockState) and hidesNeighborFace(BlockGetter, BlockPos, BlockState,
    // BlockState, Direction) do not exist anywhere in this Fabric API surface either (same confirmation
    // as collisionExtendsVertically above) - both were NeoForge-only IBlockExtension hooks used to skip
    // rendering faces between two adjacent copycat walls that share the same material and connectivity,
    // a pure GPU/fill-rate optimization. Feature reduction: adjacent copycat walls will render their
    // shared internal faces instead of culling them (a performance cost only - block appearance,
    // texture connection via canConnectTexturesToward, and face-occlusion via canFaceBeOccluded/
    // shouldFaceAlwaysRender above are all unaffected since those hooks do have real Fabric equivalents
    // and remain in place).

    public static BlockState getMaterial(BlockGetter reader, BlockPos targetPos) {
        BlockState state = CopycatBlock.getMaterial(reader, targetPos);
        if (state.is(Blocks.AIR)) return reader.getBlockState(targetPos);
        return state;
    }

    // WallBlock.NORTH_WALL/SOUTH_WALL/EAST_WALL/WEST_WALL were renamed to plain NORTH/SOUTH/EAST/WEST
    // (confirmed via javap), and vanilla now ships a ready-made WallBlock.PROPERTY_BY_DIRECTION map
    // doing exactly what this switch used to build by hand - used directly instead (also sidesteps
    // the naming collision a straight rename would otherwise create between the switch's Direction
    // case labels and the statically-imported WallBlock.NORTH/SOUTH/EAST/WEST EnumProperty fields).
    public static EnumProperty<WallSide> byDirection(Direction direction) {
        EnumProperty<WallSide> property = WallBlock.PROPERTY_BY_DIRECTION.get(direction);
        if (property == null)
            throw new IllegalArgumentException("Vertical directions not supported");
        return property;
    }
}

