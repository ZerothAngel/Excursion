package org.tyrannyofheaven.bukkit.Excursion;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public interface ExcursionDao {

    public void saveLocation(Player player, String group, Location location);
    
    public Location loadLocation(Player player, String group);

}
