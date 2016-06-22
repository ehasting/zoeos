package com.pcmsolutions.device.EMU.E4;

import com.pcmsolutions.device.EMU.E4.gui.*;
import com.pcmsolutions.device.EMU.E4.gui.colors.UIColors;
import com.pcmsolutions.device.EMU.E4.gui.device.DefaultDeviceEnclosurePanel;
import com.pcmsolutions.device.EMU.E4.gui.device.DevicePanel;
import com.pcmsolutions.device.EMU.E4.gui.device.DeviceWorkspaceEnclosurePanel;
import com.pcmsolutions.device.EMU.E4.gui.device.PropertiesPanel;
import com.pcmsolutions.device.EMU.E4.gui.master.MasterEnclosurePanel;
import com.pcmsolutions.device.EMU.E4.gui.multimode.MultiModeEnclosurePanel;
import com.pcmsolutions.device.EMU.E4.gui.piano.MidiPiano;
import com.pcmsolutions.device.EMU.E4.gui.preset.PresetViewModes;
import com.pcmsolutions.device.EMU.E4.gui.preset.icons.PresetIcon;
import com.pcmsolutions.device.EMU.E4.gui.preset.icons.PresetUserIcon;
import com.pcmsolutions.device.EMU.E4.gui.preset.presetcontext.PresetContextEnclosurePanel;
import com.pcmsolutions.device.EMU.E4.gui.preset.preseteditor.*;
import com.pcmsolutions.device.EMU.E4.gui.preset.preseteditor.envelope.EditableAmpEnvelopePanel;
import com.pcmsolutions.device.EMU.E4.gui.preset.preseteditor.envelope.EditableAuxEnvelopePanel;
import com.pcmsolutions.device.EMU.E4.gui.preset.preseteditor.envelope.EditableFilterEnvelopePanel;
import com.pcmsolutions.device.EMU.E4.gui.preset.presetviewer.*;
import com.pcmsolutions.device.EMU.E4.gui.preset.presetviewer.envelope.AmpEnvelopePanel;
import com.pcmsolutions.device.EMU.E4.gui.preset.presetviewer.envelope.AuxEnvelopePanel;
import com.pcmsolutions.device.EMU.E4.gui.preset.presetviewer.envelope.FilterEnvelopePanel;
import com.pcmsolutions.device.EMU.E4.gui.sample.samplecontext.SampleContextEnclosurePanel;
import com.pcmsolutions.device.EMU.E4.gui.table.PopupTable;
import com.pcmsolutions.device.EMU.E4.parameter.EditableParameterModel;
import com.pcmsolutions.device.EMU.E4.preset.ContextEditablePreset;
import com.pcmsolutions.device.EMU.E4.preset.ReadablePreset;
import com.pcmsolutions.gui.ComponentGenerationException;
import com.pcmsolutions.gui.GriddedPanel;
import com.pcmsolutions.gui.UserMessaging;
import com.pcmsolutions.gui.ZCommandFactory;
import com.pcmsolutions.gui.desktop.ViewInstance;
import com.pcmsolutions.gui.desktop.ZDesktopManager;
import com.pcmsolutions.system.Indexable;
import com.pcmsolutions.system.ZDisposable;
import com.pcmsolutions.system.callback.Callback;
import com.pcmsolutions.system.paths.DesktopName;
import com.pcmsolutions.system.paths.ViewPath;
import com.pcmsolutions.system.tasking.ResourceUnavailableException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseListener;
import java.util.ArrayList;

/**
 * User: paulmeehan Date: 20-Jan-2004 Time: 09:03:00
 */
class ViewFactory {

    private static DesktopName[] join(DesktopName[] parent, DesktopName[] child) {
        DesktopName[] names = new DesktopName[parent.length + child.length];
        System.arraycopy(parent, 0, names, 0, parent.length);
        System.arraycopy(child, 0, names, parent.length, child.length);
        return names;
    }

    private static DesktopName[] join(DesktopName[] parent, DesktopName child) {
        return join(parent, new DesktopName[]{child});
    }

    public static DesktopName[] provideDefaultDesktopNames(DeviceContext device) {
        return new DesktopName[]{new DesktopName(Object.class, PathFactory.provideDevicePath(device))};
    }

