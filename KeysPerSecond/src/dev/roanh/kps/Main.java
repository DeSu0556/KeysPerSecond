/*
 * KeysPerSecond: An open source input statistics displayer.
 * Copyright (C) 2017  Roan Hofland (roan@roanh.dev).  All rights reserved.
 * GitHub Repository: https://github.com/RoanH/KeysPerSecond
 *
 * KeysPerSecond is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * KeysPerSecond is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dev.roanh.kps;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.github.kwhat.jnativehook.NativeHookException;

import dev.roanh.kps.config.Configuration;
import dev.roanh.kps.config.ThemeColor;
import dev.roanh.kps.config.UpdateRate;
import dev.roanh.kps.config.group.CommandSettings;
import dev.roanh.kps.config.group.GraphSettings;
import dev.roanh.kps.config.group.KeyPanelSettings;
import dev.roanh.kps.config.group.SpecialPanelSettings;
import dev.roanh.kps.event.EventManager;
import dev.roanh.kps.event.source.NativeHookInputSource;
import dev.roanh.kps.layout.GridPanel;
import dev.roanh.kps.layout.Layout;
import dev.roanh.kps.panels.BasePanel;
import dev.roanh.kps.panels.GraphPanel;
import dev.roanh.kps.ui.dialog.MainDialog;
import dev.roanh.kps.ui.listener.CloseListener;
import dev.roanh.util.Dialog;
import dev.roanh.util.ExclamationMarkPath;
import dev.roanh.util.Util;

/**
 * This program can be used to display
 * information about how many times
 * certain keys are pressed and what the
 * average, maximum and current
 * number of keys pressed per second is.
 * <p>
 * Besides the tracking of the assigned keys
 * this program responds to 6 key events these are:
 * <ol><li><b>Ctrl + P</b>: Causes the program to reset the average and maximum value
 * And to print the statistics to standard output
 * </li><li><b>Ctrl + O</b>: Terminates the program
 * </li><li><b>Ctrl + I</b>: Causes the program to reset the amount of times a key is pressed
 * And to print the statistics to standard output
 * </li><li><b>Ctrl + Y</b>: Hides/shows the GUI
 * </li><li><b>Ctrl + T</b>: Pauses/resumes the counter
 * </li><li><b>Ctrl + R</b>: Reloads the configuration</li></ol>
 * The program also constantly prints the current keys per second to
 * the standard output.<br>
 * A key is only counted as being pressed if the key has been released before
 * this deals with the issue of holding a key firing multiple key press events<br>
 * This program also has support for saving and loading configurations
 * @author Roan Hofland (<a href="mailto:roan@roanh.dev">roan@roanh.dev</a>)
 */
public class Main{
	/**
	 * String holding the version of the program.
	 */
	public static final String VERSION = "v8.7";//XXX the version number  - don't forget build.gradle
	/**
	 * The number of seconds the average has
	 * been calculated for
	 */
	protected static long n = 0;
	/**
	 * The number of keys pressed in the
	 * ongoing second
	 */
	protected static AtomicInteger tmp = new AtomicInteger(0);
	/**
	 * The average keys per second
	 */
	public static double avg;
	/**
	 * The maximum keys per second value reached so far
	 */
	public static int max;
	/**
	 * Total number of hits
	 */
	public static int hits;
	/**
	 * The keys per second of the previous second
	 * used for displaying the current keys per second value
	 */
	public static int prev;
	/**
	 * HashMap containing all the tracked keys and their
	 * virtual codes<br>Used to increment the count for the
	 * keys
	 */
	public static Map<Integer, Key> keys = new HashMap<Integer, Key>();
	/**
	 * Main panel used for showing all the sub panels that
	 * display all the information
	 */
	public static final GridPanel content = new GridPanel();
	/**
	 * Graph panel.
	 */
	protected static List<GraphPanel> graphs = new ArrayList<GraphPanel>();
	/**
	 * Linked list containing all the past key counts per time frame
	 */
	private static LinkedList<Integer> timepoints = new LinkedList<Integer>();
	/**
	 * The program's main frame
	 */
	public static final JFrame frame = new JFrame("KeysPerSecond");
	/**
	 * Whether or not the counter is paused
	 */
	protected static boolean suspended = false;
	/**
	 * The configuration
	 */
	public static Configuration config = new Configuration();
	/**
	 * The loop timer
	 */
	protected static ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor();
	/**
	 * The loop timer task
	 */
	protected static ScheduledFuture<?> future = null;
	/**
	 * The layout for the main panel of the program
	 */
	public static final Layout layout = new Layout(content);
	/**
	 * Small icon for the program
	 */
	public static final Image iconSmall;
	/**
	 * Icon for the program
	 */
	public static final Image icon;
	/**
	 * Dummy key for getOrDefault operations
	 */
	protected static final Key DUMMY_KEY;
	/**
	 * Best text rendering hints.
	 */
	public static Map<?, ?> desktopHints;
	/**
	 * Event manager responsible for forwarding input events.
	 */
	public static EventManager eventManager = new EventManager();
	
