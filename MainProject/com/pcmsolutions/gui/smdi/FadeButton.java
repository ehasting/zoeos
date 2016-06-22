package com.pcmsolutions.gui.smdi;

import com.pcmsolutions.device.EMU.E4.gui.colors.UIColors;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;

/**
 * Created by IntelliJ IDEA.
 * User: paulmeehan
 * Date: 15-Nov-2003
 * Time: 02:58:33
 * To change this template use Options | File Templates.
 */
public class FadeButton extends JButton {
    protected boolean foregroundPolarity = false;
    protected boolean pressed = false;
    protected float xFact;
    protected float yFact;
    protected int xArc = 3;
    protected int yArc = 3;
    protected boolean cyclic = false;
    protected float xInsetFact = 0.10F;
    protected float yInsetFact = 0.15F;

    public FadeButton(boolean polarity, float xFact, float yFact) {
        this.foregroundPolarity = polarity;
        this.xFact = xFact;
        this.yFact = yFact;
        this.setBorder(null);
        enableEvents(AWTEvent.MOUSE_EVENT_MASK);
        this.setText("test");
    }

    protected void processMouseEvent(MouseEvent e) {
        switch(e.getID()){
            case MouseEvent.MOUSE_PRESSED:
                pressed = !pressed;
                repaint();
                break;
                case MouseEvent.MOUSE_RELEASED:
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
                pressed = !pressed;
                repaint();
                break;
        }
        super.processMouseEvent(e);
    }

    public FadeButton(Action a, boolean polarity, float xDiv, float yDiv) {
        this(polarity, xDiv, yDiv);
        setAction(a);
    }

    public boolean isForegroundPolarity() {
        return foregroundPolarity;
    }

    public void setForegroundPolarity(boolean foregroundPolarity) {
        this.foregroundPolarity = foregroundPolarity;
    }

    protected void paintComponent(Graphics g) {
        int w = getWidth();
        int h = getHeight();
        Graphics2D g2 = (Graphics2D) g;

        g2.setColor(Color.white);
        g2.fillRect(0, 0, w, h);
        for (int i = 0; i < h; i++) {
            g2.setColor(UIColors.applyAlpha(getBackground(), 127));
            g2.drawLine(0, i, w, i);
        }

        GradientPaint gp;
        gp = new GradientPaint(0, 0, (pressed ? getBackground(): getForeground()), w * xFact, h * yFact, (pressed ? getForeground():getBackground()), cyclic);
        g2.setPaint(gp);
        g2.fillRoundRect((int) (xInsetFact * w), (int) (yInsetFact * h), (int) (w - xInsetFact * w * 2), (int) (h - yInsetFact * h * 2), xArc, yArc);
        //g2.fillRect((int) (xInsetFact * w), (int) (yInsetFact * h), (int) (w - xInsetFact * w * 2), (int) (h - yInsetFact * h * 2));
    }
}
