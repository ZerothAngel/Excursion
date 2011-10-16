package org.tyrannyofheaven.bukkit.Excursion;

import org.bukkit.Location;

public class CurrentLocation {

    private final String currentPrimaryWorldName;
    
    private final Location location;

    private final int delay;

    public CurrentLocation(String currentPrimaryWorldName, Location location, int delay) {
        this.currentPrimaryWorldName = currentPrimaryWorldName;
        this.location = location;
        this.delay = delay;
    }

    public String getCurrentPrimaryWorldName() {
        return currentPrimaryWorldName;
    }

    public Location getLocation() {
        return location;
    }

    public int getDelay() {
        return delay;
    }
    
}
