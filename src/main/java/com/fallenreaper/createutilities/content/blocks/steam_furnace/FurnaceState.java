package com.fallenreaper.createutilities.content.blocks.steam_furnace;

public enum FurnaceState {
    PRODUCING,
    ACTIVE,
    NONE, LOADED, RUNNING;


    public boolean isActive() {
        return this == ACTIVE;
    }

    public boolean isProducing() {
        return this == PRODUCING;
    }

    public boolean isPaused() {
        return this == NONE;
    }

    public boolean isLoaded() {
        return this == LOADED;
    }

    public boolean isRunning() {
        return this == RUNNING;
    }
}
