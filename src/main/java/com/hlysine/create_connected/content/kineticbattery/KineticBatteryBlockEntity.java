package com.hlysine.create_connected.content.kineticbattery;

import com.hlysine.create_connected.CreateConnected;
import com.hlysine.create_connected.config.CServer;
import com.hlysine.create_connected.content.ISplitShaftBlockEntity;
import com.hlysine.create_connected.datagen.advancements.AdvancementBehaviour;
import com.hlysine.create_connected.datagen.advancements.CCAdvancements;
import com.hlysine.create_connected.mixin.kineticbattery.KineticNetworkAccessor;
import com.hlysine.create_connected.registries.CCDataComponents;
import com.zurrtum.create.content.contraptions.bearing.WindmillBearingBlockEntity;
import com.zurrtum.create.content.kinetics.KineticNetwork;
import com.zurrtum.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.zurrtum.create.content.kinetics.base.KineticBlockEntity;
import com.zurrtum.create.content.kinetics.belt.BeltBlock;
import com.zurrtum.create.content.redstone.thresholdSwitch.ThresholdSwitchObservable;
import com.zurrtum.create.api.behaviour.BlockEntityBehaviour;
import com.zurrtum.create.foundation.blockEntity.behaviour.scrollValue.ServerScrollOptionBehaviour;
import joptsimple.internal.Strings;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Math;

import java.util.List;

import static com.hlysine.create_connected.content.kineticbattery.KineticBatteryBlock.*;

public class KineticBatteryBlockEntity extends GeneratingKineticBlockEntity implements ISplitShaftBlockEntity, ThresholdSwitchObservable {

    private static final int SYNC_RATE = 20;

    private double batteryLevel;
    private DataComponentPatch componentPatch = DataComponentPatch.EMPTY;

    private int syncCooldown;
    protected boolean queuedSync;
    private float consumedStress = -1;
    private boolean applyMinStress = false;

    // Real Create Fly's own com.zurrtum.create.client...ScrollOptionBehaviour is client-only, is
    // abstract (can't be instantiated directly), and has a completely different constructor shape
    // (icon getter + label + ValueBoxTransform) than what was used here before - it's a *rendering
    // wrapper* around a ServerScrollOptionBehaviour instance, not something to construct directly in
    // common-sourceset addBehaviours(). The real common-sourceset value-storage type (used by Create
    // Fly's own WindmillBearingBlockEntity, whose RotationDirection enum this field reuses) is
    // com.zurrtum.create.foundation.blockEntity.behaviour.scrollValue.ServerScrollOptionBehaviour -
    // see KineticBatteryScrollOptionBehaviour (src/client/java) for the client-side rendering
    // counterpart, registered separately via BlockEntityBehaviour.addClient() in
    // CreateConnectedClient.onInitializeClient() (mirrors the FluidVesselTooltipBehaviour /
    // AllBlockEntityBehaviours pattern - see PORTING_NOTES.md session 15).
    protected ServerScrollOptionBehaviour<WindmillBearingBlockEntity.RotationDirection> movementDirection;

