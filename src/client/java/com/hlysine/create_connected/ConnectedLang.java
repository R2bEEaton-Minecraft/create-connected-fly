package com.hlysine.create_connected;

import com.zurrtum.create.client.catnip.lang.Lang;
import com.zurrtum.create.client.catnip.lang.LangBuilder;
import com.zurrtum.create.client.catnip.lang.LangNumberFormat;
import com.zurrtum.create.infrastructure.fluids.FluidStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

// Lang/LangBuilder/LangNumberFormat live under com.zurrtum.create.client.catnip.lang in Create
// Fly (client-only), not com.zurrtum.create.catnip.lang - this class is therefore client-only
// too and moved to src/client/java. Its ~16 call sites across content/ (tooltips, block name
// lookups) still live in the unconverted main-sourceset content/ package; each one needs to
// either move to src/client/java itself (if it's genuinely client-only rendering/tooltip code,
// which vanilla tooltip generation always is) or be rewritten against plain
// net.minecraft.network.chat.Component if it's reachable from common code - resolve case by
// case during the content/ conversion pass, don't blanket-assume.
public class ConnectedLang extends Lang {

    /**
     * legacy-ish. Use CreateLang.translate and other builder methods where possible
     */
    public static MutableComponent translateDirect(String key, Object... args) {
        Object[] args1 = LangBuilder.resolveBuilders(args);
        return Component.translatable(CreateConnected.MODID + "." + key, args1);
    }

    public static List<Component> translatedOptions(String prefix, String... keys) {
        List<Component> result = new ArrayList<>(keys.length);
        for (String key : keys)
            result.add(translate((prefix != null ? prefix + "." : "") + key).component());
        return result;
    }

    //

    public static LangBuilder builder() {
        return new LangBuilder(CreateConnected.MODID);
    }

    public static LangBuilder blockName(BlockState state) {
        return builder().add(state.getBlock()
                .getName());
    }

    public static LangBuilder itemName(ItemStack stack) {
        return builder().add(stack.getHoverName()
                .copy());
    }

    public static LangBuilder fluidName(FluidStack stack) {
        return builder().add(stack.getName()
                .copy());
    }

    public static LangBuilder number(double d) {
        return builder().text(LangNumberFormat.format(d));
    }

    public static LangBuilder translate(String langKey, Object... args) {
        return builder().translate(langKey, args);
    }

    public static LangBuilder text(String text) {
        return builder().text(text);
    }

    @Deprecated // Use while implementing and replace all references with Lang.translate
    public static LangBuilder temporaryText(String text) {
        return builder().text(text);
    }

}
