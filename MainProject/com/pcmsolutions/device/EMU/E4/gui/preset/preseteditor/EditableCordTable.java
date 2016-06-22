package com.pcmsolutions.device.EMU.E4.gui.preset.preseteditor;

import com.pcmsolutions.device.EMU.E4.gui.ParameterModelUtilities;
import com.pcmsolutions.device.EMU.E4.gui.preset.presetviewer.CordTable;
import com.pcmsolutions.device.EMU.E4.parameter.EditableParameterModel;
import com.pcmsolutions.device.EMU.E4.preset.ContextEditablePreset;
import com.pcmsolutions.device.EMU.E4.selections.CordParameterSelection;
import com.pcmsolutions.device.EMU.E4.selections.VoiceParameterSelection;
import com.pcmsolutions.system.threads.Impl_ZThread;

/**
 * Created by IntelliJ IDEA.
 * User: pmeehan
 * Date: 28-Jun-2003
 * Time: 22:20:30
 * To change this template use Options | File Templates.
 */
public class EditableCordTable extends CordTable implements VoiceParameterSelectionAcceptor {
    protected ContextEditablePreset.EditableVoice[] voices;

    public EditableCordTable(ContextEditablePreset.EditableVoice[] voices, EditableParameterModel[] parameterModels, String title)  {
        this(voices, new EditableCordTableModel(parameterModels), title);
    }

    public EditableCordTable(ContextEditablePreset.EditableVoice[] voices, EditableCordTableModel tm, String title) {
        super(voices[0], tm, title);
        this.voices = voices;
        ParameterModelUtilities.registerTableForEditableParameterModelShortcuts(this);
        setDropChecker(defaultDropGridChecker);
    }

    @Override
    public void setSelection(final VoiceParameterSelection sel) {
      //  Impl_ZThread.ddTQ.postTask(new Impl_ZThread.Task(){
       //     public void doTask() {
                if (sel instanceof CordParameterSelection) {
                    ((CordParameterSelection) sel).render(voices, getSelectedRow(), true);
                } else {
                    sel.render(voices);
                }
      //      }
      //  });
    }

    @Override
    public boolean willAcceptCategory(int category) {
        return category == VoiceParameterSelection.VOICE_CORDS;
    }

    @Override
    public ContextEditablePreset.EditableVoice getEditableVoice() {
        return (ContextEditablePreset.EditableVoice) voices[0];
    }
}



