package com.pcmsolutions.device.EMU.E4;

import com.pcmsolutions.device.EMU.E4.gui.TitleProvider;
import com.pcmsolutions.device.EMU.E4.preset.ContextEditablePreset;
import com.pcmsolutions.device.EMU.E4.preset.PresetException;
import com.pcmsolutions.device.EMU.E4.preset.ReadablePreset;
import com.pcmsolutions.device.EMU.database.EmptyException;
import com.pcmsolutions.gui.ComponentGenerationException;
import com.pcmsolutions.gui.UserMessaging;
import com.pcmsolutions.gui.ZoeosFrame;
import com.pcmsolutions.gui.desktop.*;
import com.pcmsolutions.system.IllegalStateTransitionException;
import com.pcmsolutions.system.ZDeviceCannotBeRemovedException;
import com.pcmsolutions.system.ZExternalDevice;
import com.pcmsolutions.system.callback.CallbackTask;
import com.pcmsolutions.system.callback.CallbackTaskResult;
import com.pcmsolutions.system.callback.CallbackTaskRunnable;
import com.pcmsolutions.system.paths.ViewPath;
import com.pcmsolutions.system.tasking.ManageableTicketedQ;
import com.pcmsolutions.system.tasking.QueueFactory;
import com.pcmsolutions.system.tasking.ResourceUnavailableException;
import com.pcmsolutions.system.tasking.TicketRunnable;

import javax.swing.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * User: paulmeehan Date: 20-Jan-2004 Time: 10:20:29
 */
class ViewMediator {

    private final static ManageableTicketedQ viewMediatorQ = QueueFactory.createTicketedQueue(ViewMediator.class.getClass(), "viewMediatorQ", 6);

    static {
        viewMediatorQ.start();
    }

    public static interface PresetIndexProvider {

        Integer getPresetIndex();
    }

    public static interface ViewTask {

        public CallbackTaskResult open(boolean activate);

        public CallbackTaskResult open();

        public CallbackTaskResult close();
    }

    private static abstract class AbstractViewTask extends ViewCallbackTask implements ViewTask {

        private DesktopElement[] desktopElements;

        public DesktopElement[] getDesktopElements() {
            return (DesktopElement[]) desktopElements.clone();
        }

        public DesktopElement getFirstDesktopElement() {
            return desktopElements[0];
        }

        public void init(DesktopElement desktopElement) {
            this.desktopElements = new DesktopElement[]{desktopElement};
        }

        public void init(DesktopElement[] desktopElements) {
            this.desktopElements = desktopElements;
        }

        // result is Boolean - false for view already assertOpen, true for view opened
        public CallbackTaskResult open(boolean activate) {
            return openViews(desktopElements, (activate ? 0 : -1));
        }

        // result is Boolean - false for view already assertOpen, true for view opened
        public CallbackTaskResult open() {
            return openViews(desktopElements, 0);
        }

        public CallbackTaskResult close() {
            return closeViews(desktopElements);
        }
    }

    private static class DeviceCloseBehaviour implements ActivityContext, Serializable {

        private DeviceContext device;

        public DeviceCloseBehaviour(DeviceContext device) {
            this.device = device;
        }

        public boolean tryClosing() {
            try {
                viewMediatorQ.getPostableTicket(new TicketRunnable() {
                    public void run() throws Exception {
                        try {
                            if (device.getState() != ZExternalDevice.STATE_REMOVED) {
                                if (UserMessaging.askYesNo("Remove device " + device.getName() + " ?")) {
                                    if (device.getState() == ZExternalDevice.STATE_RUNNING) {
                                        device.stopDevice(true, "User request");
                                    }
                                    device.removeDevice(true);
                                }
                            }
                        } catch (IllegalStateTransitionException e1) {
                            e1.printStackTrace();
                        } catch (ZDeviceCannotBeRemovedException e1) {
                            e1.printStackTrace();
                        }
                    }
                }, "tryClosing").post();
            } catch (ResourceUnavailableException e) {
                e.printStackTrace();
            }
            return false;
        }

        public void sendMessage(String msg) {
        }

        public boolean testCondition(String condition) {
            return false;
        }

        public void closed() {
        }

        public void activated() {
        }

        public void deactivated() {
        }
    }

    public static class TaskDeviceWorkspace extends AbstractViewTask {

        public TaskDeviceWorkspace(final DeviceContext device) {
            init(makeDeviceWorkspaceDesktopElement(device, ViewFactory.provideDefaultView(device),
                    new DeviceCloseBehaviour(device), new Impl_DesktopNodeDescriptor(new Object[]{device}, false)));
        }
    }

    public static class PresetDesktopElement extends DeviceDesktopElement implements PresetIndexProvider {

        Integer preset;

        public PresetDesktopElement(Integer preset, ViewInstance vi, ActivityContext activityContext, DesktopNodeDescriptor nodalDescriptor, DeviceContext device) {
            super(vi, activityContext, nodalDescriptor, device);
            this.preset = preset;
        }

        public Integer getPresetIndex() {
            return preset;
        }

