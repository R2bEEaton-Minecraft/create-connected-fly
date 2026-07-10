package com.hlysine.create_connected.compat;

import mezz.jei.api.runtime.IIngredientManager;
import net.minecraft.resources.Identifier;

// NeoForge's lightweight Event base class doesn't exist on Fabric; this only ever had
// in-process listeners within this mod (none currently registered), so it's simplified to a
// plain record instead of pulling in Fabric API's full Event<T>/EventFactory machinery.
public record FeatureRefreshEvent(Identifier jeiPluginId, IIngredientManager ingredientManager, boolean pre) {
}
