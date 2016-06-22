/*
 * ZoeosFrame.java
 *
 * Created on 04 October 2002, 00:37
 */

package com.pcmsolutions.gui;

import com.jidesoft.converter.ObjectConverterManager;
import com.jidesoft.grid.CellEditorManager;
import com.jidesoft.grid.CellRendererManager;
import com.jidesoft.plaf.LookAndFeelFactory;
import com.jidesoft.status.MemoryStatusBarItem;
import com.jidesoft.status.ProgressStatusBarItem;
import com.jidesoft.status.StatusBar;
import com.jidesoft.swing.JideBoxLayout;
import com.jidesoft.tipoftheday.TipOfTheDayDialog;
import com.jidesoft.tipoftheday.TipOfTheDaySource;
import com.pcmsolutions.device.EMU.E4.gui.GeneralTableCellRenderer;
import com.pcmsolutions.device.EMU.E4.gui.colors.UIColors;
import com.pcmsolutions.gui.desktop.*;
import com.pcmsolutions.gui.license.ZLicenseManagerDialog;
import com.pcmsolutions.gui.midi.ZMidiManagerDialog;
import com.pcmsolutions.gui.smdi.ZSmdiManagerDialog;
import com.pcmsolutions.smdi.SMDIAgent;
import com.pcmsolutions.system.*;

import javax.help.CSH;
import javax.help.HelpBroker;
import javax.help.HelpSet;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.plaf.ColorUIResource;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;

/**
 * @author pmeehan
 */

public class ZoeosFrame extends ZJFrame {
    private Zoeos zi;
    private static ZoeosFrame INSTANCE;
    private ZDeviceManagerDialog deviceManager;
    private ZMidiManagerDialog midiManager;
    private ZSmdiManagerDialog smdiManager;
    private ZLicenseManagerDialog licenseManager;
    private TipOfTheDayDialog tipOfTheDay;

    private static final String DOCK_PROFILE_KEY = "Zoeos_Layouts";

    private javax.swing.JMenu jmFile;
    private javax.swing.JMenu jmManage;
    private javax.swing.JMenu jmWindow;
    private javax.swing.JMenu jmHelp;

    public JMenuItem redoMenuItem;
    public JMenuItem undoMenuItem;
    private boolean autohideAll = false;

    private javax.swing.JMenuItem jmiDeviceManager;
    private javax.swing.JMenuItem jmiMidiManager;
    private javax.swing.JMenuItem jmiSMDIManager;
    // private javax.swing.JMenuItem jmiTaskManager;

    private javax.swing.JMenuItem jmiLicenseManager;

    private javax.swing.JMenuItem jmiTipOfTheDay;
    private javax.swing.JMenuItem jmiProductTour;
    private javax.swing.JMenuItem jmiHelp;
    private javax.swing.JMenuItem jmiRequestFunctionality;
    private javax.swing.JMenuItem jmiReportBug;
    private javax.swing.JMenuItem jmiAboutBox;

    private javax.swing.JMenuItem jmiExit;
    private javax.swing.JMenuBar jMainMenu;

    private StatusBar statusBar;

    private HelpBroker hb;

    private class ShutdownDialog extends ZDialog {
        public ShutdownDialog() throws HeadlessException {
            super(ZoeosFrame.this, "ZoeOS is shutting down", true);
            JProgressBar pb = new JProgressBar(0, 100);
            pb.setString(ZUtilities.makeExactLengthString("Shutdown", 80));
            pb.setStringPainted(true);
            pb.setIndeterminate(true);
            getContentPane().add(pb);
            pack();
        }
    };