        public DesktopElement getCopy() {
            retrieveComponentSessionString();
            PresetDesktopElement clone = new PresetDesktopElement(preset, viewInstance, activityContext, nodalDescriptor, device);
            clone.sessionString = this.sessionString;
            clone.componentSessionString = componentSessionString;
            return clone;
        }
    }

    public static class TaskPreset extends AbstractViewTask {

        private static final String asChildTitle = "Main";
        private static final String asChildReducedTitle = "Main";
        private TaskPresetUser presetUserTask;

        static class PresetActivityContext implements ActivityContext, Serializable {

            ReadablePreset preset;
            DesktopElement de;

            public PresetActivityContext(ReadablePreset p) {
                this.preset = p;
            }

            public boolean tryClosing() {
                return true;
            }

            void closePresetView() {
                try {
                    viewMediatorQ.getPostableTicket(new TicketRunnable() {
                        public void run() throws Exception {
                            closeDesktopElements(new DesktopElement[]{de});
                        }
                    }, "closeDesktopElements").post();
                } catch (ResourceUnavailableException e) {
                    e.printStackTrace();
                }
            }

            public void sendMessage(String msg) {
                if (msg.equals(ViewMessaging.MSG_CLOSE_PRESET_EMPTY) || msg.equals(ViewMessaging.MSG_BROADCAST_CLOSE_EMPTY)) {
                    try {
                        if (preset.isEmpty()) {
                            closePresetView();
                        }
                    } catch (PresetException e) {
                        e.printStackTrace();
                    }
                } else if (msg.equals(ViewMessaging.MSG_CLOSE_PRESET_FLASH) && !preset.isUser()) {
                    closePresetView();
                } else if (msg.equals(ViewMessaging.MSG_CLOSE_PRESET_USER) && preset.isUser()) {
                    closePresetView();
                }
            }

            public boolean testCondition(String condition) {
                return condition.startsWith(ViewMessaging.CONDITION_IS_OPEN_PRESET);
            }

            public void closed() {
            }

            public void activated() {
            }

            public void deactivated() {
            }
        }

        public TaskPreset(final ContextEditablePreset p) {
            presetUserTask = new TaskPresetUser(p);
            PresetActivityContext pac = new PresetActivityContext(p);
            /*
            init(new DesktopElement[]{makeDeviceWorkspaceDesktopElement(p.getDeviceContext(), ViewFactory.provideDefaultView(p),
                    false, pac, new Impl_DesktopNodeDescriptor(new Object[]{p}, asChildTitle, asChildReducedTitle)), presetUserTask.getFirstDesktopElement()});
             */
            init(new DesktopElement[]{new PresetDesktopElement(p.getPreset(), ViewFactory.provideDefaultView(p), pac, new Impl_DesktopNodeDescriptor(new Object[]{p}, asChildTitle, asChildReducedTitle), p.getDeviceContext()), presetUserTask.getFirstDesktopElement()});
            pac.de = getFirstDesktopElement();
        }

        public TaskPreset(final ReadablePreset p) {
            presetUserTask = new TaskPresetUser(p);
            PresetActivityContext pac = new PresetActivityContext(p);
            /*
            init(new DesktopElement[]{makeDeviceWorkspaceDesktopElement(p.getDeviceContext(), ViewFactory.provideDefaultView(p),
                    false, pac, new Impl_DesktopNodeDescriptor(new Object[]{p}, asChildTitle, asChildReducedTitle)), presetUserTask.getFirstDesktopElement()});
             */
            init(new DesktopElement[]{new PresetDesktopElement(p.getPreset(), ViewFactory.provideDefaultView(p), pac, new Impl_DesktopNodeDescriptor(new Object[]{p}, asChildTitle, asChildReducedTitle), p.getDeviceContext()), presetUserTask.getFirstDesktopElement()});
            pac.de = getFirstDesktopElement();
        }
    }

    private static class TaskPresetUser extends AbstractViewTask {
        //private static final String asChildTitle = "Main";
        //private static final String asChildReducedTitle = "Main";

        static class PresetUserActivityContext implements ActivityContext, Serializable {

            ReadablePreset p;
            DesktopElement de;

            public PresetUserActivityContext(ReadablePreset p) {
                this.p = p;
            }

            public boolean tryClosing() {
                return false;
            }

            void closePresetView() {
                try {
                    viewMediatorQ.getPostableTicket(new TicketRunnable() {
                        public void run() throws Exception {
                            closeDesktopElements(new DesktopElement[]{de});

                        }
                    }, "closeDesktopElements").post();
                } catch (ResourceUnavailableException e) {
                    e.printStackTrace();
                }
            }

            public void sendMessage(String msg) {
                if (msg.equals(ViewMessaging.MSG_CLOSE_PRESET_EMPTY) || msg.equals(ViewMessaging.MSG_BROADCAST_CLOSE_EMPTY)) {
                    try {
                        if (p.isEmpty()) {
                            closePresetView();
                        }
                    } catch (PresetException e) {
                        e.printStackTrace();
                    }
                } else if (msg.equals(ViewMessaging.MSG_CLOSE_PRESET_FLASH) && !p.isUser()) {
                    closePresetView();
                } else if (msg.equals(ViewMessaging.MSG_CLOSE_PRESET_USER) && p.isUser()) {
                    closePresetView();
                }
            }

