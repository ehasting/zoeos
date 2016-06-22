package com.pcmsolutions.device.EMU.E4.gui.preset;

import com.pcmsolutions.device.EMU.E4.gui.AbstractTableCellRenderer;
import com.pcmsolutions.device.EMU.E4.gui.colors.UIColors;
import com.pcmsolutions.device.EMU.E4.gui.table.ColumnData;
import com.pcmsolutions.device.EMU.E4.gui.table.RowHeaderedAndSectionedTable;
import com.pcmsolutions.device.EMU.E4.gui.table.SectionData;
import com.pcmsolutions.system.ZUtilities;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;

/**
 * Created by IntelliJ IDEA.
 * User: paulmeehan
 * Date: 09-Dec-2003
 * Time: 17:01:12
 * To change this template use Options | File Templates.
 */
public class WinTableCellRenderer extends AbstractTableCellRenderer {

    public static final WinTableCellRenderer[] CANONICAL_RENDERERS = new WinTableCellRenderer[]
    {
        new WinTableCellRenderer(WinValueProfile.WIN_POS_LOW),
        new WinTableCellRenderer(WinValueProfile.WIN_POS_LOW_FADE),
        new WinTableCellRenderer(WinValueProfile.WIN_POS_HIGH),
        new WinTableCellRenderer(WinValueProfile.WIN_POS_HIGH_FADE)
    };

    private final int winPos;
    private WinValueProfile wvp;
    private boolean isRectangle;

    public WinTableCellRenderer(int winPos) {
        this.winPos = winPos;
    }

    private static final int graphUnderTextAlpha = 200;

  // static int count=0;
    @Override
    protected void paintComponent(Graphics g) {
       // if ( true)
         //   return;
        //System.out.println("Painting win " + count++);
        Graphics2D g2d = ((Graphics2D) g);
        //RenderingHints hints = g2d.getRenderingHints();
        //g2d.setRenderingHints(UIColors.tableRH);
        try {
            int w = getWidth();
            int h = getHeight();
            Color e_bg = getBackground();
            Color bg = UIColors.applyAlpha(e_bg, UIColors.tableAlpha);
            Color sbg = e_bg;
            Color e_fg = getForeground();
            Color e_color = g2d.getColor();
            Paint e_paint = g2d.getPaint();
            Insets ins = this.getInsets();
            //e_bg= UIColors.applyAlpha(Color.lightGray, 200);
            try {
                if (wvp != null) {
                    int mode;
                    int type = wvp.getType();
                    switch (type) {
                        case WinValueProfile.KEY_WIN:
                            mode = WinValueProfile.ZPREF_keyWinDisplayMode.getValue();
                            break;
                        case WinValueProfile.VEL_WIN:
                            mode = WinValueProfile.ZPREF_velWinDisplayMode.getValue();
                            break;
                        case WinValueProfile.RT_WIN:
                            mode = WinValueProfile.ZPREF_rtWinDisplayMode.getValue();
                            break;
                        default:
                            return;
                    }
                    switch (mode) {
                        case WinValueProfile.MODE_DISPLAY_TEXT_AND_GRAPH:
                            drawGraph(g2d, UIColors.applyAlpha(e_bg, graphUnderTextAlpha), UIColors.applyAlpha(e_bg, graphUnderTextAlpha), e_bg);
                            if (isDropCell) {
                                GradientPaint gp;
                                gp = new GradientPaint(0, h * UIColors.tableDropTargetGradientFactor, UIColors.getDropColor(), 0, 0, Color.white, false);
                                g2d.setPaint(gp);
                                g2d.fillRoundRect(0, 0, w, h, 10, 10);
                                setBorder(bdrNormal);
                                setOpaque(true);
                            } else if (selected) {
                                setBorder(bdrSel);
                                setOpaque(true);
                            } else {
                                setBorder(bdrNormal);
                                setOpaque(true);
                            }
                            setBackground(bg);
                            super.paintComponent(g);
                            return;
                        case WinValueProfile.MODE_DISPLAY_GRAPH:
                            if (isDropCell) {
                                drawGraph(g2d, e_bg, e_bg, e_bg);
                                GradientPaint gp;
                                gp = new GradientPaint(0, h * UIColors.tableDropTargetGradientFactor, UIColors.getDropColor(), 0, 0, Color.white, false);
                                g2d.setPaint(gp);
                                g2d.fillRoundRect(0, 0, w, h, 10, 10);
                                setBorder(bdrNormal);
                                //setOpaque(true);
                                setForeground(bg);
                                setBackground(bg);
                                paintBackground(g2d, bg);
                                //super.paintComponent(g);
                            } else if (selected) {
                                setBorder(bdrSel);
                                ///setOpaque(true);
                                setForeground(bg);
                                setBackground(bg);
                                //super.paintComponent(g);
                                paintBackground(g2d, bg);
                                drawGraph(g2d, e_bg, e_bg, e_bg);
                            } else {
                                setBorder(bdrNormal);
                                //setOpaque(true);
                                setForeground(bg);
                                setBackground(bg);
                                drawGraph(g2d, e_bg, e_bg, e_bg);
                                drawGraph(g2d, e_bg, e_bg, e_bg);
                                paintBackground(g2d, bg);
                            }
                            return;
                        case WinValueProfile.MODE_DISPLAY_TEXT:
                        default:
                            // drop out to default code below
                    }
                }
                if (isDropCell) {
                    GradientPaint gp;
                    gp = new GradientPaint(0, h * UIColors.tableDropTargetGradientFactor, UIColors.getDropColor(), 0, 0, Color.white, false);
                    g2d.setPaint(gp);
                    g2d.fillRoundRect(0, 0, w, h, 10, 10);
                    setBorder(bdrNormal);
                    setBackground(bg);
                    setOpaque(true);
                    super.paintComponent(g);
                } else if (selected) {
                    GradientPaint gp;
                    gp = new GradientPaint(0, h * UIColors.tableSelectionGradientFactor, bg, 0, 0, sbg, false);
                    g2d.setPaint(gp);
                    g2d.fillRect(0, 0, w, h);
                    setBorder(bdrSel);
                    setOpaque(false);
                    super.paintComponent(g);
                } else {
                    setBorder(bdrNormal);
                    setBackground(bg);
                    setOpaque(true);
                    super.paintComponent(g);
                }
            } finally {
                g2d.setPaint(e_paint);
                g2d.setColor(e_color);
                setBackground(e_bg);
                setForeground(e_fg);
            }
        } finally {
           // g2d.setRenderingHints(hints);
        }
    }

