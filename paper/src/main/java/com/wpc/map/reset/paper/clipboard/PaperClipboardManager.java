package com.wpc.map.reset.paper.clipboard;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.structure.Structure;
import org.bukkit.structure.StructureManager;
import org.bukkit.util.Vector;

import java.io.File;
import java.util.*;

public class PaperClipboardManager {
    public static final Map<UUID, Location> pos1Map = new HashMap<>();
    public static final Map<UUID, Location> pos2Map = new HashMap<>();

    public record BlockRecord(int x, int y, int z, BlockData data) {}

    public static class Clipboard {
        private final Vector offset;
        private final List<BlockRecord> blocks;
        private final Structure structure;

        public Clipboard(Vector offset, List<BlockRecord> blocks, Structure structure) {
            this.offset = offset;
            this.blocks = blocks;
            this.structure = structure;
        }

        public Vector getOffset() {
            return offset;
        }

        public List<BlockRecord> getBlocks() {
            return blocks;
        }

        public Structure getStructure() {
            return structure;
        }
    }

    private static final Map<UUID, Clipboard> clipboardMap = new HashMap<>();

    public static void setClipboard(UUID player, Clipboard clipboard) {
        clipboardMap.put(player, clipboard);
    }

    public static Clipboard getClipboard(UUID player) {
        return clipboardMap.get(player);
    }

    public static void clearAll() {
        clipboardMap.clear();
        pos1Map.clear();
        pos2Map.clear();
    }

    public static boolean saveClipboard(UUID player, World world, String filename, String extension) {
        Clipboard clipboard = clipboardMap.get(player);
        if (clipboard == null || clipboard.getStructure() == null) {
            return false;
        }

        try {
            File wpcMapsDir = new File(world.getWorldFolder(), "WPC MAPS");
            if (!wpcMapsDir.exists() && !wpcMapsDir.mkdirs()) {
                return false;
            }

            String fullFileName = filename;
            if (!fullFileName.endsWith("." + extension)) {
                fullFileName += "." + extension;
            }

            File targetFile = new File(wpcMapsDir, fullFileName);
            StructureManager structureManager = Bukkit.getStructureManager();
            structureManager.saveStructure(targetFile, clipboard.getStructure());
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean loadClipboard(UUID player, World world, String filename) {
        try {
            File wpcMapsDir = new File(world.getWorldFolder(), "WPC MAPS");
            if (!wpcMapsDir.exists()) {
                return false;
            }

            File[] files = wpcMapsDir.listFiles((dir, name) ->
                    name.equalsIgnoreCase(filename) ||
                    name.equalsIgnoreCase(filename + ".nbt") ||
                    name.equalsIgnoreCase(filename + ".schem"));

            if (files == null || files.length == 0) {
                return false;
            }

            File loadFile = files[0];
            StructureManager structureManager = Bukkit.getStructureManager();
            Structure structure = structureManager.loadStructure(loadFile);
            if (structure == null) {
                return false;
            }

            // Also populate block records for custom 3D rotation paste
            List<BlockRecord> records = new ArrayList<>();
            structure.getPalettes().forEach(palette -> {
                palette.getBlocks().forEach(blockState -> {
                    records.add(new BlockRecord(
                            blockState.getX(),
                            blockState.getY(),
                            blockState.getZ(),
                            blockState.getBlockData()
                    ));
                });
            });

            clipboardMap.put(player, new Clipboard(new Vector(0, 0, 0), records, structure));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