            public boolean testCondition(String condition) {
                return false;
            }

            public void closed() {
            }

            public void activated() {
            }

            public void deactivated() {
            }
        }

        public TaskPresetUser(final ContextEditablePreset p) {
            PresetUserActivityContext pac = new PresetUserActivityContext(p);
            /*
            init(makeDeviceWorkspaceDesktopElement(p.getDeviceContext(), ViewFactory.provideUserView(p),
                    false, pac, new Impl_DesktopNodeDescriptor(new Object[]{p}, true)));
             */
            init(new DesktopElement[]{new PresetDesktopElement(p.getPreset(), ViewFactory.provideUserView(p), pac, new Impl_DesktopNodeDescriptor(new Object[]{p}, true), p.getDeviceContext())});
            pac.de = getFirstDesktopElement();
        }

        public TaskPresetUser(final ReadablePreset p) {
            PresetUserActivityContext pac = new PresetUserActivityContext(p);

            //init(makeDeviceWorkspaceDesktopElement(p.getDeviceContext(), ViewFactory.provideUserView(p),
            //        pac, new Impl_DesktopNodeDescriptor(new Object[]{p}, true)));
            init(new DesktopElement[]{new PresetDesktopElement(p.getPreset(), ViewFactory.provideUserView(p), pac, new Impl_DesktopNodeDescriptor(new Object[]{p}, true), p.getDeviceContext())});
            pac.de = getFirstDesktopElement();
        }
    }

    public static class TabbedTaskVoice implements ViewTask {

        private TaskVoice[] tasks;

        public TabbedTaskVoice(final ReadablePreset.ReadableVoice voice, boolean groupEnvelopes) {
            tasks = new TaskVoice[2];
            tasks[0] = new TaskVoice(voice, true);
            tasks[1] = new TaskVoice(voice, VoiceSections.VOICE_EDIT, VoiceSections.voiceEditTitle);
            /*
            if (groupEnvelopes) {
                tasks = new TaskVoice[6];
                tasks[0] = new TaskVoice(voice, true);
                tasks[1] = new TaskVoice(voice, VoiceSections.VOICE_TUNING, VoiceSections.voiceTuningTitle);
                tasks[2] = new TaskVoice(voice, VoiceSections.VOICE_AMP_FILTER, VoiceSections.voiceAmpFilterTitle);
                //tasks[2] = new TaskVoice(voice, VoiceSections.VOICE_FILTER, VoiceSections.voiceFilterTitle);
                tasks[3] = new TaskVoice(voice, VoiceSections.VOICE_ENVELOPES, VoiceSections.voiceEnvelopesTitle);
                tasks[4] = new TaskVoice(voice, VoiceSections.VOICE_LFO, VoiceSections.voiceLfoTitle);
                tasks[5] = new TaskVoice(voice, VoiceSections.VOICE_CORDS, VoiceSections.voiceCordsTitle);
            } else {
                tasks = new TaskVoice[8];
                tasks[0] = new TaskVoice(voice, true);
                tasks[1] = new TaskVoice(voice, VoiceSections.VOICE_TUNING, VoiceSections.voiceTuningTitle);
                tasks[2] = new TaskVoice(voice, VoiceSections.VOICE_AMP_FILTER, VoiceSections.voiceAmpFilterTitle);
                //tasks[2] = new TaskVoice(voice, VoiceSections.VOICE_FILTER, VoiceSections.voiceFilterTitle);
                tasks[3] = new TaskVoice(voice, VoiceSections.VOICE_AMP_ENVELOPE, VoiceSections.voiceAmpEnvelopeTitle);
                tasks[4] = new TaskVoice(voice, VoiceSections.VOICE_FILTER_ENVELOPE, VoiceSections.voiceFilterEnvelopeTitle);
                tasks[5] = new TaskVoice(voice, VoiceSections.VOICE_AUX_ENVELOPE, VoiceSections.voiceAuxEnvelopeTitle);
                tasks[6] = new TaskVoice(voice, VoiceSections.VOICE_LFO, VoiceSections.voiceLfoTitle);
                tasks[7] = new TaskVoice(voice, VoiceSections.VOICE_CORDS, VoiceSections.voiceCordsTitle);
            }
             */
        }

        public TabbedTaskVoice(final ContextEditablePreset.EditableVoice voice, boolean groupEnvelopes) {
            this(new ContextEditablePreset.EditableVoice[]{voice}, groupEnvelopes);
        }

