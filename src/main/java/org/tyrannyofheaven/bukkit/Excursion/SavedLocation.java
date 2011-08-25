package org.tyrannyofheaven.bukkit.Excursion;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name="locations")
public class SavedLocation {

    @EmbeddedId
    private SavedLocationId id;

    private double x;
    
    private double y;
    
    private double z;
    
    private float yaw;
    
    private float pitch;

    // Should be considered private. But Avaje doesn't like that.
    public SavedLocation() {
    }
    
    public SavedLocation(String world, String player, double x, double y, double z, float yaw, float pitch) {
        setId(new SavedLocationId(world, player));
        setX(x);
        setY(y);
        setZ(z);
        setYaw(yaw);
        setPitch(pitch);
    }

    public SavedLocationId getId() {
        return id;
    }

    public void setId(SavedLocationId id) {
        this.id = id;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getZ() {
        return z;
    }

    public void setZ(double z) {
        this.z = z;
    }

    public float getYaw() {
        return yaw;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof SavedLocation)) return false;
        SavedLocation o = (SavedLocation)obj;
        // NB: equal if their IDs are equal
        return getId().equals(o.getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

    @Override
    public String toString() {
        return String.format("SavedLocation[world=%s,player=%s, (x,y,z)=(%f,%f,%f), yaw=%f, pitch=%f]",
                id.getWorld(), id.getPlayer(),
                getX(), getY(), getZ(),
                getYaw(), getPitch());
    }

}
