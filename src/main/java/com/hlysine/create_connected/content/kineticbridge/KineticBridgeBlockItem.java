package com.hlysine.create_connected.content.kineticbridge;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;

import java.util.function.Consumer;

// CORRECTION: an earlier version of this file kept showBounds() here directly, reasoning that
// "guarded by isClientSide(), so its client-only imports (Outliner/LocalPlayer) are never
// resolved on a dedicated server" was safe. That reasoning was WRONG - Loom's
// splitEnvironmentSourceSets() filters the *compile-time* classpath per source set, so importing
// a client-only class from a main-sourceset file is a compile error regardless of any runtime
// guard around its use; "guarded" only helps for classes that exist on both sides. The real fix
// is this hook: showBounds' implementation now lives in
// src/client/java/.../content/kineticbridge/KineticBridgeBlockItemClient.java, which
// CreateConnectedClient.onInitializeClient() wires into this field.
public class KineticBridgeBlockItem extends BlockItem {
    public static Consumer<BlockPlaceContext> showBoundsHook = ctx -> {
    };

    public KineticBridgeBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult place(BlockPlaceContext ctx) {
        InteractionResult result = super.place(ctx);
        if (result == InteractionResult.FAIL && ctx.getLevel().isClientSide())
            showBoundsHook.accept(ctx);
        return result;
    }
}
