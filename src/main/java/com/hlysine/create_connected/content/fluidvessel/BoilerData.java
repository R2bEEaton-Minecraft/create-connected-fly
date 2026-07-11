package com.hlysine.create_connected.content.fluidvessel;

import com.hlysine.create_connected.config.CServer;
import com.zurrtum.create.AllBlocks;
import com.zurrtum.create.AllSoundEvents;
import com.zurrtum.create.api.boiler.BoilerHeater;
import com.zurrtum.create.api.stress.BlockStressValues;
import com.zurrtum.create.content.decoration.steamWhistle.WhistleBlock;
import com.zurrtum.create.content.decoration.steamWhistle.WhistleBlockEntity;
import com.zurrtum.create.content.fluids.tank.FluidTankBlockEntity;
import com.zurrtum.create.content.fluids.tank.SoundPool;
import com.zurrtum.create.content.kinetics.steamEngine.SteamEngineBlock;
import com.zurrtum.create.foundation.advancement.AdvancementBehaviour;
import com.zurrtum.create.AllAdvancements;
import com.zurrtum.create.client.foundation.utility.CreateLang;
import joptsimple.internal.Strings;
import com.zurrtum.create.catnip.animation.LerpedFloat;
import com.zurrtum.create.catnip.data.Iterate;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import com.zurrtum.create.infrastructure.fluids.FluidStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static net.minecraft.core.Direction.Axis;

public class BoilerData extends com.zurrtum.create.content.fluids.tank.BoilerData {

    static final int SAMPLE_RATE = 5;

    private static final int waterSupplyPerLevel = 10;
    private static final float passiveEngineEfficiency = 1 / 8f;

    // pooled water supply
    int gatheredSupply;
    float[] supplyOverTime = new float[10];
    int ticksUntilNextSample;
    int currentIndex;
    int configLevelCap = 18;

    // display
    private int maxHeatForSize = 0;
    private int maxHeatForWater = 0;
    private int minValue = 0;
    private int maxValue = 0;

    // client only sound control

    // re-use the same lambda for each side
    private final SoundPool.Sound sound = (level, pos) -> {
        float volume = 3f / Math.max(2, attachedEngines / 6);
        float pitch = 1.18f - level.random.nextFloat() * .25f;
        level.playLocalSound(pos.getX(), pos.getY(), pos.getZ(),
                SoundEvents.CANDLE_EXTINGUISH, SoundSource.BLOCKS, volume, pitch, false);

        AllSoundEvents.STEAM.playAt(level, pos, volume / 16, .8f, false);
    };
    // separate pools for each side so they sound distinct when standing at corners of the boiler
    private final EnumMap<Direction, SoundPool> pools = new EnumMap<>(Direction.class);


    @Override
    public void tick(FluidTankBlockEntity controller) {
        if (!isActive())
            return;

        configLevelCap = CServer.VesselMaxLevel.get();

        Level level = controller.getLevel();
        if (level.isClientSide()) {
            pools.values().forEach(p -> p.play(level));
            gauge.tickChaser();
            float current = gauge.getValue(1);
            if (current > 1 && level.random.nextFloat() < 1 / 2f)
                gauge.setValueNoUpdate(current + Math.min(-(current - 1) * level.random.nextFloat(), 0));
            return;
        }
        if (needsHeatLevelUpdate && updateTemperature(controller))
            controller.notifyUpdate();
        ticksUntilNextSample--;
        if (ticksUntilNextSample > 0)
            return;
        int capacity = controller.getTankInventory().getMaxAmountPerStack();
        if (capacity == 0)
            return;

        ticksUntilNextSample = SAMPLE_RATE;
        supplyOverTime[currentIndex] = gatheredSupply / (float) SAMPLE_RATE;
        waterSupply = Math.max(waterSupply, supplyOverTime[currentIndex]);
        currentIndex = (currentIndex + 1) % supplyOverTime.length;
        gatheredSupply = 0;

        if (currentIndex == 0) {
            waterSupply = 0;
            for (float i : supplyOverTime)
                waterSupply = Math.max(i, waterSupply);
        }

        if (controller instanceof CreativeFluidVesselBlockEntity)
            waterSupply = waterSupplyPerLevel * 20;

        if (getActualHeat(controller.getTotalTankSize()) == 18)
            controller.award(AllAdvancements.STEAM_ENGINE_MAXED);

        controller.notifyUpdate();
    }

