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

package me.untouchedodin0.plugin.commands;

import me.untouchedodin0.plugin.PrivateMines;
import me.untouchedodin0.plugin.PrivateMinesAPI;
import me.untouchedodin0.plugin.config.MenuConfig;
import me.untouchedodin0.plugin.factory.MineFactory;
import me.untouchedodin0.plugin.mines.Mine;
import me.untouchedodin0.plugin.mines.MineType;
import me.untouchedodin0.plugin.mines.WorldEditMine;
import me.untouchedodin0.plugin.mines.WorldEditMineType;
import me.untouchedodin0.plugin.mines.data.WorldEditMineData;
import me.untouchedodin0.plugin.storage.MineStorage;
import me.untouchedodin0.plugin.util.Utils;
import me.untouchedodin0.plugin.world.MineWorldManager;
import me.untouchedodin0.privatemines.we_6.worldedit.MineFactory6;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import redempt.redlib.blockdata.BlockDataManager;
import redempt.redlib.blockdata.DataBlock;
import redempt.redlib.commandmanager.CommandHook;
import redempt.redlib.commandmanager.Messages;
import redempt.redlib.configmanager.ConfigManager;
import redempt.redlib.inventorygui.InventoryGUI;
import redempt.redlib.inventorygui.ItemButton;
import redempt.redlib.itemutils.ItemBuilder;
import redempt.redlib.misc.Task;
import redempt.redlib.misc.WeightedRandom;
import redempt.redlib.multiblock.Structure;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PrivateMinesCommand {

    private final MineFactory6 mineFactory6;
    private final MineFactory mineFactory;
    private final MineStorage mineStorage;
    private final MineWorldManager mineWorldManager;
    private final PrivateMines privateMines;
    private final PrivateMinesAPI privateMinesAPI;

    Utils utils;

    public PrivateMinesCommand(PrivateMines privateMine) {
        this.privateMines = privateMine;
        this.mineFactory6 = privateMine.getMineFactory6();
        this.mineFactory = privateMine.getMineFactory();
        this.mineStorage = privateMine.getMineStorage();
        this.mineWorldManager = privateMine.getMineWorldManager();
        this.utils = privateMine.getUtils();
        this.privateMinesAPI = PrivateMines.getAPI();
    }

    @CommandHook("main")
    public void mainHook(Player player) {
        Map<String, MenuConfig> menuConfig = privateMines.getInventory();
        String inventoryTitle = privateMines.getMainMenuTitle();
        String inventoryTitleColored = utils.color(inventoryTitle);

        InventoryGUI gui = new InventoryGUI(Bukkit.createInventory(null, 27, inventoryTitleColored));

        WorldEditMine worldEditMine = mineStorage.getWorldEditMine(player.getUniqueId());

        menuConfig.forEach((s, c) -> {
            String name = utils.color(c.getName());
            List<String> lore = utils.color(c.getLore());

            int slot = c.getSlot();

            ItemStack itemStack = new ItemStack(c.getType());
            ItemMeta itemMeta = itemStack.getItemMeta();

            if (itemMeta != null) {
                itemMeta.setDisplayName(name);
                itemMeta.setLore(lore);
            }

            itemStack.setItemMeta(itemMeta);

            ItemButton itemButton = ItemButton.create(itemStack, inventoryClickEvent -> {
                if (c.getSlot() == inventoryClickEvent.getSlot()) {
                    String action = c.getAction();
                    utils.doAction(player, worldEditMine, action);
                    player.closeInventory();
                }
            });
            gui.addButton(slot, itemButton);
        });

        gui.open(player);
    }

    @CommandHook("give")
    public void give(CommandSender commandSender, Player target) {

        String alreadyOwnsMine = "targetAlreadyOwnsAMine";
        MineStorage mineStorage = privateMinesAPI.getMineStorage();

        try {
            if (mineStorage.hasWorldEditMine(target.getUniqueId())) {
                commandSender.sendMessage(ChatColor.RED + "User already has a mine!");
                return;
            }
            commandSender.sendMessage(ChatColor.GREEN + "Giving " + target.getName() + " a private mine!");
            Location location = mineWorldManager.getNextFreeLocation();
            if (privateMines.isWorldEditEnabled()) {
                if (privateMines.useWorldEdit6()) {
                    mineFactory6.createMine(target, target.getLocation(), privateMines.getDefaultWorldEdit6MineType(), false);
                } else {
                    WorldEditMineType worldEditMineType = privateMines.getWorldEditMineTypeTreeMap().firstEntry().getValue();
                    mineFactory.createMine(target, location, worldEditMineType, false);
                }
            }
        } catch (ArrayIndexOutOfBoundsException arrayIndexOutOfBoundsException) {
            arrayIndexOutOfBoundsException.printStackTrace();
        }
    }

    @CommandHook("delete")
    public void delete(CommandSender commandSender, Player target) {
        UUID uuid = target.getUniqueId();

        String deletedPlayersMine = "deletedPlayersMine";
        String yourMineHasBeenDeleted = "deletedMine";

        File minesDirectory = privateMines.getMinesDirectory();
        String fileName = target.getUniqueId() + ".json";
        File jsonFile = new File(minesDirectory, fileName);

        if (!privateMines.getMineStorage().hasWorldEditMine(uuid)) {
            commandSender.sendMessage(ChatColor.RED + "Player doesn't own a mine!");
        } else {
            WorldEditMine worldEditMine = privateMines.getMineStorage().getWorldEditMine(uuid);
            Task task = worldEditMine.getTask();
            task.cancel();
            worldEditMine.delete();
            privateMines.getMineStorage().removeWorldEditMine(uuid);

            if (jsonFile.exists()) {
                boolean deleted = jsonFile.delete();
                if (deleted) {
                    privateMines.getLogger().info("The file has been deleted!");
                }
            }
            utils.sendMessage(commandSender, deletedPlayersMine);
            utils.sendMessage(target, yourMineHasBeenDeleted);
        }
    }

    @CommandHook("reset")
    public void reset(Player player) {
        UUID uuid = player.getUniqueId();
        String doNotOwnMine = "doNotOwnMine";
        String mineReset = "mineReset";
        String teleportedToMine = "teleportedToMine";

        if (!mineStorage.hasWorldEditMine(uuid)) {
            utils.sendMessage(player, doNotOwnMine);
        } else {
            WorldEditMine worldEditMine = mineStorage.getWorldEditMine(uuid);
            WorldEditMineData worldEditMineData = worldEditMine.getWorldEditMineData();
            String mineType = worldEditMineData.getMineType();
            WorldEditMineType worldEditMineType = privateMines.getWorldEditMineType(mineType);
            worldEditMine.fill(worldEditMineType.getMaterials());
            utils.sendMessage(player, mineReset);
//            worldEditMine.teleport(player); - PUT BACK ASAP
            utils.sendMessage(player, teleportedToMine);
        }
    }

    @CommandHook("teleport")
    public void teleport(Player player) {
        UUID uuid = player.getUniqueId();
        String doNotOwnMine = "doNotOwnMine";
        String teleportedToMine = Messages.msg("teleportedToMine");

        if (!mineStorage.hasWorldEditMine(uuid)) {
            utils.sendMessage(player, doNotOwnMine);
            return;
        }

        WorldEditMine worldEditMine = mineStorage.getWorldEditMine(uuid);
        worldEditMine.teleport(player);
        player.sendMessage(teleportedToMine);
    }

    //todo this should really be tidied up into its own simple method somewhere lol.

    @CommandHook("teleportOther")
    public void teleportOther(Player player, Player target) {
        UUID uuid = target.getUniqueId();
        String targetDoesNotOwnMine = Messages.msg("targetDoesNotOwnMine");
        String notWhitelisted = Messages.msg("notWhitelisted");

        if (!mineStorage.hasWorldEditMine(uuid)) {
            utils.sendMessage(player, targetDoesNotOwnMine);
            return;
        }

        WorldEditMine worldEditMine = mineStorage.getWorldEditMine(uuid);
        WorldEditMineData worldEditMineData = worldEditMine.getWorldEditMineData();

        UUID coowner = worldEditMineData.getCoOwner();
        boolean isCoOwner = coowner.equals(player.getUniqueId());

        List<UUID> whitelistedPlayers = worldEditMineData.getWhitelistedPlayers();
        boolean isOpen = worldEditMineData.isOpen();

        if (!isOpen) {
            boolean isWhitelisted = whitelistedPlayers.contains(player.getUniqueId());

            // If they're whitelisted, or they're co-owner they can enter.

            if (isWhitelisted || isCoOwner) {
                worldEditMine.teleport(player);
            } else {
                player.sendMessage(notWhitelisted);
            }
        } else {
            worldEditMine.teleport(player);
        }
    }

    @CommandHook("upgrade")
    public void upgrade(CommandSender commandSender, Player target) {
        String targetDoesNotOwnMine = Messages.msg("targetDoesNotOwnMine");

        if (!mineStorage.hasWorldEditMine(target.getUniqueId())) {
            utils.sendMessage(commandSender, targetDoesNotOwnMine);
            return;
        }

        WorldEditMine worldEditMine = mineStorage.getWorldEditMine(target.getUniqueId());
        privateMines.getLogger().info("worldEditMine: " + worldEditMine);
        worldEditMine.upgrade();
    }

    // Add 1 to whatever args you put so if you want to expand by one do /pmine expand 2

    @CommandHook("expand")
    public void expand(CommandSender commandSender, Player target, int amount) {
        Player player = (Player) commandSender;
        String targetDoesNotOwnMine = Messages.msg("targetDoesNotOwnMine");

        if (!mineStorage.hasWorldEditMine(target.getUniqueId())) {
            utils.sendMessage(commandSender, targetDoesNotOwnMine);
            return;
        }
        WorldEditMine worldEditMine = mineStorage.getWorldEditMine(target.getUniqueId());
        worldEditMine.expand(amount);
        mineStorage.replaceMine(player.getUniqueId(), worldEditMine);
    }

    @CommandHook("setblocks")
    public void setBlocks(CommandSender commandSender, Player target, Material[] materials) {
        Player player = (Player) commandSender;
        WeightedRandom<Material> weightedRandom = new WeightedRandom<>();
        Mine mine;
        String targetDoesNotOwnMine = Messages.msg("targetDoesNotOwnMine");

        for (Material material : materials) {
            weightedRandom.set(material, 1);
        }

        if (!mineStorage.hasMine(target.getUniqueId())) {
            utils.sendMessage(commandSender, targetDoesNotOwnMine);
        } else {
            mine = mineStorage.getMine(target.getUniqueId());
            player.sendMessage("Setting " + target.getName() + "'s blocks to " + weightedRandom.getWeights());
            mine.setWeightedRandom(weightedRandom);

            BlockDataManager blockDataManager = privateMines.getBlockDataManager();
            DataBlock dataBlock = privateMines.getBlockDataManager().getDataBlock(mine.getMineLocation().getBlock());
            dataBlock.set("weightedRandom", weightedRandom);
            mine.reset();
            blockDataManager.save();
        }
    }

    @CommandHook("settype")
    public void setType(CommandSender commandSender, Player target, String type) {
        Player player = (Player) commandSender;
        Mine mine;
        MineType newType;
        Structure structure;
        String invalidMineType = Messages.msg("invalidMineType");

        if (mineStorage.hasMine(target.getUniqueId())) {
            mine = mineStorage.getMine(target.getUniqueId());
            newType = privateMines.getMineType(type);
            if (newType == null) {
                player.sendMessage(ChatColor.RED + "Invalid mine type!");
                utils.sendMessage(commandSender, invalidMineType);
                return;
            }
            mine.cancelResetTask();
            structure = mine.getStructure();
            structure.getRegion().forEachBlock(block -> block.setType(Material.AIR, false));
            mine.setMineType(newType);
//            mine.build();
            mine.startAutoResetTask();
            mineStorage.replaceMine(player.getUniqueId(), mine);
            mine.teleportPlayer(target);
        }
    }

    @CommandHook("open")
    public void open(Player player) {
        WorldEditMine worldEditMine;
        WorldEditMineData worldEditMineData;
        UUID uuid = player.getUniqueId();
        String doNotOwnMine = Messages.msg("doNotOwnMine");
        String mineAlreadyOpen = Messages.msg("mineAlreadyOpen");
        String mineOpened = Messages.msg("mineOpened");

        if (!mineStorage.hasWorldEditMine(uuid)) {
            utils.sendMessage(player, doNotOwnMine);
            return;
        }

        worldEditMine = mineStorage.getWorldEditMine(player.getUniqueId());
        worldEditMineData = worldEditMine.getWorldEditMineData();
        boolean isOpen = worldEditMineData.isOpen();

        if (isOpen) {
            player.sendMessage(mineAlreadyOpen);
        } else {
            worldEditMineData.setOpen(true);
            worldEditMine.setWorldEditMineData(worldEditMineData);
            privateMines.getMineStorage().replaceMine(uuid, worldEditMine);
            player.sendMessage(mineOpened);
        }
    }


    @CommandHook("close")
    public void close(Player player) {
        WorldEditMine worldEditMine;
        WorldEditMineData worldEditMineData;
        UUID uuid = player.getUniqueId();
        String doNotOwnMine = Messages.msg("doNotOwnMine");
        String mineAlreadyClosed = Messages.msg("mineAlreadyClosed");
        String mineClosed = Messages.msg("mineClosed");

        if (!mineStorage.hasWorldEditMine(uuid)) {
            utils.sendMessage(player, doNotOwnMine);
            return;
        }

        worldEditMine = mineStorage.getWorldEditMine(player.getUniqueId());
        worldEditMineData = worldEditMine.getWorldEditMineData();
        boolean isOpen = worldEditMineData.isOpen();

        if (!isOpen) {
            player.sendMessage(mineAlreadyClosed);
        } else {
            worldEditMineData.setOpen(false);
            worldEditMine.setWorldEditMineData(worldEditMineData);
            privateMines.getMineStorage().replaceMine(uuid, worldEditMine);
            player.sendMessage(mineClosed);
        }
    }

    @CommandHook("whitelist")
    public void whitelist(Player player, Player target) {
        WorldEditMine worldEditMine;
        WorldEditMineData worldEditMineData;
        UUID uuid = player.getUniqueId();
        UUID targetUUID = target.getUniqueId();

        String doNotOwnMine = Messages.msg("doNotOwnMine");
        String youHaveBeenWhitelisted = Messages.msg("youHaveBeenWhitelistedAtMine");
        String youHaveAddedPlayerToYourMine = Messages.msg("youHaveAddedPlayerToYourMine");

        String replacedAdded = youHaveAddedPlayerToYourMine.replace("%name%", target.getName());
        String addedPlayerReplaced = replacedAdded.replace("%name%", player.getName());
        String addedReplaced = youHaveBeenWhitelisted.replace("%owner%", player.getName());

        if (!privateMines.getMineStorage().hasWorldEditMine(uuid)) {
            player.sendMessage(doNotOwnMine);
        }
        worldEditMine = privateMines.getMineStorage().getWorldEditMine(uuid);
        worldEditMineData = worldEditMine.getWorldEditMineData();
        worldEditMineData.addWhitelistedPlayer(targetUUID);
        player.sendMessage(addedPlayerReplaced);
        target.sendMessage(addedReplaced);

        worldEditMine.setWorldEditMineData(worldEditMineData);
        privateMines.getMineStorage().replaceMine(uuid, worldEditMine);
    }

    @CommandHook("unwhitelist")
    public void unWhitelist(Player player, Player target) {
        WorldEditMine worldEditMine;
        WorldEditMineData worldEditMineData;
        UUID uuid = player.getUniqueId();
        UUID targetUUID = target.getUniqueId();

        String doNotOwnMine = Messages.msg("doNotOwnMine");
        String youHaveBeenUnWhitelisted = Messages.msg("youHaveBeenUnWhitelistedFromUsersMine");
        String youHaveRemovedPlayerFromYourMine = Messages.msg("youHaveUnWhitelistedPlayerFromYourMine");

        String replacedYouHaveBeenUnwhitelisted = youHaveBeenUnWhitelisted.replace("%name%", target.getName());
        String YouHaveBeenUnwhitelistedPlayerReplaced = replacedYouHaveBeenUnwhitelisted.replace("%name%", player.getName());

        String replacedYouHaveRemoved = youHaveRemovedPlayerFromYourMine.replace("%name%", target.getName());
        String YouHaveRemovedPlayerReplaced = replacedYouHaveRemoved.replace("%name%", player.getName());

        if (!privateMines.getMineStorage().hasWorldEditMine(uuid)) {
            player.sendMessage(doNotOwnMine);
        }
        worldEditMine = privateMines.getMineStorage().getWorldEditMine(uuid);
        worldEditMineData = worldEditMine.getWorldEditMineData();
        worldEditMineData.removeWhitelistedPlayer(targetUUID);
        player.sendMessage(YouHaveRemovedPlayerReplaced);
        target.sendMessage(YouHaveBeenUnwhitelistedPlayerReplaced);
        worldEditMine.setWorldEditMineData(worldEditMineData);
        privateMines.getMineStorage().replaceMine(uuid, worldEditMine);
    }

    @CommandHook("coowner")
    public void coOwner(Player player, Player target) {
        WorldEditMine worldEditMine;
        WorldEditMineData worldEditMineData;
        UUID uuid = player.getUniqueId();
        UUID targetUUID = target.getUniqueId();

        String doNotOwnMine = Messages.msg("doNotOwnMine");

        String youHaveSetUserAsCoOwner = Messages.msg("youHaveSetUserAsCoOwner");
        String youHaveBeenSetAsACoOwnerAtMine = Messages.msg("youHaveBeenSetAsACoOwnerAtMine");

        String replacedYouHaveSetUserAsCoOwner = youHaveSetUserAsCoOwner.replace("%name%", target.getName());
        String replacedYouHaveBeenSetAsCoOwner = youHaveBeenSetAsACoOwnerAtMine.replace("%name%", player.getName());

        String replacedYouHaveSetUser = replacedYouHaveSetUserAsCoOwner.replace("%name%", player.getName());
        String replacedYouHaveBeenSet = replacedYouHaveBeenSetAsCoOwner.replace("%name%", target.getName());

        if (!privateMines.getMineStorage().hasWorldEditMine(uuid)) {
            player.sendMessage(doNotOwnMine);
        }

        worldEditMine = privateMines.getMineStorage().getWorldEditMine(uuid);
        worldEditMineData = worldEditMine.getWorldEditMineData();
        worldEditMineData.setCoOwner(targetUUID);
        player.sendMessage(replacedYouHaveSetUser);
        target.sendMessage(replacedYouHaveBeenSet);
        worldEditMine.setWorldEditMineData(worldEditMineData);
        privateMines.getMineStorage().replaceMine(uuid, worldEditMine);
    }

    @CommandHook("tax")
    public void tax(Player player, Double tax) {
        UUID uuid = player.getUniqueId();

        PrivateMines privateMines = PrivateMines.getPrivateMines();

        player.sendMessage(ChatColor.GREEN + "Setting your tax to " + tax);
        if (!mineStorage.hasWorldEditMine(uuid)) {
            player.sendMessage("You can't set tax as you don't own a mine!");
        }
        WorldEditMine worldEditMine = mineStorage.getWorldEditMine(uuid);
        worldEditMine.setTax(tax);
        mineStorage.replaceMine(uuid, worldEditMine);
    }

    @CommandHook("list")
    public void list(Player player) {
        player.sendMessage("" + mineStorage);
        player.sendMessage("" + mineStorage.getWorldEditMinesCount());
        InventoryGUI inventoryGUI = new InventoryGUI(Bukkit.createInventory(null, 27, "Public Mines"));

        mineStorage.getWorldEditMines().forEach((uuid, worldEditMine) -> {
            ItemButton itemButton = ItemButton.create(new ItemBuilder(Material.EMERALD_BLOCK).setName("Click me"), inventoryClickEvent -> {
               Player clickPlayer = (Player) inventoryClickEvent.getWhoClicked();
               clickPlayer.sendMessage("howdy " + this);
            });
            player.sendMessage(String.valueOf(worldEditMine.getTax()));
        });
        inventoryGUI.open(player);
    }

    @CommandHook("reload")
    public void reload(Player player) {
        ConfigManager configManager = privateMines.getConfigManager();
        configManager.load();
    }
}

