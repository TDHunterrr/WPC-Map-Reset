package com.wpc.map.reset.paper;

import com.wpc.map.reset.paper.clipboard.PaperClipboardManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.structure.Structure;
import org.bukkit.structure.StructureManager;
import org.bukkit.util.Vector;

import java.io.File;
import java.util.*;

public class WPCMapResetPaper extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {

    private NamespacedKey wandKey;
    private boolean isFolia;

    @Override
    public void onEnable() {
        wandKey = new NamespacedKey(this, "wpc_wand");

        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            isFolia = true;
            getLogger().info("Running in Folia environment with region-threading support enabled!");
        } catch (ClassNotFoundException e) {
            isFolia = false;
            getLogger().info("Running in Paper/Spigot environment!");
        }

        getServer().getPluginManager().registerEvents(this, this);

        registerCommand("wpc");
        registerCommand("copy");
        registerCommand("paste");
        registerCommand("save");
        registerCommand("load");

        getLogger().info("WPC Map Reset plugin initialized successfully!");
    }

    @Override
    public void onDisable() {
        PaperClipboardManager.clearAll();
        getLogger().info("Cleared all clipboard data.");
    }

    private void registerCommand(String name) {
        PluginCommand cmd = getCommand(name);
        if (cmd != null) {
            cmd.setExecutor(this);
            cmd.setTabCompleter(this);
        }
    }

    public boolean isWand(ItemStack item) {
        if (item == null || item.getType() != Material.STICK) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }

        if (meta.getPersistentDataContainer().has(wandKey, PersistentDataType.BYTE)) {
            return true;
        }

        Component displayName = meta.displayName();
        if (displayName != null) {
            String plain = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(displayName);
            return plain.contains("WPC Wand");
        }
        return false;
    }

    public ItemStack createWand() {
        ItemStack wand = new ItemStack(Material.STICK);
        ItemMeta meta = wand.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("WPC Wand")
                    .color(NamedTextColor.DARK_RED)
                    .decorate(TextDecoration.BOLD, TextDecoration.UNDERLINED));
            meta.getPersistentDataContainer().set(wandKey, PersistentDataType.BYTE, (byte) 1);
            wand.setItemMeta(meta);
        }
        return wand;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        if (!isWand(item)) {
            return;
        }

        Player player = event.getPlayer();
        if (!player.hasPermission("wpc.use")) {
            return;
        }

        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            Block clicked = event.getClickedBlock();
            if (clicked != null) {
                event.setCancelled(true);
                PaperClipboardManager.pos1Map.put(player.getUniqueId(), clicked.getLocation());
                player.sendMessage(Component.text("§dPosition 1 set to " + clicked.getX() + ", " + clicked.getY() + ", " + clicked.getZ()));
            }
        } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getHand() == EquipmentSlot.HAND) {
            Block clicked = event.getClickedBlock();
            if (clicked != null) {
                event.setCancelled(true);
                PaperClipboardManager.pos2Map.put(player.getUniqueId(), clicked.getLocation());
                player.sendMessage(Component.text("§dPosition 2 set to " + clicked.getX() + ", " + clicked.getY() + ", " + clicked.getZ()));
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("§cOnly players can use WPC Map Reset commands."));
            return true;
        }

        if (!player.hasPermission("wpc.use")) {
            player.sendMessage(Component.text("§cYou do not have permission to use this command."));
            return true;
        }

        String cmdName = command.getName().toLowerCase();
        switch (cmdName) {
            case "wpc" -> {
                player.getInventory().addItem(createWand());
                player.sendMessage(Component.text("§aGiven WPC Wand!"));
                return true;
            }
            case "copy" -> {
                Location pos1 = PaperClipboardManager.pos1Map.get(player.getUniqueId());
                Location pos2 = PaperClipboardManager.pos2Map.get(player.getUniqueId());

                if (pos1 == null || pos2 == null || !Objects.equals(pos1.getWorld(), pos2.getWorld())) {
                    player.sendMessage(Component.text("§cYou must select two positions first using the wand."));
                    return true;
                }

                int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
                int minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
                int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());

                int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
                int maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
                int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());

                World world = pos1.getWorld();
                Location minLoc = new Location(world, minX, minY, minZ);
                Location maxLoc = new Location(world, maxX, maxY, maxZ);

                StructureManager sm = Bukkit.getStructureManager();
                Structure structure = sm.createStructure();
                structure.fill(minLoc, maxLoc, true);

                Location playerLoc = player.getLocation();
                Vector offset = new Vector(minX - playerLoc.getBlockX(), minY - playerLoc.getBlockY(), minZ - playerLoc.getBlockZ());

                List<PaperClipboardManager.BlockRecord> records = new ArrayList<>();
                for (int x = minX; x <= maxX; x++) {
                    for (int y = minY; y <= maxY; y++) {
                        for (int z = minZ; z <= maxZ; z++) {
                            Block block = world.getBlockAt(x, y, z);
                            records.add(new PaperClipboardManager.BlockRecord(
                                    x - minX,
                                    y - minY,
                                    z - minZ,
                                    block.getBlockData().clone()
                            ));
                        }
                    }
                }

                PaperClipboardManager.setClipboard(player.getUniqueId(), new PaperClipboardManager.Clipboard(offset, records, structure));
                int sizeX = maxX - minX + 1;
                int sizeY = maxY - minY + 1;
                int sizeZ = maxZ - minZ + 1;
                player.sendMessage(Component.text("§aCopied area of size " + sizeX + ", " + sizeY + ", " + sizeZ));
                return true;
            }
            case "paste" -> {
                PaperClipboardManager.Clipboard clipboard = PaperClipboardManager.getClipboard(player.getUniqueId());
                if (clipboard == null) {
                    player.sendMessage(Component.text("§cClipboard is empty."));
                    return true;
                }

                Location pasteBase;
                float rotX = 0, rotY = 0, rotZ = 0;

                if (args.length >= 3) {
                    try {
                        double px = parseCoord(args[0], player.getLocation().getX());
                        double py = parseCoord(args[1], player.getLocation().getY());
                        double pz = parseCoord(args[2], player.getLocation().getZ());
                        pasteBase = new Location(player.getWorld(), px, py, pz);

                        if (args.length >= 6) {
                            rotX = Float.parseFloat(args[3]);
                            rotY = Float.parseFloat(args[4]);
                            rotZ = Float.parseFloat(args[5]);
                        }
                    } catch (Exception ex) {
                        player.sendMessage(Component.text("§cInvalid paste arguments. Usage: /paste [<x> <y> <z> <rotX> <rotY> <rotZ>]"));
                        return true;
                    }
                } else {
                    pasteBase = player.getLocation().clone();
                }

                Location pasteTarget = pasteBase.clone().add(clipboard.getOffset());
                pasteBlocks(player, clipboard, pasteTarget, rotX, rotY, rotZ);
                return true;
            }
            case "save" -> {
                if (args.length < 2) {
                    player.sendMessage(Component.text("§cUsage: /save <filename> <extension>"));
                    return true;
                }
                String filename = args[0];
                String ext = args[1];
                boolean ok = PaperClipboardManager.saveClipboard(player.getUniqueId(), player.getWorld(), filename, ext);
                if (ok) {
                    player.sendMessage(Component.text("§aSaved clipboard to " + filename + "." + ext));
                } else {
                    player.sendMessage(Component.text("§cFailed to save clipboard. Is it empty?"));
                }
                return true;
            }
            case "load" -> {
                if (args.length < 1) {
                    player.sendMessage(Component.text("§cUsage: /load <filename>"));
                    return true;
                }
                String filename = args[0];
                boolean ok = PaperClipboardManager.loadClipboard(player.getUniqueId(), player.getWorld(), filename);
                if (ok) {
                    player.sendMessage(Component.text("§aLoaded structure " + filename + " to clipboard. Use /paste to place it."));
                } else {
                    player.sendMessage(Component.text("§cFailed to load structure. File not found."));
                }
                return true;
            }
        }
        return false;
    }

    private double parseCoord(String arg, double current) {
        if (arg.startsWith("~")) {
            if (arg.length() == 1) return current;
            return current + Double.parseDouble(arg.substring(1));
        }
        return Double.parseDouble(arg);
    }

    private void pasteBlocks(Player player, PaperClipboardManager.Clipboard clipboard, Location targetPos, float rotX, float rotY, float rotZ) {
        World world = targetPos.getWorld();
        if (world == null) return;

        double radX = Math.toRadians(rotX);
        double radY = Math.toRadians(rotY);
        double radZ = Math.toRadians(rotZ);

        Map<Long, List<Runnable>> chunkTasks = new HashMap<>();

        for (PaperClipboardManager.BlockRecord record : clipboard.getBlocks()) {
            int x = record.x();
            int y = record.y();
            int z = record.z();

            // Full 3D rotation math identical to common/ClipboardCommands
            double y1 = y * Math.cos(radX) - z * Math.sin(radX);
            double z1 = y * Math.sin(radX) + z * Math.cos(radX);

            double x2 = x * Math.cos(radY) + z1 * Math.sin(radY);
            double z2 = -x * Math.sin(radY) + z1 * Math.cos(radY);

            double x3 = x2 * Math.cos(radZ) - y1 * Math.sin(radZ);
            double y3 = x2 * Math.sin(radZ) + y1 * Math.cos(radZ);

            int finalX = targetPos.getBlockX() + (int) Math.round(x3);
            int finalY = targetPos.getBlockY() + (int) Math.round(y3);
            int finalZ = targetPos.getBlockZ() + (int) Math.round(z2);

            int chunkX = finalX >> 4;
            int chunkZ = finalZ >> 4;
            long chunkKey = (((long) chunkX) << 32) | (chunkZ & 0xFFFFFFFFL);

            Runnable task = () -> {
                Block b = world.getBlockAt(finalX, finalY, finalZ);
                b.setBlockData(record.data(), false);
            };

            chunkTasks.computeIfAbsent(chunkKey, k -> new ArrayList<>()).add(task);
        }

        // Execute placement - supports Folia regionized threading and standard Paper
        if (isFolia) {
            chunkTasks.forEach((chunkKey, tasks) -> {
                int cx = (int) (chunkKey >> 32);
                int cz = (int) (chunkKey.longValue());
                Bukkit.getRegionScheduler().execute(this, world, cx, cz, () -> {
                    for (Runnable r : tasks) {
                        r.run();
                    }
                });
            });
        } else {
            chunkTasks.values().forEach(tasks -> tasks.forEach(Runnable::run));
        }

        player.sendMessage(Component.text("§aPasted clipboard at " + targetPos.getBlockX() + ", " + targetPos.getBlockY() + ", " + targetPos.getBlockZ() +
                " with rotation (" + rotX + ", " + rotY + ", " + rotZ + ")"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String cmd = command.getName().toLowerCase();
        if (cmd.equals("save") && args.length == 2) {
            return List.of("nbt", "schem");
        }
        if (cmd.equals("load") && args.length == 1 && sender instanceof Player player) {
            File dir = new File(player.getWorld().getWorldFolder(), "WPC MAPS");
            if (dir.exists()) {
                String[] files = dir.list();
                if (files != null) {
                    return Arrays.stream(files)
                            .filter(f -> f.toLowerCase().startsWith(args[0].toLowerCase()))
                            .toList();
                }
            }
        }
        return Collections.emptyList();
    }
}
