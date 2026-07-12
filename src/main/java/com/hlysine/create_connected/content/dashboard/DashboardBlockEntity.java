package com.hlysine.create_connected.content.dashboard;

import com.zurrtum.create.foundation.blockEntity.SmartBlockEntity;
import com.zurrtum.create.api.behaviour.BlockEntityBehaviour;
import com.zurrtum.create.api.behaviour.display.DisplayHolder;
import com.zurrtum.create.catnip.data.Iterate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

// DashboardDisplayTarget.reserve(...)/isReserved(...) (the real DisplayTarget base class helpers used
// to track which display-link line already targets which line on a block) require a DisplayHolder
// (confirmed via javap on the real interface: getDisplayLinkData()/setDisplayLinkData(CompoundTag),
// backed by a single stored CompoundTag field - matches real Create Fly's own FlapDisplayBlockEntity
// pattern exactly), not a plain BlockEntity - implemented here.
public class DashboardBlockEntity extends SmartBlockEntity implements DisplayHolder {

    SignText text = new SignText().setColor(DyeColor.WHITE);
    int cycleTimer = 0;
    boolean wasDisplaying;
    private static final int LAZY_TICK_RATE = 4;
    private static final int CYCLE_INTERVAL = 40;
    private CompoundTag displayLink;

    // com.hlysine.create_connected.content.dashboard.ClientPlayerAccess (Minecraft.getInstance().player)
    // is a client-only class - calling it directly from this common-sourceset block entity was a real
    // cross-boundary bug (same class of bug as the addToGoggleTooltip cases elsewhere this session),
    // caught only by real per-sourceset ./gradlew compileJava. Rather than moving all of
    // displayStatus()/lazyTick()'s logic to a client wrapper class (nontrivial - it reads several
    // private fields), used the same static-hook indirection already established for
    // OverstressClutchBlockEntity.uncoupledTooltipHook: populated with the real
    // Minecraft.getInstance().player accessor from CreateConnectedClient.onInitializeClient().
    public static Supplier<Player> localPlayerHook = () -> null;

    public DashboardBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setLazyTickRate(LAZY_TICK_RATE);
    }

    @Override
    public CompoundTag getDisplayLinkData() {
        return displayLink;
    }

    @Override
    public void setDisplayLinkData(CompoundTag data) {
        displayLink = data;
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour<?>> behaviours) {
    }

    public SignText getText() {
        return text;
    }

    public void setText(SignText text) {
        this.text = text;
        notifyUpdate();
    }

    public void setLine(int line, Component text) {
        this.setText(this.getText().setMessage(line, text));
    }

    public void clearText() {
        SignText text = this.getText();
        for (int i = 0; i < SignText.LINES; i++) {
            text = text.setMessage(i, Component.empty());
        }
        this.setText(text);
    }

    public int getMaxTextLineWidth() {
        return 90;
    }

    public int getTextLineHeight() {
        return 10;
    }

    public @Nullable BlockPos getSeatPos() {
        if (!getBlockState().getValue(DashboardBlock.OPEN))
            return null;
        return getBlockPos().relative(getBlockState().getValue(DashboardBlock.FACING));
    }

    private @Nullable Component getStatusLine() {
        MutableComponent status = Component.empty();
        boolean needSpacer = false;
        for (int i = 0; i < SignText.LINES; i++) {
            Component line = this.text.getMessage(i, false);
            if (line.getString().isEmpty()) continue;
            if (needSpacer)
                status.append("   ");
            status.append(line).withColor(this.text.getColor().getTextColor());
            needSpacer = true;
        }
        if (!needSpacer)
            return null;
        return status;
    }

    private @Nullable List<Component> getAllDisplays(BlockPos seatPos) {
        List<Component> list = new ArrayList<>(4);
        for (Direction direction : Iterate.horizontalDirections) {
            BlockPos dashboardPos = seatPos.relative(direction);
            if (dashboardPos.equals(getBlockPos())) {
                if (!list.isEmpty()) return null; // one dashboard takes care of displaying status text for all
                Component status = getStatusLine();
                if (status == null) return null;
                list.add(status);
                continue;
            }
            BlockState state = getLevel().getBlockState(dashboardPos);
            if (state.getBlock() instanceof DashboardBlock && state.getValue(DashboardBlock.FACING) == direction.getOpposite() && state.getValue(DashboardBlock.OPEN)) {
                BlockEntity blockEntity = getLevel().getBlockEntity(dashboardPos);
                if (blockEntity instanceof DashboardBlockEntity dashboard) {
                    Component status = dashboard.getStatusLine();
                    if (status != null)
                        list.add(status);
                }
            }
        }
        return list;
    }

    private boolean displayStatus() {
        BlockPos seatPos = getSeatPos();
        if (seatPos == null)
            return false;

        Player player = localPlayerHook.get();
        if (player == null)
            return false;
        if (!player.isPassenger())
            return false;

        Vec3 center = Vec3.atCenterOf(seatPos);
        if (player.distanceToSqr(center) > 1.2)
            return false;
        List<Component> list = getAllDisplays(seatPos);
        if (list == null || list.isEmpty()) return false;

        Component status = list.get((cycleTimer / CYCLE_INTERVAL) % list.size());
        player.displayClientMessage(status, true);
        cycleTimer += LAZY_TICK_RATE;
        return true;
    }

    static void displayOpenStatus(Player player, boolean open) {
        player.displayClientMessage(Component.translatable("create_connected."
                + (open ? "dashboard.activate_hud" : "dashboard.deactivate_hud")), true);
    }

    @Override
    public void lazyTick() {
        super.lazyTick();

        if (getLevel().isClientSide()) {
            boolean success = displayStatus();
            if (!success && wasDisplaying) {
                Player player = localPlayerHook.get();
                if (player != null) {
                    if (!getBlockState().getValue(DashboardBlock.OPEN))
                        displayOpenStatus(player, false); // avoid flickering on wrench by displaying the open status instead of empty
                    else
                        player.displayClientMessage(Component.empty(), true);
                }
            }
            wasDisplaying = success;
        }
    }

    @Override
    public void write(ValueOutput view, boolean clientPacket) {
        super.write(view, clientPacket);
        view.store("text", SignText.DIRECT_CODEC, this.text);
        writeDisplayLink(view);
    }

    @Override
    protected void read(ValueInput view, boolean clientPacket) {
        super.read(view, clientPacket);
        view.read("text", SignText.DIRECT_CODEC).ifPresent(signText -> this.text = signText);
        readDisplayLink(view);
    }
}
