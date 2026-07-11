package com.hlysine.create_connected.content.sequencedpulsegenerator;

import com.hlysine.create_connected.content.sequencedpulsegenerator.instructions.*;
import com.hlysine.create_connected.datagen.advancements.AdvancementBehaviour;
import com.hlysine.create_connected.datagen.advancements.CCAdvancements;
import com.zurrtum.create.foundation.blockEntity.SmartBlockEntity;
import com.zurrtum.create.api.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Vector;

import static com.hlysine.create_connected.content.sequencedpulsegenerator.SequencedPulseGeneratorBlock.POWERING;
import static net.minecraft.world.level.block.DiodeBlock.POWERED;

public class SequencedPulseGeneratorBlockEntity extends SmartBlockEntity {

    public static final int INSTRUCTION_CAPACITY = 7;
    private static final int MAX_RECURSION_DEPTH = 10;
    private static final float PARTICLE_DENSITY = 0.2f;

    static {
        Instruction.register(new OutputInstruction(10, 15));
        Instruction.register(new TransformInstruction(2, 15));
        Instruction.register(new WaitForInstruction(1, 0));
        Instruction.register(new WaitForMinInstruction(8, 0));
        Instruction.register(new WaitForMaxInstruction(7, 0));
        Instruction.register(new WaitForExactInstruction(7, 0));
        Instruction.register(new LoopForInstruction(3));
        Instruction.register(new LoopIfInstruction(1));
        Instruction.register(new LoopIfMinInstruction(8));
        Instruction.register(new LoopIfMaxInstruction(7));
        Instruction.register(new LoopIfExactInstruction(7));
        Instruction.register(new LoopInstruction());
        Instruction.register(new EndInstruction());
    }

    Vector<Instruction> instructions;
    int currentInstruction;
    int currentSignal;
    int previousInput;
    int currentInput;
    int infiniteLoopCounter;

    public SequencedPulseGeneratorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        instructions = Instruction.createDefault();
        currentInstruction = -1;
        currentSignal = 0;
        previousInput = 0;
        infiniteLoopCounter = 0;
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour<?>> behaviours) {
        AdvancementBehaviour.registerAwardables(this, behaviours, CCAdvancements.PULSE_GEN_INFINITE_LOOP);
    }

    public boolean isIdle() {
        return currentInstruction < 0;
    }

    public int getCurrentSignal() {
        return currentSignal;
    }

    public int getPreviousInput() {
        return previousInput;
    }

    /**
     * More reliable than checking block state because that may not be updated yet
     */
    public int getCurrentInput() {
        return currentInput;
    }

    public Instruction getCurrentInstruction() {
        return currentInstruction >= 0 && currentInstruction < instructions.size()
                ? instructions.get(currentInstruction)
                : null;
    }

    private void applySignal() {
        level.setBlock(getBlockPos(), getBlockState().setValue(POWERING, currentSignal > 0), Block.UPDATE_ALL);
        level.updateNeighborsAt(this.worldPosition, this.getBlockState().getBlock());
        level.updateNeighborsAt(this.worldPosition.relative(this.getBlockState().getValue(SequencedPulseGeneratorBlock.FACING).getOpposite()), this.getBlockState().getBlock());
    }

    private void executeInstruction(boolean allowImmediate, int recursionDepth) {
        Instruction instruction = getCurrentInstruction();
        if (instruction == null) {
            currentInstruction = -1;
            if (currentSignal != 0) {
                currentSignal = 0;
                applySignal();
            }
            return;
        }
        InstructionResult result = instruction.tick(this);
        int prevSignal = currentSignal;
        currentSignal = instruction.transformOutput(this, instruction.getSignal());
        if (prevSignal != currentSignal) {
            applySignal();
        }
        currentInstruction = result.getNextInstruction(currentInstruction);
        if (result.isImmediate() && allowImmediate) {
            if (recursionDepth < MAX_RECURSION_DEPTH) {
                executeInstruction(true, recursionDepth + 1);
            } else {
                infiniteLoopCounter++;
                if (level.getRandom().nextFloat() < PARTICLE_DENSITY) {
                    Vec3 loc = Vec3.atBottomCenterOf(getBlockPos());
                    ((ServerLevel) level).sendParticles(ParticleTypes.SMOKE, loc.x, loc.y, loc.z, 2, 0.1, 0, 0.1, 0.01);
                }
                if (!level.isClientSide() && infiniteLoopCounter > 101) {
                    infiniteLoopCounter = 0;
                    AdvancementBehaviour.tryAward(this, CCAdvancements.PULSE_GEN_INFINITE_LOOP);
                }
            }
        } else {
            infiniteLoopCounter = 0;
        }
        if (recursionDepth == 0) {
            notifyUpdate();
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (isIdle())
            return;
        if (level.isClientSide())
            return;

        executeInstruction(true, 0);
        previousInput = currentInput;
    }

    private void resetAllInstructions() {
        Vector<Instruction> newInstructions = new Vector<>(instructions.capacity());
        instructions.forEach(i -> newInstructions.add(i.copy()));
        instructions = newInstructions;
    }

    public void onRedstoneUpdate(int input) {
        this.currentInput = input;
        if (currentInput == previousInput) return;
        if (!isIdle() || currentInput == 0) {
            previousInput = currentInput;
            return;
        }
        if (!level.hasNeighborSignal(worldPosition)) {
            level.setBlock(worldPosition, getBlockState().setValue(POWERED, false), 3);
            previousInput = currentInput;
            return;
        }
        currentInstruction = 0;
        resetAllInstructions();
        executeInstruction(true, 0);
        previousInput = currentInput;
    }

    public void reset() {
        resetAllInstructions();
        currentInstruction = -1;
        infiniteLoopCounter = 0;
        currentSignal = 0;
        applySignal();
        notifyUpdate();
    }

    // MC 1.21.11 replaced BlockEntity's CompoundTag-based write/read with a Codec-based
    // ValueOutput/ValueInput view (see PORTING_NOTES.md "ValueInput/ValueOutput" section - this
    // affects every BlockEntity that overrides write/read, a mod-wide change, not specific to this
    // class). Instruction.serializeAll/deserializeAll still work in terms of a plain ListTag (their
    // own API is unaffected and still used by the client Screen/network packet), so this method
    // just bridges that ListTag to/from a ValueOutput/ValueInput typed list of CompoundTag via
    // CompoundTag.CODEC.
    @Override
    protected void write(ValueOutput view, boolean clientPacket) {
        view.putInt("InstructionIndex", currentInstruction);
        view.putInt("PrevInput", previousInput);
        view.putInt("CurrentInput", currentInput);
        view.putInt("CurrentSignal", currentSignal);
        ValueOutput.TypedOutputList<CompoundTag> list = view.list("Instructions", CompoundTag.CODEC);
        for (net.minecraft.nbt.Tag t : Instruction.serializeAll(instructions))
            list.add((CompoundTag) t);
        super.write(view, clientPacket);
    }

    @Override
    protected void read(ValueInput view, boolean clientPacket) {
        currentInstruction = view.getIntOr("InstructionIndex", 0);
        previousInput = view.getIntOr("PrevInput", 0);
        currentInput = view.getIntOr("CurrentInput", 0);
        currentSignal = view.getIntOr("CurrentSignal", 0);
        ListTag list = new ListTag();
        view.listOrEmpty("Instructions", CompoundTag.CODEC).forEach(list::add);
        instructions = Instruction.deserializeAll(list);
        super.read(view, clientPacket);
    }
}
