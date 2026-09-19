import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

/**
 * A small flat, rounded, antialiased button used by the control bar so it
 * doesn't fall back to the OS's default (pixelated, dated-looking) button
 * chrome. Purely cosmetic - behaves like a normal JButton otherwise.
 */
public class FlatButton extends JButton {
    private final Color base;
    private final Color hover;
    private final Color pressed;
    private boolean isHovered = false;
    private boolean isPressed = false;

    public FlatButton(String text, Color base) {
        super(text);
        this.base = base;
        this.hover = base.brighter();
        this.pressed = base.darker();
        setContentAreaFilled(false);
        setFocusPainted(false);
        setBorderPainted(false);
        setForeground(Color.WHITE);
        setFont(UiFonts.bold(12));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { isHovered = true; repaint(); }
            @Override public void mouseExited(MouseEvent e) { isHovered = false; repaint(); }
            @Override public void mousePressed(MouseEvent e) { isPressed = true; repaint(); }
            @Override public void mouseReleased(MouseEvent e) { isPressed = false; repaint(); }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Color fill = isPressed ? pressed : (isHovered ? hover : base);
        g2.setColor(fill);
        g2.fill(new RoundRectangle2D.Double(0, 0, getWidth() - 1, getHeight() - 1, 10, 10));
        g2.dispose();
        super.paintComponent(g);
    }
}