    public void updateOcclusion(FluidTankBlockEntity base) {
        if (!(base instanceof FluidVesselBlockEntity controller)) {
            super.updateOcclusion(base);
            return;
        }
        if (!controller.getLevel().isClientSide())
            return;
        if (attachedEngines + attachedWhistles == 0)
            return;
        for (Direction d : Iterate.horizontalDirections) {
            boolean alongLength = d.getAxis() == controller.getAxis();
            int size = alongLength ? controller.getHeight() : controller.getWidth();
            AABB aabb =
                    new AABB(controller.getBlockPos()).move(size / 2f - .5f, 0, size / 2f - .5f)
                            .deflate(5f / 8);
            aabb = aabb.move(d.getStepX() * (size / 2f + 1 / 4f), 0,
                    d.getStepZ() * (size / 2f + 1 / 4f));
            aabb = aabb.inflate(Math.abs(d.getStepZ()) / 2f, 0.25f, Math.abs(d.getStepX()) / 2f);
            occludedDirections[d.get2DDataValue()] = !controller.getLevel()
                    .noCollision(aabb);
        }
    }

    @Override
    public int getMaxHeatLevelForWaterSupply() {
        return (int) Math.min(18, Mth.ceil(waterSupply) / waterSupplyPerLevel);
    }

    @Override
    public boolean isPassive() {
        return passiveHeat && maxHeatForSize > 0 && maxHeatForWater > 0;
    }

    @Override
    public boolean isPassive(int boilerSize) {
        calcMinMaxForSize(boilerSize);
        return isPassive();
    }

    public void queueSoundOnSide(BlockPos pos, Direction side) {
        SoundPool pool = pools.get(side);
        if (pool == null) {
            pool = new SoundPool(4, 2, sound);
            pools.put(side, pool);
        }
        pool.queueAt(pos);
    }

    @Override
    public float getEngineEfficiency(int boilerSize) {
        if (isPassive(boilerSize))
            return passiveEngineEfficiency / attachedEngines * CServer.VesselBoilerStressMultiplier.get().floatValue();
        if (activeHeat == 0)
            return 0;
        int actualHeat = getActualHeat(boilerSize);
        return (attachedEngines <= actualHeat ? 1 : (float) actualHeat / attachedEngines) * CServer.VesselBoilerStressMultiplier.get().floatValue();
    }

    private int getActualHeat(int boilerSize) {
        int forBoilerSize = getMaxHeatLevelForBoilerSize(boilerSize);
        int forWaterSupply = getMaxHeatLevelForWaterSupply();
        int actualHeat = Math.min(Math.min(activeHeat, Math.min(forWaterSupply, forBoilerSize)), configLevelCap);
        return actualHeat;
    }