    // DEVICE
    public static ViewInstance provideDefaultView(final DeviceContext device) {
        final DesktopName[] names = ViewFactory.provideDefaultDesktopNames(device);
        final ViewPath vp = new ViewPath(ZDesktopManager.dockWORKSPACE, names);

        // final DesktopName name = new DesktopName(Object.class, PathFactory.provideDevicePath(device));
        return new ViewInstance() {
            // private transient DefaultDeviceEnclosurePanel view;

            @Override
            public JComponent getView() throws ComponentGenerationException {
                DefaultDeviceEnclosurePanel view;
                //  if (view == null) {
                view = new DefaultDeviceEnclosurePanel();
                try {
                    view.init(device, new JPanel() {
                        @Override
                        public Color getForeground() {
                            return UIColors.getDefaultFG();
                        }

                        @Override
                        public Color getBackground() {
                            return UIColors.getDefaultBG();
                        }
                    });
                } catch (Exception e) {
                    throw new ComponentGenerationException(e.getMessage());
                }
                // }
                return view;
            }

            @Override
            public DesktopName getDesktopName() {
                return names[names.length - 1];
            }

            @Override
            public ViewPath getViewPath() {
                return vp;
            }
        };
    }

    //PIANO
    public static ViewInstance providePianoView(final DeviceContext device) {
        final DesktopName name = new DesktopName(JPanel.class, PathFactory.providePianoPath(device));
        final ViewPath vp = new ViewPath(ZDesktopManager.dockPIANO, name);
        return new ViewInstance() {
            @Override
            public JComponent getView() throws ComponentGenerationException {
                try {
                    JPanel piano = new MidiPiano(device) {
                        @Override
                        public Color getBackground() {
                            return UIColors.getDefaultBG();
                        }

                        @Override
                        public Color getForeground() {
                            return UIColors.getDefaultFG();
                        }
                    };
                    return piano;
                } catch (Exception e) {
                    throw new ComponentGenerationException(e.getMessage());
                }
            }

            @Override
            public DesktopName getDesktopName() {
                return name;
            }

            @Override
            public ViewPath getViewPath() {
                return vp;
            }
        };
    }

    // PRESET
    public static DesktopName[] provideDefaultDesktopNames(ReadablePreset p) {
        return join(provideDefaultDesktopNames(p.getDeviceContext()), new DesktopName(PresetPanel.class, PathFactory.providePresetPath(p)));
    }

    // PRESET
    public static DesktopName[] provideUserDesktopNames(ReadablePreset p) {
        return join(provideDefaultDesktopNames(p), new DesktopName(PresetPanel.class, PathFactory.providePresetUserPath(p)));
    }

    public static ViewInstance provideDefaultView(final ReadablePreset p) {
        final DesktopName[] names = provideDefaultDesktopNames(p);
        final ViewPath vp = new ViewPath(ZDesktopManager.dockWORKSPACE, names);
        return new ViewInstance() {
            @Override
            public JComponent getView() throws ComponentGenerationException {
                try {
                    DeviceWorkspaceEnclosurePanel dep = new DeviceWorkspaceEnclosurePanel();
                    dep.init(p.getDeviceContext(), new PresetPanel(p, true, true, true, PresetViewModes.VOICE_MODE_ALL_BUT_USER));
                    return dep;
                } catch (Exception e) {
                    throw new ComponentGenerationException(e.getMessage());
                }
            }

            @Override
            public DesktopName getDesktopName() {
                return names[names.length - 1];
            }

            @Override
            public ViewPath getViewPath() {
                return vp;
            }
        };
    }

    public static ViewInstance provideUserView(final ReadablePreset p) {
        final DesktopName[] names = provideUserDesktopNames(p);
        final ViewPath vp = new ViewPath(ZDesktopManager.dockWORKSPACE, names);
        return new ViewInstance() {
            @Override
            public JComponent getView() throws ComponentGenerationException {
                try {
                    DeviceWorkspaceEnclosurePanel dep = new DeviceWorkspaceEnclosurePanel();
                    dep.init(p.getDeviceContext(), new PresetPanel(p, true, false, false, PresetViewModes.VOICE_MODE_USER) {
                        @Override
                        public String getTitle() {
                            return "User";
                        }

                        @Override
                        public Integer getIndex() {
                            return ViewIndexFactory.PRESET_USER_INDEX;
                        }

                        @Override
                        public Icon getIcon() {
                            Icon i = p.getIcon();
                            if (i != null) {
                                return new PresetUserIcon((PresetIcon) i);
                            } else {
                                return null;
                            }
                        }
                    });
                    return dep;
                } catch (Exception e) {
                    throw new ComponentGenerationException(e.getMessage());
                }
            }

            @Override
            public DesktopName getDesktopName() {
                return names[names.length - 1];
            }

            @Override
            public ViewPath getViewPath() {
                return vp;
            }
        };
    }

