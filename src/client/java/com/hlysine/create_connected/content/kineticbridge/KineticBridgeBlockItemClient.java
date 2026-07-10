package com.hlysine.create_connected.content.kineticbridge;

import com.hlysine.create_connected.ConnectedLang;
import com.zurrtum.create.catnip.data.Pair;
import com.zurrtum.create.client.catnip.outliner.Outliner;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.phys.AABB;

public class KineticBridgeBlockItemClient {
    public static void register() {
        KineticBridgeBlockItem.showBoundsHook = KineticBridgeBlockItemClient::showBounds;
    }

    private static void showBounds(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        BlockItem item = (BlockItem) context.getItemInHand().getItem();
        Direction facing = ((KineticBridgeBlock) item.getBlock()).getDirectionForPlacement(context);
        if (!(context.getPlayer() instanceof LocalPlayer localPlayer))
            return;
        Outliner.getInstance().showAABB(Pair.of("kinetic_bridge", pos), new AABB(pos).expandTowards(facing.getNormal().getX(), facing.getNormal().getY(), facing.getNormal().getZ()))
                .colored(0xFF_ff5d6c);
        ConnectedLang.translate("kinetic_bridge.not_enough_space")
                .color(0xFF_ff5d6c)
                .sendStatus(localPlayer);
    }
}
