package com.hlysine.create_connected.content.redstonelinkwildcard;

import com.hlysine.create_connected.registries.CCItems;
import com.hlysine.create_connected.CreateConnected;
import com.hlysine.create_connected.config.CServer;
import com.hlysine.create_connected.config.FeatureToggle;
import com.zurrtum.create.content.redstone.link.IRedstoneLinkable;
import com.zurrtum.create.content.redstone.link.ServerLinkBehaviour;
import com.zurrtum.create.content.redstone.link.RedstoneLinkNetworkHandler;
import com.zurrtum.create.content.redstone.link.RedstoneLinkNetworkHandler.Frequency;
import com.zurrtum.create.infrastructure.config.AllConfigs;
import com.zurrtum.create.catnip.data.Couple;
import com.zurrtum.create.catnip.registry.RegisteredObjectsHelper;
import com.zurrtum.create.catnip.levelWrappers.WorldHelper;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.LevelAccessor;
import org.joml.Vector3d;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

// Real logic preserved as-is, just re-hosted on Fabric APIs:
// - net.neoforged...EventBusSubscriber/@SubscribeEvent/LevelEvent.Load/Unload -> Fabric API's
//   ServerWorldEvents.LOAD/UNLOAD (registered from CreateConnected's main entrypoint - see
//   register() below, called from CreateConnected.onInitialize()).
// - client.content.redstone.link.LinkBehaviour (client-only UI wrapper) -> the real Create Fly
//   RedstoneLinkNetworkHandler.updateNetworkOf itself already uses ServerLinkBehaviour (the actual
//   state holder, verified in the real sources jar) for this exact same instanceof check, so this
//   is not a workaround but matching Create Fly's own real pattern.
public class LinkWildcardNetworkHandler {
    static final Map<LevelAccessor, Map<Couple<Frequency>, Set<Couple<Frequency>>>> transmitter_connections =
            new IdentityHashMap<>();
    static final Map<LevelAccessor, Map<Couple<Frequency>, Set<Couple<Frequency>>>> receiver_connections =
            new IdentityHashMap<>();

    public static void register() {
        ServerWorldEvents.LOAD.register((server, level) -> {
            transmitter_connections.put(level, new HashMap<>());
            receiver_connections.put(level, new HashMap<>());
            CreateConnected.LOGGER.debug("Link-Wildcard: Prepared Redstone Network Wildcards for {}", WorldHelper.getDimensionID(level));
        });
        ServerWorldEvents.UNLOAD.register((server, level) -> {
            transmitter_connections.remove(level);
            receiver_connections.remove(level);
            CreateConnected.LOGGER.debug("Link-Wildcard: Removed Redstone Network Wildcards for {}", WorldHelper.getDimensionID(level));
        });
    }

    public static Map<Couple<Frequency>, Set<Couple<Frequency>>> transmittersIn(LevelAccessor world) {
        if (!transmitter_connections.containsKey(world)) {
            CreateConnected.LOGGER.warn("Link-Wildcard: Tried to Access unprepared network transmitters of {}", WorldHelper.getDimensionID(world));
            return new HashMap<>();
        }
        return transmitter_connections.get(world);
    }

    public static Map<Couple<Frequency>, Set<Couple<Frequency>>> receiversIn(LevelAccessor world) {
        if (!receiver_connections.containsKey(world)) {
            CreateConnected.LOGGER.warn("Link-Wildcard: Tried to Access unprepared network receivers of {}", WorldHelper.getDimensionID(world));
            return new HashMap<>();
        }
        return receiver_connections.get(world);
    }

    public static boolean updateNetworkOf(RedstoneLinkNetworkHandler handler, LevelAccessor world, IRedstoneLinkable actor) {
        if (!FeatureToggle.isEnabled(RegisteredObjectsHelper.getKeyOrThrow(CCItems.REDSTONE_LINK_WILDCARD)))
            return false;

        Couple<Frequency> key = actor.getNetworkKey();
        updateNetworkForReceiver(handler, world, actor, key);
        if (actor.isListening())
            return true;
        Map<Couple<Frequency>, Set<Couple<Frequency>>> transmitters = transmittersIn(world);
        if (transmitters.containsKey(key)) {
            Set<Couple<Frequency>> connections = transmitters.get(key);
            for (Couple<Frequency> connection : connections) {
                updateNetworkForReceiver(handler, world, actor, connection);
            }
        }
        return true;
    }