    public static ViewInstance provideDefaultView(final ContextEditablePreset p) {
        final DesktopName[] names = provideDefaultDesktopNames(p);
        final ViewPath vp = new ViewPath(ZDesktopManager.dockWORKSPACE, names);
        return new ViewInstance() {
            @Override
            public JComponent getView() throws ComponentGenerationException {
                try {
                    DeviceWorkspaceEnclosurePanel dep = new DeviceWorkspaceEnclosurePanel();
                    dep.init(p.getDeviceContext(), new EditablePresetPanel(p, true, true, true, PresetViewModes.VOICE_MODE_ALL_BUT_USER));
                    return dep;
                } catch (Exception e) {
                    throw new ComponentGenerationException(e.getMessage());
                }
            }

            @Override
            public DesktopName getDesktopName() {
                return names[names.length - 1];
            }

            @Override
            public ViewPath getViewPath() {
                return vp;
            }
        };
    }

    public static ViewInstance provideUserView(final ContextEditablePreset p) {
        final DesktopName[] names = provideUserDesktopNames(p);
        final ViewPath vp = new ViewPath(ZDesktopManager.dockWORKSPACE, names);
        return new ViewInstance() {
            @Override
            public JComponent getView() throws ComponentGenerationException {
                try {
                    DeviceWorkspaceEnclosurePanel dep = new DeviceWorkspaceEnclosurePanel();
                    dep.init(p.getDeviceContext(), new EditablePresetPanel(p, true, false, false, PresetViewModes.VOICE_MODE_USER) {
                        @Override
                        public String getTitle() {
                            return "User";
                        }

                        @Override
                        public Integer getIndex() {
                            return ViewIndexFactory.PRESET_USER_INDEX;
                        }

                        @Override
                        public Icon getIcon() {
                            Icon i = p.getIcon();
                            if (i != null) {
                                return new PresetUserIcon((PresetIcon) i);
                            } else {
                                return null;
                            }
                        }
                    });
                    return dep;
                } catch (Exception e) {
                    throw new ComponentGenerationException(e.getMessage());
                }
            }

            @Override
            public DesktopName getDesktopName() {
                return names[names.length - 1];
            }

            @Override
            public ViewPath getViewPath() {
                return vp;
            }
        };
    }

    // VOICE
    // this is the invisible parent view for the sections of a readable voice in tabbed view
    private static class EmptyReadableVoicePanel extends JPanel implements ZDisposable, TitleProvider, Indexable {

        private ReadablePreset.ReadableVoice voice;
        private VoiceTitleProvider vtp;

        public void init(ReadablePreset.ReadableVoice voice) {
            this.voice = voice;
            vtp = makeVoiceTitleProvider();
        }

        protected VoiceTitleProvider makeVoiceTitleProvider() {
            VoiceTitleProvider _vtp = new VoiceTitleProvider();
            _vtp.init(voice);
            return _vtp;
        }

        @Override
        public Color getBackground() {
            return UIColors.getDefaultBG();
        }

        @Override
        public Color getForeground() {
            return UIColors.getDefaultFG();
        }

        @Override
        public void zDispose() {
            vtp.zDispose();
        }

        @Override
        public String getTitle() {
            return vtp.getTitle();
        }

        @Override
        public String getReducedTitle() {
            return vtp.getReducedTitle();
        }

        @Override
        public void addTitleProviderListener(TitleProviderListener tpl) {
            vtp.addTitleProviderListener(tpl);
        }

        @Override
        public void removeTitleProviderListener(TitleProviderListener tpl) {
            vtp.addTitleProviderListener(tpl);
        }

        @Override
        public Icon getIcon() {
            return vtp.getIcon();
        }

        @Override
        public Integer getIndex() {
            return voice.getVoiceNumber();
        }
    }

    // this is the invisible parent view for the sections of an editable voice in tabbed view
    private static class EmptyEditableVoicePanel extends EmptyReadableVoicePanel {

        private ContextEditablePreset.EditableVoice[] voices;

        public void init(ContextEditablePreset.EditableVoice[] voices) {
            this.voices = voices;
            init(voices[0]);
        }

        @Override
        protected VoiceTitleProvider makeVoiceTitleProvider() {
            EditableVoiceTitleProvider vtp = new EditableVoiceTitleProvider();
            vtp.init(voices);
            return vtp;
        }

        @Override
        public Integer getIndex() {
            if (voices.length > 1) {
                return ViewIndexFactory.getEditableVoiceIndex(voices);
            } else {
                return voices[0].getVoiceNumber();
            }
        }
    }

    // this is where the actual sections of a voice are shown in tabbed mode
    private abstract static class VoiceSectionPanel extends GriddedPanel implements MouseListener, TitleProvider, Indexable, EnclosureNorthenComponentProvider, ZDisposable {

        private TitleProvider titleProvider;
        private final Integer index;
        protected EnclosureMenuBar encMenuBar;
        protected ReadablePreset.ReadableVoice voice;
        // protected Impl_TableExclusiveSelectionContext tsc = new Impl_TableExclusiveSelectionContext();
        protected static final String selectionContext = "selection";

