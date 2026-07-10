package com.hlysine.create_connected.mixin.sequencedgearshift;

import com.zurrtum.create.content.kinetics.transmission.sequencer.Instruction;
import com.zurrtum.create.content.kinetics.transmission.sequencer.SequencerInstructions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = Instruction.class, remap = false)
public interface InstructionAccessor {
    @Accessor
    SequencerInstructions getInstruction();
}
