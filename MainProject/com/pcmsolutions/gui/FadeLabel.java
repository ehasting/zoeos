package com.pcmsolutions.gui;

import com.pcmsolutions.device.EMU.E4.gui.colors.UIColors;

import javax.swing.*;
import java.awt.*;

/**
 * Created by IntelliJ IDEA.
 * User: paulmeehan
 * Date: 15-Nov-2003
 * Time: 02:58:33
 * To change this template use Options | File Templates.
 */
public class FadeLabel extends JLabel {
    protected boolean fadingIn = false;

    public FadeLabel(String text, Icon icon, int horizontalAlignment) {
        super(text, icon, horizontalAlignment);
    }

    public FadeLabel(String text, int horizontalAlignment) {
        super(text, horizontalAlignment);
    }

    public FadeLabel(String text) {
        super(text);
    }

    public FadeLabel(Icon image, int horizontalAlignment) {
        super(image, horizontalAlignment);
    }

    public FadeLabel(Icon image) {
        super(image);
    }

    public FadeLabel(boolean fadingIn) {
        this.fadingIn = fadingIn;
    }

    public FadeLabel() {
    }

    public boolean isFadingIn() {
        return fadingIn;
    }

    public void setFadingIn(boolean fadingIn) {
        this.fadingIn = fadingIn;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Color oldColor = g.getColor();
        int i;
        int w = getWidth();
        int h = getHeight();
        for (i = 0; i < h; i++) {
            g.setColor(UIColors.applyAlpha(getBackground(), 127));
            g.drawLine(0, i, w, i);
        }
        g.setColor(oldColor);
        super.paintComponent(g);
    }
}