        public VoiceSectionPanel(ReadablePreset.ReadableVoice voice, TitleProvider titleProvider, Integer index) {
            this.titleProvider = titleProvider;
            this.index = index;
            this.voice = voice;
            this.addMouseListener(this);
        }

        @Override
        public Color getBackground() {
            return UIColors.getDefaultBG();
        }

        @Override
        public Color getForeground() {
            return UIColors.getDefaultFG();
        }

        @Override
        public String getTitle() {
            return titleProvider.getTitle();
        }

        @Override
        public String getReducedTitle() {
            return titleProvider.getReducedTitle();
        }

        @Override
        public void addTitleProviderListener(TitleProviderListener tpl) {
            titleProvider.addTitleProviderListener(tpl);
        }

        @Override
        public void removeTitleProviderListener(TitleProviderListener tpl) {
            titleProvider.removeTitleProviderListener(tpl);
        }

        @Override
        public Icon getIcon() {
            return titleProvider.getIcon();
        }

        @Override
        public Integer getIndex() {
            return index;
        }

        public void mouseDragged(java.awt.event.MouseEvent mouseEvent) {
        }

        @Override
        public void mouseClicked(java.awt.event.MouseEvent e) {
            if (e.getClickCount() == 2) {
                try {
                    voice.getPreset().audition().post((Exception e1, boolean wasCancelled) -> {
                        if (e1 != null && !wasCancelled) {
                            UserMessaging.flashWarning(null, e1.getMessage());
                        }
                    });
                } catch (ResourceUnavailableException e1) {
                    UserMessaging.flashWarning(null, e1.getMessage());
                }
            }
        }

        @Override
        public void mouseEntered(java.awt.event.MouseEvent e) {
        }

        @Override
        public void mouseExited(java.awt.event.MouseEvent e) {
        }

        @Override
        public void mousePressed(java.awt.event.MouseEvent e) {
        }

        @Override
        public void mouseReleased(java.awt.event.MouseEvent e) {
        }

        @Override
        public void zDispose() {
            titleProvider = null;
            encMenuBar.zDispose();
            encMenuBar = null;
            this.removeMouseListener(this);
        }
    }

    public static DesktopName[] provideDefaultDesktopNames(ReadablePreset.ReadableVoice v) {
        return join(provideDefaultDesktopNames(v.getPreset()), new DesktopName(VoicePanel.class, PathFactory.provideVoicePath(v)));
    }

    public static DesktopName[] provideDefaultDesktopNames(ReadablePreset.ReadableVoice v, int sections) {
        return join(provideDefaultDesktopNames(v), new DesktopName(VoiceSectionPanel.class, PathFactory.provideVoiceSectionPath(v, sections)));
    }

    public static DesktopName[] provideDefaultDesktopNames(ContextEditablePreset.EditableVoice v, int sections) {
        return (provideDefaultDesktopNames(new ContextEditablePreset.EditableVoice[]{v}, sections));
        /*     if (v.getGroupMode())
                 return join(provideDefaultDesktopNames(v), new DesktopName(VoiceSectionPanel.class, PathFactory.provideGroupSectionPath(v, sections)));
             else
                 return join(provideDefaultDesktopNames(v), new DesktopName(VoiceSectionPanel.class, PathFactory.provideVoiceSectionPath(v, sections)));
         */
    }

    public static DesktopName[] provideDefaultDesktopNames(ContextEditablePreset.EditableVoice[] voices) {
        // if (v.getGroupMode())
        return join(provideDefaultDesktopNames(voices[0].getPreset()), new DesktopName(EditableVoicePanel.class, PathFactory.provideVoicePaths(voices)));
        //  else
        //      return join(provideDefaultDesktopNames(v.getIndex()), new DesktopName(EditableVoicePanel.class, PathFactory.provideVoicePath(v)));
    }

    public static DesktopName[] provideDefaultDesktopNames(ContextEditablePreset.EditableVoice[] voices, int sections) {
        // if (v.getGroupMode())
        return join(provideDefaultDesktopNames(voices), new DesktopName(EditableVoicePanel.class, PathFactory.provideVoiceSectionPaths(voices, sections)));
        //  else
        //      return join(provideDefaultDesktopNames(v.getIndex()), new DesktopName(EditableVoicePanel.class, PathFactory.provideVoicePath(v)));
    }

