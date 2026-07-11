package com.hlysine.create_connected.content.inventoryaccessport;

import com.zurrtum.create.content.redstone.DirectedDirectionalBlock;
import com.zurrtum.create.foundation.blockEntity.SmartBlockEntity;
import com.zurrtum.create.api.behaviour.BlockEntityBehaviour;
import com.zurrtum.create.foundation.blockEntity.behaviour.inventory.CapManipulationBehaviourBase;
import com.zurrtum.create.foundation.blockEntity.behaviour.inventory.InvManipulationBehaviour;
import com.zurrtum.create.catnip.math.BlockFace;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

import static com.hlysine.create_connected.content.inventoryaccessport.InventoryAccessPortBlock.ATTACHED;

public class InventoryAccessPortBlockEntity extends SmartBlockEntity {
    protected Container itemCapability;
    private InvManipulationBehaviour observedInventory;
    private boolean powered;

    public InventoryAccessPortBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);

        // No more lazy NeoForge capability invalidation to guard against - InventoryAccessHandler
        // just wraps live lookups on every call, so there's nothing stale to ever need refreshing.
        itemCapability = new InventoryAccessHandler();
        powered = false;
    }

    @Override
    public void initialize() {
        super.initialize();
        updateConnectedInventory();
    }

    // NeoForge's RegisterCapabilitiesEvent registration is gone - this type is registered onto
    // Fabric's ItemStorage.SIDED via CCTransfer.register() (InventoryStorage.of(itemCapability, side),
    // matching Create Fly's own AllTransfer.registerItemSide() pattern).

    @Override
    public void addBehaviours(List<BlockEntityBehaviour<?>> behaviours) {
        CapManipulationBehaviourBase.InterfaceProvider towardBlockFacing =
                (w, p, s) -> new BlockFace(p, DirectedDirectionalBlock.getTargetDirection(s));
        behaviours.add(observedInventory = new InvManipulationBehaviour(this, towardBlockFacing));
    }

    public boolean isAttached() {
        return !powered && observedInventory.hasInventory() && !(observedInventory.getInventory() instanceof WrappedItemHandler);
    }

    public @Nullable BlockState getAttachedBlock() {
        if (!isAttached()) return null;
        return level.getBlockState(observedInventory.getTarget().getConnectedPos());
    }

    // Public entry point for CCTransfer.register()'s ItemStorage.SIDED lookup.
    public Container getItemCapability() {
        return itemCapability;
    }

    public void updateConnectedInventory() {
        observedInventory.findNewCapability();
        boolean previouslyPowered = powered;
        assert level != null;
        powered = level.hasNeighborSignal(worldPosition);
        if (powered != previouslyPowered) {
            notifyUpdate();
        }
        if (isAttached() != getBlockState().getValue(ATTACHED)) {
            BlockState state = getBlockState().cycle(ATTACHED);
            level.setBlockAndUpdate(worldPosition, state);
        }
    }

    @Override
    protected void read(ValueInput tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        powered = tag.getBooleanOr("Powered", false);
    }

    @Override
    protected void write(ValueOutput tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        tag.putBoolean("Powered", powered);
    }

    private Container getConnectedItemHandler() {
        if (powered) return null;
        Container handler = observedInventory.getInventory();
        if (handler instanceof WrappedItemHandler) return null;
        return handler;
    }

    // NeoForge's IItemHandler (getSlots/getStackInSlot/insertItem/extractItem/getSlotLimit/
    // isItemValid) is gone - this now implements plain vanilla Container. Create Fly's own
    // ContainerMixin mixes its BaseInventory extension methods (insert/extract/count/etc, the same
    // ones InvManipulationBehaviour itself uses) into the Container interface directly, so only the
    // raw slot primitives need implementing here - insert/extract-with-remainder logic for callers
    // comes free via those inherited default methods, it doesn't need reimplementing by hand.
    private class InventoryAccessHandler implements WrappedItemHandler {

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
                Container handler = getConnectedItemHandler();
                return handler == null ? 0 : handler.getContainerSize();
            }, 0);
        }

        @Override
        public boolean isEmpty() {
            return preventRecursion(() -> {
                Container handler = getConnectedItemHandler();
                return handler == null || handler.isEmpty();
            }, true);
        }

        @Override
        public ItemStack getItem(int slot) {
            return preventRecursion(() -> {
                Container handler = getConnectedItemHandler();
                return handler == null ? ItemStack.EMPTY : handler.getItem(slot);
            }, ItemStack.EMPTY);
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            return preventRecursion(() -> {
                Container handler = getConnectedItemHandler();
                return handler == null ? ItemStack.EMPTY : handler.removeItem(slot, amount);
            }, ItemStack.EMPTY);
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            return preventRecursion(() -> {
                Container handler = getConnectedItemHandler();
                return handler == null ? ItemStack.EMPTY : handler.removeItemNoUpdate(slot);
            }, ItemStack.EMPTY);
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            preventRecursion(() -> {
                Container handler = getConnectedItemHandler();
                if (handler != null)
                    handler.setItem(slot, stack);
                return null;
            }, null);
        }

        @Override
        public int getMaxStackSize() {
            return preventRecursion(() -> {
                Container handler = getConnectedItemHandler();
                return handler == null ? 64 : handler.getMaxStackSize();
            }, 64);
        }

        @Override
        public void setChanged() {
            preventRecursion(() -> {
                Container handler = getConnectedItemHandler();
                if (handler != null)
                    handler.setChanged();
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
                Container handler = getConnectedItemHandler();
                if (handler != null)
                    handler.clearContent();
                return null;
            }, null);
        }

        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            return preventRecursion(() -> {
                Container handler = getConnectedItemHandler();
                return handler != null && handler.canPlaceItem(slot, stack);
            }, false);
        }
    }
}
