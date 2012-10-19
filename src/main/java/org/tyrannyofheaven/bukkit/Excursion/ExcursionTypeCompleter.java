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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.util.StringUtil;
import org.tyrannyofheaven.bukkit.util.command.TypeCompleter;

class ExcursionTypeCompleter implements TypeCompleter {

    private final ExcursionPlugin plugin;

    public ExcursionTypeCompleter(ExcursionPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> complete(Class<?> clazz, String arg, CommandSender sender, String partial) {
        if (clazz == String.class) {
            // Build list of actual available worlds
            Set<String> worlds = new HashSet<String>();
            for (World world : Bukkit.getWorlds()) {
                worlds.add(world.getName());
            }
            // Remove non-primary members of groups
            worlds.removeAll(plugin.getGroupMap().keySet());
            // Remove aliased worlds
            worlds.removeAll(plugin.getAliasMap().values());
            // Add all aliases
            worlds.addAll(plugin.getAliasMap().keySet());
            
            // Filter worlds according to permission
            if (!sender.hasPermission("excursion.access.*")) {
                // Doesn't have wildcard permission
                for (Iterator<String> i = worlds.iterator(); i.hasNext();) {
                    String world = i.next();
                    // Get primary world
                    String alias = plugin.getAliasMap().get(world);
                    if (alias == null)
                        alias = world;
                    if (!sender.hasPermission("excursion.access." + alias))
                        i.remove();
                }
            }

            List<String> result = new ArrayList<String>(worlds.size());
            StringUtil.copyPartialMatches(partial, worlds, result);
            Collections.sort(result);
            return result;
        }
        return Collections.emptyList();
    }

}
