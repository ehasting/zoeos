package com.pcmsolutions.device.EMU.E4.gui.device;

import com.pcmsolutions.device.EMU.E4.DeviceContext;
import com.pcmsolutions.device.EMU.E4.gui.EnclosureNorthenComponentProvider;
import com.pcmsolutions.device.EMU.E4.gui.colors.UIColors;
import com.pcmsolutions.gui.desktop.SessionableComponent;
import com.pcmsolutions.system.Indexable;
import com.pcmsolutions.system.IntPool;
import com.pcmsolutions.system.ZDisposable;
import com.pcmsolutions.system.ZUtilities;

import javax.swing.*;
import java.awt.*;


public class DefaultDeviceEnclosurePanel extends AbstractDeviceEnclosurePanel implements ZDisposable, Indexable, SessionableComponent {
    protected JComponent enclosedComponent;
    protected JScrollPane scrollPane;

    public void init(DeviceContext device, JComponent enclosedComponent) throws Exception {
        super.init(device);

        this.enclosedComponent = enclosedComponent;
        syncPanel();
    }

    @Override
    protected void buildRunningPanel() {
        runningPanel = new JPanel() {
            @Override
            public Color getForeground() {
                return UIColors.getDefaultFG();
            }

            @Override
            public Color getBackground() {
                return UIColors.getDefaultBG();
            }
        };
        scrollPane = new JScrollPane(this.enclosedComponent);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUnitIncrement(UIColors.getTableRowHeight());
        scrollPane.getVerticalScrollBar().setBlockIncrement(UIColors.getTableRowHeight() * 4);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(scrollPane.getVerticalScrollBar().getUnitIncrement() * 2);
        scrollPane.getHorizontalScrollBar().setBlockIncrement(scrollPane.getVerticalScrollBar().getBlockIncrement() * 2);
        scrollPane.setWheelScrollingEnabled(true);
        runningPanel.setLayout(new BorderLayout());
        runningPanel.add(scrollPane, BorderLayout.CENTER);

        if (enclosedComponent instanceof EnclosureNorthenComponentProvider) {
            if (((EnclosureNorthenComponentProvider) enclosedComponent).isEnclosureNorthenComponentAvailable())
                runningPanel.add(((EnclosureNorthenComponentProvider) enclosedComponent).getEnclosureNorthenComponent(), BorderLayout.NORTH);
        }
    }

    public JScrollPane getScrollPane() {
        return scrollPane;
    }

    public JComponent getEnclosedComponent() {
        return enclosedComponent;
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
        super.zDispose();
        if (enclosedComponent instanceof ZDisposable)
            ((ZDisposable) enclosedComponent).zDispose();
        enclosedComponent = null;
        scrollPane = null;
    }

    @Override
    public Integer getIndex() {
        if (enclosedComponent instanceof Indexable)
            return ((Indexable) enclosedComponent).getIndex();
        return IntPool.get(0);
    }

    private static final String vScrollTagId = DefaultDeviceEnclosurePanel.class.toString() + "VSCROLL";
    private static final String hScrollTagId = DefaultDeviceEnclosurePanel.class.toString() + "HSCROLL";

    @Override
    public String retrieveComponentSession() {
        StringBuilder sb = new StringBuilder();
        try {
            if (enclosedComponent instanceof SessionableComponent)
                sb.append(((SessionableComponent) enclosedComponent).retrieveComponentSession());

            if (scrollPane != null) {
                int vv = scrollPane.getVerticalScrollBar().getValue();
                sb.append(ZUtilities.makeTaggedField(vScrollTagId, String.valueOf(vv)));
                int hv = scrollPane.getHorizontalScrollBar().getValue();
                sb.append(ZUtilities.makeTaggedField(hScrollTagId, String.valueOf(hv)));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    @Override
    public void restoreComponentSession(final String sessStr) {

        if (sessStr != null && !sessStr.equals("")) {
            try {
                if (enclosedComponent instanceof SessionableComponent)
                    ((SessionableComponent) enclosedComponent).restoreComponentSession(sessStr);

                SwingUtilities.invokeLater(() -> {
                    String vsSess = ZUtilities.extractTaggedField(sessStr, vScrollTagId);
                    if (vsSess != null)
                        scrollPane.getVerticalScrollBar().setValue(Integer.parseInt(vsSess));
                    String hsSess = ZUtilities.extractTaggedField(sessStr, hScrollTagId);
                    if (hsSess != null)
                        scrollPane.getHorizontalScrollBar().setValue(Integer.parseInt(hsSess));
                    new DefaultListSelectionModel();
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    /*
    public String getTitle() {
        return ((TitleProvider) enclosedComponent).getTitle();
    }

    public String getReducedTitle() {
        return ((TitleProvider) enclosedComponent).getReducedTitle();
    }

    public final void addTitleProviderListener(TitleProviderListener tpl) {
        if (enclosedComponent instanceof TitleProvider)
            ((TitleProvider) enclosedComponent).addTitleProviderListener(tpl);
    }

    public final void removeTitleProviderListener(TitleProviderListener tpl) {
        if (enclosedComponent instanceof TitleProvider)
            ((TitleProvider) enclosedComponent).removeTitleProviderListener(tpl);
    }

    public Icon getIcon() {
        return ((TitleProvider) enclosedComponent).getIcon();
    }
    */
}
