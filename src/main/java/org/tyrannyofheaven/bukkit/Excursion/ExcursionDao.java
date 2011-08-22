package org.tyrannyofheaven.bukkit.Excursion;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

public interface ExcursionDao {

    public void saveLocation(Player player, Location location);
    
    public Location loadLocation(Player player, World world);

}
