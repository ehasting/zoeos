/*
 * PresetInitializeEvent.java
 *
 * Created on February 14, 2003, 10:15 PM
 */

package com.pcmsolutions.device.EMU.E4.events.preset;

import com.pcmsolutions.device.EMU.E4.events.preset.PresetEvent;
import com.pcmsolutions.device.EMU.E4.preset.PresetListener;


/**
 * @author pmeehan
 */
public class PresetInitializationStatusChangedEvent extends PresetEvent {
    private double status;

    public PresetInitializationStatusChangedEvent(Object source, Integer preset, double status) {
        super(source, preset);
        this.status = status;
    }

    public PresetInitializationStatusChangedEvent(Object source, Integer preset) {
        this(source, preset, 0);
    }

    public double getStatus() {
        return status;
    }

    public void fire(PresetListener pl) {
        if (pl != null)
            pl.presetInitializationStatusChanged(this);
    }
}
