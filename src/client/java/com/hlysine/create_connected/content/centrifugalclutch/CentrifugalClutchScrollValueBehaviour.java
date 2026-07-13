package com.hlysine.create_connected.content.centrifugalclutch;

import com.hlysine.create_connected.content.ClutchValueBox;
import com.zurrtum.create.client.foundation.blockEntity.ValueSettingsBoard;
import com.zurrtum.create.client.foundation.blockEntity.ValueSettingsFormatter;
import com.zurrtum.create.client.foundation.blockEntity.behaviour.scrollValue.KineticScrollValueBehaviour;
import com.zurrtum.create.client.foundation.utility.CreateLang;
import com.zurrtum.create.foundation.blockEntity.behaviour.ValueSettings;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;

/** Presents the signed threshold as maximum/minimum speed instead of rotation direction. */
public class CentrifugalClutchScrollValueBehaviour extends KineticScrollValueBehaviour {
    public CentrifugalClutchScrollValueBehaviour(CentrifugalClutchBlockEntity blockEntity) {
        super(Component.translatable("create_connected.centrifugal_clutch.speed_threshold"),
                blockEntity, new ClutchValueBox());
    }

    @Override
    public ValueSettingsBoard createBoard(Player player, BlockHitResult hitResult) {
        List<Component> rows = List.of(
                Component.translatable("create_connected.centrifugal_clutch.max_speed"),
                Component.translatable("create_connected.centrifugal_clutch.min_speed"));
        return new ValueSettingsBoard(label, 256, 32, rows,
                new ValueSettingsFormatter(this::formatSettings));
    }

    @Override
    public MutableComponent formatSettings(ValueSettings settings) {
        return CreateLang.text(settings.row() == 0 ? "≤" : "≥")
                .add(CreateLang.number(Math.max(1, Math.abs(settings.value()))))
                .component();
    }
}
