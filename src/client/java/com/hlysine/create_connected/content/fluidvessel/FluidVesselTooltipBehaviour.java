package com.hlysine.create_connected.content.fluidvessel;

import com.hlysine.create_connected.ConnectedLang;
import com.zurrtum.create.AllBlocks;
import com.zurrtum.create.api.stress.BlockStressValues;
import com.zurrtum.create.client.api.goggles.IHaveGoggleInformation;
import com.zurrtum.create.client.foundation.blockEntity.behaviour.tooltip.TooltipBehaviour;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.List;

// com.zurrtum.create.client.api.goggles.IHaveGoggleInformation is client-only - FluidVesselBlockEntity
// used to implement it directly, a real cross-boundary bug (main-sourceset referencing a client-only
// type), caught only by a real ./gradlew compileJava run (the combined-classpath javac-direct
// workaround can't see this class of bug - see PORTING_NOTES.md). Extracted here, mirroring Create
// Fly's own real FluidTankTooltipBehaviour/AllBlockEntityBehaviours pattern: goggle tooltip info is
// registered per-block-entity-type via BlockEntityBehaviour.CLIENT_REGISTRY (see
// CreateConnectedClient.onInitializeClient()), not implemented directly on the block entity class.
public class FluidVesselTooltipBehaviour extends TooltipBehaviour<FluidVesselBlockEntity> implements IHaveGoggleInformation {
    public FluidVesselTooltipBehaviour(FluidVesselBlockEntity be) {
        super(be);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        FluidVesselBlockEntity controllerBE = blockEntity.getControllerBE();
        if (controllerBE == null)
            return false;
        if (addBoilerToGoggleTooltip(controllerBE.boiler, tooltip, isPlayerSneaking, controllerBE.getTotalTankSize()))
            return true;
        return containedFluidTooltip(tooltip, isPlayerSneaking, controllerBE.fluidCapability);
    }

    // Moved here from com.hlysine.create_connected.content.fluidvessel.BoilerData.addToGoggleTooltip
    // (common sourceset) - that method used the client-only CreateLang/LangBuilder fluent API from
    // common code, a real cross-boundary bug (see BoilerData.java's own comment for the full writeup
    // and the real Create Fly precedent this follows: common BoilerData has no addToGoggleTooltip at
    // all, it's a separate client-only FluidTankTooltipBehaviour there too). Logic is otherwise
    // unchanged, just using ConnectedLang (legal here) instead of CreateLang, and reading BoilerData's
    // package-private fields directly (same package name across source sets).
    private static boolean addBoilerToGoggleTooltip(BoilerData boiler, List<Component> tooltip, boolean isPlayerSneaking, int boilerSize) {
        if (!boiler.isActive())
            return false;

        boiler.calcMinMaxForSize(boilerSize);

        if (boiler.configLevelCap < 18)
            ConnectedLang.translate("boiler.status", boiler.getHeatLevelTextComponent().withStyle(ChatFormatting.GREEN).append(Component.literal(" / " + boiler.configLevelCap).withStyle(ChatFormatting.GRAY)))
                    .forGoggles(tooltip);
        else
            ConnectedLang.translate("boiler.status", boiler.getHeatLevelTextComponent().withStyle(ChatFormatting.GREEN))
                    .forGoggles(tooltip);
        ConnectedLang.builder().add(boiler.getSizeComponent(true, false)).forGoggles(tooltip, 1);
        ConnectedLang.builder().add(boiler.getWaterComponent(true, false)).forGoggles(tooltip, 1);
        ConnectedLang.builder().add(boiler.getHeatComponent(true, false)).forGoggles(tooltip, 1);

        if (boiler.attachedEngines == 0)
            return true;

        int boilerLevel = Math.min(boiler.activeHeat, Math.min(boiler.maxHeatForWater, boiler.maxHeatForSize));
        double totalSU = boiler.getEngineEfficiency(boilerSize) * 16 * Math.max(boilerLevel, boiler.attachedEngines)
                * BlockStressValues.getCapacity(AllBlocks.STEAM_ENGINE);

        tooltip.add(CommonComponents.EMPTY);

        if (boiler.attachedEngines > 0 && boiler.maxHeatForSize > 0 && boiler.maxHeatForWater == 0 && (boiler.passiveHeat ? 1 : boiler.activeHeat) > 0) {
            ConnectedLang.translate("boiler.water_input_rate")
                    .style(ChatFormatting.GRAY)
                    .forGoggles(tooltip);
            ConnectedLang.number(boiler.waterSupply)
                    .style(ChatFormatting.BLUE)
                    .add(ConnectedLang.translate("generic.unit.millibuckets"))
                    .add(ConnectedLang.text(" / ")
                            .style(ChatFormatting.GRAY))
                    .add(ConnectedLang.translate("boiler.per_tick", ConnectedLang.number(BoilerData.waterSupplyPerLevel)
                                    .add(ConnectedLang.translate("generic.unit.millibuckets")))
                            .style(ChatFormatting.DARK_GRAY))
                    .forGoggles(tooltip, 1);
            return true;
        }

        ConnectedLang.translate("tooltip.capacityProvided")
                .style(ChatFormatting.GRAY)
                .forGoggles(tooltip);

        ConnectedLang.number(totalSU)
                .translate("generic.unit.stress")
                .style(ChatFormatting.AQUA)
                .space()
                .add((boiler.attachedEngines == 1 ? ConnectedLang.translate("boiler.via_one_engine")
                        : ConnectedLang.translate("boiler.via_engines", boiler.attachedEngines)).style(ChatFormatting.DARK_GRAY))
                .forGoggles(tooltip, 1);

        return true;
    }
}
