package com.hlysine.create_connected.content.fluidvessel;

import com.hlysine.create_connected.CreateConnected;
import com.zurrtum.create.api.connectivity.ConnectivityHandler;
import com.zurrtum.create.content.fluids.tank.FluidTankBlockEntity;
import com.zurrtum.create.foundation.blockEntity.IMultiBlockEntityContainer;
import com.zurrtum.create.infrastructure.config.AllConfigs;
import com.zurrtum.create.infrastructure.fluids.BucketFluidInventory;
import com.zurrtum.create.infrastructure.fluids.FluidStack;
import com.zurrtum.create.catnip.animation.LerpedFloat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import org.jetbrains.annotations.Nullable;

import static com.hlysine.create_connected.content.fluidvessel.FluidVesselBlock.*;
import static net.minecraft.core.Direction.Axis;

// com.zurrtum.create.client.api.goggles.IHaveGoggleInformation is client-only (real Create Fly's own
// FluidTankBlockEntity doesn't implement it either) - a real cross-boundary bug in the original port
// of this file. Goggle tooltip logic moved to a new client-only FluidVesselTooltipBehaviour
// (src/client/java), registered via BlockEntityBehaviour.CLIENT_REGISTRY in
// CreateConnectedClient.onInitializeClient(), mirroring Create Fly's own FluidTankTooltipBehaviour/
// AllBlockEntityBehaviours pattern exactly.
public class FluidVesselBlockEntity extends FluidTankBlockEntity implements IMultiBlockEntityContainer.Fluid {

    private static final int MAX_SIZE = 3;
    private static final int MAX_HEIGHT = 6;
    private static final int SYNC_RATE = 8;

    protected WindowType windowType;

    // For rendering purposes only
    private LerpedFloat fluidLevel;