        public TabbedTaskVoice(final ContextEditablePreset.EditableVoice[] voices, boolean groupEnvelopes) {
            tasks = new TaskVoice[2];
            tasks[0] = new TaskVoice(voices, true);
            tasks[1] = new TaskVoice(voices, VoiceSections.VOICE_EDIT, VoiceSections.voiceEditTitle);
            /*
            if (groupEnvelopes) {
                tasks = new TaskVoice[6];
                tasks[0] = new TaskVoice(voices, true);
                tasks[1] = new TaskVoice(voices, VoiceSections.VOICE_TUNING, VoiceSections.voiceTuningTitle);
                tasks[2] = new TaskVoice(voices, VoiceSections.VOICE_AMP_FILTER, VoiceSections.voiceAmpFilterTitle);
                //tasks[2] = new TaskVoice(voices, VoiceSections.VOICE_FILTER, VoiceSections.voiceFilterTitle);
                tasks[3] = new TaskVoice(voices, VoiceSections.VOICE_ENVELOPES, VoiceSections.voiceEnvelopesTitle);
                tasks[4] = new TaskVoice(voices, VoiceSections.VOICE_LFO, VoiceSections.voiceLfoTitle);
                tasks[5] = new TaskVoice(voices, VoiceSections.VOICE_CORDS, VoiceSections.voiceCordsTitle);
            } else {
                tasks = new TaskVoice[8];
                tasks[0] = new TaskVoice(voices, true);
                tasks[1] = new TaskVoice(voices, VoiceSections.VOICE_TUNING, VoiceSections.voiceTuningTitle);
                tasks[2] = new TaskVoice(voices, VoiceSections.VOICE_AMP_FILTER, VoiceSections.voiceAmpFilterTitle);
                //tasks[2] = new TaskVoice(voices, VoiceSections.VOICE_FILTER, VoiceSections.voiceFilterTitle);
                tasks[3] = new TaskVoice(voices, VoiceSections.VOICE_AMP_ENVELOPE, VoiceSections.voiceAmpEnvelopeTitle);
                tasks[4] = new TaskVoice(voices, VoiceSections.VOICE_FILTER_ENVELOPE, VoiceSections.voiceFilterEnvelopeTitle);
                tasks[5] = new TaskVoice(voices, VoiceSections.VOICE_AUX_ENVELOPE, VoiceSections.voiceAuxEnvelopeTitle);
                tasks[6] = new TaskVoice(voices, VoiceSections.VOICE_LFO, VoiceSections.voiceLfoTitle);
                tasks[7] = new TaskVoice(voices, VoiceSections.VOICE_CORDS, VoiceSections.voiceCordsTitle);
            }
             */
        }

        public CallbackTaskResult open(boolean activate) {
            CallbackTaskResult rv = null;
            for (int i = 0; i < tasks.length; i++) {
                if (i == 0) {
                    rv = tasks[i].open(activate);
                    if (!rv.suceeded() || (rv.suceeded() && !((Boolean) rv.getResult()).booleanValue())) {
                        return rv;
                    }
                } else if (i == 1) {
                    rv = tasks[i].open(activate);
                } else {
                    rv = tasks[i].open(false);
                }
            }
            return rv;
        }

        public CallbackTaskResult open() {
            return open(false);
        }

        public CallbackTaskResult close() {
            CallbackTaskResult rv = null;
            for (int i = 0; i < tasks.length; i++) {
                rv = tasks[i].close();
            }
            return rv;
        }
    }

    public static class TaskVoice extends AbstractViewTask {

        static class VoiceActivityContext implements ActivityContext, Serializable {

            ReadablePreset.ReadableVoice[] voices;
            DesktopElement de;

            public VoiceActivityContext(ReadablePreset.ReadableVoice[] voices) {
                this.voices = voices;
            }

            public VoiceActivityContext(ReadablePreset.ReadableVoice voice) {
                this.voices = new ReadablePreset.ReadableVoice[]{voice};
            }

            public boolean tryClosing() {
                return true;
            }

            void closeVoiceView() {
                try {
                    viewMediatorQ.getPostableTicket(new TicketRunnable() {
                        public void run() throws Exception {
                            closeDesktopElements(new DesktopElement[]{de});
                        }
                    }, "closeDesktopElements").post();
                } catch (ResourceUnavailableException e) {
                    e.printStackTrace();
                }
            }

            boolean checkVoices() {
                try {
                    for (int i = 0; i < voices.length; i++) {
                        if (voices[i].getVoiceNumber().intValue() > voices[i].getPreset().numVoices() - 1) {
                            return false;
                        }
                    }
                    return true;
                } catch (EmptyException e) {
                    e.printStackTrace();
                } catch (PresetException e) {
                    e.printStackTrace();
                }
                return false;
            }

            public void sendMessage(String msg) {
                if (msg.equals(ViewMessaging.MSG_CLOSE_VOICE_EMPTY) || msg.equals(ViewMessaging.MSG_BROADCAST_CLOSE_EMPTY)) {
                    if (!checkVoices()) {
                        closeVoiceView();
                    }
                } else if (msg.equals(ViewMessaging.MSG_CLOSE_VOICE)) {
                    closeVoiceView();
                }
            }

            public boolean testCondition(String condition) {
                return false;
            }

            public void closed() {
            }

            public void activated() {
            }

            public void deactivated() {
            }
        }

