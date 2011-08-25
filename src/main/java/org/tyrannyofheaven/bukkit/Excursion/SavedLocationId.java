package org.tyrannyofheaven.bukkit.Excursion;

import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class SavedLocationId {

    private String group;
    
    private String player;

    // Should be considered private. But Avaje doesn't like that.
    public SavedLocationId() {
    }

    public SavedLocationId(String group, String player) {
        if (group == null || group.trim().length() == 0)
            throw new IllegalArgumentException("group must have a value");
        if (player == null || player.trim().length() == 0)
            throw new IllegalArgumentException("player must have a value");
        setGroup(group);
        setPlayer(player);
    }

    @Column(name="group_name")
    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
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
        return getGroup().equals(o.getGroup()) &&
            getPlayer().equals(o.getPlayer());
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 37 * result + getGroup().hashCode();
        result = 37 * result + getPlayer().hashCode();
        return result;
    }

    @Override
    public String toString() {
        return String.format("SavedLocationId[%s,%s]", getGroup(), getPlayer());
    }

}
