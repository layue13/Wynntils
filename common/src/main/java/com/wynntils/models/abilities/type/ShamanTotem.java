/*
 * Copyright © Wynntils 2023.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.abilities.type;

import com.wynntils.utils.mc.type.Location;

public class ShamanTotem {
    private final int totemNumber;
    private final int visibleEntityId;
    private int timerEntityId;
    private int time;
    private TotemState state;
    private Location location;

    public ShamanTotem(
            int totemNumber,
            int timerEntityId,
            int visibleEntityId,
            int time,
            TotemState totemState,
            Location location) {
        this.totemNumber = totemNumber;
        this.timerEntityId = timerEntityId;
        this.visibleEntityId = visibleEntityId;
        this.time = time;
        this.state = totemState;
        this.location = location;
    }

    public int getTotemNumber() {
        return totemNumber;
    }

    public int getVisibleEntityId() {
        return visibleEntityId;
    }

    public int getTimerEntityId() {
        return timerEntityId;
    }

    public void setTimerEntityId(int timerEntityId) {
        this.timerEntityId = timerEntityId;
    }

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public TotemState getState() {
        return state;
    }

    public void setState(TotemState state) {
        this.state = state;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public enum TotemState {
        SUMMONED,
        ACTIVE
    }
}