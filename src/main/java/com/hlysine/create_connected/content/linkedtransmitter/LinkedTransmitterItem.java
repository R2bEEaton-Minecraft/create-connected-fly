package com.hlysine.create_connected.content.linkedtransmitter;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.LinkedList;
import java.util.List;

public class LinkedTransmitterItem extends Item {
    public static final List<LinkedTransmitterBlock> MODULE_BLOCKS = new LinkedList<>();

    public LinkedTransmitterItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext ctx) {
        Player player = ctx.getPlayer();
        if (player == null)
            return InteractionResult.PASS;
        Level world = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
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

        return use(world, player, ctx.getHand()).getResult();
    }
}
