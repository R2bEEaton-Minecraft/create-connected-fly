package com.hlysine.create_connected.datagen.recipes;

import com.hlysine.create_connected.registries.CCBlocks;
import com.hlysine.create_connected.CreateConnected;
import com.zurrtum.create.AllBlocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;

import java.util.concurrent.CompletableFuture;

@SuppressWarnings("unused")
public class CuttingRecipeGen extends com.zurrtum.create.api.data.recipe.CuttingRecipeGen {

    GeneratedRecipe SHEAR_PIN = create(AllBlocks.SHAFT::get, b -> b.duration(200)
            .withCondition(new FeatureEnabledCondition(CCBlocks.SHEAR_PIN.getId()))
            .output(CCBlocks.SHEAR_PIN));

    public CuttingRecipeGen(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, CreateConnected.MODID);
    }
}