    public FluidVesselBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        windowType = WindowType.SIDE_WIDE;
        boiler = new BoilerData();
        refreshCapability();
    }

    // NeoForge's RegisterCapabilitiesEvent-based capability registration is gone - both this type
    // and CREATIVE_FLUID_VESSEL are now registered onto Fabric's FluidStorage.SIDED via
    // CCTransfer.register(), following Create Fly's own AllTransfer.registerFluidSide() pattern
    // (fluidCapability is exposed directly, same field this class already maintains).

    @Override
    protected FluidVesselTank createInventory() {
        return new FluidVesselTank(getCapacityMultiplier(), this::onFluidStackChanged);
    }

    // refreshCapability()/handlerForCapability() are inherited unchanged from FluidTankBlockEntity -
    // the base class's own implementation already does exactly this (including dispatching virtually
    // into our own BoilerData.isActive()/createHandler() overrides via the shared `boiler` field), so
    // no override is needed here anymore now that NeoForge's per-instance invalidateCapabilities()
    // call (which these overrides used to also perform) no longer exists.

    // Real FluidTankBlockEntity.updateConnectivity() is `protected` and declared in a different
    // package (com.zurrtum.create.content.fluids.tank) than FluidVesselBlock (this mod's own
    // package) - Create Fly's own FluidTankBlock can call it via method reference because it lives in
    // the SAME package as FluidTankBlockEntity, but FluidVesselBlock cannot reach a same-package-only
    // protected member from a different package. Widening visibility here (behavior unchanged, just
    // delegates to super) is what lets FluidVesselBlock's onPlace() keep using the same
    // `FluidVesselBlockEntity::updateConnectivity` method-reference idiom as upstream.
    @Override
    public void updateConnectivity() {
        super.updateConnectivity();
    }

    // Same story as updateConnectivity() above, for FluidVesselBlock.updateBoilerState()'s use of
    // controllerBE.updateBoilerState() - already public in the base class, no change needed there.

    // Real FluidTankBlockEntity.updateStateLuminosity() hardcodes com.zurrtum.create.content.fluids.
    // tank.FluidTankBlock.LIGHT_LEVEL, which is a different BlockStateProperty instance than our own
    // FluidVesselBlock.LIGHT_LEVEL (added specifically to mirror this - see FluidVesselBlock.java) -
    // state.getValue()/setValue() with a property the state's own definition doesn't contain throws,
    // so this must be overridden to target the right property for our own block hierarchy.
    @Override
    protected void updateStateLuminosity() {
        if (level.isClientSide())
            return;
        int actualLuminosity = luminosity;
        FluidVesselBlockEntity controllerBE = getControllerBE();
        if (controllerBE == null || !controllerBE.window)
            actualLuminosity = 0;
        refreshBlockState();
        BlockState state = getBlockState();
        if (state.getValue(LIGHT_LEVEL) != actualLuminosity)
            level.setBlock(worldPosition, state.setValue(LIGHT_LEVEL, actualLuminosity), 23);
    }

    // Moved here from FluidVesselBlock.onRemove(state, level, pos, newState, isMoving), which no
    // longer exists as an overridable Block method - vanilla now calls this hook on the block entity
    // itself only when it's genuinely being discarded (matching the old override's own
    // state.getBlock() != newState.getBlock() guard, so no equivalent check is needed here).
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        ConnectivityHandler.splitMulti(this);
    }

    @Override
    public void tick() {
        super.tick();
        if (fluidLevel != null)
            fluidLevel.tickChaser();
    }

    @Override
    public BlockPos getLastKnownPos() {
        return lastKnownPos;
    }

    public Axis getAxis() {
        return getBlockState().getValue(AXIS);
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

    @Override
    protected void onFluidStackChanged(FluidStack newFluidStack) {
        if (!hasLevel())
            return;

        // Real vanilla Fluid has no NeoForge FluidType light-level/isLighterThanAir concept. Create
        // Fly's own FluidTankBlockEntity.onFluidStackChanged works around the light level by reusing
        // the fluid's legacy BlockState light emission; isLighterThanAir has no Fabric replacement at
        // all yet (Create Fly's own upstream leaves this hardcoded false too - see FluidTankBlock.java/
        // SpoutRenderer.java "TODO" comments in its own sources), so gas-type fluids would render as
        // heavier-than-air - this mod ships none, so it's a no-op in practice.
        int luminosity = (int) (newFluidStack.getFluid().defaultFluidState().createLegacyBlock().getLightEmission() / 1.2f);
        boolean reversed = false;
        int maxY = (int) ((getFillState() * width) + 1);
        Axis axis = getAxis();

        for (int yOffset = 0; yOffset < width; yOffset++) {
            boolean isBright = reversed ? (width - yOffset <= maxY) : (yOffset < maxY);
            int actualLuminosity = isBright ? luminosity : luminosity > 0 ? 1 : 0;

            for (int lengthOffset = 0; lengthOffset < height; lengthOffset++) {
                for (int widthOffset = 0; widthOffset < width; widthOffset++) {
                    BlockPos pos = this.worldPosition.offset(
                            axis == Axis.X ? lengthOffset : widthOffset,
                            yOffset,
                            axis == Axis.Z ? lengthOffset : widthOffset
                    );
                    FluidVesselBlockEntity vesselAt = ConnectivityHandler.partAt(getType(), level, pos);
                    if (vesselAt == null)
                        continue;
                    level.updateNeighbourForOutputSignal(pos, vesselAt.getBlockState()
                            .getBlock());
                    if (vesselAt.luminosity == actualLuminosity)
                        continue;
                    vesselAt.setLuminosity(actualLuminosity);
                }
            }
        }

        if (!level.isClientSide()) {
            setChanged();
            sendData();
        }

        if (isVirtual()) {
            if (fluidLevel == null)
                fluidLevel = LerpedFloat.linear()
                        .startWithValue(getFillState());
            fluidLevel.chase(getFillState(), .5f, LerpedFloat.Chaser.EXP);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public FluidVesselBlockEntity getControllerBE() {
        if (isController())
            return this;
        BlockEntity blockEntity = level.getBlockEntity(controller);
        if (blockEntity instanceof FluidVesselBlockEntity)
            return (FluidVesselBlockEntity) blockEntity;
        return null;
    }

    @Override
    public void removeController(boolean keepFluids) {
        if (level.isClientSide())
            return;
        updateConnectivity = true;
        if (!keepFluids)
            applyFluidTankSize(1);
        controller = null;
        width = 1;
        height = 1;
        boiler.clear();
        onFluidStackChanged(tankInventory.getFluid());

        BlockState state = getBlockState();
        if (isVessel(state)) {
            state = state.setValue(POSITIVE, true);
            state = state.setValue(NEGATIVE, true);
            state = state.setValue(SHAPE, window ? Shape.WINDOW : Shape.PLAIN);
            getLevel().setBlock(worldPosition, state, 22);
        }

        refreshCapability();
        setChanged();
        sendData();
    }

    public boolean isWindowTypeAllowed(WindowType type) {
        return switch (type) {
            case SIDE_WIDE -> true;
            case SIDE_NARROW_ENDS -> height >= 2;
            case SIDE_NARROW_THIRDS -> height >= 3;
            case SIDE_HORIZONTAL -> width > 2 && width % 2 == 1;
        };
    }

    @Override
    public void toggleWindows() {
        FluidVesselBlockEntity be = getControllerBE();
        if (be == null)
            return;
        if (be.boiler.isActive())
            return;
        if (!be.window) {
            be.setWindowType(WindowType.SIDE_WIDE);
            be.setWindows(true);
        } else {
            WindowType[] types = WindowType.values();
            if (be.windowType.ordinal() >= types.length - 1) {
                be.setWindows(false);
                return;
            }
            WindowType nextType = types[be.windowType.ordinal() + 1];
            while (!be.isWindowTypeAllowed(nextType)) {
                if (nextType.ordinal() >= types.length - 1) {
                    be.setWindows(false);
                    return;
                }
                nextType = types[nextType.ordinal() + 1];
            }
            be.setWindowType(nextType);
            be.setWindows(true);
        }
    }

    public WindowType getWindowType() {
        return windowType;
    }

    public void setWindowType(WindowType windowType) {
        this.windowType = windowType;
    }

    @Override
    public void setWindows(boolean window) {
        this.window = window;
        Axis axis = getAxis();
        for (int yOffset = 0; yOffset < width; yOffset++) {
            for (int lengthOffset = 0; lengthOffset < height; lengthOffset++) {
                for (int widthOffset = 0; widthOffset < width; widthOffset++) {

                    BlockPos pos = this.worldPosition.offset(
                            axis == Axis.X ? lengthOffset : widthOffset,
                            yOffset,
                            axis == Axis.Z ? lengthOffset : widthOffset
                    );
                    BlockState blockState = level.getBlockState(pos);
                    if (!isVessel(blockState))
                        continue;

                    Shape shape = Shape.PLAIN;
                    if (window)
                        if (windowType == WindowType.SIDE_HORIZONTAL) {
                            if (yOffset == width / 2) {
                                shape = Shape.WINDOW;
                            }
                        } else if (windowType == WindowType.SIDE_WIDE || height <= 1) {
                            if ((widthOffset == 0 || widthOffset == width - 1)) {
                                if (width == 1)
                                    shape = Shape.WINDOW;
                                else if (yOffset == 0)
                                    shape = Shape.WINDOW_TOP;
                                else if (yOffset == width - 1)
                                    shape = Shape.WINDOW_BOTTOM;
                                else
                                    shape = Shape.WINDOW_MIDDLE;
                            }
                        } else if (windowType == WindowType.SIDE_NARROW_ENDS || windowType == WindowType.SIDE_NARROW_THIRDS) {
                            int windowOffset = windowType == WindowType.SIDE_NARROW_ENDS ? 0 : Math.max(1, height / 3 - 1);
                            if ((lengthOffset == windowOffset || lengthOffset == height - 1 - windowOffset) && (widthOffset == 0 || widthOffset == width - 1)) {
                                if (width == 1)
                                    shape = Shape.WINDOW_SINGLE;
                                else if (yOffset == 0)
                                    shape = Shape.WINDOW_TOP_SINGLE;
                                else if (yOffset == width - 1)
                                    shape = Shape.WINDOW_BOTTOM_SINGLE;
                                else
                                    shape = Shape.WINDOW_MIDDLE_SINGLE;
                            }
                        }

                    level.setBlock(pos, blockState.setValue(SHAPE, shape), 22);
                    level.getChunkSource()
                            .getLightEngine()
                            .checkBlock(pos);
                }
            }
        }
    }

    @Override
    public void updateBoilerState() {
        if (!isController())
            return;

        boolean wasBoiler = boiler.isActive();
        boolean changed = boiler.evaluate(this);

        if (wasBoiler != boiler.isActive()) {
            if (boiler.isActive())
                setWindows(false);

            Axis axis = getAxis();
            for (int yOffset = 0; yOffset < width; yOffset++)
                for (int lengthOffset = 0; lengthOffset < height; lengthOffset++)
                    for (int widthOffset = 0; widthOffset < width; widthOffset++)
                        if (level.getBlockEntity(
                                worldPosition.offset(
                                        axis == Axis.X ? lengthOffset : widthOffset,
                                        yOffset,
                                        axis == Axis.Z ? lengthOffset : widthOffset
                                )) instanceof FluidVesselBlockEntity fbe)
                            fbe.refreshCapability();
        }

        if (changed) {
            notifyUpdate();
            boiler.checkPipeOrganAdvancement(this);
        }
    }

    @Override
    public void setController(BlockPos controller) {
        if (level.isClientSide() && !isVirtual())
            return;
        if (controller.equals(this.controller))
            return;
        this.controller = controller;
        refreshCapability();
        setChanged();
        sendData();
    }

    @Override
    public BlockPos getController() {
        return isController() ? worldPosition : controller;
    }

    @Override
    protected AABB createRenderBoundingBox() {
        if (isController()) {
            Axis axis = getAxis();
            return super.createRenderBoundingBox().expandTowards(
                    axis == Axis.X ? (height - 1) : (width - 1),
                    width - 1,
                    axis == Axis.Z ? (height - 1) : (width - 1)
            );
        } else
            return super.createRenderBoundingBox();
    }

    @Override
    @Nullable
    public FluidVesselBlockEntity getOtherFluidTankBlockEntity(Direction direction) {
        BlockEntity otherBE = level.getBlockEntity(worldPosition.relative(direction));
        if (otherBE instanceof FluidVesselBlockEntity)
            return (FluidVesselBlockEntity) otherBE;
        return null;
    }

    // addToGoggleTooltip moved to the client-only FluidVesselTooltipBehaviour (see the class-level
    // comment above) - IHaveGoggleInformation/containedFluidTooltip aren't available here anymore.

    @Override
    protected void read(ValueInput view, boolean clientPacket) {
        super.read(view, clientPacket);

        BlockPos controllerBefore = controller;
        int prevWidth = width;
        int prevLength = height;
        int prevLum = luminosity;

        updateConnectivity = view.getBooleanOr("Uninitialized", false);
        luminosity = view.getIntOr("Luminosity", 0);

        lastKnownPos = view.read("LastKnownPos", BlockPos.CODEC).orElse(null);
        controller = view.read("Controller", BlockPos.CODEC).orElse(null);

        if (isController()) {
            window = view.getBooleanOr("Window", false);
            WindowType[] windowTypes = WindowType.values();
            String windowTypeName = view.getStringOr("WindowType", windowTypes[0].name());
            WindowType parsedWindowType = windowTypes[0];
            for (WindowType t : windowTypes)
                if (t.name().equals(windowTypeName))
                    parsedWindowType = t;
            windowType = parsedWindowType;
            width = view.getIntOr("Size", 0);
            height = view.getIntOr("Height", 0);
            tankInventory.setCapacity(getTotalTankSize() * getCapacityMultiplier());

            // Real com.zurrtum.create.foundation.fluid.FluidTank.read(view) reads its own "Fluid" key
            // straight off the view via FluidStack.CODEC and clamps to capacity itself (see its
            // source) - no raw CompoundTag bridging or manual overflow-drain needed anymore.
            tankInventory.read(view);
        }

        boiler.read(view.childOrEmpty("Boiler"), width * width * height);

        boolean forceFluidLevel = view.getBooleanOr("ForceFluidLevel", false);
        if (forceFluidLevel || fluidLevel == null)
            fluidLevel = LerpedFloat.linear()
                    .startWithValue(getFillState());

        updateCapability = true;

        if (!clientPacket)
            return;

        boolean changeOfController =
                controllerBefore == null ? controller != null : !controllerBefore.equals(controller);
        if (changeOfController || prevWidth != width || prevLength != height) {
            if (hasLevel())
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 16);
            if (isController())
                tankInventory.setCapacity(getCapacityMultiplier() * getTotalTankSize());
            invalidateRenderBoundingBox();
        }
        if (isController()) {
            float fillState = getFillState();
            if (forceFluidLevel || fluidLevel == null)
                fluidLevel = LerpedFloat.linear()
                        .startWithValue(fillState);
            fluidLevel.chase(fillState, 0.5f, LerpedFloat.Chaser.EXP);
        }
        if (luminosity != prevLum && hasLevel())
            level.getChunkSource()
                    .getLightEngine()
                    .checkBlock(worldPosition);

        if (view.getBooleanOr("LazySync", false))
            fluidLevel.chase(fluidLevel.getChaseTarget(), 0.125f, LerpedFloat.Chaser.EXP);
    }

    @Override
    public void write(ValueOutput view, boolean clientPacket) {
        if (updateConnectivity)
            view.putBoolean("Uninitialized", true);
        boiler.write(view.child("Boiler"));
        if (lastKnownPos != null)
            view.store("LastKnownPos", BlockPos.CODEC, lastKnownPos);
        if (!isController())
            view.store("Controller", BlockPos.CODEC, controller);
        if (isController()) {
            view.putBoolean("Window", window);
            view.putString("WindowType", windowType.name());
            tankInventory.write(view);
            view.putInt("Size", width);
            view.putInt("Height", height);
        }
        view.putInt("Luminosity", luminosity);
        super.write(view, clientPacket);

        if (!clientPacket)
            return;
        if (forceFluidLevelUpdate)
            view.putBoolean("ForceFluidLevel", true);
        if (queuedSync)
            view.putBoolean("LazySync", true);
        forceFluidLevelUpdate = false;
    }

    public static int getMaxSize() {
        return MAX_SIZE;
    }

    public static int getCapacityMultiplier() {
        // Create Fly stores fluid in Fabric droplets (81,000 per bucket), not millibuckets.
        // With the default tank capacity of 8 buckets this is reported as 8,000 mB per vessel.
        return AllConfigs.server().fluids.fluidTankCapacity.get() * BucketFluidInventory.CAPACITY;
    }

    public static int getMaxHeight() {
        return MAX_HEIGHT;
    }

    @Override
    public LerpedFloat getFluidLevel() {
        return fluidLevel;
    }

    @Override
    public void setFluidLevel(LerpedFloat fluidLevel) {
        this.fluidLevel = fluidLevel;
    }

    @Override
    public void preventConnectivityUpdate() {
        updateConnectivity = false;
    }

    @Override
    public void notifyMultiUpdated() {
        BlockState state = this.getBlockState();
        if (isVessel(state)) { // safety
            Axis axis = getAxis();
            state = state.setValue(NEGATIVE, axis == Axis.X
                    ? getController().getX() == getBlockPos().getX()
                    : getController().getZ() == getBlockPos().getZ());
            state = state.setValue(POSITIVE, axis == Axis.X
                    ? getController().getX() + height - 1 == getBlockPos().getX()
                    : getController().getZ() + height - 1 == getBlockPos().getZ());
            level.setBlock(getBlockPos(), state, 6);
        }
        if (isController())
            setWindows(window);
        onFluidStackChanged(tankInventory.getFluid());
        updateBoilerState();
        setChanged();
    }

    @Override
    public void setExtraData(@Nullable Object data) {
        if (data == null) {
            window = false;
            windowType = WindowType.SIDE_WIDE;
        } else if (data instanceof WindowType type) {
            window = true;
            windowType = type;
        }
    }

    @Override
    @Nullable
    public Object getExtraData() {
        return window ? windowType : null;
    }

    @Override
    public Object modifyExtraData(Object data) {
        if (data == null || (data instanceof WindowType)) {
            if (data != null && !window) return data;
            if (window) return windowType;
            return null;
        }
        return data;
    }

    @Override
    public Axis getMainConnectionAxis() {
        return getAxis();
    }

    @Override
    public int getMaxLength(Axis longAxis, int width) {
        if (longAxis == Axis.Y) return getMaxWidth();
        return getMaxHeight();
    }

    @Override
    public int getMaxWidth() {
        return 3;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public void setHeight(int height) {
        this.height = height;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public void setWidth(int width) {
        this.width = width;
    }

    @Override
    public int getTankSize(int tank) {
        return getCapacityMultiplier();
    }

    @Override
    public void setTankSize(int tank, int blocks) {
        applyFluidTankSize(blocks);
    }

    public boolean hasWindow() {
        return window;
    }

    public int getLuminosity() {
        return luminosity;
    }

}
