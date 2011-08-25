package org.tyrannyofheaven.bukkit.Excursion;

import javax.persistence.Embeddable;

@Embeddable
public class SavedLocationId {

    private String world;
    
    private String player;

    // Should be considered private. But Avaje doesn't like that.
    public SavedLocationId() {
    }

    public SavedLocationId(String world, String player) {
        if (world == null || world.trim().length() == 0)
            throw new IllegalArgumentException("world must have a value");
        if (player == null || player.trim().length() == 0)
            throw new IllegalArgumentException("player must have a value");
        setWorld(world);
        setPlayer(player);
    }

    public String getWorld() {
        return world;
    }

    public void setWorld(String world) {
        this.world = world;
    }

    public String getPlayer() {
        return player;
    }

    public void setPlayer(String player) {
        this.player = player;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof SavedLocationId)) return false;
        SavedLocationId o = (SavedLocationId)obj;
        return getWorld().equals(o.getWorld()) &&
            getPlayer().equals(o.getPlayer());
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 37 * result + getWorld().hashCode();
        result = 37 * result + getPlayer().hashCode();
        return result;
    }

    @Override
    public String toString() {
        return String.format("SavedLocationId[%s,%s]", getWorld(), getPlayer());
    }

}
