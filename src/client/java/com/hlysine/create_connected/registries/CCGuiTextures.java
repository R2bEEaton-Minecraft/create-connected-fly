package com.hlysine.create_connected.registries;

import com.hlysine.create_connected.CreateConnected;
import com.mojang.blaze3d.systems.RenderSystem;
// com.zurrtum.create.catnip.gui/.gui.element moved to com.zurrtum.create.client.catnip.gui/.gui.element
// (confirmed present only under the client jar's client. prefix, absent from the common jar) - these
// are inherently client-only rendering utilities, consistent with the rest of this port's pattern.
import com.zurrtum.create.client.catnip.gui.UIRenderHelper;
import com.zurrtum.create.client.catnip.gui.element.ScreenElement;
import com.zurrtum.create.catnip.theme.Color;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;

public enum CCGuiTextures implements ScreenElement {

    SEQUENCER("sequencer", 256, 205),
    SEQUENCER_INSTRUCTION("sequencer", 0, 16, 237, 22),
    SEQUENCER_DELAY("sequencer", 0, 104, 237, 22),
    SEQUENCER_END("sequencer", 0, 126, 237, 22),
    SEQUENCER_EMPTY("sequencer", 0, 148, 237, 22),
    SEQUENCER_AWAIT("sequencer", 0, 206, 237, 22),
    ;

    public static final int FONT_COLOR = 0x575F7A;

    public final Identifier location;
    public final int width;
    public final int height;
    public final int startX;
    public final int startY;

    CCGuiTextures(String location, int width, int height) {
        this(location, 0, 0, width, height);
    }

    CCGuiTextures(int startX, int startY) {
        this("icons", startX * 16, startY * 16, 16, 16);
    }

    CCGuiTextures(String location, int startX, int startY, int width, int height) {
        this(CreateConnected.MODID, location, startX, startY, width, height);
    }

    CCGuiTextures(String namespace, String location, int startX, int startY, int width, int height) {
        this.location = Identifier.fromNamespaceAndPath(namespace, "textures/gui/" + location + ".png");
        this.width = width;
        this.height = height;
        this.startX = startX;
        this.startY = startY;
    }

    public void bind() {
        RenderSystem.setShaderTexture(0, location);
    }

    public void render(GuiGraphics graphics, int x, int y) {
        graphics.blit(location, x, y, startX, startY, width, height);
    }

    public void render(GuiGraphics graphics, int x, int y, Color c) {
        bind();
        UIRenderHelper.drawColoredTexture(graphics, c, x, y, startX, startY, width, height);
    }

}