	/**
	 * Main method
	 * @param args - configuration file path
	 */
	public static void main(String[] args){
		//Work around for a JDK bug
		ExclamationMarkPath.check(args);
		
		//Basic setup and info
		String config = null;
		if(args.length >= 1 && !args[0].equalsIgnoreCase("-relaunch")){
			config = args[0];
			System.out.println("Attempting to load config: " + config);
		}
		System.out.println("Control keys:");
		System.out.println("Ctrl + P: Causes the program to reset and print the average and maximum value");
		System.out.println("Ctrl + U: Terminates the program");
		System.out.println("Ctrl + I: Causes the program to reset and print the key press statistics");
		System.out.println("Ctrl + Y: Hides/shows the GUI");
		System.out.println("Ctrl + T: Pauses/resumes the counter");
		System.out.println("Ctrl + R: Reloads the configuration");
		Util.installUI();
		
		//Set dialog defaults
		Dialog.setDialogIcon(iconSmall);
		Dialog.setParentFrame(frame);
		Dialog.setDialogTitle("KeysPerSecond");
		
		//register input sources
		try{
			eventManager.registerInputSource(new NativeHookInputSource(eventManager));
		}catch(NativeHookException ex){
			System.err.println("There was a problem registering the native hook.");
			System.err.println(ex.getMessage());
			Dialog.showErrorDialog("There was a problem registering the native hook: " + ex.getMessage());
			System.exit(1);
		}
		
		//register command handlers
		CommandKeys listener = new CommandKeys();
		eventManager.registerKeyPressListener(listener);
		eventManager.registerKeyReleaseListener(listener);
		
		//Set configuration for the keys
		if(config != null){
			try{
				Configuration toLoad = parseConfiguration(config);
				if(toLoad != null){
					Main.config = toLoad;
					System.out.println("Loaded config file: " + toLoad.getPath().toString());
				}else{
					System.out.println("The provided config file does not exist.");
				}
			}catch(IOException e){
				e.printStackTrace();
				Dialog.showErrorDialog("Failed to load the given configuration file\nCause: " + e.getMessage());
			}
		}else{
			try{
				MainDialog.configure(Main.config);
			}catch(Exception e){
				e.printStackTrace();
				try{
					Dialog.showErrorDialog("Failed to load the configuration menu, however you can use the live menu instead");
				}catch(Throwable t){
					t.printStackTrace();
				}
				System.err.println("Failed to load the configuration menu, however you can use the live menu instead");
			}
		}

		//Build GUI
		try{
			buildGUI();
		}catch(IOException e){
			e.printStackTrace();
		}
		
		//register default event handlers
		eventManager.registerButtonPressListener(Main::pressEventButton);
		eventManager.registerButtonReleaseListener(Main::releaseEventButton);
		eventManager.registerKeyPressListener(Main::pressEventKey);
		eventManager.registerKeyReleaseListener(Main::releaseEventKey);
		eventManager.registerKeyPressListener(Main::triggerCommandKeys);
		
		//Enter the main loop
		mainLoop();
	}
	
