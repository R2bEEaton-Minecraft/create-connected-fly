package com.hlysine.create_connected.compat;

import com.hlysine.create_connected.CreateConnected;
import com.hlysine.create_connected.config.CCConfigs;
import com.zurrtum.create.catnip.registry.RegisteredObjectsHelper;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.*;

// Create: Copycats+ has no Fabric build for MC 1.21.11 at all (verified: Modrinth tops out at
// 1.21.1-fabric as of this port). BLOCK_MAP/ITEM_MAP are intentionally left empty rather than
// referencing com.copycatsplus.copycats.* classes that don't exist on this platform+version -
// this makes convert()/existsInCopycats()/isFeatureEnabled() safe no-ops (matching what would
// happen anyway since Mods.COPYCATS.isLoaded() is always false right now), while keeping the
// public API intact for the ~7 content/mixin call sites that depend on it, so this integration
// re-activates automatically (just populate the maps in the static block below) once Copycats+
// ships a compatible build.
public class CopycatsManager {
    public static final Map<String, Block> BLOCK_MAP = new HashMap<>();
    public static final Map<String, Item> ITEM_MAP = new HashMap<>();

    public static final Map<Level, Set<BlockPos>> migrationQueue = Collections.synchronizedMap(new WeakHashMap<>());

    public static void registerTickListener() {
        ServerTickEvents.END_WORLD_TICK.register(CopycatsManager::onLevelTick);
    }

    public static Block convert(Block self) {
        Identifier key = RegisteredObjectsHelper.getKeyOrThrow(self);
        if (!validateNamespace(key)) return self;
        Block result = BLOCK_MAP.get(key.getPath());
        if (result != null) return result;
        return self;
    }

    public static Item convert(Item self) {
        Identifier key = RegisteredObjectsHelper.getKeyOrThrow(self);
        if (!validateNamespace(key)) return self;
        Item result = ITEM_MAP.get(key.getPath());
        if (result != null) return result;
        Block blockResult = BLOCK_MAP.get(key.getPath());
        if (blockResult != null) return blockResult.asItem();
        return self;
    }

    public static ItemLike convert(ItemLike self) {
        return convert(self.asItem());
    }

    public static BlockState convert(BlockState state) {
        Block converted = convert(state.getBlock());
        if (state.getBlock() == converted) return state;
        BlockState newState = converted.defaultBlockState();
        for (Property<?> property : state.getProperties()) {
            newState = copyProperty(state, newState, property);
        }
        return newState;
    }

    private static <T extends Comparable<T>> BlockState copyProperty(BlockState from, BlockState to, Property<T> property) {
        return from.getOptionalValue(property).map(value -> to.trySetValue(property, value)).orElse(to);
    }

    public static Block convertIfEnabled(Block block) {
        Identifier key = RegisteredObjectsHelper.getKeyOrThrow(block);
        if (!validateNamespace(key)) return block;
        if (isFeatureEnabled(key))
            return convert(block);
        return block;
    }

    public static BlockState convertIfEnabled(BlockState state) {
        Identifier key = RegisteredObjectsHelper.getKeyOrThrow(state.getBlock());
        if (!validateNamespace(key)) return state;
        if (isFeatureEnabled(key))
            return convert(state);
        return state;
    }

    public static ItemLike convertIfEnabled(ItemLike item) {
        Identifier key = RegisteredObjectsHelper.getKeyOrThrow(item.asItem());
        if (!validateNamespace(key)) return item;
        if (isFeatureEnabled(key))
            return convert(item);
        return item;
    }

    private static boolean validateNamespace(Identifier key) {
        return key.getNamespace().equals(CreateConnected.MODID) || key.getNamespace().equals(Mods.COPYCATS.id());
    }

    public static boolean existsInCopycats(Identifier key) {
        if (!validateNamespace(key)) return false;
        if (BLOCK_MAP.containsKey(key.getPath())) return true;
        return ITEM_MAP.containsKey(key.getPath());
    }

    public static boolean isFeatureEnabled(Identifier key) {
        return existsInCopycats(key);
    }

    public static void enqueueMigration(Level level, BlockPos pos) {
        synchronized (migrationQueue) {
            Set<BlockPos> list = migrationQueue.computeIfAbsent(level, $ -> Collections.synchronizedSet(new LinkedHashSet<>()));
            synchronized (list) {
                list.add(pos);
            }
        }
    }

    public static void onLevelTick(ServerLevel level) {
        if (!CCConfigs.common().migrateCopycatsOnInitialize.get()) {
            migrationQueue.clear();
            return;
        }
        synchronized (migrationQueue) {
            if (migrationQueue.containsKey(level)) {
                Set<BlockPos> list = migrationQueue.get(level);
                synchronized (list) {
                    if (!list.isEmpty())
                        CreateConnected.LOGGER.debug("Copycats: Migrated " + list.size() + " copycats in " + level.dimension().location());
                    for (Iterator<BlockPos> iterator = list.iterator(); iterator.hasNext(); ) {
                        BlockPos pos = iterator.next();
                        if (!level.isLoaded(pos)) {
                            continue;
                        }
                        BlockState state = level.getBlockState(pos);
                        BlockState converted = CopycatsManager.convert(state);
                        if (!converted.is(state.getBlock())) {
                            level.setBlock(pos, converted, 2 | 16 | 32);
                        }
                        BlockEntity be = level.getBlockEntity(pos);
                        if (be != null)
                            level.setBlockEntity(be);
                    }
                }
                migrationQueue.remove(level);
            }
        }
    }
}
