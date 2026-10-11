package com.wpc.map.reset.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.world.InteractionResult;

import com.wpc.map.reset.WPCMapReset;
import com.wpc.map.reset.command.ClipboardCommands;

public class WPCMapResetFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        WPCMapReset.init();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            ClipboardCommands.register(dispatcher, registryAccess, environment);
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            WPCMapReset.onServerStopping();
        });

        AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) -> {
            if (WPCMapReset.onLeftClickWand(player, level, hand, pos)) {
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        });

        UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
            if (WPCMapReset.onRightClickWand(player, level, hand, hitResult.getBlockPos())) {
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        });
    }
}