	/**
	 * Parses the given command line argument configuration file by
	 * loading the file from disk while treating unknown characters
	 * as wildcards to deal with Windows argument encoding issues.
	 * @param config The configuration file path.
	 * @return The loaded configuration file or <code>null</code>
	 *         if the file was not found.
	 * @throws IOException When an IOException occurs.
	 */
	private static final Configuration parseConfiguration(String config) throws IOException{
		try{
			Path path = Paths.get(config);
			if(Files.exists(path)){
				Configuration toLoad = new Configuration(path);
				toLoad.loadConfig(path);
				return toLoad;
			}else{
				return null;
			}
		}catch(InvalidPathException e){
			int index = config.lastIndexOf(File.separatorChar);
			try{
				Path dir = Paths.get(config.substring(0, index));
				final String name = config.substring(index + 1);
				Filter<Path> filter = p->{
					String other = p.getFileName().toString();
					for(int i = 0; i < name.length(); i++){
						char ch = name.charAt(i);
						if(ch == '?'){
							continue;
						}
						if(i >= other.length() || ch != other.charAt(i)){
							return false;
						}
					}
					return true;
				};
				
				try(DirectoryStream<Path> files = Files.newDirectoryStream(dir, filter)){
					Iterator<Path> iter = files.iterator();
					if(iter.hasNext()){
						Path path = iter.next();
						Configuration toLoad = new Configuration(path);
						toLoad.loadConfig(path);
						return toLoad;
					}
				}
				
				return null;
			}catch(InvalidPathException e2){
				return null;
			}
		}
	}

	/**
	 * Main loop of the program
	 * this loop updates the
	 * average, current and 
	 * maximum keys per second
	 */
	protected static final void mainLoop(){
		if(future != null){
			future.cancel(false);
		}
		
		future = timer.scheduleAtFixedRate(()->{
			if(!suspended){
				int currentTmp = tmp.getAndSet(0);
				int totaltmp = currentTmp;
				for(int i : timepoints){
					totaltmp += i;
				}
				if(totaltmp > max){
					max = totaltmp;
				}
				if(totaltmp != 0){
					avg = (avg * n + totaltmp) / (n + 1.0D);
					n++;
					hits += currentTmp;
					System.out.println("Current keys per second: " + totaltmp);
				}
				
				for(GraphPanel graph : graphs){
					graph.addPoint(totaltmp);
					graph.repaint();
				}
				
				content.repaint();
				prev = totaltmp;
				timepoints.addFirst(currentTmp);
				if(timepoints.size() >= 1000 / config.getUpdateRateMs()){
					timepoints.removeLast();
				}
			}
		}, 0, config.getUpdateRateMs(), TimeUnit.MILLISECONDS);
	}

	/**
	 * Handles a mouse button release event.
	 * @param button The ID of the button that was released.
	 */
	private static final void releaseEventButton(int button){
		keys.getOrDefault(getExtendedButtonCode(button), DUMMY_KEY).keyReleased();
	}

	/**
	 * Handles a key release event.
	 * @param rawCode The key code of the key that was released.
	 */
	private static final void releaseEventKey(int rawCode){
		int code = getExtendedKeyCode(rawCode);
		
		if(config.isKeyModifierTrackingEnabled()){
			if(code == CommandKeys.ALT){
				for(Key k : keys.values()){
					if(k.alt){
						k.keyReleased();
					}
				}
			}else if(code == CommandKeys.CTRL){
				for(Key k : keys.values()){
					if(k.ctrl){
						k.keyReleased();
					}
				}
			}else if(CommandKeys.isShift(code)){
				for(Key k : keys.values()){
					if(k.shift){
						k.keyReleased();
					}
				}
			}
			for(Entry<Integer, Key> k : keys.entrySet()){
				if(CommandKeys.getBaseKeyCode(code) == CommandKeys.getBaseKeyCode(k.getKey())){
					k.getValue().keyReleased();
				}
			}
		}else{
			keys.getOrDefault(code, DUMMY_KEY).keyReleased();
		}
	}
	
	/**
	 * Handles a button press event.
	 * @param button The ID of the button that was pressed.
	 */
	private static final void pressEventButton(int button){
		int code = getExtendedButtonCode(button);
		Key key = keys.get(code);
		
		if(config.isTrackAllButtons() && key == null){
//			key = new Key("M" + button);
			key = new Key();
			keys.put(code, key);
		}
		
		if(!suspended && key != null){
			key.keyPressed();
		}
	}