    private Runnable shutdownZoeos = new Runnable() {
        public void run() {
            final JDialog sd = new ShutdownDialog();
            try {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        sd.setVisible(true);
                    }
                });
                //ZoeosFrame.this.getDockingManager().saveLayoutDataToFile("ZoeOSLayout");
                ZoeosFrame.this.getDockingManager().saveLayoutDataAs(ZDesktopManager.LAYOUT_LAST);
                Zoeos.getInstance().zDispose();
            } finally {
                try {
                    sd.dispose();
                    ZoeosFrame.this.dispose();
                } finally {
                    System.exit(0);
                }
            }
        }
    };

    public static ZoeosFrame getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ZoeosFrame();
            INSTANCE.init();
        }
        return INSTANCE;
    }

    /**
     * Creates new form ZoeosFrame
     */
    //  private static final String PREF_uses = "zoeos" + Zoeos.versionStr + "uses ";

    public ZoeosFrame() {
        URL url = ZoeosFrame.class.getResource("/zoeosFrameIcon.gif");
        if (url != null)
            this.setIconImage(new ImageIcon(url).getImage());
        this.setTitle("ZoeOS");
        zi = Zoeos.getInstance();
        //zi.setZoeosFrame(this);
        //Dimension scrsize = Toolkit.getDefaultToolkit().getScreenSize();
        //setSize(scrsize.width, (scrsize.height - 25));
        //setLocation(0, 0);
        //this.setExtendedState(JFrame.MAXIMIZED_BOTH);
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        initJIDE();
        initComponents();
    }

    private class Impl_ProgressSession implements ProgressSession {
        ProgressStatusBarItem pi;
        int maximum;
        int status = 0;
        boolean cancelled = false;
        boolean done = false;

        public Impl_ProgressSession(final String title, int maximum, final String cancelText) {
            this.maximum = (maximum == 0 ? 1 : maximum);
            SwingUtilities.invokeLater(() -> {
                pi = new ProgressStatusBarItem();
                statusBar.add(pi, JideBoxLayout.FLEXIBLE);
                pi.setProgressStatus(title);
                pi.setStatus("Running");
                pi.setCancelText(cancelText);
                if (!cancelText.equals(""))
                    pi.setCancelCallback(() -> {
                        end(true);
                });
            });
        }

        @Override
        public void updateTitle(final String title) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    pi.setProgressStatus(title);
                }
            });
        }

        @Override
        public void updateStatus(final int st) {
            SwingUtilities.invokeLater(() -> {
                status = st;
                pi.setProgress((status * 100) / maximum);
            });
        }

        @Override
        public void end() {
            end(false);
        }

        void end(final boolean wasCancelled) {
            SwingUtilities.invokeLater(() -> {
                pi.setProgress(100);
                pi.setStatus("Done");
                statusBar.remove(pi);
                statusBar.revalidate();
                statusBar.repaint();
                synchronized (Impl_ProgressSession.this) {
                    cancelled = wasCancelled;
                    done = true;
                    Impl_ProgressSession.this.notifyAll();
                }
            });
        }

        @Override
        public synchronized boolean isActive() {
            return !done && !cancelled;
        }

        @Override
        public void setIndeterminate(final boolean ind) {
            SwingUtilities.invokeLater(() -> {
                pi.setInterminate(ind);
            });
        }

        @Override
        public void updateStatus() {
            updateStatus(++status);
        }

        @Override
        public synchronized boolean isCancelled() {
            return cancelled;
        }
    }

    public ProgressSession getProgressSession(final String title, final int maximum, boolean allowCancel) {
        return new Impl_ProgressSession(title, maximum, (allowCancel ? "Cancel" : ""));
    }

    public ProgressSession getProgressSession(final String title, final int maximum) {
        return new Impl_ProgressSession(title, maximum, "");
    }

    private void init() {
        try {
            deviceManager = new ZDeviceManagerDialog(this, false);
            midiManager = new ZMidiManagerDialog(this, false);
            licenseManager = new ZLicenseManagerDialog(this, false);
            tipOfTheDay = new TipOfTheDayDialog(this, new TipOfTheDaySource() {
                @Override
                public String getNextTip() {
                    return TipFactory.getNextTip();
                }

                @Override
                public String getPreviousTip() {
                    return TipFactory.getPreviousTip();
                }
            }, new AbstractAction("Show tips on startup") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (e.getSource() instanceof JCheckBox) {
                        JCheckBox checkBox = (JCheckBox) e.getSource();
                        ZoeosPreferences.ZPREF_showTipsAtStartup.putValue(checkBox.isSelected());
                    }
                    // change your user preference
                }
            }, null);
            tipOfTheDay.setShowTooltip(true);
            tipOfTheDay.setResizable(false);
            tipOfTheDay.pack();
            tipOfTheDay.setLocation(300, 250);
            tipOfTheDay.getShowTipCheckBox().setSelected(ZoeosPreferences.ZPREF_showTipsAtStartup.getValue());
            ZoeosPreferences.ZPREF_showTipsAtStartup.addChangeListener((ChangeEvent e) -> {
                tipOfTheDay.getShowTipCheckBox().setSelected(ZoeosPreferences.ZPREF_showTipsAtStartup.getValue());
            });
            ZoeosPreferences.ZPREF_showTipsAtStartup.addChangeListener((ChangeEvent e) -> {
                tipOfTheDay.setShowTooltip(ZoeosPreferences.ZPREF_showTipsAtStartup.getValue());
            });

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Could not initialize main ZoeOS window. Exiting.", "Fatal Error", JOptionPane.ERROR_MESSAGE);
            System.exit(-1);
        }
    }

    private void initComponents() {
        createMenuBar();
        createStatusBar();

        final ViewInstance prop_vi = SystemViewFactory.providePropertiesView();
        try {
            zDesktopManager.addDesktopElement(new AbstractDesktopElement(prop_vi.getViewPath(), StaticActivityContext.FALSE, new Impl_DesktopNodeDescriptor()) {
                @Override
                protected JComponent createView() throws ComponentGenerationException {
                    return prop_vi.getView();
                }

                @Override
                public boolean hasExpired() {
                    return false;
                }

                @Override
                public int compareTo(Object o) {
                    return -1;
                }

                @Override
                public DesktopElement getCopy() {
                    return this;
                }
            });
        } catch (ComponentGenerationException | ChildViewNotAllowedException | LogicalHierarchyException e) {
            e.printStackTrace();
        }
        /*
        final ViewInstance piano_vi = SystemViewFactory.providePianoView();
        try {
        zDesktopManager.addDesktopElement(new AbstractDesktopElement(piano_vi.getViewPath(), true, StaticActivityContext.FALSE, new Impl_DesktopNodeDescriptor()) {
        protected JComponent createView() throws ComponentGenerationException {
        return piano_vi.getView();
        }
        public boolean hasExpired() {
        return false;
        }
        public int compareTo(Object o) {
        return -1;
        }
        public DesktopElement getCopy() {
        return this;
        }
        });
        } catch (ComponentGenerationException e) {
        e.printStackTrace();
        } catch (ChildViewNotAllowedException e) {
        e.printStackTrace();
        } catch (LogicalHierarchyException e) {
        e.printStackTrace();
        }
         */ 
        /*
        final ViewInstance piano_vi = SystemViewFactory.providePianoView();
        try {
            zDesktopManager.addDesktopElement(new AbstractDesktopElement(piano_vi.getViewPath(), true, StaticActivityContext.FALSE, new Impl_DesktopNodeDescriptor()) {
                protected JComponent createView() throws ComponentGenerationException {
                    return piano_vi.getView();
                }

                public boolean hasExpired() {
                    return false;
                }

                public int compareTo(Object o) {
                    return -1;
                }

                public DesktopElement getCopy() {
                    return this;
                }
            });
        } catch (ComponentGenerationException e) {
            e.printStackTrace();
        } catch (ChildViewNotAllowedException e) {
            e.printStackTrace();
        } catch (LogicalHierarchyException e) {
            e.printStackTrace();
        }
        */
    }

    private void initJIDE() {
        this.getDockingManager().setUsePref(false);
        this.getDockingManager().setProfileKey(DOCK_PROFILE_KEY);
        this.getDockingManager().setUseFrameBounds(true);
        this.getDockingManager().setUseFrameState(true);

        getDockingManager().setUndoLimit(10);
        getDockingManager().addUndoableEditListener((UndoableEditEvent e) -> {
            refreshUndoRedoMenuItems();
        });
        ObjectConverterManager.initDefaultConverter();
        CellEditorManager.initDefaultEditor();
        CellRendererManager.initDefaultRenderer();

        GeneralTableCellRenderer gr = new GeneralTableCellRenderer() {
            {
                setForeground(UIColors.getTableFirstSectionFG());
                setBackground(UIColors.getTableFirstSectionBG());
            }
        };
        CellRendererManager.registerRenderer(Boolean.class, gr);
        CellRendererManager.registerRenderer(Integer.class, gr);
        CellRendererManager.registerRenderer(String.class, gr);
        CellRendererManager.registerRenderer(Double.class, gr);

        zDesktopManager = new Impl_ZDesktopManager(getDockingManager());

        this.getDockingManager().getWorkspace().setLayout(new BorderLayout());
        this.getDockingManager().getWorkspace().add(zDesktopManager.getWorkspaceViewTreeModel().getRootDocumentPane(), BorderLayout.CENTER);
        /* JTable t = new JTable(new TableModel() {
            public int getRowCount() {
                return 400;
            }

            public int getColumnCount() {
                return 20;
            }

            public String getColumnName(int columnIndex) {
                return "";
            }

            public Class getColumnClass(int columnIndex) {
                return String.class;
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return false;
            }

            public Object getValueAt(int rowIndex, int columnIndex) {
                return "test";
            }

            public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            }

            public void addTableModelListener(TableModelListener l) {
            }

            public void removeTableModelListener(TableModelListener l) {
            }
        });
        GeneralTableCellRenderer gtr = new GeneralTableCellRenderer() {
            protected void setupLook(JTable table, Object value, boolean isSelected, int row, int column) {
                super.setupLook(table, value, isSelected, row, column);
                setForeground(UIColors.getTableFirstSectionFG());
                setBackground(UIColors.getTableFirstSectionBG());
            }
        };
        t.setDefaultRenderer(Object.class, gtr);
        this.getDockingManager().getWorkspace().add(t);
        */
        initFromZPrefs();
    }

    private void initFromZPrefs() {
        ChangeListener prefListener = (ChangeEvent e) -> {
            if (e.getSource() == ZoeosPreferences.ZPREF_sideBarRollover)
                getDockingManager().setSidebarRollover(ZoeosPreferences.ZPREF_sideBarRollover.getValue());
            else if (e.getSource() == ZoeosPreferences.ZPREF_animationSteps)
                getDockingManager().setSteps(ZoeosPreferences.ZPREF_animationSteps.getValue());
            else if (e.getSource() == ZoeosPreferences.ZPREF_animationStepDelay)
                getDockingManager().setStepDelay(ZoeosPreferences.ZPREF_animationStepDelay.getValue());
            else if (e.getSource() == ZoeosPreferences.ZPREF_animationInitDelay)
                getDockingManager().setInitDelay(ZoeosPreferences.ZPREF_animationInitDelay.getValue());
            else if (e.getSource() == com.pcmsolutions.system.ZoeosPreferences.ZPREF_autoHideShowContentsHidden)
                getDockingManager().setAuthideShowingContentHidden(com.pcmsolutions.system.ZoeosPreferences.ZPREF_autoHideShowContentsHidden.getValue());
        };
        ZoeosPreferences.addGlobalChangeListener(prefListener);
        getDockingManager().setSidebarRollover(ZoeosPreferences.ZPREF_sideBarRollover.getValue());
        getDockingManager().setSteps(com.pcmsolutions.system.ZoeosPreferences.ZPREF_animationSteps.getValue());
        getDockingManager().setStepDelay(com.pcmsolutions.system.ZoeosPreferences.ZPREF_animationStepDelay.getValue());
        getDockingManager().setInitDelay(com.pcmsolutions.system.ZoeosPreferences.ZPREF_animationInitDelay.getValue());
        getDockingManager().setAuthideShowingContentHidden(com.pcmsolutions.system.ZoeosPreferences.ZPREF_autoHideShowContentsHidden.getValue());
    }

    private void assertSmdiManager() {
        System.out.println("Asserting SMDI Manager");
        try {
            if (SMDIAgent.isSmdiAvailable()) {
                if (smdiManager == null)
                    smdiManager = new ZSmdiManagerDialog(this, false);
            } else {
                if (smdiManager != null)
                    smdiManager.dispose();
                smdiManager = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void showDeviceManager() {
        SwingUtilities.invokeLater(() -> {
            deviceManager.setVisible(true);
        });
    }

    public void hideDeviceManager() {
        SwingUtilities.invokeLater(() -> {
            deviceManager.setVisible(false);
        });
    }

    public void showTips() {
        SwingUtilities.invokeLater(() -> {
            tipOfTheDay.setVisible(true);
        });
    }

    public void hideTips() {
        SwingUtilities.invokeLater(() -> {
            tipOfTheDay.setVisible(false);
        });
    }

    public void showMidiManager() {
        SwingUtilities.invokeLater(() -> {
            midiManager.setVisible(true);
        });
    }

    public void hideMidiManager() {
        SwingUtilities.invokeLater(() -> {
            midiManager.setVisible(false);
        });
    }

    public void showLicenseManager() {
        SwingUtilities.invokeLater(() -> {
            licenseManager.setVisible(true);
        });
    }

    public void hideLicenseManager() {
        SwingUtilities.invokeLater(() -> {
            licenseManager.setVisible(false);
        });
    }

    public void showSmdiManager() {
        SwingUtilities.invokeLater(() -> {
            assertSmdiManager();
            if (smdiManager == null)
                JOptionPane.showMessageDialog(ZoeosFrame.this, "SMDI unavailable");
            else
                smdiManager.setVisible(true);
        });
    }

    public void hideSmdiManager() {
        SwingUtilities.invokeLater(() -> {
            if (smdiManager != null)
                smdiManager.setVisible(false);
        });
    }

    public void shutdown() {
        FlashMsg.globalDisable = true;
        new Thread(shutdownZoeos).start();
    }

    private void createMenuBar() {
        jMainMenu = new JMenuBar();
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent evt) {
                if (JOptionPane.showConfirmDialog(ZoeosFrame.this, "Are you sure you want to exit?", "Shutdown ZoeOS", JOptionPane.YES_NO_OPTION) == 0)
                    shutdown();
            }
        });
        createFileMenu();
        createManageMenu();
        createWindowMenu();
        createHelpMenu();
        finalizeMenus();
    }

    private void createStatusBar() {
        statusBar = new StatusBar();

/*
final LabelStatusBarItem label = new LabelStatusBarItem("Line");
label.setText("100:42");
label.setPreferredWidth(60);
label.setAlignment(JLabel.CENTER);
statusBar.addDesktopElement(label, JideBoxLayout.FLEXIBLE);
*/

/*
final OvrInsStatusBarItem ovr = new OvrInsStatusBarItem();
 ovr.setPreferredWidth(100);
 ovr.setAlignment(JLabel.CENTER);
 statusBar.addDesktopElement(ovr, JideBoxLayout.FLEXIBLE);
*/

//final TimeStatusBarItem time = new TimeStatusBarItem();
//statusBar.addDesktopElement(time, JideBoxLayout.FLEXIBLE);

        final MemoryStatusBarItem gc = new MemoryStatusBarItem();
        gc.setPreferredWidth(100);
        statusBar.add(gc, JideBoxLayout.FIX);

        getContentPane().add(statusBar, BorderLayout.AFTER_LAST_LINE);
    }

    private void finalizeMenus() {
        jMainMenu.add(jmFile);
        jMainMenu.add(jmManage);
        jMainMenu.add(jmWindow);
        jMainMenu.add(jmHelp);
        setJMenuBar(jMainMenu);
    }

    private void createWindowMenu() {

        jmWindow = new JMenu("Window");
        jmWindow.setMnemonic('W');

        JMenuItem item;
        JMenu submenu;

        undoMenuItem = new JMenuItem("Undo");
        jmWindow.add(undoMenuItem);
        redoMenuItem = new JMenuItem("Redo");
        jmWindow.add(redoMenuItem);
        undoMenuItem.setEnabled(false);
        redoMenuItem.setEnabled(false);

        undoMenuItem.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                getDockingManager().undo();
                refreshUndoRedoMenuItems();
            }
        });
        redoMenuItem.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                getDockingManager().redo();
                refreshUndoRedoMenuItems();
            }
        });

        jmWindow.addSeparator();

        submenu = new JMenu("Load");

        item = new JMenuItem("Custom Layout 1");
        item.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    getDockingManager().loadLayoutDataFrom(ZDesktopManager.LAYOUT_CUSTOM_1);
                    getDockingManager().setSidebarRollover(ZoeosPreferences.ZPREF_sideBarRollover.getValue());
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
        });
        submenu.add(item);

        item = new JMenuItem("Custom Layout 2");
        item.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    getDockingManager().loadLayoutDataFrom(ZDesktopManager.LAYOUT_CUSTOM_2);
                    getDockingManager().setSidebarRollover(ZoeosPreferences.ZPREF_sideBarRollover.getValue());
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
        });
        submenu.add(item);

        item = new JMenuItem("Custom Layout 3");
        item.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    getDockingManager().loadLayoutDataFrom(ZDesktopManager.LAYOUT_CUSTOM_3);
                    getDockingManager().setSidebarRollover(ZoeosPreferences.ZPREF_sideBarRollover.getValue());
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
        });
        submenu.add(item);

        jmWindow.add(submenu);

        jmWindow.addSeparator();

        submenu = new JMenu("Save");
        item = new JMenuItem("as Custom Layout 1");
        item.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                getDockingManager().saveLayoutDataAs(ZDesktopManager.LAYOUT_CUSTOM_1);
            }
        });
        submenu.add(item);

        item = new JMenuItem("as Custom Layout 2");
        item.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                getDockingManager().saveLayoutDataAs(ZDesktopManager.LAYOUT_CUSTOM_2);
            }
        });
        submenu.add(item);

        item = new JMenuItem("as Custom Layout 3");
        item.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                getDockingManager().saveLayoutDataAs(ZDesktopManager.LAYOUT_CUSTOM_3);
            }
        });
        submenu.add(item);

        jmWindow.add(submenu);

        jmWindow.addSeparator();

        item = new JMenuItem("Reset Layout");
        item.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // TODO!! investigate next two lines
                getDockingManager().setUseFrameBounds(false);
                getDockingManager().setUseFrameState(false);
                getDockingManager().resetToDefault();
                getDockingManager().setSidebarRollover(ZoeosPreferences.ZPREF_sideBarRollover.getValue());
            }
        });
        jmWindow.add(item);

        jmWindow.addSeparator();

        item = new JMenuItem("Toggle Auto Hide Rollover");
        item.setMnemonic('T');
        item.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //getDockingManager().setSidebarRollover(!getDockingManager().isSidebarRollover());
                ZoeosPreferences.ZPREF_sideBarRollover.toggleValue();
            }
        });
        jmWindow.add(item);
    }

    private void refreshUndoRedoMenuItems() {
        undoMenuItem.setEnabled(getDockingManager().getUndoManager().canUndo());
        undoMenuItem.setText(getDockingManager().getUndoManager().getUndoPresentationName());
        redoMenuItem.setEnabled(getDockingManager().getUndoManager().canRedo());
        redoMenuItem.setText(getDockingManager().getUndoManager().getRedoPresentationName());
    }

    private void createHelpMenu() {
        jmHelp = new JMenu();

        jmiLicenseManager = new JMenuItem();
        jmiHelp = new JMenuItem();
        jmiTipOfTheDay = new JMenuItem();
        jmiProductTour = new JMenuItem();
        jmiRequestFunctionality = new JMenuItem();
        jmiReportBug = new JMenuItem();
        jmiAboutBox = new JMenuItem();

        jmHelp.setText("Help");
        jmHelp.setMnemonic(KeyEvent.VK_H);
        jmHelp.addActionListener((ActionEvent evt) -> {
        });

// Find the HelpSet File and create the HelpSet object:
//String helpHS = "zoeosHelpSet.hs";

        String helpHS = "zoeosHelp.hs";
        ClassLoader cl = ZoeosFrame.class.getClassLoader();
        try {
            URL hsURL = HelpSet.findHelpSet(cl, helpHS);
            hb = new HelpSet(null, hsURL).createHelpBroker();
        } catch (Exception ee) {
// Say what the exception really is
            System.out.println("HelpSet " + ee.getMessage());
            System.out.println("HelpSet " + helpHS + " not found");
        }

        jmiHelp.setText("Help Contents");
        jmiHelp.setMnemonic(KeyEvent.VK_H);

        if (hb != null) {
            jmiHelp.addActionListener(new CSH.DisplayHelpFromSource(hb));
        } else
            jmiHelp.addActionListener((ActionEvent e) -> {
                UserMessaging.showInfo("Help not available");
        });

        jmHelp.add(jmiHelp);

        jmiTipOfTheDay.setText("Tip of the Day");
        jmiTipOfTheDay.setMnemonic(KeyEvent.VK_T);
        jmiTipOfTheDay.addActionListener((ActionEvent e) -> {
            tipOfTheDay.show();
        });
        jmHelp.add(jmiTipOfTheDay);

        jmiProductTour.setText("Zuonics Homepage & Product Tour");
        jmiProductTour.setMnemonic(KeyEvent.VK_B);
        jmiProductTour.addActionListener((ActionEvent evt) -> {
            BrowserControl.displayURL("http://www.zuonics.com");
            BrowserControl.displayURL("http://code.google.com/p/zoeos/");
        });
        jmHelp.add(jmiProductTour);
//        jmHelp.addSeparator();

        jmiRequestFunctionality.setText("Enhancement request");
        jmiRequestFunctionality.setMnemonic(KeyEvent.VK_R);
        jmiRequestFunctionality.addActionListener((ActionEvent evt) -> {
            BrowserControl.displayURL("mailto:request@zuonics.com?subject=Enhancement request for " + Zoeos.versionStr);
            BrowserControl.displayURL("mailto:zoeoscommon@googlemail.com?subject=Enhancement request for " + Zoeos.versionStr);
        });
        jmHelp.add(jmiRequestFunctionality);

        jmiReportBug.setText("Report a Bug");
        jmiReportBug.setMnemonic(KeyEvent.VK_B);
        jmiReportBug.addActionListener((ActionEvent evt) -> {
            BrowserControl.displayURL("mailto:bugs@zuonics.com?subject=Bug in " + Zoeos.versionStr + "&body=< PASTE DEVICE CONFIGURATION HERE >");
            BrowserControl.displayURL("mailto:zoeoscommon@googlemail.com?subject=Bug in " + Zoeos.versionStr + "&body=< PASTE DEVICE CONFIGURATION HERE >");
//mailto:astark1@unl.edu?subject=Comments from MailTo Syntax Page
//JOptionPane.showMessageDialog(ZoeosFrame.getInstance(), "Not available in demo");
        });
        jmHelp.add(jmiReportBug);

/*        if (!Zoeos.isEvaluation()) {
            jmiLicenseManager.setText("Manage License Keys");
            jmiLicenseManager.setMnemonic(KeyEvent.VK_L);
            jmiLicenseManager.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    licenseManager.show();
                }
            });
            jmHelp.add(jmiLicenseManager);
        }
        jmHelp.addSeparator();*/

        jmiAboutBox.setText("About");
        jmiAboutBox.setMnemonic(KeyEvent.VK_A);
        jmiAboutBox.addActionListener((ActionEvent evt) -> {
            JOptionPane.showMessageDialog(ZoeosFrame.getInstance(), Zoeos.aboutMessage + "\n\nJVM version " +
                    System.getProperty("java.version") + "\n" +
                    " by " + System.getProperty("java.vendor"), "About ZoeOS", JOptionPane.INFORMATION_MESSAGE, new ImageIcon(ZoeosFrame.class.getResource("/zoeosFrameIcon.gif")));
        });
        jmHelp.add(jmiAboutBox);
    }

    private void createManageMenu() {
        jmManage = new JMenu();

        jmiDeviceManager = new JMenuItem();
        jmiMidiManager = new JMenuItem();
        jmiSMDIManager = new JMenuItem();
        // jmiTaskManager = new JMenuItem();

        jmManage.setText("Manage");
        jmManage.setMnemonic(KeyEvent.VK_M);

        jmiDeviceManager.setText("Devices");
        jmiDeviceManager.setMnemonic(KeyEvent.VK_D);
        jmiDeviceManager.addActionListener((ActionEvent evt) -> {
            deviceManager.show();
        });
        jmManage.add(jmiDeviceManager);

        jmiMidiManager.setText("Midi");
        jmiMidiManager.setMnemonic(KeyEvent.VK_M);
        jmiMidiManager.addActionListener((ActionEvent evt) -> {
            midiManager.show();
        });
        jmManage.add(jmiMidiManager);

        jmiSMDIManager.setText("SMDI");
        jmiSMDIManager.setMnemonic(KeyEvent.VK_S);
        jmiSMDIManager.addActionListener((ActionEvent evt) -> {
            assertSmdiManager();
            if (smdiManager == null)
                JOptionPane.showMessageDialog(ZoeosFrame.this, "SMDI unavailable");
            else
                smdiManager.show();
        });
        jmManage.add(jmiSMDIManager);

        /*
         jmiTaskManager.setText("Show Tasks");
         jmiTaskManager.setMnemonic(KeyEvent.VK_T);
         jmiTaskManager.addActionListener(new ActionListener() {
             public void actionPerformed(ActionEvent evt) {
                 showTasks();
             }
         });
         jmManage.add(jmiTaskManager);
         */
    }

    private void createFileMenu() {
        jmFile = new JMenu();
        jmFile.setText("File");
        jmFile.setMnemonic(KeyEvent.VK_F);

        jmiExit = new JMenuItem();
        jmiExit.setText("Exit");
        jmiExit.setMnemonic(KeyEvent.VK_X);
        jmiExit.addActionListener((ActionEvent e) -> {
            if (JOptionPane.showConfirmDialog(ZoeosFrame.this, "Are you sure you want to exit?", "Shutdown ZoeOS", JOptionPane.YES_NO_OPTION) == 0)
                shutdown();
        });
        jmFile.add(jmiExit);
    }

    private Impl_ZDesktopManager zDesktopManager;

    public ZDesktopManager getZDesktopManager() {
        return zDesktopManager;
    }

    public static void main(String args[]) {
        try {

            try {
                // initLook();
                com.jidesoft.utils.Lm.verifyLicense("Gareth Pidgeon", "ZoeOS", "hbWjU7JFKe4Sw0tM774Zm:KRKueaiaK1");

                //JFrame.setDefaultLookAndFeelDecorated(true);
                // JDialog.setDefaultLookAndFeelDecorated(true);
/*
com.incors.plaf.alloy.AlloyLookAndFeel.setProperty("alloy.licenseCode", "a#PCM_Solutions_Ltd#1pj3nkw#a522i8");
com.incors.plaf.alloy.AlloyLookAndFeel.setProperty("alloy.isLookAndFeelFrameDecoration", "true");
AlloyLookAndFeel.setProperty("alloy.isButtonPulseEffectEnabled", "false");


//com.incors.plaf.alloy.AlloyTheme theme = new com.incors.plaf.alloy.themes.glass.GlassTheme(new CustomFontTheme(new String[]{"Arial", "Courier"}, 10, 9));
//com.incors.plaf.alloy.AlloyTheme theme = new com.incors.plaf.alloy.themes.acid.AcidTheme(new CustomFontTheme(new String[]{"Arial", "Courier"}, 10, 9));
//com.incors.plaf.alloy.AlloyTheme theme = new com.incors.plaf.alloy.themes.glass.GlassTheme();
//com.incors.plaf.alloy.AlloyTheme theme = new com.incors.plaf.alloy.themes.bedouin.BedouinTheme(new CustomFontTheme(new String[]{"Arial", "Courier"}, 11, 11));
//                com.incors.plaf.alloy.AlloyTheme theme = new com.incors.plaf.alloy.themes.bedouin.BedouinTheme(new CustomFontTheme(new String[]{"Microsoft Sans Serif", "Arial"}, 11, 11));

com.incors.plaf.alloy.AlloyTheme theme = new com.incors.plaf.alloy.themes.bedouin.BedouinTheme(new AlloyFontTheme() {
    //com.incors.plaf.alloy.AlloyTheme theme = new com.incors.plaf.alloy.themes.acid.AcidTheme(new AlloyFontTheme() {
    public FontUIResource getControlTextFont() {
        return new FontUIResource("Microsoft Sans Serif", Font.PLAIN, 10);
    }

    public FontUIResource getSystemTextFont() {
        return new FontUIResource("Microsoft Sans Serif", Font.PLAIN, 10);
    }

    public FontUIResource getUserTextFont() {
        return new FontUIResource("Microsoft Sans Serif", Font.PLAIN, 10);
    }

    public FontUIResource getMenuTextFont() {
        return new FontUIResource("Microsoft Sans Serif", Font.PLAIN, 11);
    }

    public FontUIResource getWindowTitleFont() {
        return new FontUIResource("Microsoft Sans Serif", Font.PLAIN, 10);
    }

    public FontUIResource getSubTextFont() {
        return new FontUIResource("Microsoft Sans Serif", Font.PLAIN, 10);
    }
});

javax.swing.LookAndFeel alloyLnF = new com.incors.plaf.alloy.AlloyLookAndFeel(theme);
//AlloyJideLookAndFeel ajlf = new AlloyJideLookAndFeel();
//ajlf.setTheme(theme, true);
javax.swing.UIManager.setLookAndFeel(LookAndFeelFactory.ALLOY_LNF);
*/
                System.setProperty("sun.awt.noerasebackground", "true");
                //Toolkit.getDefaultToolkit().setDynamicLayout(true);
                try {
                    //UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                    String os = System.getProperty("os.name");
                    com.sun.java.swing.plaf.windows.WindowsLookAndFeel lf;

                    System.out.println(UIManager.getSystemLookAndFeelClassName());
                    if (os.contains("XP"))
                        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                    else
                        UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
                    LookAndFeelFactory.installJideExtension();
                    System.out.println(os);
                } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
                }
                Font defFont = new Font("Arial", Font.PLAIN, 10); //(Toolkit.getDefaultToolkit().getScreenSize().getHeight() > 768?11:10));
                // UIManager.getDefaults().put("Label.font", defFont);

                //using the put keys to set the font size for the different visual components.
                System.out.println("UI property count = " + UIManager.getDefaults().size());
                ArrayList al = new ArrayList();
                for (Enumeration e = UIManager.getDefaults().keys(); e.hasMoreElements();) {
                    Object o = e.nextElement();
                    if (o.toString().contains(".font"))
                        UIManager.getDefaults().put(o,
                                defFont);
                    al.add(o);

                }
//                Collections.sort(al);
               // for (Object o : al)
                //    System.out.println(o + " : " + UIManager.getDefaults().get(o));

//using the put keys to set the font size for the different visual components.
                System.out.println("UI property count = " + UIManager.getLookAndFeelDefaults().size());
                 al = new ArrayList();
                for (Enumeration e = UIManager.getLookAndFeelDefaults().keys(); e.hasMoreElements();) {
                    Object o = e.nextElement();
                    if (o.toString().contains(".font"))
                        UIManager.getLookAndFeelDefaults().put(o,
                                defFont);
                    al.add(o);

                }
  //              Collections.sort(al);
               // for (Object o : al)
                //    System.out.println(o + " : " + UIManager.getLookAndFeelDefaults().get(o));

                ColorUIResource yellowish = new ColorUIResource(UIColors.applyAlpha(Color.yellow, 200));
                ColorUIResource redish = new ColorUIResource(UIColors.applyAlpha(Color.red, 100));
                ColorUIResource orangeish = new ColorUIResource(UIColors.applyAlpha(Color.orange, 250));
                ColorUIResource whiteish = new ColorUIResource(UIColors.applyAlpha(Color.white, 250));
                Color blueish = UIColors.getDefaultBG();
                Color grayish = new Color(75, 75, 90);
                /*
                //UIManager.getDefaults().put("SidePane.margin", Color.black);
                UIManager.getDefaults().put("ComboBox.background", blueish);

                UIManager.getDefaults().put("Button.background", orangeish);
                UIManager.getDefaults().put("Button.focusedBackground", orangeish);
                UIManager.getDefaults().put("Button.light", redish);
                UIManager.getDefaults().put("Button.foreground", blueish);
                UIManager.getDefaults().put("JideButton.background", orangeish);
                UIManager.getDefaults().put("JideButton.light", redish);
                UIManager.getDefaults().put("JideButton.foreground", blueish);

                UIManager.getDefaults().put("Panel.background", blueish);
                UIManager.getDefaults().put("ScrollPane.background", blueish);
                UIManager.getDefaults().put("ProgressBar.background", blueish);
                UIManager.getDefaults().put("ProgressBar.foreground", redish);
                UIManager.getDefaults().put("ProgressBar.shadow", redish);
                UIManager.getDefaults().put("ProgressBar.selectionForeground", redish);
                UIManager.getDefaults().put("ProgressBar.selectionBackground", redish);
                UIManager.getDefaults().put("ProgressBar.highlight", redish);
                UIManager.getDefaults().put("SidePane.background", new ColorUIResource(UIColors.getDefaultBG()));
                UIManager.getDefaults().put("SidePane.foreground", Color.DARK_GRAY);
                UIManager.getDefaults().put("SidePane.buttonBackground", grayish);
                UIManager.getDefaults().put("CollapsiblePane.background", redish);

                UIManager.getDefaults().put("MenuBar.background", grayish);
                UIManager.getDefaults().put("MenuBar.foreground", orangeish);

                UIManager.getDefaults().put("PopupMenu.background", new ColorUIResource(UIColors.getDefaultBG()));
                UIManager.getDefaults().put("PopupMenu.foreground", orangeish);

                UIManager.getDefaults().put("Menu.background",new ColorUIResource(UIColors.getDefaultBG()));
                UIManager.getDefaults().put("Menu.foreground", yellowish);
                UIManager.getDefaults().put("Menu.selectionBackground", orangeish);

                UIManager.getDefaults().put("MenuItem.background", new ColorUIResource(UIColors.getDefaultBG()));
                UIManager.getDefaults().put("MenuItem.foreground", orangeish);
                UIManager.getDefaults().put("MenuItem.selectionBackground", orangeish);

                UIManager.getDefaults().put("JideButton.background", new ColorUIResource(UIColors.getDefaultBG()));
                UIManager.getDefaults().put("Tree.background", new ColorUIResource(UIColors.getDefaultBG()));
                UIManager.getDefaults().put("JideTabbedPane.tabAreaBackground", grayish);
                UIManager.getDefaults().put("JideTabbedPane.selectedTabBackground", UIColors.getTableSecondSectionBG());
                UIManager.getDefaults().put("JideTabbedPane.background", grayish);
                UIManager.getDefaults().put("JideTabbedPane.foreground", whiteish);
                UIManager.getDefaults().put("JideTabbedPane.unselectedTabTextForeground", yellowish);
                UIManager.getDefaults().put("JideTabbedPane.light", blueish);
                UIManager.getDefaults().put("DockableFrame.background", new ColorUIResource(UIColors.getDefaultBG()));
                UIManager.getDefaults().put("ScrollBar.trackForeground", new ColorUIResource(UIColors.getDefaultBG()));
                */
            } finally {
            }

            System.out.println(Thread.currentThread());
            final ZoeosFrame zf = ZoeosFrame.getInstance();
            zf.setVisible(true);
            if (ZoeosPreferences.ZPREF_showTipsAtStartup.getValue())
                zf.showTips();

            if (ZoeosPreferences.ZPREF_autoHuntAtStartup.getValue() == true)
                SwingUtilities.invokeLater(() -> {
                    new Thread() {
                        @Override
                        public void run() {
                            try {
                                Zoeos.getInstance().getDeviceManager().performHunt();
                            } finally {
                                try {
//                                        com.exe4j.Controller.hide();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }.start();
            });
            else
                try {
//                    com.exe4j.Controller.hide();
                } catch (Exception e) {
                    e.printStackTrace();
                }
        } catch (OutOfMemoryError e) {
            System.out.println(e);
            System.exit(-1);
        }
    }
}
