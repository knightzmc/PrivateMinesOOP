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

package me.untouchedodin0.plugin;

import com.cryptomorin.xseries.XMaterial;
import com.google.gson.Gson;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import de.jeff_media.updatechecker.UpdateChecker;
import me.untouchedodin0.plugin.commands.PrivateMinesCommand;
import me.untouchedodin0.plugin.config.MenuConfig;
import me.untouchedodin0.plugin.config.MineConfig;
import me.untouchedodin0.plugin.factory.MineFactory;
import me.untouchedodin0.plugin.mines.MineType;
import me.untouchedodin0.plugin.mines.WorldEditMine;
import me.untouchedodin0.plugin.mines.WorldEditMineType;
import me.untouchedodin0.plugin.mines.data.WorldEditMineData;
import me.untouchedodin0.plugin.storage.MineStorage;
import me.untouchedodin0.plugin.util.Metrics;
import me.untouchedodin0.plugin.util.Utils;
import me.untouchedodin0.plugin.util.placeholderapi.PrivateMinesExpansion;
import me.untouchedodin0.plugin.world.MineWorldManager;
import me.untouchedodin0.privatemines.compat.WorldEditUtilities;
import me.untouchedodin0.privatemines.we_6.worldedit.MineFactory6;
import me.untouchedodin0.privatemines.we_6.worldedit.WorldEdit6MineType;
import me.untouchedodin0.privatemines.we_6.worldedit.WorldEditMine6;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.codemc.worldguardwrapper.WorldGuardWrapper;
import org.codemc.worldguardwrapper.region.IWrappedRegion;
import redempt.redlib.RedLib;
import redempt.redlib.blockdata.BlockDataManager;
import redempt.redlib.commandmanager.ArgType;
import redempt.redlib.commandmanager.CommandParser;
import redempt.redlib.commandmanager.Messages;
import redempt.redlib.configmanager.ConfigManager;
import redempt.redlib.configmanager.annotations.ConfigValue;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

public class PrivateMines extends JavaPlugin {

    private static PrivateMines privateMines;
    private static PrivateMinesAPI privateMinesAPI;

    private final Map<String, MineType> mineDataMap = new HashMap<>();
    private final TreeMap<String, MineType> mineTypeTreeMap = new TreeMap<>();
    private final TreeMap<String, WorldEdit6MineType> worldEdit6MineTypeTreeMap = new TreeMap<>();
    private final TreeMap<String, WorldEditMineType> worldEditMineTypeTreeMap = new TreeMap<>();

    private MineFactory6 mineFactory6;
    private MineFactory mineFactory;
    private MineWorldManager mineWorldManager;
    private MineStorage mineStorage;
    private BlockDataManager blockDataManager;
    private Utils utils;
    private WorldEditUtilities worldEditUtils;
    private ConfigManager configManager;
    private boolean isWorldEditEnabled = false;
    private boolean useWorldEdit6 = false;
    private final File minesDirectory = new File("plugins/PrivateMines/mines");
    private final File schematicsDirectory = new File("plugins/PrivateMines/schematics");
    private final File addonsDirectory = new File("plugins/PrivateMines/addons");

    private final Pattern filePattern = Pattern.compile("(.*?)\\.(json)");
    private final Pattern jarPattern = Pattern.compile("(.*?)\\.(jar)");


    private Gson gson;
    private Material material;
    IWrappedRegion globalRegion;

    private Map<Material, Double> materials = new HashMap<>();

    @ConfigValue
    private Material spawnPoint;

    @ConfigValue
    private Material mineCorner;

    @ConfigValue
    private Material sellNpc;

    @ConfigValue
    private Material upgradeMaterial;

    @ConfigValue
    private String mainMenuTitle;

    @ConfigValue
    private boolean debugMode = false;

    @ConfigValue
    private boolean useWorldEdit = false;

    @ConfigValue
    private boolean notifyForUpdates = false;

    @ConfigValue
    private int mineDistance = 150;

    @ConfigValue
    private Map<String, MineConfig> mineTypes = ConfigManager.map(MineConfig.class);

    @ConfigValue
    private Map<String, MenuConfig> inventory = ConfigManager.map(MenuConfig.class);

    public static PrivateMines getPrivateMines() {
        return privateMines;
    }

    public static PrivateMinesAPI getAPI() {
        return privateMinesAPI;
    }

    /*
        Disables the plugin, clears the map and saves the block data manager
     */

