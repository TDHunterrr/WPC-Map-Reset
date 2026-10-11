package com.wpc.map.reset.fabric.mixin;

import com.wpc.map.reset.WPCMapReset;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {
    @Inject(method = "stopServer", at = @At("HEAD"))
    private void onServerStopping(CallbackInfo ci) {
        WPCMapReset.onServerStopping();
    }
}
