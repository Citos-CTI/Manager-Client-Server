/*
 * Copyright (c) 2018.  Johannes Engler, Citos CTI
 */
package citos.server.pluginlicensecheck;

public class PluginLicenseState {
    private int state;
    private long daysLeft;

    public PluginLicenseState(int state) {
        this.state = state;
    }

    public PluginLicenseState(int state, long daysLeft) {
        this.state = state;
        this.daysLeft = daysLeft;
    }

    public int getState() {
        return state;
    }

    public long getDaysLeft() {
        return daysLeft;
    }
}
