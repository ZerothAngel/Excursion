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
package org.tyrannyofheaven.bukkit.Excursion.model;

import static org.tyrannyofheaven.bukkit.util.uuid.UuidUtils.uncanonicalizeUuid;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Transient;

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

    @Transient
    public UUID getUuid() {
        return uncanonicalizeUuid(getPlayer());
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
