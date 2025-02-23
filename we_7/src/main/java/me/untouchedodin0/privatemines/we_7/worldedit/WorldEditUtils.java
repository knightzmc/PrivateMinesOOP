/*
MIT License

Copyright (c) 2021 - 2022 Kyle Hicks

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
 */

package me.untouchedodin0.privatemines.we_7.worldedit;

import com.sk89q.worldedit.*;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.session.SessionManager;
import com.sk89q.worldedit.world.block.BlockType;
import me.untouchedodin0.privatemines.compat.WorldEditUtilities;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import redempt.redlib.region.CuboidRegion;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Objects;

@SuppressWarnings("unused")
public class WorldEditUtils extends WorldEditUtilities {

    private final SessionManager sessionManager = WorldEdit.getInstance().getSessionManager();
    private EditSession editSession;

    public Location blockVector3toBukkit(World world, BlockVector3 blockVector3) {
        Block block = world.getBlockAt(blockVector3.getBlockX(), blockVector3.getBlockY(), blockVector3.getBlockZ());
        return block.getLocation();
    }

    public CuboidRegion getCuboidRegion(Player player) {
        LocalSession localSession = sessionManager.get(BukkitAdapter.adapt(player));

        com.sk89q.worldedit.world.World world = localSession.getSelectionWorld();

        try {
            if (world == null) {
                throw new IllegalStateException("Invalid world!");
            }

            BlockVector3 minBV3 = localSession.getSelection(world).getMinimumPoint();
            BlockVector3 maxBV3 = localSession.getSelection(world).getMaximumPoint();
            Location minLocation = blockVector3toBukkit(BukkitAdapter.adapt(world), minBV3);
            Location maxLocation = blockVector3toBukkit(BukkitAdapter.adapt(world), maxBV3);
            return new CuboidRegion(minLocation, maxLocation);
        } catch (IncompleteRegionException incompleteRegionException) {
            incompleteRegionException.printStackTrace();
        }
        return null;
    }

    private WorldEditPlugin getWorldEdit() {
        Plugin plugin = Bukkit.getServer().getPluginManager().getPlugin("WorldEdit");
        return (WorldEditPlugin) plugin;
    }

    @Override
    public void setBlocks(CuboidRegion cuboidRegion, String material) {
        Location start = cuboidRegion.getStart();
        Location end = cuboidRegion.getEnd();
        World world = start.getWorld();

        if (start.getWorld() != null) {
            int startX = start.getBlockX();
            int startY = start.getBlockY();
            int startZ = start.getBlockZ();

            int endX = end.getBlockX();
            int endY = end.getBlockY();
            int endZ = end.getBlockZ();

            BlockVector3 startVector3 = BlockVector3.at(startX, startY, startZ);
            BlockVector3 endVector3 = BlockVector3.at(endX, endY, endZ);

            BlockType blockType = BlockType.REGISTRY.get(material.toLowerCase());
            Region cube = new com.sk89q.worldedit.regions.CuboidRegion(startVector3, endVector3);
        }
    }

    @Override
    public void setBlock(Location location, String material) {
        World world = location.getWorld();
        String worldName;

        if (location.getWorld() != null) {
            worldName = location.getWorld().getName();
            BlockType blockType = BlockType.REGISTRY.get(material.toLowerCase());
            int x = location.getBlockX();
            int y = location.getBlockY();
            int z = location.getBlockZ();
        }
    }

    @Override
    public Region pasteSchematic(Location location, File file) {
        // fill this one
        ClipboardFormat clipboardFormat = ClipboardFormats.findByFile(file);
        Clipboard clipboard;
        BlockVector3 centerVector;
        Operation operation;
        Region region;
        com.sk89q.worldedit.world.World world = BukkitAdapter.adapt(Objects.requireNonNull(location.getWorld()));

        if (clipboardFormat != null) {
            try (ClipboardReader clipboardReader = clipboardFormat.getReader(new FileInputStream(file))) {
                clipboard = clipboardReader.read();
                if (clipboard == null) {
                    Bukkit.getLogger().info("Clipboard was null");
                    return null;
                }

                try (EditSession editSession = WorldEdit.getInstance().newEditSession(world)) {
                    centerVector = BlockVector3.at(location.getX(), location.getY(), location.getZ());

                        // If the clipboard isn't null prepare to create a paste operation, complete it and set the region stuff.
                        operation = new ClipboardHolder(clipboard)
                                .createPaste(editSession)
                                .to(centerVector)
                                .ignoreAirBlocks(true)
                                .build();
                        try {
                            Operations.complete(operation);
                            region = clipboard.getRegion();

                            if (centerVector != null) {
                                region.shift(centerVector.subtract(clipboard.getOrigin()));
                                return region;
                            }
                        } catch (WorldEditException worldEditException) {
                            worldEditException.printStackTrace();
                        }
                    }
                return null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public Region pasteSchematic(Location location, Clipboard clipboard) {

        com.sk89q.worldedit.world.World world = BukkitAdapter.adapt(Objects.requireNonNull(location.getWorld()));
        Region region;
        BlockVector3 centerVector;
        Operation operation;

        // we 7 paste schem
        try (EditSession editSession = WorldEdit.getInstance().newEditSession(world)) {
            centerVector = BlockVector3.at(location.getX(), location.getY(), location.getZ());

            // If the clipboard isn't null prepare to create a paste operation, complete it and set the region stuff.
            if (clipboard != null) {
                operation = new ClipboardHolder(clipboard)
                        .createPaste(editSession)
                        .to(centerVector)
                        .ignoreAirBlocks(true)
                        .build();
                try {
                    Operations.complete(operation);
                    region = clipboard.getRegion();

                    if (centerVector != null) {
                        region.shift(centerVector.subtract(clipboard.getOrigin()));
                        return region;
                    }
                } catch (WorldEditException worldEditException) {
                    worldEditException.printStackTrace();
                }
            }
        }
        return null;
    }
}
