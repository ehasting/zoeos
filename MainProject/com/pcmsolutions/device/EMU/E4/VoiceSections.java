package com.pcmsolutions.device.EMU.E4;

import com.pcmsolutions.device.EMU.E4.gui.TitleProvider;
import com.pcmsolutions.system.ZUtilities;

/**
 * User: paulmeehan
 * Date: 10-Feb-2004
 * Time: 21:35:21
 */
class VoiceSections {
    public static final int VOICE_TUNING = 0x01;
    public static final int VOICE_AMP_FILTER = 0x02;
    public static final int VOICE_AMP = 0x04;
    public static final int VOICE_FILTER = 0x08;
    public static final int VOICE_ENVELOPES = 0x10;
    public static final int VOICE_AMP_ENVELOPE = 0x20;
    public static final int VOICE_FILTER_ENVELOPE = 0x40;
    public static final int VOICE_AUX_ENVELOPE = 0x80;
    public static final int VOICE_LFO = 0x100;
    public static final int VOICE_CORDS = 0x200;
    public static final int VOICE_EDIT = 0x250;

    public static final String VOICE_AMP_TITLE = "Amp";
    public static final String VOICE_FILTER_TITLE = "Filter";
    public static final String VOICE_LFO_TITLE = "LFO";
    public static final String VOICE_TUNING_TITLE = "Tuning";
    public static final String VOICE_AMP_ENVELOPE_TITLE = "Amp Env";
    public static final String VOICE_AUX_ENVELOPE_TITLE = "Aux Env";
    public static final String VOICE_FILTER_ENVELOPE_TITLE = "Filt Env";
    public static final String VOICE_CORDS_TITLE = "Cords";
    public static final String VOICE_ENVELOPES_TITLE = "Envelopes";
    public static final String VOICE_AMP_FILTER_TITLE = "Amp/Filt";
    public static final String VOICE_EDIT_TITLE = "Voice Edit";

    public static final String VOICE_AMP_REDUCED_TITLE = "Amp";
    public static final String VOICE_FILTER_REDUCED_TITLE = "Filt";
    public static final String VOICE_LFO_REDUCED_TITLE = "LFO";
    public static final String VOICE_TUNING_REDUCED_TITLE = "Tuning";
    public static final String VOICE_AMP_ENVELOPE_REDUCED_TITLE = "AmpEnv";
    public static final String VOICE_AUX_ENVELOPE_REDUCED_TITLE = "AuxEnv";
    public static final String VOICE_FILTER_ENVELOPE_REDUCED_TITLE = "FiltEnv";
    public static final String VOICE_CORDS_REDUCED_TITLE = "Cords";
    public static final String VOICE_ENVELOPES_REDUCED_TITLE = "Env's";
    public static final String VOICE_AMP_FILTER_REDUCED_TITLE = "Amp/Filt";
    public static final String VOICE_EDIT_REDUCED_TITLE = "Voice";

    public static final TitleProvider voiceAmpTitle = new TitleProvider.StaticTitleProvider(VOICE_AMP_TITLE, VOICE_AMP_REDUCED_TITLE, null);
    public static final TitleProvider voiceFilterTitle = new TitleProvider.StaticTitleProvider(VOICE_FILTER_TITLE, VOICE_FILTER_REDUCED_TITLE, null);
    public static final TitleProvider voiceLfoTitle = new TitleProvider.StaticTitleProvider(VOICE_LFO_TITLE, VOICE_LFO_REDUCED_TITLE, null);
    public static final TitleProvider voiceTuningTitle = new TitleProvider.StaticTitleProvider(VOICE_TUNING_TITLE, VOICE_TUNING_REDUCED_TITLE, null);
    public static final TitleProvider voiceAmpEnvelopeTitle = new TitleProvider.StaticTitleProvider(VOICE_AMP_ENVELOPE_TITLE, VOICE_AMP_ENVELOPE_REDUCED_TITLE, null);
    public static final TitleProvider voiceFilterEnvelopeTitle = new TitleProvider.StaticTitleProvider(VOICE_FILTER_ENVELOPE_TITLE, VOICE_FILTER_ENVELOPE_REDUCED_TITLE, null);
    public static final TitleProvider voiceAuxEnvelopeTitle = new TitleProvider.StaticTitleProvider(VOICE_AUX_ENVELOPE_TITLE, VOICE_AUX_ENVELOPE_REDUCED_TITLE, null);
    public static final TitleProvider voiceCordsTitle = new TitleProvider.StaticTitleProvider(VOICE_CORDS_TITLE, VOICE_CORDS_REDUCED_TITLE, null);
    public static final TitleProvider voiceEnvelopesTitle = new TitleProvider.StaticTitleProvider(VOICE_ENVELOPES_TITLE, VOICE_ENVELOPES_REDUCED_TITLE, null);
    public static final TitleProvider voiceAmpFilterTitle = new TitleProvider.StaticTitleProvider(VOICE_AMP_FILTER_TITLE, VOICE_AMP_FILTER_REDUCED_TITLE, null);
    public static final TitleProvider voiceEditTitle = new TitleProvider.StaticTitleProvider(VOICE_EDIT_TITLE, VOICE_EDIT_REDUCED_TITLE, null);
    
    public static String makeEnumeratedSectionString(int sections) {
        String s = "";
        if ((sections & VoiceSections.VOICE_CORDS) != 0)
            s += ZUtilities.STRING_FIELD_SEPERATOR + VOICE_CORDS_TITLE;
        if ((sections & VoiceSections.VOICE_AMP) != 0)
            s += ZUtilities.STRING_FIELD_SEPERATOR + VOICE_AMP_TITLE;
        if ((sections & VoiceSections.VOICE_FILTER) != 0)
            s += ZUtilities.STRING_FIELD_SEPERATOR + VOICE_FILTER_TITLE;
        if ((sections & VoiceSections.VOICE_LFO) != 0)
            s += ZUtilities.STRING_FIELD_SEPERATOR + VOICE_LFO_TITLE;
        if ((sections & VoiceSections.VOICE_TUNING) != 0)
            s += ZUtilities.STRING_FIELD_SEPERATOR + VOICE_TUNING_TITLE;
        if ((sections & VoiceSections.VOICE_AMP_ENVELOPE) != 0)
            s += ZUtilities.STRING_FIELD_SEPERATOR + VOICE_AMP_ENVELOPE_TITLE;
        if ((sections & VoiceSections.VOICE_FILTER_ENVELOPE) != 0)
            s += ZUtilities.STRING_FIELD_SEPERATOR + VOICE_FILTER_ENVELOPE_TITLE;
        if ((sections & VoiceSections.VOICE_AUX_ENVELOPE) != 0)
            s += ZUtilities.STRING_FIELD_SEPERATOR + VOICE_AUX_ENVELOPE_TITLE;
        if ((sections & VoiceSections.VOICE_ENVELOPES) != 0)
            s += ZUtilities.STRING_FIELD_SEPERATOR + VOICE_ENVELOPES_TITLE;
        if ((sections & VoiceSections.VOICE_AMP_FILTER) != 0)
            s += ZUtilities.STRING_FIELD_SEPERATOR + VOICE_AMP_FILTER_TITLE;
        return s;
    }
}
