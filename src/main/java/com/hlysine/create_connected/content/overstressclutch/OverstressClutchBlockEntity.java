package com.hlysine.create_connected.content.overstressclutch;

import com.hlysine.create_connected.registries.CCBlocks;
import com.hlysine.create_connected.content.overstressclutch.OverstressClutchBlock.ClutchState;
import com.hlysine.create_connected.datagen.advancements.AdvancementBehaviour;
import com.hlysine.create_connected.datagen.advancements.CCAdvancements;
import com.zurrtum.create.content.kinetics.RotationPropagator;
import com.zurrtum.create.content.kinetics.base.IRotate;
import com.zurrtum.create.content.kinetics.transmission.SplitShaftBlockEntity;
import com.zurrtum.create.content.redstone.diodes.BrassDiodeBlock;
import com.zurrtum.create.api.behaviour.BlockEntityBehaviour;
import com.zurrtum.create.foundation.blockEntity.behaviour.*;
import com.zurrtum.create.foundation.blockEntity.behaviour.scrollValue.ServerScrollValueBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.ticks.TickPriority;

import java.util.List;

import static com.hlysine.create_connected.content.overstressclutch.OverstressClutchBlock.POWERED;
import static com.hlysine.create_connected.content.overstressclutch.OverstressClutchBlock.STATE;
import static net.minecraft.ChatFormatting.GOLD;

public class OverstressClutchBlockEntity extends SplitShaftBlockEntity {

    public int delay;
    // Client-only "ticks/seconds/minutes" UI board (previously TimeDelayScrollValueBehaviour's
    // createBoard()) not yet re-implemented - see PORTING_NOTES.md's ScrollValueBehaviour split note.
    public TimeDelayScrollValueBehaviour maxDelay;

