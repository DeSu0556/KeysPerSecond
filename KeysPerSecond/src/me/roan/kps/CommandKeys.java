package me.roan.kps;

import java.awt.BorderLayout;
import java.awt.GridLayout;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.jnativehook.keyboard.NativeKeyEvent;

/**
 * This class is used to configure
 * the command keys for the program
 * @author Roan
 */
public class CommandKeys{
	/**
	 * Whether or not ctrl is down
	 */
	protected static boolean isCtrlDown = false;
	/**
	 * Whether or not alt is down
	 */
	protected static boolean isAltDown = false;
	/**
	 * Whether or not shift is down
	 */
	protected static boolean isShiftDown = false;
	public static final int KEYCODE_MASK = 0xFFFF;
	public static final int CTRL_MASK = 0b0001_00000000_00000000;
	public static final int ALT_MASK = 0b0010_00000000_00000000;
	public static final int SHIFT_MASK = 0b0100_00000000_00000000;
	public static final int RIGHT_MASK = 0b1000_00000000_00000000;
	public static final int MOUSE_MASK = 0x80000000;
	public static final int FORMAT_MASK = 0b1_0000_00000000_00000000;

	/**
	 * Simple class to represent
	 * a command key
	 * @author Roan
	 */
	protected static class CMD{
		/**
		 * Whether or not alt has
		 * to be pressed
		 */
		private final boolean alt;
		/**
		 * Whether or not ctrl has
		 * to be pressed
		 */
		private final boolean ctrl;
		/**
		 * Key code
		 */
		private final int keycode;

		/**
		 * Constructs a new command key
		 * @param keycode The key code
		 * @param alt Whether or not alt has to be pressed
		 * @param ctrl Whether or not ctrl has to be pressed
		 */
		protected CMD(int keycode, boolean alt, boolean ctrl){
			this.alt = alt;
			this.ctrl = ctrl;
			this.keycode = keycode;
		}

		/**
		 * Check to see if the given state
		 * triggers this command key
		 * @param keycode The key that was pressed
		 * @return Whether or not this key code triggers this command key
		 */
		protected final boolean matches(int keycode){
			return (this.keycode == keycode) && (this.alt == isAltDown) && (this.ctrl == isCtrlDown);
		}

		@Override
		public String toString(){
			return (ctrl ? "Ctrl + " : "") + (alt ? "Alt + " : "") + NativeKeyEvent.getKeyText(keycode);
		}

		/**
		 * @return The save form of this command key
		 */
		public String toSaveString(){
			return "[keycode=" + keycode + ",ctrl=" + ctrl + ",alt=" + alt + "]";
		}
	}

	/**
	 * Prompts the user for a new command key
	 * @return The new command key or null
	 */
	protected static CMD askForNewKey(){
		JPanel form = new JPanel(new GridLayout(3, 1));
		JLabel txt = new JLabel("Press a key and click 'OK'");
		JPanel a = new JPanel(new BorderLayout());
		JPanel c = new JPanel(new BorderLayout());
		JCheckBox ctrl = new JCheckBox();
		JCheckBox alt = new JCheckBox();
		c.add(ctrl, BorderLayout.LINE_START);
		c.add(new JLabel("Ctrl"), BorderLayout.CENTER);
		a.add(alt, BorderLayout.LINE_START);
		a.add(new JLabel("Alt"), BorderLayout.CENTER);
		form.add(txt);
		form.add(c);
		form.add(a);
		if(Main.showOptionDialog(form)){
			if(Main.lastevent == null){
				return null;
			}
			CMD cmd = new CMD(Main.lastevent.getKeyCode(), isAltDown || alt.isSelected(), isCtrlDown || ctrl.isSelected());
			if(Main.showConfirmDialog("Set command key to: " + cmd.toString())){
				return cmd;
			}
		}
		return null;
	}
}
