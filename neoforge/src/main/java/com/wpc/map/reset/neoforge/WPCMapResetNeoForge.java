package com.wpc.map.reset.neoforge;

import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;

import com.wpc.map.reset.WPCMapReset;
import com.wpc.map.reset.command.ClipboardCommands;

@Mod(WPCMapReset.MOD_ID)
public class WPCMapResetNeoForge {
    public WPCMapResetNeoForge() {
        WPCMapReset.init();
        NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        ClipboardCommands.register(event.getDispatcher(), event.getBuildContext(), event.getCommandSelection());
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        WPCMapReset.onServerStopping();
    }

    @SubscribeEvent
    public void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (WPCMapReset.onLeftClickWand(event.getEntity(), event.getLevel(), event.getHand(), event.getPos())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (WPCMapReset.onRightClickWand(event.getEntity(), event.getLevel(), event.getHand(), event.getPos())) {
            event.setCanceled(true);
        }
    }
}
