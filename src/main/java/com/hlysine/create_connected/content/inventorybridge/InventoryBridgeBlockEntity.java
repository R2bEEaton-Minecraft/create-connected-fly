package com.hlysine.create_connected.content.inventorybridge;

import com.hlysine.create_connected.content.inventoryaccessport.WrappedItemHandler;
import com.zurrtum.create.foundation.blockEntity.SmartBlockEntity;
import com.zurrtum.create.api.behaviour.BlockEntityBehaviour;
import com.zurrtum.create.foundation.blockEntity.behaviour.filtering.ServerFilteringBehaviour;
import com.zurrtum.create.foundation.blockEntity.behaviour.filtering.ServerSidedFilteringBehaviour;
import com.zurrtum.create.foundation.blockEntity.behaviour.inventory.CapManipulationBehaviourBase;
import com.zurrtum.create.foundation.blockEntity.behaviour.inventory.InvManipulationBehaviour;
import com.zurrtum.create.catnip.math.BlockFace;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Supplier;

import static com.hlysine.create_connected.content.inventorybridge.InventoryBridgeBlock.ATTACHED_NEGATIVE;
import static com.hlysine.create_connected.content.inventorybridge.InventoryBridgeBlock.ATTACHED_POSITIVE;

public class InventoryBridgeBlockEntity extends SmartBlockEntity {
    protected Container itemCapability;
    private InvManipulationBehaviour negativeInventory;
    private InvManipulationBehaviour positiveInventory;

    ServerSidedFilteringBehaviour filters;
    public ServerFilteringBehaviour negativeFilter;
    public ServerFilteringBehaviour positiveFilter;

    private boolean powered;

