package com.hlysine.create_connected.content.shearpin;

import com.hlysine.create_connected.registries.CCBlocks;
import com.hlysine.create_connected.datagen.advancements.AdvancementBehaviour;
import com.hlysine.create_connected.datagen.advancements.CCAdvancements;
import com.zurrtum.create.content.kinetics.base.IRotate;
import com.zurrtum.create.api.contraption.transformable.TransformableBlockEntity;
import com.zurrtum.create.content.contraptions.StructureTransform;
import com.zurrtum.create.content.decoration.bracket.BracketedBlockEntityBehaviour;
import com.zurrtum.create.content.kinetics.simpleRelays.AbstractSimpleShaftBlock;
import com.zurrtum.create.content.kinetics.simpleRelays.SimpleKineticBlockEntity;
import com.zurrtum.create.api.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.ticks.TickPriority;

import java.util.List;

public class ShearPinBlockEntity extends SimpleKineticBlockEntity implements TransformableBlockEntity {

    static final int RANDOM_DELAY = 5;

    // Real BracketedKineticBlockEntity's constructor is 2-arg (BlockPos, BlockState) - see
    // LinkedAnalogLeverBlockEntity.java for the full writeup on this pattern.
    public ShearPinBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void initialize() {
        onKineticUpdate();
        super.initialize();
    }

    private void onKineticUpdate() {
        if (IRotate.StressImpact.isEnabled()) {
            if (isOverStressed()) {
                if (level != null) {
                    level.scheduleTick(getBlockPos(), CCBlocks.SHEAR_PIN, level.random.nextInt(RANDOM_DELAY), TickPriority.EXTREMELY_HIGH);
                }
            }
        }
    }

    @Override
    public void updateFromNetwork(float maxStress, float currentStress, int networkSize) {
        super.updateFromNetwork(maxStress, currentStress, networkSize);
        onKineticUpdate();
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour<?>> behaviours) {
        behaviours.add(new BracketedBlockEntityBehaviour(this,
                state -> state.getBlock() instanceof AbstractSimpleShaftBlock));
        super.addBehaviours(behaviours);
        AdvancementBehaviour.registerAwardables(this, behaviours, CCAdvancements.SHEAR_PIN);
    }

    @Override
    public void transform(net.minecraft.world.level.block.entity.BlockEntity blockEntity, StructureTransform transform) {
        BracketedBlockEntityBehaviour bracket = getBehaviour(BracketedBlockEntityBehaviour.TYPE);
        if (bracket != null)
            bracket.transformBracket(transform);
    }
}

