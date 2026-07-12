package com.hlysine.create_connected.registries;

import com.zurrtum.create.catnip.registry.RegisteredObjectsHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;

import java.util.HashSet;
import java.util.Set;

public class PreciseItemUseOverrides {

    public static final Set<Identifier> OVERRIDES = new HashSet<>();

    // com.zurrtum.create.foundation.block.ItemUseOverrides doesn't exist in Create Fly (confirmed
    // absent from the real decompiled sources) - it moved this pattern into direct mixins on vanilla's
    // ServerPlayerGameMode instead (see ItemUseOverridesMixin.java, this mod's own self-contained
    // mixin-based re-implementation). The call to the missing class here was dead/redundant - this
    // mod's own OVERRIDES set + ItemUseOverridesMixin is already the complete mechanism.
    public static void addBlock(Block block) {
        OVERRIDES.add(RegisteredObjectsHelper.getKeyOrThrow(block));
    }
}
