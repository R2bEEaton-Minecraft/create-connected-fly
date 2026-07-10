package com.hlysine.create_connected.registries;

import net.minecraft.world.level.block.state.BlockBehaviour;

// Original delegated to Create's own BuilderTransformers.copycat() (a Registrate transform);
// that whole foundation.data package (BuilderTransformers, SharedProperties, AssetLookup,
// CreateRegistrate) is gone from Create Fly - see PORTING_NOTES.md. Copycat blocks mimic
// whatever block they're placed as, so their own declared properties barely matter at
// runtime (shape/hardness/etc. are overridden per-instance); noOcclusion matches the
// original's intent of not rendering as a full opaque cube by default.
public class CCBuilderTransformers {
    public static BlockBehaviour.Properties copycatProperties() {
        return BlockBehaviour.Properties.of().noOcclusion();
    }
}
