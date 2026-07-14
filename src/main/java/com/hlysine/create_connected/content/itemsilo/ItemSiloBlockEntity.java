package com.hlysine.create_connected.content.itemsilo;

import com.hlysine.create_connected.registries.CCBlockEntityTypes;
import com.zurrtum.create.api.connectivity.ConnectivityHandler;
import com.zurrtum.create.api.packager.InventoryIdentifier;
import com.zurrtum.create.foundation.blockEntity.IMultiBlockEntityContainer;
import com.zurrtum.create.foundation.blockEntity.SmartBlockEntity;
import com.zurrtum.create.api.behaviour.BlockEntityBehaviour;
import com.zurrtum.create.infrastructure.config.AllConfigs;
import com.zurrtum.create.infrastructure.items.CombinedInvWrapper;
import com.zurrtum.create.infrastructure.items.ItemStackHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.Container;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.Clearable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.List;

public class ItemSiloBlockEntity extends SmartBlockEntity implements IMultiBlockEntityContainer.Inventory, Clearable {

    // NeoForge's ICapabilityProvider<IItemHandler> lazy/invalidatable wrapper doesn't exist in real
    // Create Fly at all (confirmed absent from the sources jar - see PORTING_NOTES.md). itemCapability
    // is now just the plain Container this block entity exposes directly, registered onto Fabric's
    // ItemStorage.SIDED via CCTransfer.register() (InventoryStorage.of(itemCapability, side)).
    // Fabric's own BlockApiCache handles staleness/revalidation at the querying side, so the old
    // "isRemoved() guard inside a lazily-resolved provider" ceremony isn't needed anymore either -
    // initCapability() is simply re-run (cheaply, since it early-returns once cached) on every query.
    protected Container itemCapability = null;
    protected InventoryIdentifier invId;

    protected ItemStackHandler inventory;
    protected BlockPos controller;
    protected BlockPos lastKnownPos;
    protected boolean updateConnectivity;
    protected int radius;
    protected int length;
    protected Axis axis;

