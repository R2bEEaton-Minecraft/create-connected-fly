package com.hlysine.create_connected.content.kineticbattery;

import com.hlysine.create_connected.CreateConnected;
import com.zurrtum.create.content.redstone.displayLink.DisplayLinkContext;
import com.zurrtum.create.content.redstone.displayLink.source.PercentOrProgressBarDisplaySource;
import com.zurrtum.create.content.trains.display.FlapDisplayBlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.text.NumberFormat;
import java.util.Locale;

// Real Create Fly moved DisplaySource's initConfigurationWidgets (client-only, uses
// ModularGuiLineBuilder) entirely out of the base class into a client-only DisplaySourceRender
// attached via `source.attachRender`/AllDisplaySourceRenders (see e.g. KineticStressDisplaySource
// vs. KineticStressDisplaySourceRender in the real jar - verified). Following that same real
// pattern here: this class keeps only the main-safe gameplay text logic, and
// KineticBatteryDisplaySourceRender (src/client/java) holds initConfigurationWidgets, wired via
// CreateConnectedClient.onInitializeClient() setting KINETIC_BATTERY.attachRender directly (no
// AllDisplaySourceRenders-style helper needed for just one custom source). ConnectedLang (client-
// only, see its own file comment) is likewise replaced with plain Component/NumberFormat here.
public class KineticBatteryDisplaySource extends PercentOrProgressBarDisplaySource {

    @Override
    protected String getTranslationKey() {
        return "kinetic_battery";
    }

    @Override
    public boolean allowsLabeling(DisplayLinkContext context) {
        return true;
    }

    @Override
    protected Float getProgress(DisplayLinkContext context) {
        BlockEntity entity = context.getSourceBlockEntity();
        if (!(entity instanceof KineticBatteryBlockEntity kbe)) return null;
        return (float) (kbe.getBatteryLevel() / KineticBatteryBlockEntity.getMaxBatteryLevel());
    }

    @Override
    protected MutableComponent formatNumeric(DisplayLinkContext context, Float currentLevel) {
        if (context.sourceConfig().getIntOr("Mode", 0) == 1)
            return super.formatNumeric(context, currentLevel);
        long hours = Math.round(currentLevel * KineticBatteryBlockEntity.getMaxBatteryLevel() / 3600 / 20);
        MutableComponent text = Component.literal(NumberFormat.getNumberInstance(Locale.ROOT).format(hours));
        if (context.getTargetBlockEntity() instanceof FlapDisplayBlockEntity)
            text.append(Component.literal(" "));
        return text.append(Component.translatable(CreateConnected.MODID + ".generic.unit.su_hours"));
    }

    @Override
    protected boolean progressBarActive(DisplayLinkContext context) {
        return context.sourceConfig()
                .getIntOr("Mode", 0) == 2;
    }

}