    public static ViewInstance provideDefaultView(final ReadablePreset.ReadableVoice voice, final boolean empty) {
        final DesktopName[] names = provideDefaultDesktopNames(voice);
        final ViewPath vp = new ViewPath(ZDesktopManager.dockWORKSPACE, names);
        return new ViewInstance() {
            @Override
            public JComponent getView() throws ComponentGenerationException {
                try {
                    DeviceWorkspaceEnclosurePanel dep = new DeviceWorkspaceEnclosurePanel();
                    if (empty) {
                        EmptyReadableVoicePanel evp = new EmptyReadableVoicePanel();
                        evp.init(voice);
                        dep.init(voice.getPreset().getDeviceContext(), evp);
                    } else {
                        dep.init(voice.getPreset().getDeviceContext(), new VoicePanel().init(voice));
                    }
                    return dep;
                } catch (Exception e) {
                    throw new ComponentGenerationException(e.getMessage());
                }
            }

            @Override
            public DesktopName getDesktopName() {
                return names[names.length - 1];
            }

            @Override
            public ViewPath getViewPath() {
                return vp;
            }
        };
    }

    public static ViewInstance provideDefaultView(final ReadablePreset.ReadableVoice voice, final int sections, final TitleProvider tp) {
        final DesktopName[] names = provideDefaultDesktopNames(voice, sections);
        final ViewPath vp = new ViewPath(ZDesktopManager.dockWORKSPACE, names);
        return new ViewInstance() {
            @Override
            public JComponent getView() throws ComponentGenerationException {
                try {
                    final Impl_TableExclusiveSelectionContext tsc = new Impl_TableExclusiveSelectionContext();
                    DeviceWorkspaceEnclosurePanel dep = new DeviceWorkspaceEnclosurePanel();
                    VoiceSectionPanel vsp = new VoiceSectionPanel(voice, tp, ViewIndexFactory.getVoiceSectionIndex(sections)) {
                        {
                            /*
                             tsc.setSelectionAction(new Impl_TableExclusiveSelectionContext.SelectionAction() {
                                 public void newSelection(PopupTable t) {
                                     encMenuBar.removeDynamicMenuContext(selectionContext);
                                     if (t != null) {
                                         Object[] selObjs = t.getSelObjects();
                                         Component[] items = ZCommandFactory.getToolBarStyleMenuComponents(selObjs, "Parameter");
                                         encMenuBar.addDynamicMenuContext(items, selectionContext);
                                     }
                                 }

                                 public void clearedSelection(PopupTable t) {
                                     //To change body of implemented methods use File | Settings | File Templates.
                                 }
                             });
                             */
                        }

                        @Override
                        public boolean isEnclosureNorthenComponentAvailable() {
                            return false;
                        }

                        @Override
                        public Component getEnclosureNorthenComponent() {
                            return null;
                        }
                    };
                    int gridIndex = 0;
                    if ((sections & VoiceSections.VOICE_CORDS) != 0) {
                        CordPanel p = new CordPanel();
                        p.init(voice, tsc);
                        vsp.addAnchoredComponent(p, gridIndex++, 0, GridBagConstraints.NORTH);
                    }
                    if ((sections & VoiceSections.VOICE_AMP_FILTER) != 0) {
                        AmplifierPanel amp = new AmplifierPanel();
                        amp.init(voice, tsc);
                        vsp.addAnchoredComponent(amp, gridIndex++, 0, GridBagConstraints.NORTH);

                        FilterPanel filt = new FilterPanel();
                        filt.init(voice, tsc);
                        vsp.addAnchoredComponent(filt, gridIndex++, 0, GridBagConstraints.NORTH);

                        LFOPanel lfo = new LFOPanel();
                        lfo.init(voice, tsc);
                        vsp.addAnchoredComponent(lfo, gridIndex++, 0, GridBagConstraints.NORTH);

                        TuningPanel tuning = new TuningPanel();
                        tuning.init(voice, tsc);
                        vsp.addAnchoredComponent(tuning, gridIndex++, 0, GridBagConstraints.NORTH);

                        AmpEnvelopePanel ampenv = new AmpEnvelopePanel();
                        ampenv.init(voice, tsc);
                        vsp.addAnchoredComponent(ampenv, gridIndex++, 0, GridBagConstraints.NORTH);

                        FilterEnvelopePanel filtenv = new FilterEnvelopePanel();
                        filtenv.init(voice, tsc);
                        vsp.addAnchoredComponent(filtenv, gridIndex++, 0, GridBagConstraints.NORTH);

                        AuxEnvelopePanel aux = new AuxEnvelopePanel();
                        aux.init(voice, tsc);
                        vsp.addAnchoredComponent(aux, gridIndex++, 0, GridBagConstraints.NORTH);

                    } else {
                        if ((sections & VoiceSections.VOICE_AMP) != 0) {
                            AmplifierPanel p = new AmplifierPanel();
                            p.init(voice, tsc);
                            vsp.addAnchoredComponent(p, gridIndex++, 0, GridBagConstraints.NORTH);
                        }
                        if ((sections & VoiceSections.VOICE_FILTER) != 0) {
                            FilterPanel p = new FilterPanel();
                            p.init(voice, tsc);
                            vsp.addAnchoredComponent(p, gridIndex++, 0, GridBagConstraints.NORTH);
                        }
                    }
                    if ((sections & VoiceSections.VOICE_LFO) != 0) {
                        LFOPanel p = new LFOPanel();
                        p.init(voice, tsc);
                        vsp.addAnchoredComponent(p, gridIndex++, 0, GridBagConstraints.NORTH);
                    }
                    if ((sections & VoiceSections.VOICE_TUNING) != 0) {
                        TuningPanel p = new TuningPanel();
                        p.init(voice, tsc);
                        vsp.addAnchoredComponent(p, gridIndex++, 0, GridBagConstraints.NORTH);
                    }
                    if ((sections & VoiceSections.VOICE_ENVELOPES) != 0) {
                        AmpEnvelopePanel amp = new AmpEnvelopePanel();
                        amp.init(voice, tsc);
                        vsp.addAnchoredComponent(amp, gridIndex++, 0, GridBagConstraints.NORTH);

                        FilterEnvelopePanel filt = new FilterEnvelopePanel();
                        filt.init(voice, tsc);
                        vsp.addAnchoredComponent(filt, gridIndex++, 0, GridBagConstraints.NORTH);

                        AuxEnvelopePanel aux = new AuxEnvelopePanel();
                        aux.init(voice, tsc);
                        vsp.addAnchoredComponent(aux, gridIndex++, 0, GridBagConstraints.NORTH);
                    } else {
                        if ((sections & VoiceSections.VOICE_AMP_ENVELOPE) != 0) {
                            AmpEnvelopePanel p = new AmpEnvelopePanel();
                            p.init(voice, tsc);
                            vsp.addAnchoredComponent(p, gridIndex++, 0, GridBagConstraints.NORTH);
                        }
                        if ((sections & VoiceSections.VOICE_FILTER_ENVELOPE) != 0) {
                            FilterEnvelopePanel p = new FilterEnvelopePanel();
                            p.init(voice, tsc);
                            vsp.addAnchoredComponent(p, gridIndex++, 0, GridBagConstraints.NORTH);
                        }
                        if ((sections & VoiceSections.VOICE_AUX_ENVELOPE) != 0) {
                            AuxEnvelopePanel p = new AuxEnvelopePanel();
                            p.init(voice, tsc);
                            vsp.addAnchoredComponent(p, gridIndex++, 0, GridBagConstraints.NORTH);
                        }
                    }
                    dep.init(voice.getPreset().getDeviceContext(), vsp);
                    return dep;
                } catch (Exception e) {
                    throw new ComponentGenerationException(e.getMessage());
                }
            }

            @Override
            public DesktopName getDesktopName() {
                return names[names.length - 1];
            }

            @Override
            public ViewPath getViewPath() {
                return vp;
            }
        };
    }

