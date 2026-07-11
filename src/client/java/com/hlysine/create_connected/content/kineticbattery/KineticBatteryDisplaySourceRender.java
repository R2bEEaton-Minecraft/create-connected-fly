package com.hlysine.create_connected.content.kineticbattery;

import com.hlysine.create_connected.ConnectedLang;
import com.zurrtum.create.api.behaviour.display.DisplaySource;
import com.zurrtum.create.client.content.redstone.displayLink.source.SingleLineDisplaySourceRender;
import com.zurrtum.create.client.foundation.gui.ModularGuiLineBuilder;
import com.zurrtum.create.content.redstone.displayLink.DisplayLinkContext;

// Client-only counterpart of KineticBatteryDisplaySource, following the real Create Fly
// DisplaySourceRender split (see KineticStressDisplaySourceRender for the reference pattern).
// Registered via `KineticBatteryDisplaySource.attachRender = new KineticBatteryDisplaySourceRender()`
// in CreateConnectedClient.onInitializeClient().
public class KineticBatteryDisplaySourceRender extends SingleLineDisplaySourceRender {
    @Override
    public void initConfigurationWidgets(
            DisplaySource source,
            DisplayLinkContext context,
            ModularGuiLineBuilder builder,
            boolean isFirstLine
    ) {
        super.initConfigurationWidgets(source, context, builder, isFirstLine);
        if (isFirstLine)
            return;
        builder.addSelectionScrollInput(0, 120,
                (si, l) -> si.forOptions(ConnectedLang.translatedOptions("display_source.kinetic_battery", "number", "percentage", "progress_bar"))
                        .titled(ConnectedLang.translateDirect("display_source.kinetic_battery.display")),
                "Mode");
    }
}
