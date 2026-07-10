package com.hlysine.create_connected.compat;

import com.hlysine.create_connected.CreateConnected;
import com.hlysine.create_connected.config.FeatureToggle;
import com.hlysine.create_connected.registries.CCCreativeTabs;
import com.zurrtum.create.catnip.registry.RegisteredObjectsHelper;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.runtime.IIngredientManager;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

@JeiPlugin
public class CreateConnectedJEI implements IModPlugin {
    private static final Identifier ID = CreateConnected.asResource("jei_plugin");

    public static IIngredientManager MANAGER;

    @Override
    @NotNull
    public Identifier getPluginUid() {
        return ID;
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        MANAGER = jeiRuntime.getIngredientManager();
    }

    public static void refreshItemList() {
        if (MANAGER != null && Minecraft.getInstance().level != null) {
            List<ItemStack> stacks = CCCreativeTabs.ITEMS.stream().map(ItemLike::asItem).map(ItemStack::new).collect(Collectors.toList());
            MANAGER.removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, stacks);
            MANAGER.addIngredientsAtRuntime(
                    VanillaTypes.ITEM_STACK,
                    CCCreativeTabs.ITEMS.stream()
                            .filter(x -> FeatureToggle.isEnabled(RegisteredObjectsHelper.getKeyOrThrow(x.asItem())))
                            .map(x -> new ItemStack(x.asItem()))
                            .collect(Collectors.toList())
            );
        }
    }
}