    public static ViewInstance provideDefaultView(final ContextEditablePreset.EditableVoice[] voices, final boolean empty) {
        final DesktopName[] names = provideDefaultDesktopNames(voices);
        final ViewPath vp = new ViewPath(ZDesktopManager.dockWORKSPACE, names);
        return new ViewInstance() {
            @Override
            public JComponent getView() throws ComponentGenerationException {
                try {
                    DeviceWorkspaceEnclosurePanel dep = new DeviceWorkspaceEnclosurePanel();
                    if (empty) {
                        EmptyEditableVoicePanel evp = new EmptyEditableVoicePanel();
                        evp.init(voices);
                        dep.init(voices[0].getPreset().getDeviceContext(), evp);
                    } else {
                        dep.init(voices[0].getPreset().getDeviceContext(), new EditableVoicePanel().init(voices));
                    }
                    return dep;
                } catch (Exception e) {
                    throw new ComponentGenerationException(e.getMessage());
                }
            }

            @Override
            public DesktopName getDesktopName() {
                return names[names.length - 1];
            }

            @Override
            public ViewPath getViewPath() {
                return vp;
            }
        };
    }

    public static ViewInstance provideDefaultView(final ContextEditablePreset.EditableVoice[] voices, final int sections, final TitleProvider tp) {
        final DesktopName[] names = provideDefaultDesktopNames(voices, sections);
        final ViewPath vp = new ViewPath(ZDesktopManager.dockWORKSPACE, names);
        return new ViewInstance() {
            @Override
            public JComponent getView() throws ComponentGenerationException {
                try {
                    DeviceWorkspaceEnclosurePanel dep = new DeviceWorkspaceEnclosurePanel();
                    final Impl_TableExclusiveSelectionContext tsc = new Impl_TableExclusiveSelectionContext();
                    VoiceSectionPanel vsp = new VoiceSectionPanel(voices[0], tp, ViewIndexFactory.getVoiceSectionIndex(sections)) {
                        ZCommandFactory.ZCommandPresentationContext parameterCommandPresentationContext;

                        {
                            parameterCommandPresentationContext = ZCommandFactory.getToolbarPresentationContext(EditableParameterModel.cmdProviderHelper.getSupportedMarkers(), null);
                            tsc.setSelectionAction(new Impl_TableExclusiveSelectionContext.SelectionAction() {
                                @Override
                                public void newSelection(PopupTable t) {
                                    if (t != null) {
                                        Object[] selObjs = t.getSelObjects();
                                        parameterCommandPresentationContext.setTargets(selObjs);
                                    } else {
                                        parameterCommandPresentationContext.disableContext();
                                    }
                                }

                                @Override
                                public void clearedSelection(PopupTable t) {
                                }
                            });
                        }

                        @Override
                        public boolean isEnclosureNorthenComponentAvailable() {
                            return true;
                        }

                        @Override
                        public Component getEnclosureNorthenComponent() {
                            if (encMenuBar == null) {
                                encMenuBar = new EnclosureMenuBar();
                                encMenuBar.addStaticMenuContext(parameterCommandPresentationContext.getComponents(), selectionContext);
                            }
                            return encMenuBar.getjMenuBar();
                        }

                        @Override
                        public void zDispose() {
                            super.zDispose();
                            parameterCommandPresentationContext.zDispose();
                            parameterCommandPresentationContext = null;
                        }
                    };

                    if ((sections & VoiceSections.VOICE_EDIT) != 0) {
                        int ROW_0 = 0;
                        int ROW_1 = 1;
                        int ROW_2 = 2;
                        int ROW_3 = 3;

                        int COL_0 = 0;
                        int COL_1 = 1;
                        int COL_2 = 2;
                        int COL_3 = 3;
                        
                        // INIT
                        EditableAmplifierPanel amp = new EditableAmplifierPanel();
                        amp.init(voices, tsc);
                        EditableTuningPanel tuning = new EditableTuningPanel();
                        tuning.init(voices, tsc);
                        EditableCordPanel cord = new EditableCordPanel();
                        cord.init(voices, tsc);
                        EditableAmpEnvelopePanel ampenv = new EditableAmpEnvelopePanel();
                        ampenv.init(voices, tsc);
                        EditableFilterEnvelopePanel filtenv = new EditableFilterEnvelopePanel();
                        filtenv.init(voices, tsc);
                        EditableFilterPanel filt = new EditableFilterPanel();
                        filt.init(voices, tsc);
                        EditableAuxEnvelopePanel auxenv = new EditableAuxEnvelopePanel();
                        auxenv.init(voices, tsc);
                        EditableLFOPanel lfo = new EditableLFOPanel();
                        lfo.init(voices, tsc);

                        // Placement

                        vsp.addAnchoredComponent(ampenv,    ROW_0, COL_0, GridBagConstraints.NORTHWEST);
                        vsp.addAnchoredComponent(cord,      ROW_0, COL_3, 1, 2, GridBagConstraints.NORTHWEST);
                        
                        vsp.addAnchoredComponent(filtenv,   ROW_1, COL_0, GridBagConstraints.NORTHWEST);
                        vsp.addAnchoredComponent(filt,      ROW_1, COL_1, GridBagConstraints.NORTHWEST);
                        
                        vsp.addAnchoredComponent(auxenv,    ROW_2, COL_0, GridBagConstraints.NORTHWEST);
                        vsp.addAnchoredComponent(lfo,       ROW_2, COL_1, GridBagConstraints.NORTHWEST);
                        
                        vsp.addAnchoredComponent(tuning,    ROW_3, COL_0, GridBagConstraints.NORTHWEST);
                        vsp.addAnchoredComponent(amp,       ROW_3, COL_1, GridBagConstraints.NORTHWEST);
                        

                    }
                    dep.init(voices[0].getPreset().getDeviceContext(), vsp);
                    return dep;
                } catch (Exception e) {
                    throw new ComponentGenerationException(e.getMessage());
                }
            }

            @Override
            public DesktopName getDesktopName() {
                return names[names.length - 1];
            }

            @Override
            public ViewPath getViewPath() {
                return vp;
            }
        };
    }

