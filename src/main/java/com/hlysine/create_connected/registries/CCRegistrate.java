package com.hlysine.create_connected.registries;

import com.hlysine.create_connected.CreateConnected;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.function.BiFunction;
import java.util.function.Function;

// Replaces the original Registrate-based CreateRegistrate builder chain (Registrate does not
// exist in Create Fly at all - see PORTING_NOTES.md). Mirrors the direct-registration pattern
// CreateModAddon uses against the real Create Fly + MC 1.21.11 API (block/item properties need
// an explicit ResourceKey baked in via .setId(key) before construction).
public class CCRegistrate {
    public static <T extends Block> T block(String path, Function<BlockBehaviour.Properties, T> factory, BlockBehaviour.Properties properties) {
        Identifier id = CreateConnected.asResource(path);
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, id);
        return Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(properties.setId(key)));
    }

    public static <T extends Item> T item(String path, Function<Item.Properties, T> factory, Item.Properties properties) {
        Identifier id = CreateConnected.asResource(path);
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id);
        return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
    }

    public static <T extends Item> T item(String path, Function<Item.Properties, T> factory) {
        return item(path, factory, new Item.Properties());
    }

    public static BlockItem blockItem(Block block, String path) {
        return blockItem(block, path, BlockItem::new, new Item.Properties());
    }

    public static BlockItem blockItem(Block block, String path, Item.Properties properties) {
        return blockItem(block, path, BlockItem::new, properties);
    }

    public static <T extends BlockItem> T blockItem(Block block, String path, BiFunction<Block, Item.Properties, T> factory) {
        return blockItem(block, path, factory, new Item.Properties());
    }

    public static <T extends BlockItem> T blockItem(Block block, String path, BiFunction<Block, Item.Properties, T> factory, Item.Properties properties) {
        Identifier id = CreateConnected.asResource(path);
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id);
        T item = factory.apply(block, properties.useBlockDescriptionPrefix().setId(key));
        item.registerBlocks(Item.BY_BLOCK, item);
        return Registry.register(BuiltInRegistries.ITEM, key, item);
    }
}
