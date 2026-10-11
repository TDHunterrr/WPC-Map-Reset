package com.wpc.map.reset.fabric;

import net.fabricmc.api.ModInitializer;
import com.wpc.map.reset.WPCMapReset;

public class WPCMapResetFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        WPCMapReset.init();
    }
}
