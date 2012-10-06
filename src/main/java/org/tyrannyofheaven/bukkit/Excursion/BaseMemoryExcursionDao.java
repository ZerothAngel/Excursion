/*
 * Copyright 2012 ZerothAngel <zerothangel@tyrannyofheaven.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.tyrannyofheaven.bukkit.Excursion;

import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

public abstract class BaseMemoryExcursionDao implements ExcursionDao {

    private final HashMap<SavedLocationId, SavedLocation> savedLocations = new HashMap<SavedLocationId, SavedLocation>();

    protected HashMap<SavedLocationId, SavedLocation> getSavedLocations() {
        return savedLocations;
    }

    @Override
    public void saveLocation(Player player, String group, Location location) {
        SavedLocationId key = new SavedLocationId(group, player.getName());
        SavedLocation sl = savedLocations.get(key);
        if (sl == null) {
            sl = new SavedLocation(group, location.getWorld().getName(), player.getName(), location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
            savedLocations.put(key, sl);
        }
        else {
            // Update values
            sl.setWorld(location.getWorld().getName());
            sl.setX(location.getX());
            sl.setY(location.getY());
            sl.setZ(location.getZ());
            sl.setYaw(location.getYaw());
            sl.setPitch(location.getPitch());
        }
        createOrUpdateSavedLocation(sl);
    }

    protected abstract void createOrUpdateSavedLocation(SavedLocation sl);

    @Override
    public Location loadLocation(Player player, String group) {
        SavedLocationId key = new SavedLocationId(group, player.getName());
        SavedLocation sl = savedLocations.get(key);
        if (sl != null) {
            World world = Bukkit.getWorld(sl.getWorld());
            if (world != null)
                return new Location(world, sl.getX(), sl.getY(), sl.getZ(), sl.getYaw(), sl.getPitch());
            else {
                // World no longer there
                savedLocations.remove(key);
                deleteSavedLocation(sl);
            }
        }
        return null;
    }

    protected abstract void deleteSavedLocation(SavedLocation sl);

}
