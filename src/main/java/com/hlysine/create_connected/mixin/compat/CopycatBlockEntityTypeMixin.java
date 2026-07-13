package com.hlysine.create_connected.mixin.compat;

import com.hlysine.create_connected.content.copycat.MigratingCopycatBlock;
import com.hlysine.create_connected.content.copycat.MigratingWaterloggedCopycatBlock;
import com.zurrtum.create.AllBlockEntityTypes;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Same 1.21.11 compatibility hook used by Railway's conductor vent. Create constructs all of its
// copycats with AllBlockEntityTypes.COPYCAT, while strict block-state validation only knows Create's
// own blocks. Accept Connected's copycat subclasses so they always receive the entity that stores
// and synchronizes a right-clicked material.
@Mixin(BlockEntityType.class)
public class CopycatBlockEntityTypeMixin {
    @Inject(method = "isValid", at = @At("HEAD"), cancellable = true)
    private void create_connected$allowCopycats(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this != AllBlockEntityTypes.COPYCAT)
            return;
        if (state.getBlock() instanceof MigratingCopycatBlock
                || state.getBlock() instanceof MigratingWaterloggedCopycatBlock)
            cir.setReturnValue(true);
    }
}
