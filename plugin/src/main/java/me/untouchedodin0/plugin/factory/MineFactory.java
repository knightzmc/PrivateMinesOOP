/*
MIT License

Copyright (c) 2021 Kyle Hicks

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

package me.untouchedodin0.plugin.factory;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.block.BlockTypes;
import me.untouchedodin0.plugin.PrivateMines;
import me.untouchedodin0.plugin.mines.Mine;
import me.untouchedodin0.plugin.mines.MineType;
import me.untouchedodin0.plugin.mines.WorldEditMine;
import me.untouchedodin0.plugin.mines.WorldEditMineType;
import me.untouchedodin0.plugin.storage.MineStorage;
import me.untouchedodin0.plugin.util.Utils;
import me.untouchedodin0.plugin.util.worldedit.MineFactoryCompat;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.codemc.worldguardwrapper.WorldGuardWrapper;
import redempt.redlib.blockdata.BlockDataManager;
import redempt.redlib.blockdata.DataBlock;
import redempt.redlib.misc.LocationUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MineFactory<S> {

    private final boolean debugMode;
    PrivateMines privateMines;
    Utils utils;
    MineStorage mineStorage;
    MineFactory mineFactory;
    MineType defaultMineType;
    BlockDataManager blockDataManager;
    MineFactoryCompat mineFactoryCompat;
    private EditSession editSession;
    Location spawnLocation;
    List<Location> corners = new ArrayList<>();

    public MineFactory(PrivateMines privateMines, BlockDataManager blockDataManager) {
        this.privateMines = privateMines;
        this.utils = privateMines.getUtils();
        this.mineStorage = privateMines.getMineStorage();
        this.mineFactory = privateMines.getMineFactory();
        this.defaultMineType = privateMines.getDefaultMineType();
        this.blockDataManager = blockDataManager;
        this.debugMode = privateMines.isDebugMode();
    }


    public Mine createMine(Player player, Location location) {
        if (defaultMineType == null) {
            privateMines.getLogger().warning("Failed to create mine due to defaultMineData being null");
        }

        final WorldGuardWrapper worldGuardWrapper = WorldGuardWrapper.getInstance();

        Block block = location.getBlock();
        String userUUID = player.getUniqueId().toString();
        Mine mine = new Mine(privateMines);
        mine.setMineOwner(player.getUniqueId());
        mine.setMineLocation(location);
        mine.setMineType(defaultMineType);
        mine.setWeightedRandom(defaultMineType.getWeightedRandom());
        mine.build();

        mineStorage.addMine(player.getUniqueId(), mine);

        DataBlock dataBlock = getDataBlock(block, player, location, mine);

        String regionName = String.format("mine-%s", userUUID);

//        IWrappedRegion mineRegion = WorldGuardWrapper.getInstance()
//                .addCuboidRegion(regionName, mine.getCorner1(), mine.getCorner2())
//                .orElseThrow(() -> new RuntimeException("Could not create the mine WorldGuard region!"));
//        mineRegion.getOwners().addPlayer(player.getUniqueId());

        MineType mineType = mine.getMineType();

        List<String> allowFlags = mineType.getAllowFlags();
        List<String> denyFlags = mineType.getDenyFlags();

//        Stream.of(worldGuardWrapper.getFlag("block-place", WrappedState.class),
//                        worldGuardWrapper.getFlag("mob-spawning", WrappedState.class)
//                ).filter(Optional::isPresent)
//                .map(Optional::get)
//                .forEach(flag -> mineRegion.setFlag(flag, WrappedState.DENY));
//
//        Stream.of(worldGuardWrapper.getFlag("block-break", WrappedState.class)
//                ).filter(Optional::isPresent)
//                .map(Optional::get)
//                .forEach(wrappedStateIWrappedFlag -> mineRegion.setFlag(wrappedStateIWrappedFlag, WrappedState.ALLOW));

        blockDataManager.save();
        mine.reset();
        mine.startAutoResetTask();
        return mine;
    }

    private DataBlock getDataBlock(Block block, Player player, Location location, Mine mine) {
        DataBlock dataBlock = blockDataManager.getDataBlock(block);
        dataBlock.set("owner", String.valueOf(player.getUniqueId()));
        dataBlock.set("type", defaultMineType.getName());
        dataBlock.set("location", LocationUtils.toString(location));
        dataBlock.set("spawnLocation", LocationUtils.toString(mine.getSpawnLocation()));
        dataBlock.set("npcLocation", LocationUtils.toString(mine.getNpcLocation()));
        dataBlock.set("corner1", LocationUtils.toString(mine.getCorner1()));
        dataBlock.set("corner2", LocationUtils.toString(mine.getCorner2()));
        dataBlock.set("structure", mine.getStructure());
        return dataBlock;
    }

    /**
     * @param player   - The target player to be given a mine
     * @param location - The spigot world location where to create the mine
     * @param mineType - The mine data such as the MultiBlockStructure and the Materials
     */

    public Mine createMine(Player player, Location location, MineType mineType) {
        if (mineType == null) {
            privateMines.getLogger().warning("Failed to create mine due to the minetype being null");
        } else {
            String userUUID = player.getUniqueId().toString();

            Utils utils = privateMines.getUtils();
            Mine mine = new Mine(privateMines);
            mine.setMineOwner(player.getUniqueId());
            mine.setMineLocation(location);
            mine.setMineType(mineType);
            mine.setWeightedRandom(mineType.getWeightedRandom());
            mine.build();

            Location corner1 = utils.getRelative(mine.getStructure(), mineType.getCorner1());
            Location corner2 = utils.getRelative(mine.getStructure(), mineType.getCorner2());

            Location spawnLocation = utils.getRelative(mine.getStructure(), mineType.getSpawnLocation());
            Location npcLocation = utils.getRelative(mine.getStructure(), mineType.getNpcLocation());

            mine.setCorner1(corner1);
            mine.setCorner2(corner2);
            mine.setSpawnLocation(spawnLocation);
            mine.setNpcLocation(npcLocation);

            mineStorage.addMine(player.getUniqueId(), mine);
            Block block = location.getBlock();
            DataBlock dataBlock = getDataBlock(block, player, location, mine);
            blockDataManager.save();
            mine.reset();
            mine.startAutoResetTask();

            if (debugMode) {
                privateMines.getLogger().info("createMine block: " + block);
                privateMines.getLogger().info("createMine dataBlock: " + dataBlock);
                privateMines.getLogger().info("createMine dataBlock getData: " + dataBlock.getData());
            }
            return mine;
        }
        return null;
    }

    public WorldEditMine createMine(Player player, Location location, WorldEditMineType worldEditMineType) {
        Clipboard clipboard;
        Utils utils = new Utils(privateMines);
        World world;
        final var fillType = BlockTypes.DIAMOND_BLOCK;

        if (worldEditMineType == null) {
            privateMines.getLogger().warning("Failed to create mine due to the worldedit mine type being null");
        } else {
            if (fillType == null) {
                privateMines.getLogger().warning("Failed to fill mine due to fillType being null");
            } else {

                File file = worldEditMineType.getSchematicFile();
                PasteFactory pasteFactory = new PasteFactory(privateMines);

                privateMines.getLogger().info("createMine file: " + file);

                ClipboardFormat clipboardFormat = ClipboardFormats.findByFile(file);
                privateMines.getLogger().info("createMine clipboardFormat: " + clipboardFormat);

                if (clipboardFormat != null) {
                    try (ClipboardReader clipboardReader = clipboardFormat.getReader(new FileInputStream(file))) {
                        clipboard = clipboardReader.read();
                        if (clipboard == null) {
                            privateMines.getLogger().warning("Clipboard was null");
                            return null;
                        }
                        world = location.getWorld();

                        this.editSession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(world));

                        privateMines.getLogger().info("location: " + location);
                        privateMines.getLogger().info("clipboard: " + clipboard);
                        privateMines.getLogger().info("pasteFactory: " + pasteFactory);

                        Region region = pasteFactory.paste(clipboard, location);
                        region.iterator().forEachRemaining(blockVector3 -> {
                            if (world != null) {
                                Location bukkitLocation = utils.blockVector3toBukkit(world, blockVector3);
                                Material bukkitMaterial = bukkitLocation.getBlock().getType();

                                if (bukkitMaterial == Material.CHEST) {
                                    privateMines.getLogger().info("Found chest at " + bukkitLocation);
                                    this.spawnLocation = utils.blockVector3toBukkit(world, blockVector3);
                                } else if (bukkitMaterial == Material.POWERED_RAIL) {
                                    privateMines.getLogger().info("Found powered rail at " + bukkitLocation);
                                    corners.add(bukkitLocation);
                                }
                            }
                        });

                        Location corner1 = corners.get(0);
                        Location corner2 = corners.get(1);

                        BlockVector3 blockVectorCorner1 = BlockVector3.at(
                                corner1.getBlockX(),
                                corner1.getBlockY(),
                                corner1.getBlockZ());

                        BlockVector3 blockVectorCorner2 = BlockVector3.at(
                                corner2.getBlockX(),
                                corner2.getBlockY(),
                                corner2.getBlockZ());

                        CuboidRegion cuboidRegion = new CuboidRegion(blockVectorCorner1, blockVectorCorner2);

                        privateMines.getLogger().info("region: " + region);
                        privateMines.getLogger().info("spawn location: " + spawnLocation);
                        privateMines.getLogger().info("corners: " + corners);
                        privateMines.getLogger().info("blockVectorCorner1: " + blockVectorCorner1);
                        privateMines.getLogger().info("blockVectorCorner2: " + blockVectorCorner2);
                        privateMines.getLogger().info("cuboidRegion: " + cuboidRegion);

                        try (final var session = WorldEdit.getInstance()
                                .newEditSession(BukkitAdapter.adapt(world))) {
                            session.setBlocks(cuboidRegion, fillType.getDefaultState());
                        }
                    } catch (IOException | MaxChangedBlocksException e) {
                        e.printStackTrace();
                    }
                    player.sendMessage("Your mine has been created");
                }

//            WorldEditMine worldEditMine = new WorldEditMine(privateMines);
//            worldEditMine.setWorldEditMineType(worldEditMineType);
//            worldEditMine.paste(location);
//            worldEditMine.teleport(player);
            }
        }
        return null;
    }
}


