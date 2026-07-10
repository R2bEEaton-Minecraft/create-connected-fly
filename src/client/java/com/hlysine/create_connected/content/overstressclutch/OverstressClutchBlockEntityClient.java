package com.hlysine.create_connected.content.overstressclutch;

import com.hlysine.create_connected.ConnectedLang;
import com.zurrtum.create.client.catnip.lang.FontHelper;
import com.zurrtum.create.client.foundation.item.TooltipHelper;
import net.minecraft.network.chat.Component;

import java.util.List;

import static net.minecraft.ChatFormatting.GOLD;

public class OverstressClutchBlockEntityClient {
    public static void register() {
        OverstressClutchBlockEntity.uncoupledTooltipHook = OverstressClutchBlockEntityClient::appendUncoupledTooltip;
    }

    private static void appendUncoupledTooltip(OverstressClutchBlockEntity be, List<Component> tooltip) {
        ConnectedLang.translate("gui.overstress_clutch.uncoupled")
                .style(GOLD)
                .forGoggles(tooltip);
        Component hint = ConnectedLang.translateDirect("gui.overstress_clutch.uncoupled_explanation");
        List<Component> cutString = TooltipHelper.cutTextComponent(hint, FontHelper.Palette.GRAY_AND_WHITE);
        for (Component component : cutString)
            ConnectedLang.builder()
                    .add(component.copy())
                    .forGoggles(tooltip);
    }
}
