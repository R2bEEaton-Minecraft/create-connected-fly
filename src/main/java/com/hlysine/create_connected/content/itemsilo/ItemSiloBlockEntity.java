package com.hlysine.create_connected.content.itemsilo;

import com.hlysine.create_connected.registries.CCBlockEntityTypes;
import com.hlysine.create_connected.CreateConnected;
import com.zurrtum.create.api.connectivity.ConnectivityHandler;
import com.zurrtum.create.api.packager.InventoryIdentifier;
import com.zurrtum.create.foundation.ICapabilityProvider;
import com.zurrtum.create.foundation.blockEntity.IMultiBlockEntityContainer;
import com.zurrtum.create.foundation.blockEntity.SmartBlockEntity;
import com.zurrtum.create.api.behaviour.BlockEntityBehaviour;
import com.zurrtum.create.foundation.blockEntity.behaviour.inventory.VersionedInventoryWrapper;
import com.zurrtum.create.foundation.mixin.accessor.ItemStackHandlerAccessor;
import com.zurrtum.create.infrastructure.config.AllConfigs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.Clearable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.wrapper.CombinedInvWrapper;

import java.util.List;

@EventBusSubscriber(modid = CreateConnected.MODID)
public class ItemSiloBlockEntity extends SmartBlockEntity implements IMultiBlockEntityContainer.Inventory, Clearable {

    protected ICapabilityProvider<IItemHandler> itemCapability = null;
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

        inventory = new ItemStackHandler(AllConfigs.server().logistics.vaultCapacity.get()) {
            @Override
            protected void onContentsChanged(int slot) {
                super.onContentsChanged(slot);
                updateComparators();
                level.blockEntityChanged(worldPosition);
            }
        };

        radius = 1;
        length = 1;
    }

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                CCBlockEntityTypes.ITEM_SILO,
                (be, context) -> {
                    be.initCapability();
                    if (be.itemCapability == null)
                        return null;
                    return be.itemCapability.getCapability();
                }
        );
    }

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
        invalidateCapabilities();
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
        invalidateCapabilities();
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
            // NeoForge's ItemStackHandler.deserializeNBT still expects a raw CompoundTag - bridged
            // via CompoundTag.CODEC here; ItemStackHandler itself is part of the separate NeoForge
            // Capabilities -> Fabric Transfer API rewrite (see PORTING_NOTES.md), not yet done.
            inventory.deserializeNBT(view.lookup(), view.read("Inventory", CompoundTag.CODEC).orElseGet(CompoundTag::new));
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
            view.store("Inventory", CompoundTag.CODEC, inventory.serializeNBT(view.lookup()));
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

    public void applyInventoryToBlock(ItemStackHandler handler) {
        for (int i = 0; i < inventory.getSlots(); i++)
            inventory.setStackInSlot(i, i < handler.getSlots() ? handler.getStackInSlot(i) : ItemStack.EMPTY);
    }

    private void initCapability() {
        if (itemCapability != null && itemCapability.getCapability() != null)
            return;
        if (!isController()) {
            ItemSiloBlockEntity controllerBE = getControllerBE();
            if (controllerBE == null)
                return;
            controllerBE.initCapability();
            itemCapability = ICapabilityProvider.of(() -> {
                if (controllerBE.isRemoved())
                    return null;
                if (controllerBE.itemCapability == null)
                    return null;
                return controllerBE.itemCapability.getCapability();
            });
            invId = controllerBE.invId;
            return;
        }

        IItemHandlerModifiable[] invs = new IItemHandlerModifiable[length * radius * radius];
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

        itemCapability = ICapabilityProvider.of(new VersionedInventoryWrapper(new CombinedInvWrapper(invs)));

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
        invalidateCapabilities();
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
        ((ItemStackHandlerAccessor) inventory).create$getStacks().clear();
    }
}

