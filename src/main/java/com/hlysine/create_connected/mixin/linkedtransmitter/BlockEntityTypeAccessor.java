package com.hlysine.create_connected.mixin.linkedtransmitter;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BlockEntity.class)
public interface BlockEntityTypeAccessor {
    @Accessor("type")
    @Mutable
    void create_connected$setType(BlockEntityType<?> type);
}
