package me.untouchedodin0.plugin;

import me.untouchedodin0.plugin.factory.MineFactory;
import me.untouchedodin0.plugin.storage.MineStorage;

public class PrivateMinesAPI {

    private final MineFactory mineFactory;
    private final MineStorage mineStorage;

    public PrivateMinesAPI(PrivateMines privateMines) {
        this.mineFactory = privateMines.getMineFactory();
        this.mineStorage = privateMines.getMineStorage();
    }


    public MineFactory getMineFactory() {
        return mineFactory;
    }

    public MineStorage getMineStorage() {
        return mineStorage;
    }
}
