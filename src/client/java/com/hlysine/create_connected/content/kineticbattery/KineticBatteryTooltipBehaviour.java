package com.hlysine.create_connected.content.kineticbattery;

import com.hlysine.create_connected.ConnectedLang;
import com.zurrtum.create.client.api.goggles.IHaveGoggleInformation;
import com.zurrtum.create.client.foundation.blockEntity.behaviour.tooltip.KineticTooltipBehaviour;
import com.zurrtum.create.client.foundation.utility.CreateLang;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.List;

// addToGoggleTooltip used to live directly on KineticBatteryBlockEntity, but it never actually
// overrode anything from the real supertype chain (GeneratingKineticBlockEntity has no such method -
// confirmed absent from the sources jar), and it used client-only ConnectedLang/CreateLang from
// common-sourceset code - a real cross-boundary bug (see PORTING_NOTES.md session 15/16). Extracted
// here, mirroring FluidVesselTooltipBehaviour/Create Fly's own FluidTankTooltipBehaviour pattern:
// registered per-block-entity-type via BlockEntityBehaviour.addClient() in
// CreateConnectedClient.onInitializeClient(), not implemented on the block entity itself.
public class KineticBatteryTooltipBehaviour extends KineticTooltipBehaviour<KineticBatteryBlockEntity> implements IHaveGoggleInformation {
    public KineticBatteryTooltipBehaviour(KineticBatteryBlockEntity be) {
        super(be);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        ConnectedLang.translate("battery.status", blockEntity.getBatteryStatusTextComponent().withStyle(ChatFormatting.GREEN))
                .forGoggles(tooltip);
        ConnectedLang.builder().add(ConnectedLang.translateDirect("battery.charge")
                        .withStyle(ChatFormatting.GRAY)
                        .append(" ")
                        .append(KineticBatteryBlockEntity.barComponent(0, KineticBatteryBlockEntity.getCrudeBatteryLevel(blockEntity.getBatteryLevel(), 20), 20)))
                .forGoggles(tooltip);
        ConnectedLang.number(blockEntity.getBatteryLevel() / 3600 / 20)
                .style(ChatFormatting.BLUE)
                .add(ConnectedLang.text(" / ")
                        .style(ChatFormatting.GRAY))
                .add(ConnectedLang.number(KineticBatteryBlockEntity.getMaxBatteryLevel() / 3600 / 20)
                        .add(Component.literal(" "))
                        .add(ConnectedLang.translate("generic.unit.su_hours"))
                        .style(ChatFormatting.DARK_GRAY))
                .forGoggles(tooltip, 1);
        if (KineticBatteryBlock.isDischarging(blockEntity.getBlockState()) && blockEntity.getBatteryLevel() > 0) {
            ConnectedLang.translate("battery.consumption")
                    .style(ChatFormatting.GRAY)
                    .forGoggles(tooltip);
            if (blockEntity.getRawConsumedStress() == 0 && blockEntity.getConsumedStress() > 0) {
                CreateLang.number(blockEntity.getConsumedStress())
                        .translate("generic.unit.stress")
                        .style(ChatFormatting.BLUE)
                        .space()
                        .add(ConnectedLang.translate("battery.powering_belts").style(ChatFormatting.DARK_GRAY))
                        .forGoggles(tooltip, 1);
            } else {
                CreateLang.number(blockEntity.getConsumedStress())
                        .translate("generic.unit.stress")
                        .style(ChatFormatting.BLUE)
                        .forGoggles(tooltip, 1);
            }
        }
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        return true;
    }
}
