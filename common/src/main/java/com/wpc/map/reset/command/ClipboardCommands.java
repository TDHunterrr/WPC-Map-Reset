package com.wpc.map.reset.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.wpc.map.reset.clipboard.ClipboardManager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.component.DataComponents;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class ClipboardCommands {
    
    public static boolean checkPerm(CommandSourceStack source) {
        return source.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER);
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess,
            Commands.CommandSelection environment) {

        dispatcher.register(Commands.literal("wpc")
            .requires(ClipboardCommands::checkPerm)
            .executes(context -> {
            ServerPlayer player = context.getSource().getPlayerOrException();
            ItemStack wand = new ItemStack(Items.STICK);
            wand.set(DataComponents.CUSTOM_NAME, Component.literal("WPC Wand").withStyle(style -> style.withColor(net.minecraft.ChatFormatting.DARK_RED).withBold(true).withUnderlined(true).withItalic(false)));
            net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
            tag.putBoolean("wpc_wand", true);
            wand.set(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
            player.getInventory().add(wand);
            context.getSource().sendSuccess(() -> Component.literal("\u00A7aGiven WPC Wand!"), false);
            return 1;
        }));

        dispatcher.register(Commands.literal("copy")
            .requires(ClipboardCommands::checkPerm)
            .executes(context -> {
            ServerPlayer player = context.getSource().getPlayerOrException();
            BlockPos pos1 = ClipboardManager.pos1Map.get(player.getUUID());
            BlockPos pos2 = ClipboardManager.pos2Map.get(player.getUUID());

            if (pos1 == null || pos2 == null) {
                context.getSource()
                        .sendFailure(Component.literal("§cYou must select two positions first using the wand."));
                return 0;
            }

            BlockPos min = new BlockPos(
                    Math.min(pos1.getX(), pos2.getX()),
                    Math.min(pos1.getY(), pos2.getY()),
                    Math.min(pos1.getZ(), pos2.getZ()));
            BlockPos max = new BlockPos(
                    Math.max(pos1.getX(), pos2.getX()),
                    Math.max(pos1.getY(), pos2.getY()),
                    Math.max(pos1.getZ(), pos2.getZ()));

            StructureTemplate template = new StructureTemplate();
            ServerLevel level = (ServerLevel) player.level();

            Vec3i size = max.subtract(min).offset(1, 1, 1);

            template.fillFromWorld(level, min, size, true, List.of(Blocks.STRUCTURE_VOID));

            BlockPos offset = min.subtract(player.blockPosition());
            template.setAuthor("wpc:" + offset.getX() + "," + offset.getY() + "," + offset.getZ());

            ClipboardManager.setClipboard(player.getUUID(), template);
            context.getSource().sendSuccess(() -> Component.literal("§aCopied area of size " + size.toShortString()),
                    false);
            return 1;
        }));

        dispatcher.register(Commands.literal("paste")
                .requires(ClipboardCommands::checkPerm)
                .executes(context -> executePaste(context, null, 0, 0, 0))
                .then(Commands.argument("pos", Vec3Argument.vec3())
                        .then(Commands.argument("rotX", FloatArgumentType.floatArg())
                                .then(Commands.argument("rotY", FloatArgumentType.floatArg())
                                        .then(Commands.argument("rotZ", FloatArgumentType.floatArg())
                                                .executes(context -> executePaste(
                                                        context,
                                                        Vec3Argument.getVec3(context, "pos"),
                                                        FloatArgumentType.getFloat(context, "rotX"),
                                                        FloatArgumentType.getFloat(context, "rotY"),
                                                        FloatArgumentType.getFloat(context, "rotZ"))))))));

        dispatcher.register(Commands.literal("save")
                .requires(ClipboardCommands::checkPerm)
                .then(Commands.argument("filename", StringArgumentType.string())
                        .then(Commands.argument("extension", StringArgumentType.word())
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    String filename = StringArgumentType.getString(context, "filename");
                                    String extension = StringArgumentType.getString(context, "extension");
                                    ServerLevel level = context.getSource().getLevel();

                                    boolean success = ClipboardManager.saveClipboard(player.getUUID(), level, filename,
                                            extension);
                                    if (success) {
                                        context.getSource().sendSuccess(
                                                () -> Component
                                                        .literal("§aSaved clipboard to " + filename + "." + extension),
                                                true);
                                        return 1;
                                    } else {
                                        context.getSource().sendFailure(
                                                Component.literal("§cFailed to save clipboard. Is it empty?"));
                                        return 0;
                                    }
                                }))));

        dispatcher.register(Commands.literal("load")
                .requires(ClipboardCommands::checkPerm)
                .then(Commands.argument("filename", StringArgumentType.string())
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            String filename = StringArgumentType.getString(context, "filename");
                            ServerLevel level = context.getSource().getLevel();

                            boolean success = ClipboardManager.loadClipboard(player.getUUID(), level, filename);
                            if (success) {
                                context.getSource().sendSuccess(() -> Component.literal(
                                        "§aLoaded structure " + filename + " to clipboard. Use /paste to place it."),
                                        true);
                                return 1;
                            } else {
                                context.getSource()
                                        .sendFailure(Component.literal("§cFailed to load structure. File not found."));
                                return 0;
                            }
                        })));
    }

    private static int executePaste(CommandContext<CommandSourceStack> context, Vec3 posVec, float rotX, float rotY,
            float rotZ)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        StructureTemplate template = ClipboardManager.getClipboard(player.getUUID());

        if (template == null) {
            context.getSource().sendFailure(Component.literal("§cClipboard is empty."));
            return 0;
        }

        String author = template.getAuthor();
        BlockPos storedOffset = BlockPos.ZERO;
        if (author != null && author.startsWith("wpc:")) {
            String[] parts = author.substring(4).split(",");
            if (parts.length == 3) {
                try {
                    storedOffset = new BlockPos(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
                } catch (Exception e) {}
            }
        }

        BlockPos pastePos;
        if (posVec == null) {
            pastePos = player.blockPosition().offset(storedOffset);
        } else {
            pastePos = BlockPos.containing(posVec).offset(storedOffset);
        }

        ServerLevel level = context.getSource().getLevel();

        CompoundTag nbt = template.save(new CompoundTag());
        ListTag blocks = nbt.getListOrEmpty("blocks");
        ListTag palette = nbt.getListOrEmpty("palette");

        double radX = Math.toRadians(rotX);
        double radY = Math.toRadians(rotY);
        double radZ = Math.toRadians(rotZ);

        for (int i = 0; i < blocks.size(); i++) {
            CompoundTag blockNbt = blocks.getCompoundOrEmpty(i);
            ListTag posList = blockNbt.getListOrEmpty("pos");

            if (posList.isEmpty())
                continue;
            int x = posList.getIntOr(0, 0);
            int y = posList.getIntOr(1, 0);
            int z = posList.getIntOr(2, 0);

            double y1 = y * Math.cos(radX) - z * Math.sin(radX);
            double z1 = y * Math.sin(radX) + z * Math.cos(radX);

            double x2 = x * Math.cos(radY) + z1 * Math.sin(radY);
            double z2 = -x * Math.sin(radY) + z1 * Math.cos(radY);

            double x3 = x2 * Math.cos(radZ) - y1 * Math.sin(radZ);
            double y3 = x2 * Math.sin(radZ) + y1 * Math.cos(radZ);

            BlockPos targetPos = pastePos.offset((int) Math.round(x3), (int) Math.round(y3), (int) Math.round(z2));
            int stateId = blockNbt.getIntOr("state", 0);

            if (stateId >= 0 && stateId < palette.size()) {
                CompoundTag stateNbt = palette.getCompoundOrEmpty(stateId);
                String blockName = stateNbt.getStringOr("Name", "");
                if (blockName.isEmpty())
                    continue;

                Block block = net.minecraft.core.registries.BuiltInRegistries.BLOCK
                        .getValue(net.minecraft.resources.Identifier.parse(blockName));
                net.minecraft.world.level.block.state.BlockState stateToPlace = block.defaultBlockState();

                level.setBlock(targetPos, stateToPlace, 3);

                if (blockNbt.contains("nbt")) {
                    net.minecraft.world.level.block.entity.BlockEntity be = level.getBlockEntity(targetPos);
                    if (be != null) {
                        CompoundTag entityNbt = blockNbt.getCompoundOrEmpty("nbt").copy();
                        entityNbt.putInt("x", targetPos.getX());
                        entityNbt.putInt("y", targetPos.getY());
                        entityNbt.putInt("z", targetPos.getZ());
                        net.minecraft.world.level.block.entity.BlockEntity newBe = net.minecraft.world.level.block.entity.BlockEntity.loadStatic(targetPos, stateToPlace, entityNbt, level.registryAccess());
                        if (newBe != null) {
                            level.setBlockEntity(newBe);
                        }
                    }
                }
            }
        }

        context.getSource().sendSuccess(() -> Component.literal(
                "§aPasted clipboard at " + pastePos.toShortString() +
                        " with rotation (" + rotX + ", " + rotY + ", " + rotZ + ")"),
                true);
        return 1;
    }
}