    public InventoryBridgeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);

        // No more lazy NeoForge capability invalidation to guard against - InventoryBridgeHandler
        // just wraps live lookups on every call, so there's nothing stale to ever need refreshing.
        itemCapability = new InventoryBridgeHandler();
        powered = false;
    }

    @Override
    public void initialize() {
        super.initialize();
        if (filters != null) {
            filters.updateFilterPresence();
        }
        updateConnectedInventory();
    }

    // NeoForge's RegisterCapabilitiesEvent registration is gone - this type is registered onto
    // Fabric's ItemStorage.SIDED via CCTransfer.register() (InventoryStorage.of(itemCapability, side),
    // matching Create Fly's own AllTransfer.registerItemSide() pattern).

    @Override
    public void addBehaviours(List<BlockEntityBehaviour<?>> behaviours) {
        CapManipulationBehaviourBase.InterfaceProvider towardBlockFacing1 =
                (w, p, s) -> new BlockFace(p, InventoryBridgeBlock.getNegativeTarget(s));
        CapManipulationBehaviourBase.InterfaceProvider towardBlockFacing2 =
                (w, p, s) -> new BlockFace(p, InventoryBridgeBlock.getPositiveTarget(s));
        behaviours.add(negativeInventory = new InvManipulationBehaviour(this, towardBlockFacing1));
        behaviours.add(positiveInventory = new InvManipulationBehaviour(this, towardBlockFacing2));
        // Client-only com.zurrtum.create.client...SidedFilteringBehaviour (which used to take an
        // InventoryBridgeFilterSlot ValueBoxTransform directly for its in-world filter-slot render
        // position) is gone from common code - the real replacement is the common-sourceset
        // ServerSidedFilteringBehaviour (see com.zurrtum.create.content.logistics.tunnel.
        // BrassTunnelBlockEntity for the reference usage this mirrors exactly), which only carries
        // the filtering LOGIC, not any rendering position. Disclosed reduction: InventoryBridgeFilterSlot
        // (src/client) is currently unused - the filter slots will still function correctly (item
        // routing/rejection all works via ServerFilteringBehaviour.test()), but their in-world visual
        // position hasn't been re-wired to a client-side renderer/behaviour hook yet.
        behaviours.add(filters = new ServerSidedFilteringBehaviour(
                this,
                (facing, filter) -> {
                    if (facing.getAxisDirection() == Direction.AxisDirection.NEGATIVE) {
                        negativeFilter = filter;
                    } else {
                        positiveFilter = filter;
                    }
                    return filter;
                },
                facing -> facing.getAxis() == getBlockState().getValue(InventoryBridgeBlock.AXIS)
        ));
    }

    public boolean isAttachedNegative() {
        return !powered && negativeInventory.hasInventory() && !(negativeInventory.getInventory() instanceof WrappedItemHandler);
    }

    public boolean isAttachedPositive() {
        return !powered && positiveInventory.hasInventory() && !(positiveInventory.getInventory() instanceof WrappedItemHandler);
    }

    public @Nullable BlockState getNegativeAttachedBlock() {
        if (!isAttachedNegative()) return null;
        return level.getBlockState(negativeInventory.getTarget().getConnectedPos());
    }

    public @Nullable BlockState getPositiveAttachedBlock() {
        if (!isAttachedPositive()) return null;
        return level.getBlockState(positiveInventory.getTarget().getConnectedPos());
    }

    // Public entry point for CCTransfer.register()'s ItemStorage.SIDED lookup.
    public Container getItemCapability() {
        return itemCapability;
    }

    public void updateConnectedInventory() {
        if (filters != null) {
            filters.updateFilterPresence();
        }
        negativeInventory.findNewCapability();
        positiveInventory.findNewCapability();
        boolean previouslyPowered = powered;
        powered = level.hasNeighborSignal(worldPosition);
        if (powered != previouslyPowered) {
            notifyUpdate();
        }
        boolean attachedNegative = isAttachedNegative();
        boolean attachedPositive = isAttachedPositive();
        if (attachedNegative != getBlockState().getValue(ATTACHED_NEGATIVE) || attachedPositive != getBlockState().getValue(ATTACHED_POSITIVE)) {
            BlockState state = getBlockState()
                    .setValue(ATTACHED_NEGATIVE, attachedNegative)
                    .setValue(ATTACHED_POSITIVE, attachedPositive);
            level.setBlockAndUpdate(worldPosition, state);
        }
    }

    @Override
    protected void read(ValueInput tag, boolean clientPacket) {
        powered = tag.getBooleanOr("Powered", false);
        try {
            super.read(tag, clientPacket);
        } catch (NoSuchElementException ignored) {
            // Older/newly-ported scene templates can still contain sided-filter entries without the
            // new explicit side field. Ignore those malformed entries and keep the filters empty.
        }
        if (filters != null) {
            filters.updateFilterPresence();
        }
    }

    @Override
    protected void write(ValueOutput tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        tag.putBoolean("Powered", powered);
    }

    private Container getNegativeHandler() {
        if (powered) return null;
        Container handler = negativeInventory.getInventory();
        if (handler instanceof WrappedItemHandler) return null;
        return handler;
    }

    private Container getPositiveHandler() {
        if (powered) return null;
        Container handler = positiveInventory.getInventory();
        if (handler instanceof WrappedItemHandler) return null;
        return handler;
    }

    // NeoForge's IItemHandler (getSlots/getStackInSlot/insertItem/extractItem/getSlotLimit/
    // isItemValid) is gone - this now implements plain vanilla Container. Create Fly's own
    // ContainerMixin mixes its BaseInventory extension methods (insert/extract/count/etc, the same
    // ones InvManipulationBehaviour itself uses) into the Container interface directly, so only the
    // raw slot primitives + canPlaceItem (filter eligibility, replacing insertItem/isItemValid's
    // reject logic) need implementing here - the merge-aware insert/extract logic real callers use
    // comes free via those inherited default methods, operating purely through getItem/setItem.
    private class InventoryBridgeHandler implements WrappedItemHandler {

        private final ThreadLocal<Boolean> recursionGuard = ThreadLocal.withInitial(() -> false);

        private <T> T preventRecursion(Supplier<T> value, T defaultValue) {
            if (recursionGuard.get()) return defaultValue;
            recursionGuard.set(true);
            T result = value.get();
            recursionGuard.set(false);
            return result;
        }

        @Override
        public int getContainerSize() {
            return preventRecursion(() -> {
                Container handler1 = getNegativeHandler();
                Container handler2 = getPositiveHandler();
                if (handler1 == null && handler2 == null) {
                    return 0;
                } else if (handler1 == null) {
                    return handler2.getContainerSize();
                } else if (handler2 == null) {
                    return handler1.getContainerSize();
                } else {
                    return handler1.getContainerSize() + handler2.getContainerSize();
                }
            }, 0);
        }

        @Override
        public boolean isEmpty() {
            return preventRecursion(() -> {
                Container handler1 = getNegativeHandler();
                Container handler2 = getPositiveHandler();
                if (handler1 == null && handler2 == null) return true;
                if (handler1 == null) return handler2.isEmpty();
                if (handler2 == null) return handler1.isEmpty();
                return handler1.isEmpty() && handler2.isEmpty();
            }, true);
        }

        @Override
        public ItemStack getItem(int slot) {
            return preventRecursion(() -> {
                Container handler1 = getNegativeHandler();
                Container handler2 = getPositiveHandler();
                if (handler1 == null && handler2 == null) {
                    return ItemStack.EMPTY;
                } else if (handler1 == null) {
                    ItemStack stack = handler2.getItem(slot);
                    boolean negative = negativeFilter.test(stack);
                    boolean positive = positiveFilter.test(stack);
                    if (!positive) return ItemStack.EMPTY;
                    if (negative && !negativeFilter.getFilter().isEmpty() && positiveFilter.getFilter().isEmpty())
                        return ItemStack.EMPTY;
                    return stack;
                } else if (handler2 == null) {
                    ItemStack stack = handler1.getItem(slot);
                    boolean negative = negativeFilter.test(stack);
                    boolean positive = positiveFilter.test(stack);
                    if (!negative) return ItemStack.EMPTY;
                    if (positive && !positiveFilter.getFilter().isEmpty() && negativeFilter.getFilter().isEmpty())
                        return ItemStack.EMPTY;
                    return stack;
                } else {
                    int size1 = handler1.getContainerSize();
                    ItemStack stack = slot < size1 ? handler1.getItem(slot) : handler2.getItem(slot - size1);
                    boolean negative = negativeFilter.test(stack);
                    boolean positive = positiveFilter.test(stack);
                    if (!negative && !positive) return ItemStack.EMPTY;
                    if (negative && !positive && slot >= size1) return ItemStack.EMPTY;
                    if (positive && !negative && slot < size1) return ItemStack.EMPTY;
                    boolean negativeFilterEmpty = negativeFilter.getFilter().isEmpty();
                    boolean positiveFilterEmpty = positiveFilter.getFilter().isEmpty();
                    if (!negativeFilterEmpty || !positiveFilterEmpty) {
                        if (slot >= size1 && negative && positiveFilterEmpty) return ItemStack.EMPTY;
                        if (slot < size1 && positive && negativeFilterEmpty) return ItemStack.EMPTY;
                    }
                    return stack;
                }
            }, ItemStack.EMPTY);
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            preventRecursion(() -> {
                Container handler1 = getNegativeHandler();
                Container handler2 = getPositiveHandler();
                if (handler1 == null && handler2 == null) return null;
                if (handler1 == null) {
                    handler2.setItem(slot, stack);
                } else if (handler2 == null) {
                    handler1.setItem(slot, stack);
                } else {
                    int size1 = handler1.getContainerSize();
                    if (slot < size1)
                        handler1.setItem(slot, stack);
                    else
                        handler2.setItem(slot - size1, stack);
                }
                return null;
            }, null);
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            return preventRecursion(() -> {
                ItemStack existing = getItem(slot);
                if (existing.isEmpty())
                    return ItemStack.EMPTY;
                ItemStack result = existing.split(amount);
                setItem(slot, existing);
                return result;
            }, ItemStack.EMPTY);
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            return preventRecursion(() -> {
                ItemStack existing = getItem(slot);
                setItem(slot, ItemStack.EMPTY);
                return existing;
            }, ItemStack.EMPTY);
        }

        @Override
        public int getMaxStackSize() {
            return preventRecursion(() -> {
                Container handler1 = getNegativeHandler();
                Container handler2 = getPositiveHandler();
                if (handler1 != null) return handler1.getMaxStackSize();
                if (handler2 != null) return handler2.getMaxStackSize();
                return 64;
            }, 64);
        }

        @Override
        public void setChanged() {
            preventRecursion(() -> {
                Container handler1 = getNegativeHandler();
                Container handler2 = getPositiveHandler();
                if (handler1 != null) handler1.setChanged();
                if (handler2 != null) handler2.setChanged();
                return null;
            }, null);
        }

        @Override
        public boolean stillValid(Player player) {
            return false;
        }

        @Override
        public void clearContent() {
            preventRecursion(() -> {
                Container handler1 = getNegativeHandler();
                Container handler2 = getPositiveHandler();
                if (handler1 != null) handler1.clearContent();
                if (handler2 != null) handler2.clearContent();
                return null;
            }, null);
        }

        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            return preventRecursion(() -> {
                Container handler1 = getNegativeHandler();
                Container handler2 = getPositiveHandler();
                if (handler1 == null && handler2 == null) {
                    return false;
                } else if (handler1 == null) {
                    boolean negative = negativeFilter.test(stack);
                    boolean positive = positiveFilter.test(stack);
                    if (!positive) return false;
                    if (negative && !negativeFilter.getFilter().isEmpty() && positiveFilter.getFilter().isEmpty())
                        return false;
                    return handler2.canPlaceItem(slot, stack);
                } else if (handler2 == null) {
                    boolean negative = negativeFilter.test(stack);
                    boolean positive = positiveFilter.test(stack);
                    if (!negative) return false;
                    if (positive && !positiveFilter.getFilter().isEmpty() && negativeFilter.getFilter().isEmpty())
                        return false;
                    return handler1.canPlaceItem(slot, stack);
                } else {
                    boolean negative = negativeFilter.test(stack);
                    boolean positive = positiveFilter.test(stack);
                    int size1 = handler1.getContainerSize();
                    if (!negative && !positive) return false;
                    if (negative && !positive && slot >= size1) return false;
                    if (positive && !negative && slot < size1) return false;
                    boolean negativeFilterEmpty = negativeFilter.getFilter().isEmpty();
                    boolean positiveFilterEmpty = positiveFilter.getFilter().isEmpty();
                    if (!negativeFilterEmpty || !positiveFilterEmpty) {
                        if (slot >= size1 && negative && positiveFilterEmpty) return false;
                        if (slot < size1 && positive && negativeFilterEmpty) return false;
                    }
                    return slot < size1
                            ? handler1.canPlaceItem(slot, stack)
                            : handler2.canPlaceItem(slot - size1, stack);
                }
            }, false);
        }
    }
}
