/*
 * Copyright 2011 ZerothAngel <zerothangel@tyrannyofheaven.org>
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