        protected TaskVoice(final ReadablePreset.ReadableVoice voice, boolean empty) {
            Impl_DesktopNodeDescriptor nd = new Impl_DesktopNodeDescriptor(false);
            VoiceActivityContext vac = new VoiceActivityContext(voice);
            nd.setShowingCloseButton(false);
            init(makeDeviceWorkspaceDesktopElement(voice.getPreset().getDeviceContext(), ViewFactory.provideDefaultView(voice, empty),
                    vac,
                    nd));
            vac.de = getFirstDesktopElement();
        }

        public TaskVoice(final ReadablePreset.ReadableVoice voice) {
            this(voice, false);
        }

        public TaskVoice(final ReadablePreset.ReadableVoice voice, final int sections, final TitleProvider tp) {
            //VoiceActivityContext vac = new VoiceActivityContext(voice);
            init(makeDeviceWorkspaceDesktopElement(voice.getPreset().getDeviceContext(), ViewFactory.provideDefaultView(voice, sections, tp),
                    StaticActivityContext.FALSE,
                    new Impl_DesktopNodeDescriptor(false)));
            //vac.de =desktopElement;
        }

        public TaskVoice(final ContextEditablePreset.EditableVoice voice) {
            this(voice, false);
        }

        protected TaskVoice(final ContextEditablePreset.EditableVoice voice, boolean empty) {
            this(new ContextEditablePreset.EditableVoice[]{voice}, empty);
        }

        public TaskVoice(final ContextEditablePreset.EditableVoice voice, int sections, TitleProvider tp) {
            this(new ContextEditablePreset.EditableVoice[]{voice}, sections, tp);
        }

        public TaskVoice(final ContextEditablePreset.EditableVoice[] voices, final int sections, final TitleProvider tp) {
            init(makeDeviceWorkspaceDesktopElement(voices[0].getPreset().getDeviceContext(), ViewFactory.provideDefaultView(voices, sections, tp),
                    StaticActivityContext.FALSE,
                    new Impl_DesktopNodeDescriptor(false)));
        }

        public TaskVoice(final ContextEditablePreset.EditableVoice[] voices) {
            this(voices, false);
        }

        protected TaskVoice(final ContextEditablePreset.EditableVoice[] voices, boolean empty) {
            Impl_DesktopNodeDescriptor nd = new Impl_DesktopNodeDescriptor(false);
            VoiceActivityContext vac = new VoiceActivityContext(voices);
            nd.setShowingCloseButton(false);
            init(makeDeviceWorkspaceDesktopElement(voices[0].getPreset().getDeviceContext(), ViewFactory.provideDefaultView(voices, empty),
                    vac,
                    nd));
            vac.de = getFirstDesktopElement();
        }
    }

    public static class TaskDevice extends AbstractViewTask {

        public TaskDevice(final DeviceContext device) {
            init(makeDeviceDesktopElement(device, ViewFactory.provideDeviceView(device),
                    StaticActivityContext.FALSE, new Impl_DesktopNodeDescriptor(false)));
        }
    }

    public static class TaskProperties extends AbstractViewTask {

        public TaskProperties(final DeviceContext device) {
            init(makeDeviceDesktopElement(device, ViewFactory.providePropertiesView(device),
                    StaticActivityContext.FALSE, new Impl_DesktopNodeDescriptor(false)));
        }
    }

    public static class TaskPiano extends AbstractViewTask {

        public TaskPiano(final DeviceContext device) {
            init(makeDeviceDesktopElement(device, ViewFactory.providePianoView(device),
                    StaticActivityContext.FALSE, new Impl_DesktopNodeDescriptor(false)));
        }
    }

    public static class TaskDefaultPresetContext extends AbstractViewTask {

        static class PresetContextActivityContext implements ActivityContext {

            DesktopElement de;

            public boolean tryClosing() {
                return false;
            }

            public void sendMessage(String msg) {
                if (msg.startsWith(ViewMessaging.MSG_PRESET_CONTEXT_PREFIX)) {
                    try {
                        if (de.isComponentGenerated() && de.getComponent() instanceof ViewMessageReceiver) {
                            ((ViewMessageReceiver) de.getComponent()).receiveMessage(msg);
                        }
                    } catch (ComponentGenerationException e) {
                        e.printStackTrace();
                    }
                }
            }

            public boolean testCondition(String condition) {
                return false;
            }

            public void closed() {
            }

            public void activated() {
            }

            public void deactivated() {
            }
        };

        public TaskDefaultPresetContext(final DeviceContext device) {
            PresetContextActivityContext ac = new PresetContextActivityContext();
            init(makeDeviceDesktopElement(device, ViewFactory.provideDefaultPresetContextView(device),
                    ac, new Impl_DesktopNodeDescriptor(false)));
            ac.de = getFirstDesktopElement();
        }
    }

    public static class TaskDefaultSampleContext extends AbstractViewTask {

        static class SampleContextActivityContext implements ActivityContext {

            DesktopElement de;

            public boolean tryClosing() {
                return false;
            }