    private static void updateNetworkForReceiver(RedstoneLinkNetworkHandler handler, LevelAccessor world, IRedstoneLinkable actor, Couple<Frequency> key) {
        Map<Couple<Frequency>, Set<IRedstoneLinkable>> networksInWorld = handler.networksIn(world);
        Map<Couple<Frequency>, Set<Couple<Frequency>>> receiversInWorld = receiversIn(world);
        Set<IRedstoneLinkable> network = networksInWorld.get(key);
        Set<Couple<Frequency>> receivers = receiversInWorld.get(key);

        handler.globalPowerVersion.incrementAndGet();
        AtomicInteger power = new AtomicInteger(0);

        Consumer<Set<IRedstoneLinkable>> updatePower = (set) -> {
            if (set == null || set.isEmpty())
                return;
            for (Iterator<IRedstoneLinkable> iterator = set.iterator(); iterator.hasNext(); ) {
                IRedstoneLinkable other = iterator.next();
                if (!other.isAlive()) {
                    iterator.remove();
                    continue;
                }

                if (other.isListening())
                    continue;

                if (!withinRange(actor, other, world))
                    continue;

                if (power.get() < 15)
                    power.accumulateAndGet(other.getTransmittedStrength(), Math::max);
            }
        };

        updatePower.accept(network);
        if (receivers != null)
            for (Couple<Frequency> wildcard : receivers) {
                Set<IRedstoneLinkable> wildcardNetwork = networksInWorld.get(wildcard);
                updatePower.accept(wildcardNetwork);
            }

        if (actor instanceof ServerLinkBehaviour linkBehaviour) {
            // fix one-to-one loading order problem
            if (linkBehaviour.isListening()) {
                linkBehaviour.newPosition = true;
                linkBehaviour.setReceivedStrength(power.get());
            }
        }

        if (network != null && !network.isEmpty())
            for (IRedstoneLinkable other : network) {
                if (other != actor && other.isListening() && withinRange(actor, other, world))
                    other.setReceivedStrength(power.get());
            }
    }

    public static void addToNetwork(RedstoneLinkNetworkHandler handler, LevelAccessor world, IRedstoneLinkable actor) {
        Couple<Frequency> key = actor.getNetworkKey();
        Map<Couple<Frequency>, Set<Couple<Frequency>>> wildcards = actor.isListening() ? receiversIn(world) : transmittersIn(world);
//        CreateConnected.LOGGER.debug("Link-Wildcard: New {}: {}", actor.isListening() ? "receiver" : "transmitter", keyToString(key));
        if (!wildcards.containsKey(key)) {
            HashSet<Couple<Frequency>> connections = new LinkedHashSet<>();
            Map<Couple<Frequency>, Set<IRedstoneLinkable>> networks = handler.networksIn(world);
            for (Couple<Frequency> otherKey : networks.keySet()) {
                if (!otherKey.equals(key) && test(key, otherKey)) {
                    if (connections.add(otherKey)) {
//                        CreateConnected.LOGGER.debug("Link-Wildcard: - {} {}", actor.isListening() ? "Receiving from" : "Transmitting to", keyToString(otherKey));
                    }
                }
            }
            wildcards.put(key, connections);
        }
        Map<Couple<Frequency>, Set<Couple<Frequency>>> oppositeSet = actor.isListening() ? transmittersIn(world) : receiversIn(world);
        for (Map.Entry<Couple<Frequency>, Set<Couple<Frequency>>> entry : oppositeSet.entrySet()) {
            if (!entry.getKey().equals(key) && test(entry.getKey(), key)) {
                if (entry.getValue().add(key)) {
//                    CreateConnected.LOGGER.debug("Link-Wildcard: - Reverse: {} {}", actor.isListening() ? "Receiving from" : "Transmitting to", keyToString(entry.getKey()));
                }
            }
        }
    }

