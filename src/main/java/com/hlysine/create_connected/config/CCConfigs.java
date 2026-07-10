package com.hlysine.create_connected.config;

import com.hlysine.create_connected.CreateConnected;
import com.zurrtum.create.api.stress.BlockStressValues;
import com.zurrtum.create.catnip.config.Builder;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

import java.util.function.Supplier;

public class CCConfigs {

    private static CCommon common;
    private static CServer server;

    public static CCommon common() {
        return common;
    }

    public static CServer server() {
        return server;
    }

    public static <T> Supplier<T> safeGetter(Supplier<T> getter, T defaultValue) {
        return () -> {
            try {
                return getter.get();
            } catch (IllegalStateException | NullPointerException ex) {
                // the config is accessed too early (before registration or before config load)
                return defaultValue;
            }
        };
    }

    public static void register() {
        SyncConfigBase.registerPayloadType();

        common = Builder.create(CCommon::new, CreateConnected.MODID, "common");
        server = Builder.create(CServer::new, CreateConnected.MODID, "server");

        CStress stress = server().stressValues;
        BlockStressValues.IMPACTS.registerProvider(stress::getImpact);
        BlockStressValues.CAPACITIES.registerProvider(stress::getCapacity);

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server1) -> common.syncToPlayer(handler.player));
    }
}