            public void sendMessage(String msg) {
                if (msg.startsWith(ViewMessaging.MSG_SAMPLE_CONTEXT_PREFIX)) {
                    try {
                        if (de.isComponentGenerated() && de.getComponent() instanceof ViewMessageReceiver) {
                            ((ViewMessageReceiver) de.getComponent()).receiveMessage(msg);
                        }
                    } catch (ComponentGenerationException e) {
                        e.printStackTrace();
                    }
                }
            }

            public boolean testCondition(String condition) {
                return false;
            }

            public void closed() {
            }

            public void activated() {
            }

            public void deactivated() {
            }
        };

        public TaskDefaultSampleContext(final DeviceContext device) {
            SampleContextActivityContext ac = new SampleContextActivityContext();
            init(makeDeviceDesktopElement(device, ViewFactory.provideDefaultSampleContextView(device),
                    ac, new Impl_DesktopNodeDescriptor(false)));
            ac.de = getFirstDesktopElement();
        }
    }

    public static class TaskMultiMode extends AbstractViewTask {

        public TaskMultiMode(final DeviceContext device) {
            init(makeDeviceDesktopElement(device, ViewFactory.provideMultiModeView(device),
                    StaticActivityContext.FALSE, new Impl_DesktopNodeDescriptor(false)));
        }
    }

    public static class TaskMaster extends AbstractViewTask {

        public TaskMaster(DeviceContext device) {
            init(makeDeviceDesktopElement(device, ViewFactory.provideMasterView(device),
                    StaticActivityContext.FALSE, new Impl_DesktopNodeDescriptor(false)));
        }
    }

    public static void openDesktopElements(final DesktopElement[] elems) {
        CallbackTask ct = new CallbackTask();
        for (int i = 0; i < elems.length; i++) {
            final DesktopElement elem = elems[i];
            ct.init(new CallbackTaskRunnable() {
                public Object run() throws ComponentGenerationException, ChildViewNotAllowedException, LogicalHierarchyException {
                    return new Boolean(ZoeosFrame.getInstance().getZDesktopManager().addDesktopElement(elem));
                }
            });
            CallbackTaskResult ctr = ct.execute();
        }
    }

    public static void closeDesktopElements(final DesktopElement[] elems) {
        CallbackTask ct = new CallbackTask();
        for (int i = elems.length - 1; i >= 0; i--) {
            final DesktopElement elem = elems[i];
            ct.init(new CallbackTaskRunnable() {
                public Object run() {
                    return new Boolean(ZoeosFrame.getInstance().getZDesktopManager().removeDesktopElement(elem));
                }
            });
            CallbackTaskResult ctr = ct.execute();
        }
    }

    public static void sendWorkspaceMessage(final DeviceContext device, final String msg) throws Exception {
        executeViewTask(new CallbackTaskRunnable() {
            public Object run() throws Exception {
                ZoeosFrame.getInstance().getZDesktopManager().sendMessage(new ViewPath(ZDesktopManager.dockWORKSPACE, ViewFactory.provideDefaultDesktopNames(device)), msg);
                return null;
            }
        });
    }

    public static DesktopElement[] evaluateWorkspaceCondition(final DeviceContext device, final String condition) throws Exception {
        return (DesktopElement[]) executeViewTask(new CallbackTaskRunnable() {
            public Object run() throws Exception {
                return ZoeosFrame.getInstance().getZDesktopManager().evaluateCondition(new ViewPath(ZDesktopManager.dockWORKSPACE, ViewFactory.provideDefaultDesktopNames(device)), condition);
            }
        });
    }

    public static void sendPresetContextMessage(final DeviceContext device, final String msg) throws Exception {
        executeViewTask(new CallbackTaskRunnable() {
            public Object run() throws Exception {
                ZoeosFrame.getInstance().getZDesktopManager().sendMessage(ViewFactory.provideDefaultPresetContextView(device).getViewPath(), msg);
                return null;
            }
        });
    }

    public static void sendSampleContextMessage(final DeviceContext device, final String msg) throws Exception {
        executeViewTask(new CallbackTaskRunnable() {
            public Object run() throws Exception {
                ZoeosFrame.getInstance().getZDesktopManager().sendMessage(ViewFactory.provideDefaultSampleContextView(device).getViewPath(), msg);
                return null;
            }
        });
    }

    public static void modifyBranch(final DesktopBranch branch, final boolean activate, final int clipIndex) {
        CallbackTask ct = new CallbackTask();
        ct.init(new CallbackTaskRunnable() {
            public Object run() throws ComponentGenerationException, ChildViewNotAllowedException, LogicalHierarchyException {
                ZoeosFrame.getInstance().getZDesktopManager().modifyBranch(branch, activate, clipIndex);
                return null;
            }
        });
        CallbackTaskResult ctr = ct.execute();
    }

    public static Object executeViewTask(CallbackTaskRunnable ctr) throws Exception {
        CallbackTask ct = new CallbackTask();
        ct.init(ctr);
        CallbackTaskResult ctres = ct.execute();
        if (ctres.suceeded()) {
            return ctres.getResult();
        } else {
            throw ctres.getException();
        }
    }