	/**
	 * Handles a key press event.
	 * @param rawCode The key code of the key that was pressed.
	 */
	private static final void pressEventKey(int rawCode){
		int code = getExtendedKeyCode(rawCode);
		Key key = keys.get(code);
		
		if(config.isTrackAllKeys() && key == null){
//			key = new Key(KeyInformation.getKeyName(NativeKeyEvent.getKeyText(rawCode), code));
			key = new Key();//TODO this should probably pass alt/ctrl/shift status, technically that is a bug that existed in v8.7 too
			keys.put(code, key);
		}
		
		if(!suspended && key != null){
			key.keyPressed();
			if(config.isKeyModifierTrackingEnabled()){
				if(key.alt){
					keys.getOrDefault(CommandKeys.ALT, DUMMY_KEY).keyReleased();
				}
				if(key.ctrl){
					keys.getOrDefault(CommandKeys.CTRL, DUMMY_KEY).keyReleased();
				}
				if(key.shift){
					keys.getOrDefault(CommandKeys.RSHIFT, DUMMY_KEY).keyReleased();
					keys.getOrDefault(CommandKeys.LSHIFT, DUMMY_KEY).keyReleased();
				}
			}
		}
	}
	
	/**
	 * Handles a received key press and triggers command keys.
	 * @param code The received key press key code.
	 */
	private static void triggerCommandKeys(int code){
		CommandSettings commands = Main.config.getCommands();
		if(commands.getCommandResetStats().matches(code)){
			resetStats();
		}else if(commands.getCommandExit().matches(code)){
			exit();
		}else if(commands.getCommandResetTotals().matches(code)){
			resetTotals();
		}else if(commands.getCommandHide().matches(code)){
			if(frame.getContentPane().getComponentCount() != 0){
				frame.setVisible(!frame.isVisible());
			}
		}else if(commands.getCommandPause().matches(code)){
			suspended = !suspended;
			Menu.pause.setSelected(suspended);
		}else if(commands.getCommandReload().matches(code)){
			config.reloadConfig();
			Menu.resetData();
		}
	}

	/**
	 * Gets the extended key code for this event, this key code includes modifiers.
	 * @param rawCode The received key code for the key that was pressed.
	 * @return The extended key code for this event.
	 * @see #getExtendedButtonCode(int)
	 */
	public static final int getExtendedKeyCode(int rawCode){
		if(config.isKeyModifierTrackingEnabled()){
			return CommandKeys.getExtendedKeyCode(rawCode);
		}else{
			return CommandKeys.getExtendedKeyCode(rawCode, false, false, false);
		}
	}
	
	/**
	 * Gets the extended button code for this event.
	 * @param button The button that was pressed.
	 * @return The extended key code for this event.
	 * @see #getExtendedKeyCode(int)
	 */
	public static final int getExtendedButtonCode(int button){
		return -button;
	}

	/**
	 * Changes the update rate
	 * @param newRate The new update rate
	 */
	protected static final void changeUpdateRate(UpdateRate newRate){
		n *= (double)config.getUpdateRateMs() / (double)newRate.getRate();
		tmp.set(0);
		timepoints.clear();
		config.setUpdateRate(newRate);
		mainLoop();
	}

	/**
	 * Builds the main GUI of the program
	 * @throws IOException When an IO Exception occurs, this can be thrown
	 *         when the program fails to load its resources
	 */
	protected static final void buildGUI() throws IOException{
		Menu.createMenu();
		frame.setResizable(false);
		frame.setIconImage(icon);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setUndecorated(true);
		new Listener(frame);
		frame.addWindowListener(new CloseListener());
		reconfigure();
	}
	
	public static final void resetPanels(){
		for(Component component : content.getComponents()){
			if(component instanceof BasePanel){
				((BasePanel)component).sizeChanged();
			}
		}
	}

