package com.hlysine.create_connected.content.overstressclutch;

import com.hlysine.create_connected.content.ClutchValueBox;
import com.zurrtum.create.client.foundation.blockEntity.ValueSettingsBoard;
import com.zurrtum.create.client.foundation.blockEntity.ValueSettingsFormatter;
import com.zurrtum.create.client.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import com.zurrtum.create.client.foundation.utility.CreateLang;
import com.zurrtum.create.foundation.blockEntity.behaviour.ValueSettings;
import com.zurrtum.create.foundation.blockEntity.behaviour.scrollValue.ServerScrollValueBehaviour;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;

/** Client presentation for the synchronized uncoupling-delay behaviour. */
public class OverstressClutchScrollValueBehaviour
        extends ScrollValueBehaviour<OverstressClutchBlockEntity, ServerScrollValueBehaviour> {

    public OverstressClutchScrollValueBehaviour(OverstressClutchBlockEntity blockEntity) {
        super(Component.translatable("create_connected.overstress_clutch.uncouple_delay"),
                blockEntity, new ClutchValueBox());
        withFormatter(OverstressClutchScrollValueBehaviour::formatValue);
    }

    @Override
    public ValueSettingsBoard createBoard(Player player, BlockHitResult hitResult) {
        return new ValueSettingsBoard(label, 60, 10,
                CreateLang.translatedOptions("generic.unit", "ticks", "seconds", "minutes"),
                new ValueSettingsFormatter(this::formatSettings));
    }

    private static String formatValue(int value) {
        if (value < 60)
            return value + "t";
        if (value < 20 * 60)
            return value / 20 + "s";
        return value / 20 / 60 + "m";
    }

    private MutableComponent formatSettings(ValueSettings settings) {
        int value = Math.max(1, settings.value());
        return Component.literal(switch (settings.row()) {
            case 0 -> value + "t";
            case 1 -> "0:" + (value < 10 ? "0" : "") + value;
            default -> value + ":00";
        });
    }
}