    private void paintBackground(Graphics2D g, Color bg) {
        Insets ins = this.getInsets();
        g.setPaint(bg);
        g.fill(new Rectangle.Double(ins.left, ins.top, getWidth() - ins.left - ins.right, getHeight() - ins.top - ins.bottom));
        paintBorder(g);
    }

    private static final double graphCoverage = 0.50;
    private static Stroke lineStroke = new BasicStroke(2);
    protected static Stroke axisStroke = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{1, 1}, 0);

    protected void drawGraph(Graphics2D g2d, Color low, Color high, Color axis) {
        if (wvp != null) {
            int w = this.getWidth();
            int h = this.getHeight();

            /*
            Insets ins = this.getInsets();
            int fw = w - ins.left - ins.right;
            int fh = h - ins.top - ins.bottom;
            */

            if (wvp.getHighFade() == 0 && wvp.getLowFade() == 0)
                isRectangle = true;
            else
                isRectangle = false;

            int type = wvp.getType();
            GeneralPath p;
            switch (type) {
                case WinValueProfile.KEY_WIN:
                    p = ZUtilities.getKeyWinAxes();
                    break;
                case WinValueProfile.VEL_WIN:
                    p = ZUtilities.getVelWinAxes();
                    break;
                case WinValueProfile.RT_WIN:
                    p = ZUtilities.getRTWinAxes();
                    break;
                default:
                    return;
            }

            GeneralPath ls = null;
            GeneralPath hs = null;

            int gm = WinValueProfile.ZPREF_graphMode.getValue();
            switch (gm) {
                case WinValueProfile.MODE_GRAPH_HIGH:
                    hs = ZUtilities.getHighWindowShape(wvp.getLow(), wvp.getHigh(), wvp.getHighFade());
                    break;
                case WinValueProfile.MODE_GRAPH_LOW:
                    ls = ZUtilities.getLowWindowShape(wvp.getLow(), wvp.getLowFade(), wvp.getHigh());
                    break;
                case WinValueProfile.MODE_GRAPH_MERGE:
                    ls = ZUtilities.getWindowShape(wvp.getLow(), wvp.getLowFade(), wvp.getHigh(), wvp.getHighFade());
                    break;
                default:
                case WinValueProfile.MODE_GRAPH_OVERLAY:
                    ls = ZUtilities.getLowWindowShape(wvp.getLow(), wvp.getLowFade(), wvp.getHigh());
                    hs = ZUtilities.getHighWindowShape(wvp.getLow(), wvp.getHigh(), wvp.getHighFade());
            }

            /*
            ls.transform(AffineTransform.getScaleInstance((fw * 4) / 128.0, graphCoverage * (fh / 128.0)));
            hs.transform(AffineTransform.getScaleInstance((fw * 4) / 128.0, graphCoverage * (fh / 128.0)));
            */
            if (ls != null)
                ls.transform(AffineTransform.getScaleInstance((w * 4) / 128.0, graphCoverage * (h / 128.0)));

            if (hs != null)
                hs.transform(AffineTransform.getScaleInstance((w * 4) / 128.0, graphCoverage * (h / 128.0)));

            p.transform(AffineTransform.getScaleInstance((w * 4) / 128.0, h / 128.0));

            switch (winPos) {
                case WinValueProfile.WIN_POS_LOW:
                    /*
                    ls.transform(AffineTransform.getTranslateInstance(ins.left, ins.top + (graphCoverage * fh) / 2));
                    hs.transform(AffineTransform.getTranslateInstance(ins.left, ins.top + (graphCoverage * fh) / 2));
                    */
                    if (ls != null)
                        ls.transform(AffineTransform.getTranslateInstance(0, (1 - graphCoverage) * h / 2));
                    if (hs != null)
                        hs.transform(AffineTransform.getTranslateInstance(0, (1 - graphCoverage) * h / 2));
                    p.transform(AffineTransform.getTranslateInstance(0, 0));

                    break;
                case WinValueProfile.WIN_POS_LOW_FADE:
                    /*
                    ls.transform(AffineTransform.getTranslateInstance(ins.left - fw, ins.top + (graphCoverage * fh) / 2));
                    hs.transform(AffineTransform.getTranslateInstance(ins.left - fw, ins.top + (graphCoverage * fh) / 2));
                    */
                    if (ls != null)
                        ls.transform(AffineTransform.getTranslateInstance(-w, (1 - graphCoverage) * h / 2));
                    if (hs != null)
                        hs.transform(AffineTransform.getTranslateInstance(-w, (1 - graphCoverage) * h / 2));
                    p.transform(AffineTransform.getTranslateInstance(-w, 0));
                    break;
                case WinValueProfile.WIN_POS_HIGH:
                    /*
                    ls.transform(AffineTransform.getTranslateInstance(ins.left - fw * 2, ins.top + (graphCoverage * fh) / 2));
                    hs.transform(AffineTransform.getTranslateInstance(ins.left - fw * 2, ins.top + (graphCoverage * fh) / 2));
                    */
                    if (ls != null)
                        ls.transform(AffineTransform.getTranslateInstance(-w * 2, (1 - graphCoverage) * h / 2));
                    if (hs != null)
                        hs.transform(AffineTransform.getTranslateInstance(-w * 2, (1 - graphCoverage) * h / 2));
                    p.transform(AffineTransform.getTranslateInstance(-w * 2, 0));
                    break;
                case WinValueProfile.WIN_POS_HIGH_FADE:
                    /*
                    ls.transform(AffineTransform.getTranslateInstance(ins.left - fw * 3, ins.top + (graphCoverage * fh) / 2));
                    hs.transform(AffineTransform.getTranslateInstance(ins.left - fw * 3, ins.top + (graphCoverage * fh) / 2));
                    */
                    if (ls != null)
                        ls.transform(AffineTransform.getTranslateInstance(-w * 3, (1 - graphCoverage) * h / 2));
                    if (hs != null)
                        hs.transform(AffineTransform.getTranslateInstance(-w * 3, (1 - graphCoverage) * h / 2));
                    p.transform(AffineTransform.getTranslateInstance(-w * 3, 0));
                    break;
                default:
                    return;
            }

            Stroke os = g2d.getStroke();

            // draw axes
            g2d.setStroke(axisStroke);
            g2d.setColor(axis);
            g2d.draw(p);

            g2d.setStroke(lineStroke);
            try {
                gm = WinValueProfile.ZPREF_graphMode.getValue();
                switch (gm) {
                    case WinValueProfile.MODE_GRAPH_HIGH:
                        g2d.setColor(high);
                        g2d.draw(hs);
                        if (!wvp.isChildWindow()) {
                            g2d.setPaint(high);
                            g2d.fill(hs);
                        }
                        break;
                    case WinValueProfile.MODE_GRAPH_MERGE:
                    case WinValueProfile.MODE_GRAPH_LOW:
                        g2d.setColor(low);
                        g2d.draw(ls);
                        if (!wvp.isChildWindow()) {
                            // GradientPaint gp;
                            // gp = new GradientPaint(0, 0, low, getWidth()*2, 0, Color.orange, false);
                            //g2d.setPaint(gp);
                            g2d.setPaint(low);
                            g2d.fill(ls);
                        }
                        break;
                    case WinValueProfile.MODE_GRAPH_OVERLAY:
                        g2d.setColor(low);
                        g2d.draw(ls);
                        g2d.setColor(high);
                        g2d.draw(hs);
                        if (!wvp.isChildWindow()) {
                            g2d.setPaint(low);
                            g2d.fill(ls);
                            g2d.setPaint(high);
                            g2d.fill(hs);
                        }
                }
            } finally {
                g2d.setStroke(os);
            }
        }
    }

    protected void setupLook(JTable table, Object value, boolean isSelected, int row, int column) {
        super.setupLook(table, value, isSelected, row, column);
        /*
        if (isSelected)
        bdrSel = new BevelBorder(BevelBorder.RAISED, getBackground(), getForeground());
        else
            bdrSel = null;
            */
        RowHeaderedAndSectionedTable t = (RowHeaderedAndSectionedTable) table;
        ColumnData[] cd = t.getColumnData();
        SectionData[] sd = t.getSectionData();
        setBackground(sd[cd[column].sectionIndex].sectionHeaderBG);
        setForeground(sd[cd[column].sectionIndex].sectionFG);

        if (table instanceof WinValueProfileProvider)
            wvp = ((WinValueProfileProvider) table).getWinValues(row, column);
        else
            wvp = null;
    }
}