    public ItemSiloBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);

        // Real Create Fly's own ItemStackHandler has no onContentsChanged(int) hook to override
        // (unlike the old NeoForge one) - overriding setItem() directly gets the same "notified on
        // every slot write" behavior, since Container's mixed-in insert/extract/merge logic also
        // routes all its mutations through setItem().
        inventory = new ItemStackHandler(AllConfigs.server().logistics.vaultCapacity.get()) {
            @Override
            public void setItem(int slot, ItemStack stack) {
                super.setItem(slot, stack);
                updateComparators();
                level.blockEntityChanged(worldPosition);
            }
        };

        radius = 1;
        length = 1;
    }

    // NeoForge's RegisterCapabilitiesEvent registration is gone - this type is registered onto
    // Fabric's ItemStorage.SIDED via CCTransfer.register() (be.initCapability(); return
    // be.itemCapability;, matching Create Fly's own AllTransfer.registerItemSide() pattern).

    @Override
    public void addBehaviours(List<BlockEntityBehaviour<?>> behaviours) {
    }

    protected void updateConnectivity() {
        updateConnectivity = false;
        if (level.isClientSide())
            return;
        if (!isController())
            return;
        ConnectivityHandler.formMulti(this);
    }

    protected void updateComparators() {
        ItemSiloBlockEntity controllerBE = getControllerBE();
        if (controllerBE == null)
            return;

        level.blockEntityChanged(controllerBE.worldPosition);

        BlockPos pos = controllerBE.getBlockPos();
        for (int y = 0; y < controllerBE.length; y++) {
            for (int z = 0; z < controllerBE.radius; z++) {
                for (int x = 0; x < controllerBE.radius; x++) {
                    level.updateNeighbourForOutputSignal(pos.offset(x, y, z), getBlockState().getBlock());
                }
            }
        }
    }

    // Moved here from ItemSiloBlock.onRemove(state, level, pos, newState, isMoving), which no longer
    // exists as an overridable Block method - vanilla now calls this hook on the block entity itself
    // only when it's genuinely being discarded (matching the old override's own
    // state.getBlock() != newState.getBlock() guard, so no equivalent check is needed here).
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        // com.zurrtum.create.foundation.item.ItemHelper has no dropContents(...) at all (confirmed via
        // javap - genuinely doesn't exist, not renamed) - real Create Fly's own inventory-dropping
        // blocks use plain vanilla net.minecraft.world.Containers.dropContents(Level, BlockPos,
        // NonNullList<ItemStack>) against ItemStackHandler's own getStacks() accessor instead.
        net.minecraft.world.Containers.dropContents(level, pos, inventory.getStacks());
        // Match Create Fly's vertical FluidTankBlockEntity lifecycle exactly: remove this part from
        // the level lookup before splitting, otherwise ConnectivityHandler can rediscover the block
        // being removed and write its controller/state back during the same break.
        level.removeBlockEntity(pos);
        ConnectivityHandler.splitMulti(this);
    }

    @Override
    public void tick() {
        super.tick();

        if (lastKnownPos == null)
            lastKnownPos = getBlockPos();
        else if (!lastKnownPos.equals(worldPosition) && worldPosition != null) {
            onPositionChanged();
            return;
        }

        if (updateConnectivity)
            updateConnectivity();
    }

    @Override
    public BlockPos getLastKnownPos() {
        return lastKnownPos;
    }

    @Override
    public boolean isController() {
        return controller == null || worldPosition.getX() == controller.getX()
                && worldPosition.getY() == controller.getY() && worldPosition.getZ() == controller.getZ();
    }

    private void onPositionChanged() {
        removeController(true);
        lastKnownPos = worldPosition;
    }

    @SuppressWarnings("unchecked")
    @Override
    public ItemSiloBlockEntity getControllerBE() {
        if (isController())
            return this;
        BlockEntity blockEntity = level.getBlockEntity(controller);
        if (blockEntity instanceof ItemSiloBlockEntity)
            return (ItemSiloBlockEntity) blockEntity;
        return null;
    }

    public void removeController(boolean keepContents) {
        if (level.isClientSide())
            return;
        updateConnectivity = true;
        controller = null;
        radius = 1;
        length = 1;

        BlockState state = getBlockState();
        if (ItemSiloBlock.isVault(state)) {
            state = state.setValue(ItemSiloBlock.LARGE, false);
            getLevel().setBlock(worldPosition, state, 22);
        }

        itemCapability = null;
        setChanged();
        sendData();
    }

    @Override
    public void setController(BlockPos controller) {
        if (level.isClientSide() && !isVirtual())
            return;
        if (controller.equals(this.controller))
            return;
        this.controller = controller;
        itemCapability = null;
        setChanged();
        sendData();
    }

    @Override
    public BlockPos getController() {
        return isController() ? worldPosition : controller;
    }

    @Override
    protected void read(ValueInput view, boolean clientPacket) {
        super.read(view, clientPacket);

        BlockPos controllerBefore = controller;
        int prevSize = radius;
        int prevLength = length;

        updateConnectivity = view.getBooleanOr("Uninitialized", false);

        lastKnownPos = view.read("LastKnownPos", BlockPos.CODEC).orElse(null);
        controller = view.read("Controller", BlockPos.CODEC).orElse(null);

        if (isController()) {
            radius = view.getIntOr("Size", 0);
            length = view.getIntOr("Length", 0);
        }

        if (!clientPacket) {
            // Real Create Fly's own ItemStackHandler.read(ValueInput) reads its own "Inventory" key
            // straight off the view (no raw CompoundTag/deserializeNBT bridging needed anymore).
            inventory.read(view);
            return;
        }

        boolean changeOfController =
                controllerBefore == null ? controller != null : !controllerBefore.equals(controller);
        if (hasLevel() && (changeOfController || prevSize != radius || prevLength != length))
            level.setBlocksDirty(getBlockPos(), Blocks.AIR.defaultBlockState(), getBlockState());
    }

    @Override
    protected void write(ValueOutput view, boolean clientPacket) {
        if (updateConnectivity)
            view.putBoolean("Uninitialized", true);
        if (lastKnownPos != null)
            view.store("LastKnownPos", BlockPos.CODEC, lastKnownPos);
        if (!isController())
            view.store("Controller", BlockPos.CODEC, controller);
        if (isController()) {
            view.putInt("Size", radius);
            view.putInt("Length", length);
        }

        super.write(view, clientPacket);

        if (!clientPacket) {
            view.putString("StorageType", "CombinedInv");
            inventory.write(view);
        }
    }

    public ItemStackHandler getInventoryOfBlock() {
        return inventory;
    }

    public InventoryIdentifier getInvId() {
        // ensure capability is up to date first, which sets the ID
        this.initCapability();
        return this.invId;
    }

    // Public entry point for CCTransfer.register()'s ItemStorage.SIDED lookup - initCapability()
    // itself stays private since only this class's own controller/non-controller recursion needs it.
    public Container getItemCapability() {
        initCapability();
        return itemCapability;
    }

    public void applyInventoryToBlock(ItemStackHandler handler) {
        for (int i = 0; i < inventory.getContainerSize(); i++)
            inventory.setItem(i, i < handler.getContainerSize() ? handler.getItem(i) : ItemStack.EMPTY);
    }

    // Same caching shape as before, minus the NeoForge-only lazy ICapabilityProvider/
    // VersionedInventoryWrapper machinery (neither exists in real Create Fly) - itemCapability is
    // reset to null (by setController()/removeController()/notifyMultiUpdated(), same trigger points
    // as before) whenever it needs recomputing, and this just early-returns once it's already cached.
    private void initCapability() {
        if (itemCapability != null)
            return;
        if (!isController()) {
            ItemSiloBlockEntity controllerBE = getControllerBE();
            if (controllerBE == null)
                return;
            controllerBE.initCapability();
            itemCapability = controllerBE.itemCapability;
            invId = controllerBE.invId;
            return;
        }

        Container[] invs = new Container[length * radius * radius];
        for (int yOffset = 0; yOffset < length; yOffset++) {
            for (int xOffset = 0; xOffset < radius; xOffset++) {
                for (int zOffset = 0; zOffset < radius; zOffset++) {
                    BlockPos vaultPos = worldPosition.offset(xOffset, yOffset, zOffset);
                    ItemSiloBlockEntity vaultAt =
                            ConnectivityHandler.partAt(CCBlockEntityTypes.ITEM_SILO, level, vaultPos);
                    invs[yOffset * radius * radius + xOffset * radius + zOffset] =
                            vaultAt != null ? vaultAt.inventory : new ItemStackHandler();
                }
            }
        }

        itemCapability = new CombinedInvWrapper(invs);

        // build an identifier encompassing all component vaults
        BlockPos farCorner = worldPosition.offset(radius, length, radius);
        BoundingBox bounds = BoundingBox.fromCorners(this.worldPosition, farCorner);
        this.invId = new InventoryIdentifier.Bounds(bounds);
    }

    public static int getMaxLength(int radius) {
        return radius * 3;
    }

    @Override
    public void preventConnectivityUpdate() {
        updateConnectivity = false;
    }

    @Override
    public void notifyMultiUpdated() {
        BlockState state = this.getBlockState();
        if (ItemSiloBlock.isVault(state)) { // safety
            level.setBlock(getBlockPos(), state.setValue(ItemSiloBlock.LARGE, radius > 2), 6);
        }
        itemCapability = null;
        setChanged();
    }

    @Override
    public Direction.Axis getMainConnectionAxis() {
        return getMainAxisOf(this);
    }

    @Override
    public int getMaxLength(Direction.Axis longAxis, int width) {
        if (longAxis == Direction.Axis.Y) return getMaxLength(width);
        return getMaxWidth();
    }

    @Override
    public int getMaxWidth() {
        return 3;
    }

    @Override
    public int getHeight() {
        return length;
    }

    @Override
    public int getWidth() {
        return radius;
    }

    @Override
    public void setHeight(int height) {
        this.length = height;
    }

    @Override
    public void setWidth(int width) {
        this.radius = width;
    }

    @Override
    public boolean hasInventory() {
        return true;
    }

    @Override
    public void clearContent() {
        // com.zurrtum.create.foundation.mixin.accessor.ItemStackHandlerAccessor never existed in
        // real Create Fly - its own ItemStackHandler.getStacks() is already a plain public method.
        inventory.getStacks().clear();
    }
}

