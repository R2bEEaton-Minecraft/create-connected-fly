package com.hlysine.create_connected.content.kineticbridge;

import com.google.common.collect.ImmutableList;
import com.hlysine.create_connected.ConnectedLang;
import com.zurrtum.create.foundation.blockEntity.behaviour.ValueSettings;
import com.zurrtum.create.client.foundation.blockEntity.ValueSettingsBoard;
import com.zurrtum.create.client.foundation.blockEntity.ValueSettingsFormatter;
import com.zurrtum.create.client.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import com.zurrtum.create.client.catnip.lang.LangNumberFormat;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;

public class StressImpactScrollValueBehaviour
        extends ScrollValueBehaviour<KineticBridgeBlockEntity, ServerStressImpactScrollValueBehaviour> {

    public StressImpactScrollValueBehaviour(KineticBridgeBlockEntity blockEntity) {
        super(Component.translatable("create_connected.kinetic_bridge.stress_impact"), blockEntity,
                new com.hlysine.create_connected.content.ClutchValueBox());
        withFormatter(value -> LangNumberFormat.format(convertValue(value)) + "x");
    }

    public static float convertValue(int value) {
        int absoluteValue = Math.abs(value);
        double result = Math.pow(2, absoluteValue / 10.0);
        if (absoluteValue < 40) {
            return (float) result;
        } else {
            return (int) result;
        }
    }

    @Override
    public ValueSettingsBoard createBoard(Player player, BlockHitResult hitResult) {
        ImmutableList<Component> rows = ImmutableList.of(ConnectedLang.translateDirect("kinetic_bridge.stress_impact_short"));
        ValueSettingsFormatter formatter = new ValueSettingsFormatter(this::formatSettings);
        return new ValueSettingsBoard(label, 160, 10, rows, formatter);
    }

    public MutableComponent formatSettings(ValueSettings settings) {
        return ConnectedLang.number(Math.max(0, convertValue(settings.value()))).add(Component.literal("x"))
                .component();
    }
}

