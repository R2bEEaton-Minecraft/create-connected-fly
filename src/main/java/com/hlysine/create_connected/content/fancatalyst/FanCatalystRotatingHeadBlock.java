package com.hlysine.create_connected.content.fancatalyst;

import com.zurrtum.create.content.equipment.wrench.IWrenchable;
import com.zurrtum.create.foundation.block.IBE;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class FanCatalystRotatingHeadBlock extends Block implements IBE<FanCatalystRotatingHeadBlockEntity>, IWrenchable {
    private final BlockEntityType<? extends FanCatalystRotatingHeadBlockEntity> blockEntityType;

    public FanCatalystRotatingHeadBlock(Properties properties, BlockEntityType<? extends FanCatalystRotatingHeadBlockEntity> blockEntityType) {
        super(properties);
        this.blockEntityType = blockEntityType;
    }

    @Override
    public <S extends BlockEntity> BlockEntityTicker<S> getTicker(Level level, BlockState state, BlockEntityType<S> type) {
        return null;
    }

    @Override
    public Class<FanCatalystRotatingHeadBlockEntity> getBlockEntityClass() {
        return FanCatalystRotatingHeadBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends FanCatalystRotatingHeadBlockEntity> getBlockEntityType() {
        return blockEntityType;
    }
}