    // PALETTES
    public static ViewInstance provideDeviceView(final DeviceContext device) {
        final DesktopName name = new DesktopName(DevicePanel.class, PathFactory.provideDevicePath(device));
        final ViewPath vp = new ViewPath(ZDesktopManager.dockDEVICES, name);
        return new ViewInstance() {
            @Override
            public JComponent getView() throws ComponentGenerationException {
                try {
                    DefaultDeviceEnclosurePanel dep = new DefaultDeviceEnclosurePanel();
                    DevicePanel dp = new DevicePanel(device);
                    dep.init(device, dp);
                    return dep;
                } catch (Exception e) {
                    throw new ComponentGenerationException(e.getMessage());
                }
            }

            @Override
            public DesktopName getDesktopName() {
                return name;
            }

            @Override
            public ViewPath getViewPath() {
                return vp;
            }
        };
    }

    public static ViewInstance providePropertiesView(final DeviceContext device) {
        final DesktopName name = new DesktopName(PropertiesPanel.class, PathFactory.providePropertiesPath(device));
        final ViewPath vp = new ViewPath(ZDesktopManager.dockPROPERTIES, name);
        return new ViewInstance() {
            @Override
            public JComponent getView() throws ComponentGenerationException {
                try {
                    ArrayList props = new ArrayList();
                    props.addAll(device.getDevicePreferences().getPropertyList());
                    props.addAll(device.getRemotePreferences().getPropertyList());
                    PropertiesPanel pp = new PropertiesPanel(props, device);
                    return pp;
                } catch (Exception e) {
                    throw new ComponentGenerationException(e.getMessage());
                }
            }

            @Override
            public DesktopName getDesktopName() {
                return name;
            }

            @Override
            public ViewPath getViewPath() {
                return vp;
            }
        };
    }