    public KineticBatteryBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour<?>> behaviours) {
        super.addBehaviours(behaviours);
        movementDirection = new ServerScrollOptionBehaviour<>(WindmillBearingBlockEntity.RotationDirection.class, this);
        movementDirection.withCallback(i -> {
            updateGeneratedRotation();
            sendDataImmediately();
        });
        behaviours.add(movementDirection);
        AdvancementBehaviour.registerAwardables(this, behaviours, CCAdvancements.KINETIC_BATTERY);
    }

    @Override
    public void initialize() {
        super.initialize();
        updateLevel();
        updateGeneratedRotation();
    }

    public static double getMaxBatteryLevel() {
        return CServer.BatteryCapacity.get() * 3600 * 20;
    }

    public static int getDischargeRPM() {
        return CServer.BatteryDischargeRPM.get();
    }

    public static int getCrudeBatteryLevel(double level, int totalLevels) {
        if (level >= getMaxBatteryLevel())
            return totalLevels;
        if (level <= 0)
            return 0;
        return (int) Math.floor((level / getMaxBatteryLevel()) * (totalLevels - 1)) + 1;
    }

    @Override
    public void tick() {
        super.tick();

        if (syncCooldown > 0) {
            syncCooldown--;
            if (syncCooldown == 0 && queuedSync) {
                if (!getLevel().isClientSide() && isDischarging(getBlockState()) && batteryLevel > 0) {
                    updateMinStress();
                }
                sendData();
            }
        }

        if (getSpeed() == 0 || !hasNetwork())
            return;

        boolean changed = false;
        if (isDischarging(getBlockState())) {
            if (batteryLevel > 0) {
                if (lastCapacityProvided == 0) {
                    calculateAddedStressCapacity();
                }
                if (consumedStress < 0) {
                    updateConsumedStress();
                }
                batteryLevel = Math.max(batteryLevel - getConsumedStress(), 0);
                changed = true;
            }
        } else {
            if (batteryLevel < getMaxBatteryLevel() && capacity > 0) {
                if (lastStressApplied == 0) {
                    calculateStressApplied();
                }
                batteryLevel = Math.min(batteryLevel + lastStressApplied * Math.abs(getTheoreticalSpeed()), getMaxBatteryLevel());
                changed = true;
            }
        }
        if (changed)
            updateLevel();
    }

    @Override
    public void updateFromNetwork(float maxStress, float currentStress, int networkSize) {
        super.updateFromNetwork(maxStress, currentStress, networkSize);
        updateConsumedStress();
    }

    private void updateConsumedStress() {
        if (getLevel().isClientSide()) {
            if (consumedStress < 0) {
                consumedStress = 0;
            }
            return;
        }
        KineticNetwork network = getOrCreateNetwork();

        float presentCapacity = 0;
        int batteryCount = 0;
        for (KineticBlockEntity be : network.sources.keySet()) {
            if (be instanceof KineticBatteryBlockEntity) {
                batteryCount += 1;
                continue;
            }
            presentCapacity += network.getActualCapacityOf(be);
        }
        float batteryCapacity = stress - presentCapacity - ((KineticNetworkAccessor) network).getUnloadedStress();
        if (batteryCapacity <= 0) {
            consumedStress = 0;
        } else {
            consumedStress = batteryCapacity / batteryCount;
        }

        updateMinStress();
        sendDataImmediately();
    }

    private void updateMinStress() {
        if (stress > CServer.BatteryMinDischarge.get().floatValue()) {
            applyMinStress = false;
            return;
        }
        KineticNetwork network = getOrCreateNetwork();
        applyMinStress = false;
        for (KineticBlockEntity be : network.members.keySet()) {
            if (BeltBlock.canTransportObjects(be.getBlockState())) {
                applyMinStress = true;
                break;
            }
        }
    }

    // Exposes the raw (pre min-stress-override) value for KineticBatteryTooltipBehaviour (client) to
    // distinguish "the min-discharge override is why getConsumedStress() > 0" from "genuinely
    // consuming stress" the same way the old in-class addToGoggleTooltip body used to.
    public float getRawConsumedStress() {
        return consumedStress;
    }

    public float getConsumedStress() {
        if (applyMinStress) {
            return Math.max(CServer.BatteryMinDischarge.get().floatValue(), consumedStress);
        }
        return Math.max(0, consumedStress);
    }

    private void updateLevel() {
        int crudeLevel = getCrudeBatteryLevel(getBatteryLevel(), 5);
        int oldLevel = getBlockState().getValue(LEVEL);
        if (oldLevel != crudeLevel) {
            if (crudeLevel == 5) {
                AdvancementBehaviour.tryAward(this, CCAdvancements.KINETIC_BATTERY);
            }
            switchToBlockState(getLevel(), getBlockPos(), getBlockState().setValue(LEVEL, crudeLevel));
        }
        sendData();
    }

    public WindmillBearingBlockEntity.RotationDirection getRotationDirection() {
        return movementDirection.get();
    }

    public void setRotationDirection(WindmillBearingBlockEntity.RotationDirection direction) {
        movementDirection.setValue(direction.ordinal());
    }

    public double getBatteryLevel() {
        return batteryLevel;
    }

    public void setBatteryLevel(double batteryLevel) {
        this.batteryLevel = batteryLevel;
        updateLevel();
        sendDataImmediately();
    }

    public void setComponentPatch(DataComponentPatch componentPatch) {
        this.componentPatch = componentPatch;
    }

    public DataComponentPatch getComponentPatch() {
        return componentPatch;
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter componentInput) {
        setBatteryLevel(componentInput.getOrDefault(CCDataComponents.KINETIC_BATTERY_CHARGE, 0.0));
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        components.set(CCDataComponents.KINETIC_BATTERY_CHARGE, getBatteryLevel());
    }

    public void sendDataImmediately() {
        syncCooldown = 0;
        queuedSync = false;
        sendData();
    }

    @Override
    public void sendData() {
        if (syncCooldown > 0) {
            queuedSync = true;
            return;
        }
        super.sendData();
        queuedSync = false;
        syncCooldown = SYNC_RATE;
    }

    @Override
    public float getGeneratedSpeed() {
        if (!isDischarging(getBlockState()) || isCurrentStageComplete(getBlockState()))
            return 0;
        return convertToDirection(getDischargeRPM(), getBlockState().getValue(FACING)) *
                (movementDirection.get() == WindmillBearingBlockEntity.RotationDirection.CLOCKWISE ? -1 : 1);
    }

    @Override
    public float calculateAddedStressCapacity() {
        if (!isDischarging(getBlockState()) || isCurrentStageComplete(getBlockState()))
            return 0;
        return super.calculateAddedStressCapacity();
    }

    @Override
    public float calculateStressApplied() {
        if (!isDischarging(getBlockState()) && !isCurrentStageComplete(getBlockState())) {
            return super.calculateStressApplied();
        } else {
            this.lastStressApplied = 0;
            return 0;
        }
    }

    public float getRotationSpeedModifier(Direction face) {
        if (face.getAxis() != getBlockState().getValue(FACING).getAxis())
            return 0;
        if (face != getBlockState().getValue(FACING)) {
            if (!isCurrentStageComplete(getBlockState()))
                return 0;
        }
        return 1;
    }

    @Override
    protected void read(ValueInput view, boolean clientPacket) {
        super.read(view, clientPacket);
        batteryLevel = view.getFloatOr("batteryLevel", 0f);
        queuedSync = view.getBooleanOr("queuedSync", false);
        consumedStress = view.getFloatOr("consumedStress", 0f);
        applyMinStress = view.getBooleanOr("applyMinStress", false);
        componentPatch = view.read("Components", DataComponentPatch.CODEC).orElse(DataComponentPatch.EMPTY);
    }

    @Override
    protected void write(ValueOutput view, boolean clientPacket) {
        super.write(view, clientPacket);
        view.putDouble("batteryLevel", batteryLevel);
        view.putBoolean("queuedSync", queuedSync);
        view.putFloat("consumedStress", consumedStress);
        view.putBoolean("applyMinStress", applyMinStress);
        view.store("Components", DataComponentPatch.CODEC, componentPatch);
    }

    // addToGoggleTooltip moved to the client-only KineticBatteryTooltipBehaviour (see
    // CreateConnectedClient.onInitializeClient()) - this override never actually corresponded to any
    // real supertype method to begin with (neither GeneratingKineticBlockEntity nor any of its
    // ancestors declare addToGoggleTooltip - confirmed absent from the real sources jar), and
    // ConnectedLang/CreateLang (used throughout the old body) are client-only, so this was always a
    // cross-boundary bug, not just a stale @Override. Mirrors the FluidVesselTooltipBehaviour
    // extraction pattern exactly (see PORTING_NOTES.md session 15).

    public MutableComponent getBatteryStatusTextComponent() {
        boolean complete = isCurrentStageComplete(getBlockState());
        boolean discharging = isDischarging(getBlockState());

        String key;
        if (discharging && !complete) {
            key = "battery.status.discharging";
        } else if (!discharging && !complete) {
            key = "battery.status.charging";
        } else if (!discharging && complete) {
            key = "battery.status.full";
        } else {
            key = "battery.status.empty";
        }
        return Component.translatable(CreateConnected.MODID + "." + key);
    }

    static MutableComponent barComponent(int minValue, int level, int maxValue) {
        return Component.empty()
                .append(bars(java.lang.Math.max(0, minValue - 1), ChatFormatting.DARK_GREEN))
                .append(bars(minValue > 0 ? 1 : 0, ChatFormatting.GREEN))
                .append(bars(java.lang.Math.max(0, level - minValue), ChatFormatting.DARK_GREEN))
                .append(bars(java.lang.Math.max(0, maxValue - level), ChatFormatting.DARK_RED))
                .append(bars(java.lang.Math.max(0, java.lang.Math.min(18 - maxValue, ((maxValue / 5 + 1) * 5) - maxValue)),
                        ChatFormatting.DARK_GRAY));

    }

    static MutableComponent bars(int level, ChatFormatting format) {
        return Component.literal(Strings.repeat('|', level))
                .withStyle(format);
    }

    @Override
    public int getMaxValue() {
        return (int) (getMaxBatteryLevel() / 3600.0 / 20.0);
    }

    @Override
    public int getMinValue() {
        return 0;
    }

    @Override
    public int getCurrentValue() {
        return (int) (batteryLevel / 3600.0 / 20.0);
    }

    // ConnectedLang (client-only LangBuilder wrapper) used to build this - replaced with plain
    // vanilla Component composition (drops LangBuilder's number-formatting niceties like thousands
    // separators, a minor disclosed simplification, not a functional regression: the translation key
    // still resolves and pluralizes correctly).
    @Override
    public MutableComponent format(int value) {
        return Component.literal(String.valueOf(value))
                .append(Component.literal(" "))
                .append(Component.translatable(CreateConnected.MODID + ".generic.unit.su_hours"));
    }
}

