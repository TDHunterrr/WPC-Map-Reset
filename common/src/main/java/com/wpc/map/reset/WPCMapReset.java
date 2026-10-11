package com.wpc.map.reset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.wpc.map.reset.clipboard.ClipboardManager;
import com.wpc.map.reset.command.ClipboardCommands;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.component.DataComponents;

import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;

public class WPCMapReset {
	public static final String MOD_ID = "wpc_map_reset";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static final java.util.Map<java.util.UUID, Long> lastClickTime = new java.util.HashMap<>();

	public static boolean isWand(ItemStack stack) {
		if (!stack.is(Items.STICK)) return false;
		
		Component name = stack.get(DataComponents.CUSTOM_NAME);
		if (name == null || !name.getString().contains("WPC Wand")) return false;
		
		net.minecraft.world.item.component.CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
		if (customData == null || !customData.copyTag().contains("wpc_wand")) return false;
		
		return true;
	}

	public static void init() {
		LOGGER.info("Initializing WPC Map Reset clipboard mod!");
	}

	public static void onServerStopping() {
		ClipboardManager.clearAll();
		LOGGER.info("Cleared all clipboard data.");
	}

	public static boolean onLeftClickWand(net.minecraft.world.entity.player.Player player, net.minecraft.world.level.Level level, net.minecraft.world.InteractionHand hand, BlockPos pos) {
		if (isWand(player.getItemInHand(hand))) {
			if (!level.isClientSide() && hand == net.minecraft.world.InteractionHand.MAIN_HAND) {
				if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
					if (!ClipboardCommands.checkPerm(serverPlayer.createCommandSourceStack())) return false;
				} else {
					return false;
				}
				
				long currentTime = System.currentTimeMillis();
				Long lastTime = lastClickTime.get(player.getUUID());
				if (lastTime == null || currentTime - lastTime > 200) {
					lastClickTime.put(player.getUUID(), currentTime);
					ClipboardManager.pos1Map.put(player.getUUID(), pos);
					player.displayClientMessage(Component.literal("\u00A7dPosition 1 set to " + pos.toShortString()), false);
				}
			}
			return true;
		}
		return false;
	}

	public static boolean onRightClickWand(net.minecraft.world.entity.player.Player player, net.minecraft.world.level.Level level, net.minecraft.world.InteractionHand hand, BlockPos pos) {
		if (isWand(player.getItemInHand(hand))) {
			if (!level.isClientSide() && hand == net.minecraft.world.InteractionHand.MAIN_HAND) {
				if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
					if (!ClipboardCommands.checkPerm(serverPlayer.createCommandSourceStack())) return false;
				} else {
					return false;
				}
				ClipboardManager.pos2Map.put(player.getUUID(), pos);
				player.displayClientMessage(Component.literal("\u00A7dPosition 2 set to " + pos.toShortString()), false);
			}
			return true;
		}
		return false;
	}
}
