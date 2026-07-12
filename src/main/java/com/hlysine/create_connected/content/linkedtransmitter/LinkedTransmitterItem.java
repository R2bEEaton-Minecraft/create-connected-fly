package com.hlysine.create_connected.content.linkedtransmitter;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.LinkedList;
import java.util.List;

public class LinkedTransmitterItem extends Item {
    public static final List<LinkedTransmitterBlock> MODULE_BLOCKS = new LinkedList<>();

    public LinkedTransmitterItem(Properties pProperties) {
        super(pProperties);
    }

    // Item.onItemUseFirst(ItemStack, UseOnContext) was a NeoForge-only IItemExtension hook (letting an
    // item intercept a block click BEFORE the target block's own click handling runs, e.g. before a
    // vanilla button/lever toggles) - it does not exist in Fabric at all (confirmed via javap: Item's
    // only block-click hook now is useOn(UseOnContext), which - like vanilla's own item-use pipeline -
    // only runs as a fallback AFTER the target block's own use handling, too late to intercept a
    // button/lever's toggle). The real Fabric equivalent for "run before the block" is the
    // net.fabricmc.fabric.api.event.player.UseBlockCallback.EVENT, registered once in
    // CreateConnected.onInitialize() and delegating to this static helper, preserving the original
    // interception behavior exactly.
    public static InteractionResult onUseBlockFirst(Player player, Level world, InteractionHand hand, BlockHitResult hitResult) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof LinkedTransmitterItem))
            return InteractionResult.PASS;

        BlockPos pos = hitResult.getBlockPos();
        BlockState hitState = world.getBlockState(pos);

        if (player.mayBuild()) {
            for (LinkedTransmitterBlock moduleBlock : MODULE_BLOCKS) {
                if (hitState.is(moduleBlock.getBase())) {
                    if (!world.isClientSide()) {
                        if (!player.isCreative()) stack.shrink(1);
                        moduleBlock.replaceBase(hitState, world, pos);
                    }
                    return InteractionResult.SUCCESS;
                }
            }
        }

        for (LinkedTransmitterBlock moduleBlock : MODULE_BLOCKS) {
            if (hitState.is(moduleBlock.getBlock())) {
                return InteractionResult.PASS;
            }
        }

        // The original fell back to calling Item.use(world, player, hand) here (the "use in air" hook)
        // via the now-removed InteractionResultHolder return type. That fallback doesn't make sense
        // inside a block-click event (this item has no meaningful use() of its own - Item's default
        // use() just returns PASS/CONSUME_PARTIAL depending on food/UseAction, neither relevant here),
        // so it's dropped: returning PASS here lets vanilla's normal click pipeline continue exactly as
        // it would have for any other PASS result.
        return InteractionResult.PASS;
    }
}
