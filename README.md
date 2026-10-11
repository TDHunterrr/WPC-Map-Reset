# WPC Map Reset

A powerful WorldEdit-style clipboard mod and server plugin for Minecraft. Easily select, copy, paste, save, and load structures using an in-game selection wand with full 3D rotation support.

![License](https://img.shields.io/badge/License-ARR-blue.svg)
![Minecraft](https://img.shields.io/badge/Minecraft-1.21.x%20--%2026.x-green.svg)
![Loaders](https://img.shields.io/badge/Loaders-Fabric%20%7C%20NeoForge%20%7C%20Paper%20%7C%20Folia-orange.svg)

---

## 🌟 Supported Platforms
* **Fabric** (1.21+)
* **NeoForge** (1.21+)
* **Paper / Purpur / Spigot / Bukkit** (1.21+)
* **Folia** (Full region-threading support)

---

## 🎮 How to Use

### 1. The Wand (`/wpc`)
Run `/wpc` to receive the **WPC Wand**.
* **Left-Click** a block with the wand: Sets **Position 1** (`pos1`).
* **Right-Click** a block with the wand: Sets **Position 2** (`pos2`).

### 2. Commands
| Command | Description |
| :--- | :--- |
| `/wpc` | Gives you the WPC Wand. |
| `/copy` | Copies the region between Pos 1 and Pos 2 into your clipboard. |
| `/paste` | Pastes the clipboard at your current position with player offset. |
| `/paste <x> <y> <z> <rotX> <rotY> <rotZ>` | Pastes the clipboard at coordinates with custom 3D rotation angles. |
| `/save <filename> <extension>` | Saves the clipboard structure to `<world>/WPC MAPS/<filename>.<extension>` (e.g. `/save castle nbt`). |
| `/load <filename>` | Loads a saved structure from `<world>/WPC MAPS/` back into your clipboard. |

---

## ⚙️ Building from Source

Requires **Java 21**.

```bash
# Build the universal jar (bundles Fabric, NeoForge, and Paper)
./gradlew :universal:build

# Or build individual platforms:
./gradlew :fabric:build
./gradlew :neoforge:build
./gradlew :paper:build
```

Compiled `.jar` files will be generated inside each subproject's `build/libs/` folder.

---

## 📜 License
All Rights Reserved (ARR). Developed by **TD_Hunterrr**.