    public OverstressClutchBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour<?>> behaviours) {
        AdvancementBehaviour.registerAwardables(this, behaviours, CCAdvancements.OVERSTRESS_CLUTCH);
        maxDelay = new TimeDelayScrollValueBehaviour(this);
        maxDelay.between(1, 60 * 20 * 60);
        maxDelay.withCallback(this::onMaxDelayChanged);
        maxDelay.setValue(1);
        behaviours.add(maxDelay);
    }

    private void onMaxDelayChanged(int newMax) {
        delay = Mth.clamp(delay, 0, newMax);
        sendData();
    }

    public boolean isIdle() {
        return delay == 0;
    }

    @Override
    public void initialize() {
        onKineticUpdate();
        super.initialize();
    }

    public void onKineticUpdate() {
        if (getBlockState().getValue(STATE) == ClutchState.UNCOUPLED && getBlockState().getValue(POWERED)) {
            resetClutch();
            return;
        }
        if (IRotate.StressImpact.isEnabled() && !getBlockState().getValue(POWERED)) {
            if (isOverStressed() && getBlockState().getValue(STATE) == ClutchState.COUPLED) {
                if (level != null) {
                    level.setBlock(getBlockPos(), getBlockState().setValue(STATE, ClutchState.UNCOUPLING), 2 | 16);
                    delay = maxDelay.getValue() - 1;
                    sendData();
                    return;
                }
            }
        }
        if (!isOverStressed() && getBlockState().getValue(STATE) == ClutchState.UNCOUPLING) {
            if (level != null) {
                level.setBlock(getBlockPos(), getBlockState().setValue(STATE, ClutchState.COUPLED), 2 | 16);
            }
        }
    }

    @Override
    public void updateFromNetwork(float maxStress, float currentStress, int networkSize) {
        super.updateFromNetwork(maxStress, currentStress, networkSize);
        onKineticUpdate();
    }

    @Override
    public float getRotationSpeedModifier(Direction face) {
        if (hasSource()) {
            if (face != getSourceFacing() && getBlockState().getValue(STATE) == ClutchState.UNCOUPLED)
                return 0;
        }
        return 1;
    }

    // CORRECTION: appendUncoupledTooltip's body used to live directly in this override, reasoning
    // (wrongly) that client-only ConnectedLang/TooltipHelper/FontHelper imports were safe because
    // this method is only reached from client-side goggle rendering. Loom's split source sets
    // reject the *import* at compile time regardless of runtime guards - see
    // KineticBridgeBlockItem's history and PORTING_NOTES.md for the corrected explanation. Real
    // fix: the tooltip-building logic now lives in
    // src/client/java/.../content/overstressclutch/OverstressClutchBlockEntityClient.java, wired
    // through this hook (populated by CreateConnectedClient.onInitializeClient()).
    public static java.util.function.BiConsumer<OverstressClutchBlockEntity, List<Component>> uncoupledTooltipHook = (be, tooltip) -> {
    };

    // FURTHER CORRECTION: the addToTooltip(List<Component>, boolean) override itself doesn't exist
    // either - that was never a real SmartBlockEntity/KineticBlockEntity method, it's actually
    // IHaveGoggleInformation.addToGoggleTooltip(...), a client-only interface (confirmed by reading
    // real com.zurrtum.create.client.api.goggles.IHaveGoggleInformation) - the same cross-boundary
    // "main imports client" bug already fixed for FluidVesselBlockEntity/KineticBatteryBlockEntity
    // elsewhere in this port. Fixed the same way: the override itself moved to a new client class,
    // OverstressClutchTooltipBehaviour, registered via BlockEntityBehaviour.addClient(...) in
    // CreateConnectedClient.onInitializeClient(). The uncoupledTooltipHook indirection above remains
    // in place unchanged (it solves a separate, narrower problem: letting the *content* of the
    // tooltip live client-side while this field stays referenceable from common code).

    public void resetClutch() {
        if (getBlockState().getValue(STATE) == ClutchState.UNCOUPLED && !isOverStressed()) {
            assert level != null;
            level.setBlock(getBlockPos(), getBlockState().setValue(STATE, ClutchState.COUPLED), 3);
            RotationPropagator.handleRemoved(level, getBlockPos(), this);
            RotationPropagator.handleAdded(level, getBlockPos(), this);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (getBlockState().getValue(STATE) == ClutchState.UNCOUPLING && level != null && !level.isClientSide()) {
            level.scheduleTick(getBlockPos(), CCBlocks.OVERSTRESS_CLUTCH, 0, TickPriority.EXTREMELY_HIGH);
        }
    }

    @Override
    protected void read(ValueInput compound, boolean clientPacket) {
        delay = compound.getIntOr("Delay", 0);
        super.read(compound, clientPacket);
    }

    @Override
    protected void write(ValueOutput compound, boolean clientPacket) {
        compound.putInt("Delay", delay);
        super.write(compound, clientPacket);
    }

    // Client-only createBoard()/formatSettings() (custom "ticks/seconds/minutes" UI board rows)
    // dropped from this server-side half - see PORTING_NOTES.md's ScrollValueBehaviour split note.
    public static class TimeDelayScrollValueBehaviour extends ServerScrollValueBehaviour {

        public TimeDelayScrollValueBehaviour(com.zurrtum.create.foundation.blockEntity.SmartBlockEntity be) {
            super(be);
        }

        @Override
        public void onShortInteract(Player player, InteractionHand hand, Direction side, BlockHitResult hitResult) {
            BlockState blockState = blockEntity.getBlockState();
            if (blockState.getBlock() instanceof BrassDiodeBlock bdb)
                bdb.toggle(getLevel(), getPos(), blockState, player, hand);
        }

        @Override
        public void setValueSettings(Player player, ValueSettings valueSetting, boolean ctrlHeld) {
            int value = valueSetting.value();
            int multiplier = switch (valueSetting.row()) {
                case 0 -> 1;
                case 1 -> 20;
                default -> 60 * 20;
            };
            if (!valueSetting.equals(getValueSettings()))
                playFeedbackSound(this);
            setValue(Math.max(1, Math.max(1, value) * multiplier));
        }

        @Override
        public ValueSettings getValueSettings() {
            int row = 0;
            int value = this.value;

            if (value > 60 * 20) {
                value = value / (60 * 20);
                row = 2;
            } else if (value > 60) {
                value = value / 20;
                row = 1;
            }

            return new ValueSettings(row, value);
        }

        @Override
        public String getClipboardKey() {
            return "Timings";
        }

    }
}

