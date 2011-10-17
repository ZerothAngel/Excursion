package org.tyrannyofheaven.bukkit.Excursion;

public class GroupOptions {

    private int delay = 0;
    
    private boolean cancelOnAttack = false;
    
    private boolean cancelOnDamage = false;

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

}
