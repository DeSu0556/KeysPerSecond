package me.roan.kps;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JPanel;

import me.roan.kps.Main.Key;

/**
 * Panel to display the number
 * of times a certain key has 
 * been pressed
 * @author Roan
 */
public final class KeyPanel extends JPanel {
	/**
	 * Serial ID
	 */
	private static final long serialVersionUID = 8816524158873355997L;
	/**
	 * The key object associated with this panel<br>
	 * This key object keep track of the amount of this
	 * the assigned key has been hit
	 */
	private Key key;
	/**
	 * Font 1 used to display the title of the panel
	 */
	private static final Font font1 = new Font("Dialog", Font.BOLD, 24);
	/**
	 * Font 2 used to display the value of this panel
	 * this font is changed when the value that has to
	 * be displayed goes out of the panel bounds 
	 */
	protected Font font2 = new Font("Dialog", Font.PLAIN, 18);

	/**
	 * Constructs a new KeyPanel
	 * with the given key object
	 * @param key The key object to
	 *        associate this panel with
	 * @see Key
	 * @see #key
	 */
	protected KeyPanel(Key key) {
		this.key = key;
	}

	@Override
	public void paintComponent(Graphics g1) {
		Graphics2D g = (Graphics2D) g1;
		g.setColor(Color.BLACK);
		g.fillRect(0, 0, this.getWidth(), this.getHeight());
		g.drawImage(Main.unpressed, 2, 2, null);
		if (key.down) {
			g.drawImage(Main.pressed, 2, 2, null);
		}
		g.setColor(Color.CYAN);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setFont(font1);
		g.drawString(key.name, (this.getWidth() - g.getFontMetrics().stringWidth(key.name)) / 2, 30);
		g.setFont(font2);
		String str = String.valueOf(key.count);
		g.drawString(str, (this.getWidth() - g.getFontMetrics().stringWidth(str)) / 2, 55);
	}
}