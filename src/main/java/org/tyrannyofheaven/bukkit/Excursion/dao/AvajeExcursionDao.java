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

import java.util.List;
import java.util.concurrent.Executor;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.tyrannyofheaven.bukkit.Excursion.model.SavedLocation;
import org.tyrannyofheaven.bukkit.Excursion.model.SavedLocationId;

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

}