    public static DeviceDesktopElement[] getAllDeviceDesktopElements(final DeviceContext device) throws Exception {
        DeviceDesktopElement[] wsElems = getDeviceWorkspaceDesktopElements(device);
        DeviceDesktopElement[] dockElems = getDeviceDockableDesktopElements(device);
        DeviceDesktopElement[] elems = new DeviceDesktopElement[wsElems.length + dockElems.length];
        System.arraycopy(wsElems, 0, elems, 0, wsElems.length);
        System.arraycopy(dockElems, 0, elems, wsElems.length, dockElems.length);
        return elems;
    }

    public static boolean hasWorkspaceElements(final DeviceContext device) throws Exception {
        return getDeviceWorkspaceDesktopElements(device).length > 1;
    }

    public static DeviceDesktopElement[] getDeviceWorkspaceDesktopElements(final DeviceContext device) throws Exception {
        DesktopElement[] e = (DesktopElement[]) executeViewTask(new CallbackTaskRunnable() {
            public Object run() throws Exception {
                return ZoeosFrame.getInstance().getZDesktopManager().getDesktopElementTree(new ViewPath(ZDesktopManager.dockWORKSPACE, ViewFactory.provideDefaultDesktopNames(device)));
            }
        });
        DeviceDesktopElement[] de = new DeviceDesktopElement[e.length];
        System.arraycopy(e, 0, de, 0, e.length);
        return de;
    }

    public static DeviceDesktopElement[] getDeviceDockableDesktopElements(final DeviceContext device) throws Exception {
        ArrayList<DeviceDesktopElement> elems = new ArrayList<DeviceDesktopElement>();

        DesktopElement[] pce;
        DeviceDesktopElement[] de;

        pce = (DesktopElement[]) executeViewTask(new CallbackTaskRunnable() {
            public Object run() throws Exception {
                return ZoeosFrame.getInstance().getZDesktopManager().getDesktopElementTree(new ViewPath(ZDesktopManager.dockPRESETS, ViewFactory.provideDefaultPresetContextView(device).getDesktopName()));
            }
        });
        de = new DeviceDesktopElement[pce.length];
        System.arraycopy(pce, 0, de, 0, pce.length);
        elems.addAll(Arrays.asList(de));

        pce = (DesktopElement[]) executeViewTask(new CallbackTaskRunnable() {
            public Object run() throws Exception {
                return ZoeosFrame.getInstance().getZDesktopManager().getDesktopElementTree(new ViewPath(ZDesktopManager.dockSAMPLES, ViewFactory.provideDefaultSampleContextView(device).getDesktopName()));
            }
        });
        de = new DeviceDesktopElement[pce.length];
        System.arraycopy(pce, 0, de, 0, pce.length);
        elems.addAll(Arrays.asList(de));

        pce = (DesktopElement[]) executeViewTask(new CallbackTaskRunnable() {
            public Object run() throws Exception {
                return ZoeosFrame.getInstance().getZDesktopManager().getDesktopElementTree(new ViewPath(ZDesktopManager.dockMASTER, ViewFactory.provideMasterView(device).getDesktopName()));
            }
        });
        de = new DeviceDesktopElement[pce.length];
        System.arraycopy(pce, 0, de, 0, pce.length);
        elems.addAll(Arrays.asList(de));

        pce = (DesktopElement[]) executeViewTask(new CallbackTaskRunnable() {
            public Object run() throws Exception {
                return ZoeosFrame.getInstance().getZDesktopManager().getDesktopElementTree(new ViewPath(ZDesktopManager.dockMULTI, ViewFactory.provideMultiModeView(device).getDesktopName()));
            }
        });
        de = new DeviceDesktopElement[pce.length];
        System.arraycopy(pce, 0, de, 0, pce.length);
        elems.addAll(Arrays.asList(de));

        pce = (DesktopElement[]) executeViewTask(new CallbackTaskRunnable() {
            public Object run() throws Exception {
                return ZoeosFrame.getInstance().getZDesktopManager().getDesktopElementTree(new ViewPath(ZDesktopManager.dockDEVICES, ViewFactory.provideDeviceView(device).getDesktopName()));
            }
        });
        de = new DeviceDesktopElement[pce.length];
        System.arraycopy(pce, 0, de, 0, pce.length);
        elems.addAll(Arrays.asList(de));

        pce = (DesktopElement[]) executeViewTask(new CallbackTaskRunnable() {
            public Object run() throws Exception {
                return ZoeosFrame.getInstance().getZDesktopManager().getDesktopElementTree(new ViewPath(ZDesktopManager.dockPROPERTIES, ViewFactory.providePropertiesView(device).getDesktopName()));
            }
        });
        de = new DeviceDesktopElement[pce.length];
        System.arraycopy(pce, 0, de, 0, pce.length);
        elems.addAll(Arrays.asList(de));

        return (DeviceDesktopElement[]) elems.toArray(new DeviceDesktopElement[elems.size()]);
    }