    // Not an override - real com.zurrtum.create.content.fluids.tank.BoilerData has no
    // addToGoggleTooltip at all (Create Fly moved this to a separate FluidTankTooltipBehaviour
    // client class instead); this mod's own BoilerData subclass adds it directly as a new method,
    // called straight off the `boiler` field from FluidVesselBlockEntity.addToGoggleTooltip().
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking, int boilerSize) {
        if (!isActive())
            return false;

        calcMinMaxForSize(boilerSize);

        if (configLevelCap < 18)
            CreateLang.translate("boiler.status", getHeatLevelTextComponent().withStyle(ChatFormatting.GREEN).append(Component.literal(" / " + configLevelCap).withStyle(ChatFormatting.GRAY)))
                    .forGoggles(tooltip);
        else
            CreateLang.translate("boiler.status", getHeatLevelTextComponent().withStyle(ChatFormatting.GREEN))
                    .forGoggles(tooltip);
        CreateLang.builder().add(getSizeComponent(true, false)).forGoggles(tooltip, 1);
        CreateLang.builder().add(getWaterComponent(true, false)).forGoggles(tooltip, 1);
        CreateLang.builder().add(getHeatComponent(true, false)).forGoggles(tooltip, 1);

        if (attachedEngines == 0)
            return true;

        int boilerLevel = Math.min(activeHeat, Math.min(maxHeatForWater, maxHeatForSize));
        double totalSU = getEngineEfficiency(boilerSize) * 16 * Math.max(boilerLevel, attachedEngines)
                * BlockStressValues.getCapacity(AllBlocks.STEAM_ENGINE);

        tooltip.add(CommonComponents.EMPTY);

        if (attachedEngines > 0 && maxHeatForSize > 0 && maxHeatForWater == 0 && (passiveHeat ? 1 : activeHeat) > 0) {
            CreateLang.translate("boiler.water_input_rate")
                    .style(ChatFormatting.GRAY)
                    .forGoggles(tooltip);
            CreateLang.number(waterSupply)
                    .style(ChatFormatting.BLUE)
                    .add(CreateLang.translate("generic.unit.millibuckets"))
                    .add(CreateLang.text(" / ")
                            .style(ChatFormatting.GRAY))
                    .add(CreateLang.translate("boiler.per_tick", CreateLang.number(waterSupplyPerLevel)
                                    .add(CreateLang.translate("generic.unit.millibuckets")))
                            .style(ChatFormatting.DARK_GRAY))
                    .forGoggles(tooltip, 1);
            return true;
        }

        CreateLang.translate("tooltip.capacityProvided")
                .style(ChatFormatting.GRAY)
                .forGoggles(tooltip);

        CreateLang.number(totalSU)
                .translate("generic.unit.stress")
                .style(ChatFormatting.AQUA)
                .space()
                .add((attachedEngines == 1 ? CreateLang.translate("boiler.via_one_engine")
                        : CreateLang.translate("boiler.via_engines", attachedEngines)).style(ChatFormatting.DARK_GRAY))
                .forGoggles(tooltip, 1);

        return true;
    }

    @Override
    public void calcMinMaxForSize(int boilerSize) {
        maxHeatForSize = getMaxHeatLevelForBoilerSize(boilerSize);
        maxHeatForWater = getMaxHeatLevelForWaterSupply();

        minValue = Math.min(configLevelCap, Math.min(passiveHeat ? 1 : activeHeat, Math.min(maxHeatForWater, maxHeatForSize)));
        maxValue = Math.max(passiveHeat ? 1 : activeHeat, Math.max(maxHeatForWater, maxHeatForSize));
    }

    @Override
    @NotNull
    public MutableComponent getHeatLevelTextComponent() {
        int boilerLevel = Math.min(Math.min(activeHeat, Math.min(maxHeatForWater, maxHeatForSize)), configLevelCap);

        return isPassive() ? CreateLang.translateDirect("boiler.passive")
                : (boilerLevel == 0 ? CreateLang.translateDirect("boiler.idle")
                : boilerLevel == 18 ? CreateLang.translateDirect("boiler.max_lvl")
                : CreateLang.translateDirect("boiler.lvl", String.valueOf(boilerLevel)));
    }

    @Override
    public MutableComponent getSizeComponent(boolean forGoggles, boolean useBlocksAsBars, ChatFormatting... styles) {
        return componentHelper("size", maxHeatForSize, forGoggles, useBlocksAsBars, styles);
    }

    @Override
    public MutableComponent getWaterComponent(boolean forGoggles, boolean useBlocksAsBars, ChatFormatting... styles) {
        return componentHelper("water", maxHeatForWater, forGoggles, useBlocksAsBars, styles);
    }

    @Override
    public MutableComponent getHeatComponent(boolean forGoggles, boolean useBlocksAsBars, ChatFormatting... styles) {
        return componentHelper("heat", passiveHeat ? 1 : activeHeat, forGoggles, useBlocksAsBars, styles);
    }

    private MutableComponent componentHelper(String label, int level, boolean forGoggles, boolean useBlocksAsBars,
                                             ChatFormatting... styles) {
        MutableComponent base = useBlocksAsBars ? blockComponent(level) : barComponent(level);

        if (!forGoggles)
            return base;

        ChatFormatting style1 = styles.length >= 1 ? styles[0] : ChatFormatting.GRAY;
        ChatFormatting style2 = styles.length >= 2 ? styles[1] : ChatFormatting.DARK_GRAY;

        return CreateLang.translateDirect("boiler." + label)
                .withStyle(style1)
                .append(CreateLang.translateDirect("boiler." + label + "_dots")
                        .withStyle(style2))
                .append(base);
    }

    private MutableComponent blockComponent(int level) {
        return Component.literal("" + "\u2588".repeat(minValue) + "\u2592".repeat(level - minValue) + "\u2591".repeat(maxValue - level));
    }

    private MutableComponent barComponent(int level) {
        return Component.empty()
                .append(bars(Math.max(0, minValue - 1), ChatFormatting.DARK_GREEN))
                .append(bars(minValue > 0 ? 1 : 0, ChatFormatting.GREEN))
                .append(bars(Math.max(0, level - minValue), ChatFormatting.DARK_GREEN))
                .append(bars(Math.max(0, maxValue - level), ChatFormatting.DARK_RED))
                .append(bars(Math.max(0, Math.min(18 - maxValue, ((maxValue / 5 + 1) * 5) - maxValue)),
                        ChatFormatting.DARK_GRAY));

    }

    private MutableComponent bars(int level, ChatFormatting format) {
        return Component.literal(Strings.repeat('|', level))
                .withStyle(format);
    }

    @Override
    public boolean evaluate(FluidTankBlockEntity base) {
        if (!(base instanceof FluidVesselBlockEntity controller))
            return super.evaluate(base);

        BlockPos controllerPos = controller.getBlockPos();
        Level level = controller.getLevel();
        configLevelCap = CServer.VesselMaxLevel.get();
        int prevEngines = attachedEngines;
        int prevWhistles = attachedWhistles;
        attachedEngines = 0;
        attachedWhistles = 0;

        Axis axis = controller.getAxis();
        for (int yOffset = 0; yOffset < controller.getWidth(); yOffset++) {
            for (int lengthOffset = 0; lengthOffset < controller.getHeight(); lengthOffset++) {
                for (int widthOffset = 0; widthOffset < controller.getWidth(); widthOffset++) {

                    BlockPos pos = controllerPos.offset(
                            axis == Axis.X ? lengthOffset : widthOffset,
                            yOffset,
                            axis == Axis.Z ? lengthOffset : widthOffset
                    );
                    BlockState blockState = level.getBlockState(pos);
                    if (!FluidVesselBlock.isVessel(blockState))
                        continue;
                    for (Direction d : Iterate.directions) {
                        BlockPos attachedPos = pos.relative(d);
                        BlockState attachedState = level.getBlockState(attachedPos);
                        if (attachedState.is(AllBlocks.STEAM_ENGINE) && SteamEngineBlock.getFacing(attachedState) == d)
                            attachedEngines++;
                        if (attachedState.is(AllBlocks.STEAM_WHISTLE)
                                && WhistleBlock.getAttachedDirection(attachedState)
                                .getOpposite() == d)
                            attachedWhistles++;
                    }
                }
            }
        }

        needsHeatLevelUpdate = true;
        return prevEngines != attachedEngines || prevWhistles != attachedWhistles;
    }

    @Override
    public void checkPipeOrganAdvancement(FluidTankBlockEntity base) {
        if (!(base instanceof FluidVesselBlockEntity controller)) {
            super.checkPipeOrganAdvancement(base);
            return;
        }

        if (!controller.getBehaviour(AdvancementBehaviour.TYPE)
                .isOwnerPresent())
            return;

        BlockPos controllerPos = controller.getBlockPos();
        Level level = controller.getLevel();
        Set<Integer> whistlePitches = new HashSet<>();

        Axis axis = controller.getAxis();
        for (int yOffset = 0; yOffset < controller.getWidth(); yOffset++) {
            for (int lengthOffset = 0; lengthOffset < controller.getHeight(); lengthOffset++) {
                for (int widthOffset = 0; widthOffset < controller.getWidth(); widthOffset++) {

                    BlockPos pos = controllerPos.offset(
                            axis == Axis.X ? lengthOffset : widthOffset,
                            yOffset,
                            axis == Axis.Z ? lengthOffset : widthOffset
                    );
                    BlockState blockState = level.getBlockState(pos);
                    if (!FluidVesselBlock.isVessel(blockState))
                        continue;
                    for (Direction d : Iterate.directions) {
                        BlockPos attachedPos = pos.relative(d);
                        BlockState attachedState = level.getBlockState(attachedPos);
                        if (attachedState.is(AllBlocks.STEAM_WHISTLE)
                                && WhistleBlock.getAttachedDirection(attachedState)
                                .getOpposite() == d) {
                            if (level.getBlockEntity(attachedPos) instanceof WhistleBlockEntity wbe)
                                whistlePitches.add(wbe.getPitchId());
                        }
                    }
                }
            }
        }

        if (whistlePitches.size() >= 12)
            controller.award(AllAdvancements.PIPE_ORGAN);
    }

    @Override
    public boolean updateTemperature(FluidTankBlockEntity base) {
        if (!(base instanceof FluidVesselBlockEntity controller))
            return super.updateTemperature(base);

        BlockPos controllerPos = controller.getBlockPos();
        Level level = controller.getLevel();
        needsHeatLevelUpdate = false;

        boolean prevPassive = passiveHeat;
        int prevActive = activeHeat;
        passiveHeat = false;
        activeHeat = 0;

        Axis axis = controller.getAxis();
        for (int lengthOffset = 0; lengthOffset < controller.getHeight(); lengthOffset++) {
            for (int widthOffset = 0; widthOffset < controller.getWidth(); widthOffset++) {
                BlockPos pos = controllerPos.offset(
                        axis == Axis.X ? lengthOffset : widthOffset,
                        -1,
                        axis == Axis.Z ? lengthOffset : widthOffset
                );
                BlockState blockState = level.getBlockState(pos);
                float heat = BoilerHeater.findHeat(level, pos, blockState);
                if (heat == 0) {
                    passiveHeat = true;
                } else if (heat > 0) {
                    activeHeat += heat;
                }
            }
        }

        activeHeat = Math.max(0, Math.min(18, (int) Math.floor(activeHeat * CServer.VesselHeatMultiplier.get().floatValue())));
        passiveHeat &= activeHeat == 0;

        return prevActive != activeHeat || prevPassive != passiveHeat;
    }

    @Override
    public void clear() {
        waterSupply = 0;
        activeHeat = 0;
        passiveHeat = false;
        attachedEngines = 0;
        Arrays.fill(supplyOverTime, 0);
    }

    @Override
    public void write(ValueOutput view) {
        view.putFloat("Supply", waterSupply);
        view.putInt("ActiveHeat", activeHeat);
        view.putBoolean("PassiveHeat", passiveHeat);
        view.putInt("Engines", attachedEngines);
        view.putInt("Whistles", attachedWhistles);
        view.putBoolean("Update", needsHeatLevelUpdate);
    }

    @Override
    public void read(ValueInput view, int boilerSize) {
        waterSupply = view.getFloatOr("Supply", 0f);
        activeHeat = view.getIntOr("ActiveHeat", 0);
        passiveHeat = view.getBooleanOr("PassiveHeat", false);
        attachedEngines = view.getIntOr("Engines", 0);
        attachedWhistles = view.getIntOr("Whistles", 0);
        needsHeatLevelUpdate = view.getBooleanOr("Update", false);
        Arrays.fill(supplyOverTime, (int) waterSupply);

        int forBoilerSize = getMaxHeatLevelForBoilerSize(boilerSize);
        int forWaterSupply = getMaxHeatLevelForWaterSupply();
        int actualHeat = Math.min(activeHeat, Math.min(forWaterSupply, forBoilerSize));
        float target = isPassive(boilerSize) ? 1 / 8f : forBoilerSize == 0 ? 0 : actualHeat / (forBoilerSize * 1f);
        gauge.chase(target, 0.125f, LerpedFloat.Chaser.EXP);
    }

    @Override
    public BoilerFluidHandler createHandler() {
        return new BoilerFluidHandler();
    }

    public class BoilerFluidHandler extends com.zurrtum.create.content.fluids.tank.BoilerData.BoilerFluidHandler {

        // Real Create Fly's FluidInventory has no fill()/FluidAction concept (NeoForge-only API) -
        // the base class already accumulates into its own private `fill` field via setStack() and
        // flushes it on markDirty(); overriding setStack() to route straight into our own shadowed
        // gatheredSupply field (see the class-level comment on that field) reproduces the same
        // "gather insertions, sample on tick()" behavior without going through the base's flush path.
        @Override
        public void setStack(int slot, FluidStack stack) {
            if (!isValid(slot, stack))
                return;
            gatheredSupply += stack.getAmount();
        }

    }

}