	/**
	 * Reconfigures the layout of the program
	 */
	public static final void reconfigure(){
		System.out.println("reconf");//TODO remove
		
		SwingUtilities.invokeLater(()->{
			frame.getContentPane().removeAll();
			layout.removeAll();
			
			ThemeColor background = config.getTheme().getBackground();
			boolean opaque = background.getAlpha() != 1.0F ? !ColorManager.transparency : true;
			try{
				ColorManager.prepareImages();
			}catch(IOException e){
				e.printStackTrace();
			}
			
			//key panels
			for(KeyPanelSettings info : config.getKeySettings()){
				Key key = keys.computeIfAbsent(info.getKeyCode(), code->new Key(info));
				if(info.isVisible()){
					content.add(info.createPanel(key));
				}
			}
			
			//special panels
			for(SpecialPanelSettings panel : config.getPanels()){
				content.add(panel.createPanel());
			}
			
			//graph panels
			graphs.clear();
			for(GraphSettings info : config.getGraphSettings()){
				GraphPanel graph = info.createPanel();
				graph.setOpaque(opaque);//TODO this was in the DETACHED only branch, make sure we need it?
				content.add(graph);
				graphs.add(graph);
			}
			
			if(content.getComponentCount() == 0){
				
				//TODO warn the user and probably roll back to the configuration step?
				
				System.out.println("no GUI");//TODO
				frame.setVisible(false);
				return;//don't create a GUI if there's nothing to display
			}

			Menu.repaint();

			JPanel all = new JPanel(new BorderLayout());
			all.add(content, BorderLayout.CENTER);
			all.setOpaque(opaque);
			
			frame.setAlwaysOnTop(config.isOverlayMode());
			frame.setSize(layout.getWidth(), layout.getHeight());
			if(background.getAlpha() != 1.0F){
				frame.setBackground(ColorManager.transparent);
				content.setOpaque(false);
				content.setBackground(ColorManager.transparent);
			}else{
				content.setOpaque(true);
				content.setBackground(background.getColor());
			}
			frame.add(all);
			frame.setVisible(content.getComponentCount() > 0);
			
			//Start stats saving
			Statistics.cancelScheduledTask();
			if(config.getStatsSavingSettings().isAutoSaveEnabled()){
				Statistics.saveStatsTask();
			}
		});
	}

	/**
	 * Shuts down the program
	 */
	public static final void exit(){
		Statistics.saveStatsOnExit();
		System.exit(0);
	}

	/**
	 * Resets avg, max, tot &amp; cur
	 */
	protected static final void resetStats(){
		System.out.println("Reset max & avg | max: " + max + " avg: " + avg + " tot: " + hits);
		n = 0;
		avg = 0;
		max = 0;
		hits = 0;
		tmp.set(0);
		graphs.forEach(GraphPanel::reset);
		frame.repaint();
	}

	/**
	 * Resets key count totals
	 */
	protected static final void resetTotals(){
		System.out.print("Reset key counts | ");
		for(Key k : keys.values()){
//			System.out.print(k.name + ":" + k.count + " ");
			//TODO either stop printing this or loop the entry set and call getkeyname
			k.count = 0;
		}
		System.out.println();
		frame.repaint();
	}
	
	public static void removeKey(int keycode){
		keys.remove(keycode);
	}
	
	static{
		Image img;
		try{
			img = ImageIO.read(ClassLoader.getSystemResource("kps_small.png"));
		}catch(IOException e){
			img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB);
		}
		iconSmall = img;
		try{
			img = ImageIO.read(ClassLoader.getSystemResource("kps.png"));
		}catch(IOException e){
			img = new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB);
		}
		icon = img;
		DUMMY_KEY = new Key(){

			@Override
			public void keyPressed(){
			}

			@Override
			public void keyReleased(){
			}
		};
		
		//Request the best text anti-aliasing settings
		Toolkit toolkit = Toolkit.getDefaultToolkit();
		desktopHints = (Map<?, ?>)Toolkit.getDefaultToolkit().getDesktopProperty("awt.font.desktophints");
		Map<Object, Object> defaultHints = new HashMap<Object, Object>();
		defaultHints.put(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		if(desktopHints == null){
			desktopHints = defaultHints;
		}
		toolkit.addPropertyChangeListener("awt.font.desktophints", event->{
			desktopHints = event.getNewValue() == null ? defaultHints : (Map<?, ?>)event.getNewValue();
		});
	}
}