    private static class DeviceDesktopElement extends AbstractDesktopElement {

        DeviceContext device;
        ViewInstance viewInstance;

        public DeviceDesktopElement(ViewInstance vi, ActivityContext activityContext, DesktopNodeDescriptor nodalDescriptor, DeviceContext device) {
            super(vi.getViewPath(), activityContext, nodalDescriptor);
            this.device = device;
            viewInstance = vi;
        }

        protected JComponent createView() throws ComponentGenerationException {
            return viewInstance.getView();
        }

        public boolean hasExpired() {
            return device.getState() == ZExternalDevice.STATE_REMOVED;
        }

        public DesktopElement getCopy() {
            retrieveComponentSessionString();
            DeviceDesktopElement clone = new DeviceDesktopElement(viewInstance, activityContext, nodalDescriptor, device);
            clone.sessionString = this.sessionString;
            clone.componentSessionString = componentSessionString;
            return clone;
        }
    }

    private static class DeviceWorkspaceDesktopElement extends DeviceDesktopElement {

        public DeviceWorkspaceDesktopElement(ViewInstance vi, ActivityContext activityContext, DesktopNodeDescriptor nodalDescriptor, DeviceContext device) {
            super(vi, new ActivityContextWrapper(activityContext, device), nodalDescriptor, device);
        }

        private static class ActivityContextWrapper implements ActivityContext {

            private ActivityContext enclosedAC;
            private DeviceContext device;

            public ActivityContextWrapper(ActivityContext enclosedAC, DeviceContext device) {
                this.enclosedAC = enclosedAC;
                this.device = device;
            }

            public boolean tryClosing() {
                return enclosedAC.tryClosing();
            }

            public void sendMessage(String msg) {
                enclosedAC.sendMessage(msg);
            }

            public boolean testCondition(String condition) {
                return enclosedAC.testCondition(condition);
            }

            public void closed() {
                enclosedAC.closed();
            }

            public void activated() {
                enclosedAC.activated();
                if (device.getDevicePreferences().ZPREF_syncPalettes.getValue()) {
                    try {
                        device.getViewManager().activateDevicePalettes().post();
                    } catch (ResourceUnavailableException e) {
                        e.printStackTrace();
                    }
                }
            }

            public void deactivated() {
                enclosedAC.deactivated();
            }
        }
    }

    private static class ViewCallbackTask extends CallbackTask {

        // create
        public static DesktopElement makeDeviceDesktopElement(final DeviceContext device, final ViewInstance vi, final ActivityContext activityContext, final DesktopNodeDescriptor nd) {
            return new DeviceDesktopElement(vi, activityContext, nd, device);
        }

        public static DesktopElement makeDeviceWorkspaceDesktopElement(final DeviceContext device, final ViewInstance vi, final ActivityContext activityContext, final DesktopNodeDescriptor nd) {
            return new DeviceWorkspaceDesktopElement(vi, activityContext, nd, device);
        }

        protected CallbackTaskResult openView(final DesktopElement desktopElement, final boolean activate) {
            init(new CallbackTaskRunnable() {
                public Object run() throws ComponentGenerationException, ChildViewNotAllowedException, LogicalHierarchyException {
                    return new Boolean(ZoeosFrame.getInstance().getZDesktopManager().addDesktopElement(desktopElement, activate));
                }
            });
            return execute();
        }

        protected CallbackTaskResult openViews(final DesktopElement[] desktopElements, final int activateIndex) {
            init(new CallbackTaskRunnable() {
                public Object run() throws ComponentGenerationException, ChildViewNotAllowedException, LogicalHierarchyException {
                    Boolean b = Boolean.TRUE;
                    for (int i = 0; i < desktopElements.length; i++) {
                        b = new Boolean(b.booleanValue() && ZoeosFrame.getInstance().getZDesktopManager().addDesktopElement(desktopElements[i], activateIndex == i ? true : false));
                    }
                    return b;
                }
            });
            return execute();
        }

        // closed
        protected CallbackTaskResult closeView(final DesktopElement desktopElement) {
            init(new CallbackTaskRunnable() {
                public Object run() {
                    return new Boolean(ZoeosFrame.getInstance().getZDesktopManager().removeDesktopElement(desktopElement));
                }
            });
            return execute();
        }

        protected CallbackTaskResult closeViews(final DesktopElement[] desktopElements) {
            init(new CallbackTaskRunnable() {
                public Object run() {
                    Boolean b = Boolean.TRUE;
                    for (int i = desktopElements.length - 1; i >= 0; i--) {
                        b = new Boolean(b.booleanValue() && ZoeosFrame.getInstance().getZDesktopManager().removeDesktopElement(desktopElements[i]));
                    }
                    return b;
                }
            });
            return execute();
        }

        protected CallbackTaskResult closeView(final ViewPath vp) {
            init(new CallbackTaskRunnable() {
                public Object run() {
                    return new Boolean(ZoeosFrame.getInstance().getZDesktopManager().removeElement(vp));
                }
            });
            return execute();
        }
    }
}
