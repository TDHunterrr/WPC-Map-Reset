package com.wpc.map.reset.clipboard;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.storage.LevelResource;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ClipboardManager {
    public static final Map<UUID, net.minecraft.core.BlockPos> pos1Map = new HashMap<>();
    public static final Map<UUID, net.minecraft.core.BlockPos> pos2Map = new HashMap<>();
    private static final Map<UUID, StructureTemplate> clipboardMap = new HashMap<>();

    public static void setClipboard(UUID player, StructureTemplate template) {
        clipboardMap.put(player, template);
    }

    public static StructureTemplate getClipboard(UUID player) {
        return clipboardMap.get(player);
    }

    public static void clearAll() {
        clipboardMap.clear();
    }

    public static boolean saveClipboard(UUID player, ServerLevel level, String filename, String extension) {
        StructureTemplate template = clipboardMap.get(player);
        if (template == null)
            return false;

        try {
            Path worldDir = level.getServer().getWorldPath(LevelResource.ROOT);
            Path wpcMapsDir = worldDir.resolve("WPC MAPS");
            Files.createDirectories(wpcMapsDir);

            String fullFileName = filename;
            if (!fullFileName.endsWith("." + extension)) {
                fullFileName += "." + extension;
            }

            Path savePath = wpcMapsDir.resolve(fullFileName);
            CompoundTag nbt = template.save(new CompoundTag());

            NbtIo.writeCompressed(nbt, savePath);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public static boolean loadClipboard(UUID player, ServerLevel level, String filename) {
        try {
            Path worldDir = level.getServer().getWorldPath(LevelResource.ROOT);
            Path wpcMapsDir = worldDir.resolve("WPC MAPS");

            File dir = wpcMapsDir.toFile();
            if (!dir.exists())
                return false;

            File[] files = dir.listFiles((d, name) -> name.equals(filename) || name.equals(filename + ".nbt")
                    || name.equals(filename + ".schem"));
            if (files == null || files.length == 0)
                return false;

            Path loadPath = files[0].toPath();
            CompoundTag nbt = NbtIo.readCompressed(loadPath, NbtAccounter.unlimitedHeap());

            StructureTemplate template = new StructureTemplate();
            HolderGetter<Block> blockGetter = level.getServer().registryAccess().lookupOrThrow(Registries.BLOCK);
            template.load(blockGetter, nbt);

            clipboardMap.put(player, template);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}