    @Override
    public void onEnable() {
        privateMines = this;
        privateMinesAPI = new PrivateMinesAPI(privateMines);
        gson = new Gson();
        int MID_VERSION = RedLib.MID_VERSION;
        int pluginId = 11413;
        int spigotPluginId = 90890;

        File configFile = new File(getDataFolder(), "config.yml");

        if (!configFile.exists()) {
            saveDefaultConfig();
        }

        if (!minesDirectory.exists()) {
            boolean created = minesDirectory.mkdir();
            if (created) {
                getLogger().info("Created the mines directory successfully!");
            }
        }

        if (!schematicsDirectory.exists()) {
            boolean created = schematicsDirectory.mkdir();
            if (created) {
                getLogger().info("Created the schematics directory successfully!");
            }
        }

        if (!addonsDirectory.exists()) {
            boolean created = addonsDirectory.mkdir();
            if (created) {
                getLogger().info("Created addons directory successfully!");
            }
        }

        // something in the world edit mine is breaking it...

//        configManager = new ConfigManager(this).register(this, WorldEditMine.class).load();
        blockDataManager = new BlockDataManager(
                getDataFolder()
                        .toPath()
                        .resolve("mines.db"));

        mineStorage = new MineStorage();
//        mineFactory = new MineFactory(this, blockDataManager);
        mineWorldManager = new MineWorldManager(this);
        utils = new Utils(this);


        if (MID_VERSION <= 12) {
            useWorldEdit6 = true;
            WorldEditMine6 worldEditMine6 = new WorldEditMine6();
            mineFactory6 = new MineFactory6();
            getLogger().info("world edit mine 6: " + worldEditMine6);
            configManager = new ConfigManager(this).register(this, WorldEditMine6.class).load();
            getLogger().info("spawnMaterial: " + spawnPoint);
            getLogger().info("mineCorner: " + mineCorner);
            getLogger().info("sellNpc: " + sellNpc);
            getLogger().info("upgradeMaterial: " + upgradeMaterial);
        } else  {
            useWorldEdit6 = false;
            mineFactory = new MineFactory(this, blockDataManager);
            configManager = new ConfigManager(this).register(this, WorldEditMine.class).load();
        }

        String pluginFolder = getDataFolder().getPath();
        File folder = new File(pluginFolder);
        File[] files = folder.listFiles();
        utils.moveSchematicFiles(files);

        Plugin worldEditPlugin = Bukkit.getServer().getPluginManager().getPlugin("WorldEdit");

        if (worldEditPlugin != null) {
            worldEditUtils = WorldEditUtilities.getInstance();
            getLogger().info("Loading worldedit v" + WorldEditPlugin.getPlugin(WorldEditPlugin.class)
                    .getDescription().getVersion());
                isWorldEditEnabled = true;

            files = minesDirectory.listFiles();
            if (files != null) {
                Arrays.stream(files).forEach(file -> {
                    BufferedReader bufferedReader;
                    if (file.getName().matches(String.valueOf(filePattern))) {
                        try {
                            bufferedReader = Files.newBufferedReader(file.toPath());
                            WorldEditMine worldEditMine = new WorldEditMine(this);

                            WorldEditMineData worldEditMineData = gson.fromJson(bufferedReader, WorldEditMineData.class);
                            if (XMaterial.matchXMaterial(worldEditMineData.getMaterial()).isPresent()) {
                                material = XMaterial.matchXMaterial(worldEditMineData.getMaterial()).get().parseMaterial();
                            }

                            int minX = worldEditMineData.getMinX();
                            int minY = worldEditMineData.getMinY();
                            int minZ = worldEditMineData.getMinZ();
                            int maxX = worldEditMineData.getMaxX();
                            int maxY = worldEditMineData.getMaxY();
                            int maxZ = worldEditMineData.getMaxZ();

                            Location spawn = new Location(Bukkit.getWorld(worldEditMineData.getWorldName()),
                                                          worldEditMineData.getSpawnX(), worldEditMineData.getSpawnY(), worldEditMineData.getSpawnZ());

                            BlockVector3 min = BlockVector3.at(minX, minY, minZ);
                            BlockVector3 max = BlockVector3.at(maxX, maxY, maxZ);

                            CuboidRegion cuboidRegion = new CuboidRegion(min, max);

                            materials.put(Material.STONE, 50.0);
                            materials.put(Material.EMERALD_BLOCK, 50.0);

                            worldEditMine.setSpawnLocation(spawn);
                            worldEditMine.setCuboidRegion(cuboidRegion);
                            worldEditMine.setWorldEditMineData(worldEditMineData);
                            worldEditMine.setMaterials(materials);
                            worldEditMine.setWorldEditMineType(worldEditMineTypeTreeMap.get(worldEditMineData.getMineType()));
                            worldEditMine.setMineOwner(worldEditMineData.getMineOwner());
                            String worldName = worldEditMineData.getWorldName();
                            World world = Bukkit.getWorld(worldName);
                            if (world != null) {
                                worldEditMine.setWorld(world);
                            } else {
                                getLogger().severe("World " + worldName + " was deleted.");
                            }
                            worldEditMine.setLocation(new Location(world,
                                                                   worldEditMineData.getRegionMaxX()+1, // why is this one max? hell if i know
                                                                   worldEditMineData.getRegionMinY()-3,
                                                                   worldEditMineData.getRegionMinZ())); // only pain and despair
                            worldEditMine.startResetTask();
                            mineStorage.addWorldEditMine(worldEditMineData.getMineOwner(), worldEditMine);
                            mineWorldManager.getNextFreeLocation();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        }

        PluginManager pluginManager = Bukkit.getServer().getPluginManager();

        File[] addons = addonsDirectory.listFiles();

        int loaded = mineTypes.size();
        getLogger().info("Loaded a total of {loaded} mine types!"
                .replace("{loaded}",
                        String.valueOf(loaded)));

        /*
            Does these things in order

            Sets up the private mines command
            Loads the plugins messages
         */

        new CommandParser(this.getResource("command.rdcml"))
                .setArgTypes(ArgType.of("material", Material.class))
                .parse()
                .register("privatemines",
                        new PrivateMinesCommand(this));

        Messages.load(this);

        Metrics metrics = new Metrics(this, pluginId);
        metrics.addCustomChart(new Metrics.SingleLineChart("mines", MineStorage::getLoadedMineSize));

        //TODO FIX THIS
        if (addons != null) {
            Arrays.stream(addons).forEach(file -> {
                if (file.getName().matches(String.valueOf(jarPattern))) {
                    privateMines.getLogger().info("found addon file: " + file);
                }
            });
        }

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            getLogger().info("Connecting to placeholder api and registering the placeholders");
            new PrivateMinesExpansion().register();
        } else {
            getLogger().info("PlaceholderAPI was not present, not able to establish a hook!");
        }


        World world = getMineWorldManager().getMinesWorld();
        if (WorldGuardWrapper.getInstance().getRegion(world, "__global__").isPresent()) {
            globalRegion = WorldGuardWrapper.getInstance().getRegion(world, "__global__").get();
            utils.setGlobalFlags(globalRegion);
        } else {
            privateMines.getLogger().warning("The global region was somehow null. This should be impossible.");
        }

        if (notifyForUpdates) {
            UpdateChecker.init(this, spigotPluginId).checkEveryXHours(1).setDownloadLink(spigotPluginId).checkNow();
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("Disabling Private Mines...");
        mineDataMap.clear();
        getLogger().info("Saving and closing the BlockDataManager...");
        blockDataManager.getAll().forEach(dataBlock -> Bukkit.getLogger().info("Saving data block: " + dataBlock));
        blockDataManager.saveAndClose();
    }

    /*
        Adds a MineData to the maps
     */

    public void addMineData(String name, MineType mineType) {
        mineDataMap.putIfAbsent(name, mineType);
        mineTypeTreeMap.put(name, mineType);
    }

    public void addWe6Type(String name, WorldEdit6MineType worldEdit6MineType) {
        worldEdit6MineTypeTreeMap.put(name, worldEdit6MineType);
    }

    public void addWe7Type(String name, WorldEditMineType worldEditMineType) {
        worldEditMineTypeTreeMap.put(name, worldEditMineType);
    }

    /*
        Gets a map of all the MineData types
     */

    @SuppressWarnings("unused")
    public Map<String, MineType> getMineDataMap() {
        return mineDataMap;
    }

    /*
        Gets the default mine data
     */

    public MineType getDefaultMineType() {
        if (mineTypeTreeMap.isEmpty() || isWorldEditEnabled) {
            Bukkit.getLogger().info("No default mine type was found!");
            Bukkit.getLogger().info("Create a mine type in the mineTypes");
            Bukkit.getLogger().info("section of the config.yml");
            Bukkit.getLogger().info("Please ask in the discord server" +
                    " if you need help");
            getLogger().info("HERE!");
            return null;
        }
        return mineTypeTreeMap.firstEntry().getValue();
    }

    public WorldEdit6MineType getDefaultWorldEdit6MineType() {
        if (worldEdit6MineTypeTreeMap.isEmpty()) {
            Bukkit.getLogger().info("No default world edit mine type was found!");
            Bukkit.getLogger().info("Create a mine type in the mineTypes");
            Bukkit.getLogger().info("section of the config.yml");
            Bukkit.getLogger().info("Please ask in the discord server" +
                                            " if you need help");
            return null;
        }
        return worldEdit6MineTypeTreeMap.firstEntry().getValue();
    }

    /*
        Get default world edit type
     */

    public WorldEditMineType getDefaultWorldEditMineType() {
        if (worldEditMineTypeTreeMap.isEmpty()) {
            Bukkit.getLogger().info("No default world edit mine type was found!");
            Bukkit.getLogger().info("Create a mine type in the mineTypes");
            Bukkit.getLogger().info("section of the config.yml");
            Bukkit.getLogger().info("Please ask in the discord server" +
                                            " if you need help");
            return null;
        }
        return worldEditMineTypeTreeMap.firstEntry().getValue();
    }



    /*
        Gets the spawn material
     */

    public Material getSpawnMaterial() {
        return spawnPoint;
    }

    /*
        Gets the corner material
     */

    public Material getCornerMaterial() {
        return mineCorner;
    }

    /*
        Gets the sell npc material
     */

    public Material getSellNpcMaterial() {
        return sellNpc;
    }

    /*
        Gets the upgrade material
     */

    public Material getUpgradeMaterial() {
        return upgradeMaterial;
    }

    /*
        Gets the main menu title
     */

    public String getMainMenuTitle() {
        return mainMenuTitle;
    }


    public MineType getMineType(String mineType) {
        MineType newType = mineTypeTreeMap.get(mineType);
        return newType;
    }

    public WorldEditMineType getWorldEditMineType(String mineType) {
        return worldEditMineTypeTreeMap.get(mineType);
    }

    /*
        Gets the next MineData from the TreeMap using String
     */

    @SuppressWarnings("unused")
    public MineType getNextMineType(String mineType) {

        MineType lastValue = mineTypeTreeMap.lastEntry().getValue();
        if (mineTypeTreeMap.higherEntry(mineType) == null) {
            return lastValue;
        }
        return mineTypeTreeMap.higherEntry(mineType).getValue();
    }

    /*
        Gets the next MineData from the TreeMap using MineData
     */

    public MineType getNextMineType(MineType mineType) {
        MineType lastValue = mineTypeTreeMap.lastEntry().getValue();
        if (mineTypeTreeMap.higherEntry(mineType.getName()) == null) {
            return lastValue;
        }
        return mineTypeTreeMap.higherEntry(mineType.getName()).getValue();
    }

        /*
        Gets the next MineData from the TreeMap using MineData
     */

    public WorldEditMineType getNextMineType(WorldEditMineType worldEditMineType) {
        WorldEditMineType lastValue = worldEditMineTypeTreeMap.lastEntry().getValue();
        if (mineTypeTreeMap.higherEntry(worldEditMineType.getName()) == null) {
            return lastValue;
        }
        return worldEditMineTypeTreeMap.higherEntry(worldEditMineType.getName()).getValue();
    }

    /*
        Checks is the mine is currently fully maxed out
     */

    public boolean isAtLastMineType(MineType mineType) {
        MineType lastValue = mineTypeTreeMap.lastEntry().getValue();
        return mineType.equals(lastValue);
    }

    /*
        Checks is the mine is currently fully maxed out
     */

    public boolean isAtLastMineType(WorldEditMineType worldEditMineType) {
        WorldEditMineType lastValue = worldEditMineTypeTreeMap.lastEntry().getValue();
        return worldEditMineType.equals(lastValue);
    }

    public MineFactory6 getMineFactory6() {
        return mineFactory6;
    }

    /*
        Gets the Mine Factory.
     */

    public MineFactory getMineFactory() {
        return mineFactory;
    }

    /*
        Gets the mine storage
     */

    public MineStorage getMineStorage() {
        return mineStorage;
    }

    /*
        Gets the block data manager
     */

    public BlockDataManager getBlockDataManager() {
        return blockDataManager;
    }

    public boolean isDebugMode() {
        return debugMode;
    }

    public boolean isWorldEditEnabled() {
        return isWorldEditEnabled;
    }

    public boolean useWorldEdit() {
        return useWorldEdit;
    }

    public boolean useWorldEdit6() {
        return useWorldEdit6;
    }

    public boolean isUsingAStupidlyOldVersionOfMinecraftAndWorldEditAndShouldReallyBeUpdatingToANewerVersionByNow() {
        return useWorldEdit6;
    }

    public int getMineDistance() {
        return mineDistance;
    }

    public MineWorldManager getMineWorldManager() {
        return mineWorldManager;
    }

    public Utils getUtils() {
        return utils;
    }

    public WorldEditUtilities getWorldEditUtils() {
        return worldEditUtils;
    }

    public File getMinesDirectory() {
        return minesDirectory;
    }

    public File getSchematicsDirectory() {
        return schematicsDirectory;
    }

    public TreeMap<String, WorldEditMineType> getWorldEditMineTypeTreeMap() {
        return worldEditMineTypeTreeMap;
    }

    public Map<String, MenuConfig> getInventory() {
        return inventory;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    @SuppressWarnings("unused")
    public static PrivateMines getInstance() {
        return privateMines;
    }
}