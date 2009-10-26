/*
 * NoSuchVoiceException.java
 *
 * Created on January 4, 2003, 4:56 PM
 */

package com.pcmsolutions.device.EMU.E4;

import com.pcmsolutions.device.EMU.E4.preset.PresetException;

/**
 *
 * @author  pmeehan
 */
class CannotRemoveLastVoiceException extends PresetException {
    public CannotRemoveLastVoiceException() {
        super("can't remove last voice");
    }
}
