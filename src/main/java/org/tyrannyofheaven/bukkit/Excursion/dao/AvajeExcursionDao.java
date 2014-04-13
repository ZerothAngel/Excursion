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
package org.tyrannyofheaven.bukkit.Excursion.dao;

import static org.tyrannyofheaven.bukkit.util.uuid.UuidUtils.canonicalizeUuid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.regex.Matcher;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.tyrannyofheaven.bukkit.Excursion.model.SavedLocation;
import org.tyrannyofheaven.bukkit.Excursion.model.SavedLocationId;
import org.tyrannyofheaven.bukkit.util.uuid.UuidUtils;

import com.avaje.ebean.EbeanServer;

public class AvajeExcursionDao extends BaseMemoryExcursionDao {

    private final EbeanServer ebeanServer;
    
    private final Executor executor;

    public AvajeExcursionDao(EbeanServer ebeanServer, Executor executor) {
        this.ebeanServer = ebeanServer;
        this.executor = executor != null ? executor : new Executor() {
            @Override
            public void execute(Runnable command) {
                command.run();
            }
        };
    }

    private EbeanServer getEbeanServer() {
        return ebeanServer;
    }

    private Executor getExecutor() {
        return executor;
    }

    @Override
    public synchronized void saveLocation(Player player, String group, Location location) {
        super.saveLocation(player, group, location);
    }

    @Override
    public synchronized Location loadLocation(Player player, String group) {
        return super.loadLocation(player, group);
    }

    @Override
    protected void createOrUpdateSavedLocation(SavedLocation sl) {
        final String group = sl.getId().getGroup();
        final String player = sl.getId().getPlayer();
        final String world = sl.getWorld();
        final double x = sl.getX();
        final double y = sl.getY();
        final double z = sl.getZ();
        final float yaw = sl.getYaw();
        final float pitch = sl.getPitch();

        getExecutor().execute(new Runnable() {
            public void run() {
                SavedLocationId key = new SavedLocationId(group, player);
                SavedLocation dbSl = getEbeanServer().find(SavedLocation.class, key);
                if (dbSl == null) {
                    dbSl = new SavedLocation(group, world, player, x, y, z, yaw, pitch);
                }
                else {
                    dbSl.setWorld(world);
                    dbSl.setX(x);
                    dbSl.setY(y);
                    dbSl.setZ(z);
                    dbSl.setYaw(yaw);
                    dbSl.setPitch(pitch);
                }
                getEbeanServer().save(dbSl);
            }
        });
    }

    @Override
    protected void deleteSavedLocation(SavedLocation sl) {
        final String group = sl.getId().getGroup();
        final String player = sl.getId().getPlayer();

        getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                SavedLocationId key = new SavedLocationId(group, player);
                SavedLocation dbSl = getEbeanServer().find(SavedLocation.class, key);
                if (dbSl != null)
                    getEbeanServer().delete(dbSl);
            }
        });
    }

    public void load() {
        List<SavedLocation> sls = getEbeanServer().createQuery(SavedLocation.class)
                .findList();
        load(sls);
    }

    private synchronized void load(List<SavedLocation> sls) {
        getSavedLocations().clear();
        
        for (SavedLocation sl : sls) {
            SavedLocation newSl = new SavedLocation(sl.getId().getGroup(), sl.getWorld(), sl.getId().getPlayer(), sl.getX(), sl.getY(), sl.getZ(), sl.getYaw(), sl.getPitch());
            getSavedLocations().put(newSl.getId(), newSl);
        }
    }

    public void migrate() {
        getEbeanServer().beginTransaction();
        try {
            List<SavedLocation> sls = getEbeanServer().createQuery(SavedLocation.class).findList();

            // Figure out what needs migrating
            Set<String> usernames = new HashSet<String>();
            List<SavedLocation> toMigrate = new ArrayList<SavedLocation>();
            for (SavedLocation sl : sls) {
                Matcher m = UuidUtils.SHORT_UUID_RE.matcher(sl.getId().getPlayer());
                if (!m.matches()) {
                    usernames.add(sl.getId().getPlayer().toLowerCase());
                    toMigrate.add(sl);
                }
            }

            // Resolve names using Bukkit
            Map<String, UUID> resolved = new HashMap<String, UUID>();
            for (String username : usernames) {
                OfflinePlayer player = Bukkit.getOfflinePlayer(username);
                // Not sure if it can ever be null. And #getUniqueId() used to be nullable in <1.7.6
                if (player != null && player.getUniqueId() != null) {
                    resolved.put(username, player.getUniqueId());
                }
            }

            // Update IDs
            List<SavedLocation> toSave = new ArrayList<SavedLocation>();
            for (SavedLocation sl : toMigrate) {
                UUID uuid = resolved.get(sl.getId().getPlayer().toLowerCase());
                if (uuid != null) {
                    // Create and save a new one with new ID
                    SavedLocation nsl = new SavedLocation(sl.getId().getGroup(), sl.getWorld(), canonicalizeUuid(uuid), sl.getX(), sl.getY(), sl.getZ(), sl.getYaw(), sl.getPitch());
                    toSave.add(nsl);
                }
            }

            // Delete migrated ones regardless of success
            getEbeanServer().delete(toMigrate);
            getEbeanServer().save(toSave);

            getEbeanServer().commitTransaction();
        }
        finally {
            getEbeanServer().endTransaction();
        }
    }

}
