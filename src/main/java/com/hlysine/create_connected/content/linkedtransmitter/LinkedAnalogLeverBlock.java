package com.hlysine.create_connected.content.linkedtransmitter;

import com.hlysine.create_connected.registries.CCBlockEntityTypes;
import com.hlysine.create_connected.registries.CCItems;
import com.zurrtum.create.AllSoundEvents;
import com.zurrtum.create.AllItems;
import com.zurrtum.create.api.schematic.requirement.SpecialBlockItemRequirement;
import com.zurrtum.create.content.equipment.wrench.IWrenchable;
import com.zurrtum.create.content.redstone.analogLever.AnalogLeverBlock;
import com.zurrtum.create.content.redstone.analogLever.AnalogLeverBlockEntity;
import com.zurrtum.create.content.schematics.requirement.ItemRequirement;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class LinkedAnalogLeverBlock extends AnalogLeverBlock implements SpecialBlockItemRequirement, IWrenchable, LinkedTransmitterBlock {
    public static BooleanProperty LOCKED = BlockStateProperties.LOCKED;

    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    private final Supplier<AnalogLeverBlock> baseSupplier;

    public LinkedAnalogLeverBlock(Properties pProperties, Supplier<AnalogLeverBlock> baseSupplier) {
        super(pProperties);
        registerDefaultState(defaultBlockState().setValue(POWERED, false).setValue(LOCKED, false));
        this.baseSupplier = baseSupplier;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        super.createBlockStateDefinition(pBuilder.add(POWERED, LOCKED));
    }

    @Override
    public Block getBlock() {
        return this;
    }

    @Override
    public Block getBase() {
        return baseSupplier.get();
    }

    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState state,
                                        @NotNull BlockGetter level,
                                        @NotNull BlockPos pos,
                                        @NotNull CollisionContext context) {
        return Shapes.or(getTransmitterShape(state), super.getShape(state, level, pos, context));
    }

    @Override
    public @NotNull List<ItemStack> getDrops(@NotNull BlockState state, LootParams.@NotNull Builder builder) {
        return getBase().defaultBlockState().getDrops(builder);
    }

    @Override
    public @NotNull InteractionResult useWithoutItem(@NotNull BlockState state,
                                                     @NotNull Level level,
                                                     @NotNull BlockPos pos,
                                                     @NotNull Player player,
                                                     @NotNull BlockHitResult hitResult) {
        if (player.isSpectator())
            return InteractionResult.PASS;

        if (isHittingBase(state, level, pos, hitResult)) {
            return super.useWithoutItem(state, level, pos, player, hitResult);
        }
        return LinkedTransmitterBlock.super.useTransmitter(state, level, pos, player);
    }

    @Override
    public @NotNull InteractionResult useItemOn(@NotNull ItemStack stack,
                                                @NotNull BlockState state,
                                                @NotNull Level level,
                                                @NotNull BlockPos pos,
                                                @NotNull Player player,
                                                @NotNull InteractionHand hand,
                                                @NotNull BlockHitResult hitResult) {
        if (stack.is(AllItems.WRENCH)) {
            UseOnContext context = new UseOnContext(player, hand, hitResult);
            return player.isShiftKeyDown() ? onSneakWrenched(state, context) : onWrenched(state, context);
        }

        InteractionResult waxResult = LinkedTransmitterBlock.super.useWax(stack, state, level, pos, player, hand, hitResult);
        if (waxResult != InteractionResult.TRY_WITH_EMPTY_HAND)
            return waxResult;

        if (player.isSpectator())
            return InteractionResult.PASS;

        // Create Fly's AnalogLeverBlock performs the state change in useItemOn (it no longer
        // overrides useWithoutItem at all), so base hits must be delegated here - falling through
        // to useWithoutItem would silently do nothing.
        if (isHittingBase(state, level, pos, hitResult))
            return super.useItemOn(stack, state, level, pos, player, hand, hitResult);

        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    // BlockBehaviour.onRemove(BlockState, Level, BlockPos, BlockState newState, boolean) was replaced by
    // affectNeighborsAfterRemoval(BlockState, ServerLevel, BlockPos, boolean) (see
    // MigratingCopycatBlock.java for the fuller writeup), and BlockState.onRemove(...) was replaced by
    // the matching BlockState.affectNeighborsAfterRemoval(ServerLevel, BlockPos, boolean) - both losing
    // the newState param. The `!state.is(newState.getBlock())` guard here (only pop the item when the
    // block is genuinely being replaced by something else, not just transitioning between its own
    // states) is dropped rather than replaced: per the new method's own name/contract, vanilla's call
    // site now filters same-block state transitions before invoking affectNeighborsAfterRemoval at all,
    // making this check redundant rather than unexpressable.
    @Override
    public void affectNeighborsAfterRemoval(@NotNull BlockState state, @NotNull net.minecraft.server.level.ServerLevel world, @NotNull BlockPos pos, boolean isMoving) {
        if (!isMoving && world.getBlockEntity(pos) instanceof LinkedAnalogLeverBlockEntity be && be.containsBase)
            Block.popResource(world, pos, new ItemStack(CCItems.LINKED_TRANSMITTER));
        getBase().defaultBlockState().affectNeighborsAfterRemoval(world, pos, isMoving);
    }

    @Override
    public InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {
        onWrenched(state, context);
        return IWrenchable.super.onSneakWrenched(state, context);
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        Player player = context.getPlayer();
        if (!player.isCreative()) {
            player.getInventory().placeItemBackInInventory(new ItemStack(CCItems.LINKED_TRANSMITTER));
        }
        if (context.getLevel().getBlockEntity(context.getClickedPos()) instanceof LinkedAnalogLeverBlockEntity be) {
            be.containsBase = false;
        }
        replaceWithBase(state, context.getLevel(), context.getClickedPos());
        return InteractionResult.SUCCESS;
    }

    @Override
    public void replaceBase(BlockState baseState, Level world, BlockPos pos) {
        world.removeBlockEntity(pos);
        world.setBlockAndUpdate(pos, defaultBlockState()
                .setValue(FACING, baseState.getValue(FACING))
                .setValue(FACE, baseState.getValue(FACE))
        );
        AllSoundEvents.CONTROLLER_PUT.playOnServer(world, pos);
    }

    public void replaceWithBase(BlockState state, Level world, BlockPos pos) {
        AllSoundEvents.CONTROLLER_TAKE.playOnServer(world, pos);
        world.removeBlockEntity(pos);
        world.setBlockAndUpdate(pos, getBase().defaultBlockState()
                .setValue(FACING, state.getValue(FACING))
                .setValue(FACE, state.getValue(FACE)));
    }

    @Override
    public @NotNull ItemStack getCloneItemStack(@NotNull LevelReader world, @NotNull BlockPos pos, @NotNull BlockState state, boolean includeData) {
        // Real feature reduction, disclosed: vanilla's getCloneItemStack no longer receives a
        // HitResult (see PORTING_NOTES.md), so we can no longer tell whether the player's
        // pick-block raycast hit the "base" visual part vs. the lever part the way
        // isHittingBase(...) used to distinguish - always returning this mod's own item here
        // (matching the "not hitting base" branch) rather than guessing.
        return new ItemStack(CCItems.LINKED_TRANSMITTER);
    }

    @Override
    public ItemRequirement getRequiredItems(BlockState state, BlockEntity be) {
        ArrayList<ItemStack> requiredItems = new ArrayList<>();
        requiredItems.add(new ItemStack(getBase()));
        requiredItems.add(new ItemStack(CCItems.LINKED_TRANSMITTER));
        return new ItemRequirement(ItemRequirement.ItemUseType.CONSUME, requiredItems);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class<AnalogLeverBlockEntity> getBlockEntityClass() {
        return (Class<AnalogLeverBlockEntity>) (Class<?>) LinkedAnalogLeverBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends LinkedAnalogLeverBlockEntity> getBlockEntityType() {
        return CCBlockEntityTypes.LINKED_ANALOG_LEVER;
    }
}