    public static void removeFromNetwork(RedstoneLinkNetworkHandler handler, LevelAccessor world, IRedstoneLinkable actor) {
        Couple<Frequency> key = actor.getNetworkKey();
        Map<Couple<Frequency>, Set<IRedstoneLinkable>> networks = handler.networksIn(world);
        if (networks.containsKey(key) && !networks.get(key).isEmpty())
            return;
//        CreateConnected.LOGGER.debug("Link-Wildcard: Removing {} {}", actor.isListening() ? "receiver" : "transmitter", keyToString(key));
        Map<Couple<Frequency>, Set<Couple<Frequency>>> wildcards = actor.isListening() ? receiversIn(world) : transmittersIn(world);
        wildcards.remove(key);
        Map<Couple<Frequency>, Set<Couple<Frequency>>> oppositeSet = actor.isListening() ? transmittersIn(world) : receiversIn(world);
        for (Map.Entry<Couple<Frequency>, Set<Couple<Frequency>>> entry : oppositeSet.entrySet()) {
            if (entry.getValue().remove(key)) {
//                CreateConnected.LOGGER.debug("Link-Wildcard: - No longer {} {}", actor.isListening() ? "receiving from" : "transmitting to", keyToString(entry.getKey()));
                handler.updateNetworkOf(world, new IRedstoneLinkable() {
                    @Override
                    public int getTransmittedStrength() {
                        return 0;
                    }

                    @Override
                    public void setReceivedStrength(int power) {

                    }

                    @Override
                    public boolean isListening() {
                        return false;
                    }

                    @Override
                    public boolean isAlive() {
                        return true;
                    }

                    @Override
                    public Couple<Frequency> getNetworkKey() {
                        return entry.getKey();
                    }

                    @Override
                    public BlockPos getLocation() {
                        return actor.getLocation();
                    }
                });
            }
        }
        if (actor.isListening())
            actor.setReceivedStrength(0);
    }

    private static String keyToString(Couple<Frequency> key) {
        return String.format("%s + %s",
                BuiltInRegistries.ITEM.getKey(key.getFirst().getStack().getItem()),
                BuiltInRegistries.ITEM.getKey(key.getSecond().getStack().getItem())
        );
    }

    private static boolean test(Couple<Frequency> transmitter, Couple<Frequency> receiver) {
        if (!CServer.AllowDualWildcardLink.get() && transmitter.getFirst().getStack().getItem() instanceof ILinkWildcard && transmitter.getSecond().getStack().getItem() instanceof ILinkWildcard)
            return false;
        if (!CServer.AllowDualWildcardLink.get() && receiver.getFirst().getStack().getItem() instanceof ILinkWildcard && receiver.getSecond().getStack().getItem() instanceof ILinkWildcard)
            return false;
        return wildcardTransmit(transmitter.getFirst(), receiver.getFirst()) && wildcardTransmit(transmitter.getSecond(), receiver.getSecond()) ||
                wildcardReceive(transmitter.getFirst(), receiver.getFirst()) && wildcardReceive(transmitter.getSecond(), receiver.getSecond());
    }

    private static boolean wildcardTransmit(Frequency transmitter, Frequency receiver) {
        if (transmitter.getStack().getItem() instanceof ILinkWildcard wildcard) {
            return wildcard.test(receiver);
        } else {
            return transmitter.equals(receiver);
        }
    }

    private static boolean wildcardReceive(Frequency transmitter, Frequency receiver) {
        if (receiver.getStack().getItem() instanceof ILinkWildcard wildcard) {
            return wildcard.test(transmitter);
        } else {
            return transmitter.equals(receiver);
        }
    }

    // Originally implemented a custom range check for compatibility with the "Sable" mod
    // (dev.ryanhcode.sable, sub-level/moving-structure physics), transforming positions into a
    // sub-level's logical pose before distance-checking so links keep working aboard a moving
    // Sable structure. Verified via Modrinth (WebSearch, session 7) that Sable's newest release
    // only targets MC 1.21.1 - there is no 1.21.11-compatible build to depend on (compileOnly or
    // otherwise), so the real compat integration is currently impossible to reinstate, not merely
    // skipped. This falls back to the plain distance check, which is exactly the behavior the
    // original code already had whenever Sable's helper returned no sub-level for a position (its
    // null checks were unconditional no-ops in that case) - i.e. this is the correct behavior for
    // "Sable not present", just without the dead unreachable-for-us compat branch. Revisit if/when
    // Sable ships a 1.21.11 build: add modCompileOnly + FabricLoader.isModLoaded("sable") guarded
    // reflection or a dedicated compat/ class per this mod's existing Mods.java soft-dep pattern.
    private static boolean withinRange(IRedstoneLinkable from, IRedstoneLinkable to, LevelAccessor levelAccessor) {
        if (from == to) return true;

        final BlockPos fromLocation = from.getLocation();
        final Vector3d fromPos = new Vector3d(fromLocation.getX(), fromLocation.getY(), fromLocation.getZ());
        final BlockPos toLocation = to.getLocation();
        final Vector3d toPos = new Vector3d(toLocation.getX(), toLocation.getY(), toLocation.getZ());

        final int linkRange = AllConfigs.server().logistics.linkRange.get();
        return fromPos.distanceSquared(toPos) < linkRange * linkRange;
    }
}
