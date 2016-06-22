package com.pcmsolutions.gui;

import com.pcmsolutions.device.EMU.E4.gui.colors.UIColors;

import javax.swing.border.LineBorder;
import java.awt.*;

/**
 * Created by IntelliJ IDEA.
 * User: paulmeehan
 * Date: 14-Nov-2003
 * Time: 14:54:38
 * To change this template use Options | File Templates.
 */
public class FuzzyLineBorder extends LineBorder {
    private boolean fadingIn = true;

    public FuzzyLineBorder(Color color) {
        super(color);
    }

    public FuzzyLineBorder(Color color, int thickness) {
        super(color, thickness);
    }

    public FuzzyLineBorder(Color color, int thickness, boolean roundedCorners) {
        super(color, thickness, roundedCorners);
    }

    public FuzzyLineBorder(Color color, int thickness, boolean roundedCorners, boolean fadingIn) {
        super(color, thickness, roundedCorners);
        this.fadingIn = fadingIn;
    }

    public boolean isFadingIn() {
        return fadingIn;
    }

    public void setFadingIn(boolean fadingIn) {
        this.fadingIn = fadingIn;
    }

    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        Color oldColor = g.getColor();
        /// PENDING(klobad) How/should do we support Roundtangles?
        for (int i = 0; i < thickness; i++) {
            g.setColor(UIColors.applyAlpha(lineColor, 127));
            if (!roundedCorners)
                g.drawRect(x + i, y + i, width - i - i - 1, height - i - i - 1);
            else
                g.drawRoundRect(x + i, y + i, width - i - i - 1, height - i - i - 1, thickness, thickness);
        }
        g.setColor(oldColor);
    }
}
