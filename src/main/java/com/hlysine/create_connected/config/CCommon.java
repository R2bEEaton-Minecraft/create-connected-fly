package com.hlysine.create_connected.config;

import com.zurrtum.create.catnip.config.Builder;

public class CCommon extends SyncConfigBase {

    @Override
    public String getName() {
        return "common";
    }

    public Builder builder;

    @Override
    public void registerAll(Builder builder) {
        super.registerAll(builder);
        this.builder = builder;
    }

    public final ConfigBool migrateCopycatsOnBlockUpdate = b(true, "migrateCopycatsOnBlockUpdate", Comments.migrateCopycatsOnBlockUpdate);

    public final ConfigBool migrateCopycatsOnInitialize = b(true, "migrateCopycatsOnInitialize", Comments.migrateCopycatsOnInitialize);

    public final CFeatures toggle = nested(0, CFeatures::new, Comments.toggle);

    public final CFeatureCategories categories = nested(0, CFeatureCategories::new, Comments.categories);

    private static class Comments {
        static String toggle = "Enable/disable features. Values on server override clients";
        static String categories = "Enable/disable categories of features. Disabling a category hides all related features. Values on server override clients";
        static String migrateCopycatsOnBlockUpdate = "Migrate copycats to Create: Copycats+ when they receive a block update";
        static String migrateCopycatsOnInitialize = "Migrate copycats to Create: Copycats+ when their block entities are initialized";
    }
}
