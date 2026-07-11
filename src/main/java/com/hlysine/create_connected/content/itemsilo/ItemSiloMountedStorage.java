package com.hlysine.create_connected.content.itemsilo;

import com.hlysine.create_connected.registries.CCMountedStorageTypes;
import com.mojang.serialization.MapCodec;
import com.zurrtum.create.api.contraption.storage.item.MountedItemStorageType;
import com.zurrtum.create.api.contraption.storage.item.WrapperMountedItemStorage;
import com.zurrtum.create.content.contraptions.Contraption;
import com.zurrtum.create.foundation.codec.CreateCodecs;
import com.zurrtum.create.infrastructure.items.ItemStackHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.jetbrains.annotations.Nullable;

public class ItemSiloMountedStorage extends WrapperMountedItemStorage<ItemStackHandler> {
    public static final MapCodec<ItemSiloMountedStorage> CODEC = CreateCodecs.ITEM_STACK_HANDLER.xmap(
            ItemSiloMountedStorage::new, storage -> storage.wrapped
    ).fieldOf("value");

    protected ItemSiloMountedStorage(MountedItemStorageType<?> type, ItemStackHandler handler) {
        super(type, handler);
    }

    protected ItemSiloMountedStorage(ItemStackHandler handler) {
        this(CCMountedStorageTypes.SILO, handler);
    }

    @Override
    public void unmount(Level level, BlockState state, BlockPos pos, @Nullable BlockEntity be) {
        if (be instanceof ItemSiloBlockEntity vault) {
            vault.applyInventoryToBlock(this.wrapped);
        }
    }

    @Override
    public boolean handleInteraction(ServerPlayer player, Contraption contraption, StructureTemplate.StructureBlockInfo info) {
        // vaults should never be opened.
        return false;
    }

    public static ItemSiloMountedStorage fromVault(ItemSiloBlockEntity vault) {
        // Vault inventories have a world-affecting onContentsChanged, copy to a safe one
        return new ItemSiloMountedStorage(copyToItemStackHandler(vault.getInventoryOfBlock()));
    }

    // Real Create Fly's own ItemStackHandler has no NeoForge-style deserializeNBT(registries, tag) -
    // its own CODEC (registry-context-free, ItemStack encoding doesn't need RegistryOps here) is the
    // replacement. Disclosed limitation: the new codec's NBT shape ("Stacks" list) doesn't match the
    // old NeoForge ItemStackHandler.serializeNBT() shape ("Items" list w/ per-slot "Slot" keys) this
    // "legacy" migration path was written against, so genuinely old pre-port world saves parsed here
    // will decode as an empty handler rather than restoring their contents - full legacy-format
    // migration wasn't attempted as part of this specific capability rewrite.
    public static ItemSiloMountedStorage fromLegacy(HolderLookup.Provider registries, CompoundTag nbt) {
        ItemStackHandler handler = ItemStackHandler.CODEC.parse(NbtOps.INSTANCE, nbt)
                .result()
                .orElseGet(ItemStackHandler::new);
        return new ItemSiloMountedStorage(handler);
    }
}