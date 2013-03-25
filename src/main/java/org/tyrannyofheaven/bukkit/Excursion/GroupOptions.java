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

public class GroupOptions {

    private int delay = 0;
    
    private boolean cancelOnAttack = false;
    
    private boolean cancelOnDamage = false;

    private boolean cancelOnMove = false;

    public int getDelay() {
        return delay;
    }

    public void setDelay(int delay) {
        this.delay = delay;
    }

    public boolean isCancelOnAttack() {
        return cancelOnAttack;
    }

    public void setCancelOnAttack(boolean cancelOnAttack) {
        this.cancelOnAttack = cancelOnAttack;
    }

    public boolean isCancelOnDamage() {
        return cancelOnDamage;
    }

    public void setCancelOnDamage(boolean cancelOnDamage) {
        this.cancelOnDamage = cancelOnDamage;
    }

    public boolean isCancelOnMove() {
        return cancelOnMove;
    }

    public void setCancelOnMove(boolean cancelOnMove) {
        this.cancelOnMove = cancelOnMove;
    }

}