    public static ViewInstance provideDefaultPresetContextView(final DeviceContext device) {
        final DesktopName name = new DesktopName(PresetContextEnclosurePanel.class, PathFactory.providePresetsPath(device));
        final ViewPath vp = new ViewPath(ZDesktopManager.dockPRESETS, name);
        return new ViewInstance() {
            @Override
            public JComponent getView() throws ComponentGenerationException {
                try {
                    PresetContextEnclosurePanel pcep = new PresetContextEnclosurePanel();
                    pcep.init(device);
                    return pcep;
                } catch (Exception e) {
                    throw new ComponentGenerationException(e.getMessage());
                }
            }

            @Override
            public DesktopName getDesktopName() {
                return name;
            }

            @Override
            public ViewPath getViewPath() {
                return vp;
            }
        };
    }

    public static ViewInstance provideDefaultSampleContextView(final DeviceContext device) {
        final DesktopName name = new DesktopName(SampleContextEnclosurePanel.class, PathFactory.provideSamplesPath(device));
        final ViewPath vp = new ViewPath(ZDesktopManager.dockSAMPLES, name);
        return new ViewInstance() {
            @Override
            public JComponent getView() throws ComponentGenerationException {
                try {
                    SampleContextEnclosurePanel scep = new SampleContextEnclosurePanel();
                    scep.init(device);
                    return scep;
                } catch (Exception e) {
                    throw new ComponentGenerationException(e.getMessage());
                }
            }

            @Override
            public DesktopName getDesktopName() {
                return name;
            }

            @Override
            public ViewPath getViewPath() {
                return vp;
            }
        };
    }

    public static ViewInstance provideMultiModeView(final DeviceContext device) {
        final DesktopName name = new DesktopName(MultiModeEnclosurePanel.class, PathFactory.provideMultiModePath(device));
        final ViewPath vp = new ViewPath(ZDesktopManager.dockMULTI, name);
        return new ViewInstance() {
            @Override
            public JComponent getView() throws ComponentGenerationException {
                try {
                    MultiModeEnclosurePanel mmep = new MultiModeEnclosurePanel();
                    mmep.init(device);
                    return mmep;
                } catch (Exception e) {
                    throw new ComponentGenerationException(e.getMessage());
                }
            }

            @Override
            public DesktopName getDesktopName() {
                return name;
            }

            @Override
            public ViewPath getViewPath() {
                return vp;
            }
        };
    }

    public static ViewInstance provideMasterView(final DeviceContext device) {
        final DesktopName name = new DesktopName(MasterEnclosurePanel.class, PathFactory.provideMasterPath(device));
        final ViewPath vp = new ViewPath(ZDesktopManager.dockMASTER, name);
        return new ViewInstance() {
            @Override
            public JComponent getView() throws ComponentGenerationException {
                try {
                    MasterEnclosurePanel mep = new MasterEnclosurePanel();
                    mep.init(device);
                    return mep;
                } catch (Exception e) {
                    throw new ComponentGenerationException(e.getMessage());
                }
            }

            @Override
            public DesktopName getDesktopName() {
                return name;
            }

            @Override
            public ViewPath getViewPath() {
                return vp;
            }
        };
    }
}
