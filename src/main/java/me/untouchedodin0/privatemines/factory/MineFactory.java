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

package me.untouchedodin0.privatemines.factory;

import me.untouchedodin0.privatemines.PrivateMines;
import me.untouchedodin0.privatemines.mines.Mine;
import me.untouchedodin0.privatemines.mines.MineData;
import me.untouchedodin0.privatemines.storage.MineStorage;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.UUID;

public class MineFactory {

    PrivateMines privateMines;
    MineStorage mineStorage;

    public MineFactory(PrivateMines privateMines,
                       MineStorage mineStorage) {
        this.privateMines = privateMines;
        this.mineStorage = mineStorage;
    }

    /**
     * @param player   - The target player to be given a mine
     * @param location - The spigot world location where to create the mine
     * @param mineData - The mine data such as the MultiBlockStructure and the Materials
     * @return The newly created mine
     */

    public Mine createMine(Player player, Location location, MineData mineData) {
        Mine mine = new Mine();
        mine.setMineOwner(player.getUniqueId());
        mine.setMineLocation(location);
        mine.setMineData(mineData);
        mine.build();
        mineStorage.addMine(player.getUniqueId(), mine);
        return mine;
    }

    public void deleteMine(Player player) {
        UUID uuid = player.getUniqueId();
        Mine mine = mineStorage.getMine(uuid);
        mine.delete();
    }

    public void upgradeMine(Player player, MineData mineData) {
        MineData nextMineData = privateMines.getNextMineData(mineData.getName());
        Mine mine = mineStorage.getMine(player.getUniqueId());
        mine.setMineData(nextMineData);
    }
}
