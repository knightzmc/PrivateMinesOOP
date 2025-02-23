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

package me.untouchedodin0.privatemines.we_6.worldedit;

import com.sk89q.worldedit.CuboidClipboard;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitUtil;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.DataException;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.schematic.SchematicFormat;
import com.sk89q.worldedit.session.SessionManager;
import me.untouchedodin0.privatemines.compat.WorldEditUtilities;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.BlockVector;
import redempt.redlib.region.CuboidRegion;

import java.io.File;
import java.io.IOException;

@SuppressWarnings("unused")
public class WorldEditUtils extends WorldEditUtilities {

    private final SessionManager sessionManager = WorldEdit.getInstance().getSessionManager();

//    public CuboidRegion getRegion(Player player) {
//        LocalSession localSession = sessionManager.get((SessionOwner) player);
//
//        com.sk89q.worldedit.world.World world = localSession.getSelectionWorld();
//        World bukkitWorld = player.getWorld();
//
//        try {
//            if (world == null) {
//                throw new IllegalStateException("Invalid world!");
//            }
//
//            BlockVector minBV = localSession.getSelection(world).getMinimumPoint();
//            BlockVector maxBV = localSession.getSelection(world).getMaximumPoint().toBlockPoint();
//
//            Location minLocation = blockVectorToBukkit(bukkitWorld, toBlockVector(minBV));
//            Location maxLocation = blockVectorToBukkit(bukkitWorld, toBlockVector(maxBV));
//
//            return new CuboidRegion(minLocation, maxLocation);
//        } catch (IncompleteRegionException incompleteRegionException) {
//            incompleteRegionException.printStackTrace();
//        }
//        return null;
//    }

    public org.bukkit.util.BlockVector toBlockVector(BlockVector vector3) {
        return new org.bukkit.util.BlockVector(vector3.getX(), vector3.getY(), vector3.getZ());
    }

    private WorldEditPlugin getWorldEdit() {
        Plugin plugin = Bukkit.getServer().getPluginManager().getPlugin("WorldEdit");
        return (WorldEditPlugin) plugin;
    }

    @Override
    public CuboidRegion getCuboidRegion(Player player) {
        return null;
    }


//    @Override
//    public void createMultiBlockStructure(Player player, String name) {
//        Selection selection = getWorldEdit().getSelection(player);
//        Location minimum = selection.getMinimumPoint();
//        Location maximum = selection.getMaximumPoint();
//        if (minimum == null || maximum == null) {
//            player.sendMessage("Failed to make a selection!");
//        } else {
//            String multiBlockStructure = MultiBlockStructure.stringify(minimum, maximum);
//            try {
//                Path path = Paths.get("plugins/PrivateMines/" + name + ".dat");
//                player.sendMessage("Attempting to write the file " + path.getFileName() + "...");
//                Files.write(
//                        path,
//                        multiBlockStructure.getBytes(),
//                        StandardOpenOption.CREATE,
//                        StandardOpenOption.TRUNCATE_EXISTING);
//            } catch (IOException ioException) {
//                ioException.printStackTrace();
//            }
//        }
//    }

    @Override
    public void setBlocks(CuboidRegion cuboidRegion, String blockType) {

    }

    @Override
    public void setBlock(Location location, String blockType) {

    }

    // we 6 paste schem

    @Override
    public Region pasteSchematic(Location location, File file) {
        World bukkitWorld = location.getWorld();
        EditSession editSession = new EditSession(new BukkitWorld(bukkitWorld), -1);
        editSession.enableQueue();
        CuboidClipboard cuboidClipboard;

        SchematicFormat schematicFormat = SchematicFormat.getFormat(file);
        try {
            cuboidClipboard = schematicFormat.load(file);
            cuboidClipboard.paste(editSession, BukkitUtil.toVector(location), true);
            editSession.flushQueue();
            Bukkit.broadcastMessage("pasted at " + location);
            return (Region) cuboidClipboard;
        } catch (IOException | DataException | MaxChangedBlocksException e) {
            e.printStackTrace();
        }
        return null;
    }
}
