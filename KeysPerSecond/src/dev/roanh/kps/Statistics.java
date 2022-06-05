package dev.roanh.kps;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.ParseException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JFormattedTextField.AbstractFormatter;
import javax.swing.JFormattedTextField.AbstractFormatterFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import dev.roanh.kps.panels.TotPanel;
import dev.roanh.util.ClickableLink;
import dev.roanh.util.Dialog;
import dev.roanh.util.FileSelector;
import dev.roanh.util.FileSelector.FileExtension;

/**
 * Class that handles most of the more complex
 * matters related to statistics.
 * @author Roan
 */
public class Statistics{
	/**
	 * Extension filter for KeysPerSecond statistics files.
	 */
	private static final FileExtension KPS_STATS_EXT = FileSelector.registerFileExtension("KeysPerSecond statistics", "kpsstats");
	/**
	 * Statistics save future
	 */
	protected static ScheduledFuture<?> statsFuture = null;
	/**
	 * Periodic statistics save scheduler
	 */
	protected static ScheduledExecutorService statsScheduler = null;

	/**
	 * Show the auto save statistics configuration dialog
	 * @param live Whether or not the program is already running
	 */
	protected static final void configureAutoSave(boolean live){
		JPanel endPanel = new JPanel(new BorderLayout());
		endPanel.setBorder(BorderFactory.createTitledBorder("Save on exit"));
		JCheckBox saveOnExit = new JCheckBox("Save statistics to a file on exit", Main.config.saveStatsOnExit);
		JCheckBox loadOnStart = new JCheckBox("Load saved statistics from a file on launch", Main.config.loadStatsOnLaunch);

		JPanel selectFile = new JPanel(new BorderLayout(2, 0));
		selectFile.add(new JLabel("Save location: "), BorderLayout.LINE_START);
		JTextField selectedFile = new JTextField(Main.config.statsSaveFile);
		selectFile.add(selectedFile, BorderLayout.CENTER);
		JButton select = new JButton("Select");
		selectFile.add(select, BorderLayout.LINE_END);
		select.addActionListener((e)->{
			File dir = Dialog.showFileSaveDialog(KPS_STATS_EXT, "stats");
			if(dir != null){
				selectedFile.setText(dir.getAbsolutePath());
			}
		});
		
		ActionListener stateTask = e->{
			boolean enabled = saveOnExit.isSelected() || loadOnStart.isSelected();
			selectedFile.setEnabled(enabled);
			select.setEnabled(enabled);
		};
		
		saveOnExit.addActionListener(stateTask);
		loadOnStart.addActionListener(stateTask);
		stateTask.actionPerformed(null);
		
		endPanel.add(saveOnExit, BorderLayout.PAGE_START);
		endPanel.add(loadOnStart, BorderLayout.CENTER);
		endPanel.add(selectFile, BorderLayout.PAGE_END);
		
		JPanel periodicPanel = new JPanel(new BorderLayout());
		periodicPanel.setBorder(BorderFactory.createTitledBorder("Periodic saving"));
		JCheckBox enabled = new JCheckBox("Periodically save the statistics so far to a file", Main.config.autoSaveStats);
		
		BorderLayout layout = new BorderLayout();
		layout.setHgap(2);
		JPanel settings = new JPanel(layout);
		JPanel labels = new JPanel(new GridLayout(3, 1, 0, 2));
		JPanel fields = new JPanel(new GridLayout(3, 1, 0, 2));
		JPanel extras = new JPanel(new GridLayout(3, 1, 0, 2));
		
		JButton seldest = new JButton("Select");
		JTextField ldest = new JTextField(Main.config.statsDest);
		fields.add(ldest);
		extras.add(seldest);
		labels.add(new JLabel("Save location: "));
		seldest.addActionListener((e)->{
			File dir = Dialog.showFolderOpenDialog();
			if(dir != null){
				ldest.setText(dir.getAbsolutePath());
			}
		});
		
		JComboBox<Unit> timeUnit = new JComboBox<Unit>(Unit.values());
		Unit bestUnit = Unit.fromMillis(Main.config.statsSaveInterval);
		timeUnit.setSelectedItem(bestUnit);
		JSpinner time = new JSpinner(new SpinnerNumberModel(Long.valueOf(Main.config.statsSaveInterval / bestUnit.unit.toMillis(1)), Long.valueOf(1L), Long.valueOf(Long.MAX_VALUE), Long.valueOf(1L)));
		labels.add(new JLabel("Save interval: "));
		fields.add(time);
		JPanel unitPanel = new JPanel(new BorderLayout());
		unitPanel.setBorder(BorderFactory.createEmptyBorder(0, 1, 0, 1));
		unitPanel.add(timeUnit, BorderLayout.CENTER);
		extras.add(unitPanel);
		
		JFormattedTextField format = new JFormattedTextField(new AbstractFormatterFactory(){
			@Override
			public AbstractFormatter getFormatter(JFormattedTextField tf){
				return new AbstractFormatter(){
					/**
					 * Serial ID
					 */
					private static final long serialVersionUID = 5956641218097576666L;

					@Override
					public Object stringToValue(String text) throws ParseException{
						for(char ch : new char[]{'/', '\\', '?', '%', '*', ':', '|', '"', '<', '>'}){
							int index = text.indexOf(ch);
							if(index != -1){
								throw new ParseException("Invalid character found", index);
							}
						}
						return text;
					}

					@Override
					public String valueToString(Object value) throws ParseException{
						return value instanceof String ? (String)value : null;
					}
				};
			}
		}, Main.config.statsFormat);
		format.setColumns(30);
		format.setFocusLostBehavior(JFormattedTextField.COMMIT_OR_REVERT);
		JButton help = new JButton("Help");
		labels.add(new JLabel("Save format: "));
		fields.add(format);
		extras.add(help);
		help.addActionListener(Statistics::showFormatHelp);
		
		settings.add(labels, BorderLayout.LINE_START);
		settings.add(fields, BorderLayout.CENTER);
		settings.add(extras, BorderLayout.LINE_END);
		
		periodicPanel.add(enabled, BorderLayout.PAGE_START);
		periodicPanel.add(settings, BorderLayout.CENTER);
		
		ActionListener enabledTask = (e)->{
			ldest.setEnabled(enabled.isSelected());
			seldest.setEnabled(enabled.isSelected());
			format.setEnabled(enabled.isSelected());
			help.setEnabled(enabled.isSelected());
			time.setEnabled(enabled.isSelected());
			timeUnit.setEnabled(enabled.isSelected());
		};
		enabled.addActionListener(enabledTask);
		enabledTask.actionPerformed(null);
		
		JPanel panel = new JPanel(new BorderLayout());
		panel.add(endPanel, BorderLayout.PAGE_START);
		panel.add(periodicPanel, BorderLayout.PAGE_END);
		if(Dialog.showSaveDialog(panel)){
			Main.config.autoSaveStats = enabled.isSelected();
			Main.config.statsDest = ldest.getText();
			Main.config.statsFormat = format.getText();
			long interval = ((Unit)timeUnit.getSelectedItem()).unit.toMillis((long)time.getValue());
			if(Main.config.autoSaveStats){
				if(interval != Main.config.statsSaveInterval){
					Main.config.statsSaveInterval = interval;
					if(live){
						saveStatsTask();
					}
				}
			}else{
				cancelScheduledTask();
				Main.config.statsSaveInterval = interval;
			}
			Main.config.saveStatsOnExit = saveOnExit.isSelected();
			Main.config.loadStatsOnLaunch = loadOnStart.isSelected();
			Main.config.statsSaveFile = selectedFile.getText();
		}
	}
	
	/**
	 * Saves the statistics so to the configured
	 * save file if stats saving on exit is enabled.
	 */
	public static void saveStatsOnExit(){
		if(Main.config.saveStatsOnExit){
			try{
				saveStats(Paths.get(Main.config.statsSaveFile));
			}catch(IOException e){
				e.printStackTrace();
				if(Dialog.showConfirmDialog("Failed to save statistics on exit.\nCause: " + e.getMessage() + "\nAttempt to save again?")){
					saveStatsOnExit();
				}
			}
		}
	}
	
	/**
	 * Shows a help dialog to the user that list some of
	 * the available {@link DateTimeFormatter} options
	 * @param event The {@link ActionEvent} from the button
	 *        that was clicked
	 */
	private static void showFormatHelp(ActionEvent event){
		JLabel help = new JLabel("<html>Format syntax:<br>"
			+ "- Escape strings with single quotes ( ' )<br>"
			+ "- A double single quote is a single quote ( '' becomes ' )<br>"
			+ "- Note that / \\ ? % * : | \" &lt and > are not allowed in file names<br>"
			+ "- <b>yyyy</b> represents the year<br>"
			+ "- <b>MM</b> represents the month of the year<br>"
			+ "- <b>dd</b> represents the day of the month<br>"
			+ "- <b>hh</b> represents the hour of the day<br>"
			+ "- <b>mm</b> represents the minute in the hour<br>"
			+ "- <b>ss</b> represents the second in the minute</html>");
		JLabel more = new JLabel("<html><font color=blue><u>More options can be found in the Javadoc for the DateTimeFormatter.</u></font></html>");
		more.addMouseListener(new ClickableLink("https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html"));
		JPanel text = new JPanel(new BorderLayout());
		text.add(help, BorderLayout.CENTER);
		text.add(more, BorderLayout.PAGE_END);
		Dialog.showMessageDialog(text);
	}
	
	/**
	 * Cancels the automatic statistics saving task
	 */
	public static void cancelScheduledTask(){
		if(statsFuture != null){
			statsFuture.cancel(false);
		}
	}
	
	/**
	 * Starts the statistics saving task or cancels the current
	 * one and stars a new one.
	 */
	public static void saveStatsTask(){
		if(statsScheduler == null){
			statsScheduler = Executors.newSingleThreadScheduledExecutor();
		}
		cancelScheduledTask();
		statsFuture = statsScheduler.scheduleAtFixedRate(()->{
			try{
				Path target = Paths.get(Main.config.statsDest);
				Files.createDirectories(target);
				target.resolve(DateTimeFormatter.ofPattern(Main.config.statsFormat).withLocale(Locale.getDefault()).withZone(ZoneId.systemDefault()).format(Instant.now(Clock.systemDefaultZone())));
				saveStats(target);
			}catch(Exception e){
				//Main priority here is to not interrupt whatever the user is doing
				e.printStackTrace();
			}
		}, Main.config.statsSaveInterval, Main.config.statsSaveInterval, TimeUnit.MILLISECONDS);
	}
	
	/**
	 * Saves the statistics logged so far
	 * and asks the user to provide a location
	 * to save to
	 */
	protected static void saveStats(){
		File file = Dialog.showFileSaveDialog(KPS_STATS_EXT, "stats");
		if(file != null){
			try{
				saveStats(file.toPath());
				Dialog.showMessageDialog("Statistics succesfully saved");
			}catch(IOException e){
				e.printStackTrace();
				Dialog.showErrorDialog("Failed to save the statistics!\nCause: " + e.getMessage());
			}
		}
	}

	/**
	 * Saves the statistics logged so far
	 * @param dest The file to save to
	 * @throws IOException When an IOException occurs.
	 */
	private static void saveStats(Path dest) throws IOException{
		try(PrintWriter out = new PrintWriter(Files.newBufferedWriter(dest, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING))){
			out.println("version: ");
			out.println(Main.VERSION);
			out.println();
			out.println("# General");
			out.print("total: ");
			out.println(TotPanel.hits);
			out.print("average: ");
			out.println(Main.avg);
			out.print("maximum: ");
			out.println(Main.max);
			out.print("seconds: ");
			out.println(Main.n);
			out.print("previous: ");
			out.println(Main.prev);
			out.print("current: ");
			out.println(Main.tmp.get());
			out.println();
			out.println("# Keys");
			out.println("keys:");
			for(Entry<Integer, Key> entry : Main.keys.entrySet()){
				Key key = entry.getValue();
				out.print("  - [keycode=");
				out.print(entry.getKey());
				out.print(",count=");
				out.print(key.count);
				out.print(",alt=");
				out.print(key.alt);
				out.print(",ctrl=");
				out.print(key.ctrl);
				out.print(",shift=");
				out.print(key.shift);
				out.print(",name=\"");
				out.print(key.name);
				out.println("\"]");
			}
			out.flush();
			out.close();
		}
	}
	
	/**
	 * Loads the statistics from a file, shows
	 * a prompt to the user for the file.
	 */
	protected static void loadStats(){
		File file = Dialog.showFileOpenDialog(KPS_STATS_EXT);
		if(file == null){
			return;
		}
		try{
			loadStats(file);
			Dialog.showMessageDialog("Statistics succesfully loaded");
		}catch(IOException e){
			Dialog.showErrorDialog("Failed to load the statistics!\nCause: " + e.getMessage());
		}
	}

	/**
	 * Loads the statistics from a file
	 * @param file The file to load from.
	 * @throws IOException When an IOException occurs.
	 */
	protected static void loadStats(File file) throws IOException{
		try(ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))){
			TotPanel.hits = in.readInt();
			Main.avg = in.readDouble();
			Main.max = in.readInt();
			Main.n = in.readLong();
			Main.prev = in.readInt();
			Main.tmp.set(in.readInt());
			while(in.available() > 0){
				int code = in.readInt();
				Key key = Main.keys.get(code);
				Key obj = ((Key)in.readObject());
				if(key != null){
					key.count = obj.count;
				}else{
					Main.keys.put(code, obj);
				}
			}
			in.close();
			Main.frame.repaint();
			Main.graphFrame.repaint();
		}catch(ClassNotFoundException e){
			//Shouldn't happen
			e.printStackTrace();
		}
	}
	
	/**
	 * Enum that has the different
	 * time unit values offered
	 * @author Roan
	 * @see TimeUnit
	 */
	private static enum Unit{
		/**
		 * Represents the hour unit.
		 * Larger units are not offered.
		 */
		HOUR("Hours", TimeUnit.HOURS, null),
		/**
		 * Represents the minute unit, 60 of
		 * which make up a single {@link #HOUR}.
		 */
		MINUTE("Minutes", TimeUnit.MINUTES, HOUR),
		/**
		 * Represents the second unit, 60 of
		 * which make up a single {@link #MINUTE}.
		 */
		SECOND("Seconds", TimeUnit.SECONDS, MINUTE),
		/**
		 * Represents the millisecond unit, 1000 of
		 * which make up a single {@link #SECOND}.
		 */
		MILLISECOND("Milliseconds", TimeUnit.MILLISECONDS, SECOND);
		
		/**
		 * The {@link TimeUnit} for this unit
		 */
		private TimeUnit unit;
		/**
		 * The display name for this unit
		 */
		private String name;
		/**
		 * The Unit that is one order of magnitude
		 * larger than this one
		 */
		private Unit up;
		
		/**
		 * Constructs a new Unit with the given
		 * display name, {@link TimeUnit} and
		 * Unit that is one order of magnitude larger
		 * @param name The display name for this unit
		 * @param unit The TimeUnit for this unit
		 * @param up The unit one size bigger than this one
		 */
		private Unit(String name, TimeUnit unit, Unit up){
			this.name = name;
			this.unit = unit;
			this.up = up;
		}
		
		/**
		 * Returns the biggest time unit for which a single
		 * unit is a divisor of the given number of millisecond 
		 * and for which the a single unit of the time unit
		 * that is one biggest is bigger than the given
		 * number of milliseconds. If there is no bigger
		 * unit then {@link #HOUR} is returned.
		 * @param millis The number of milliseconds to
		 *        find the best unit for
		 * @return The best unit for the given number
		 *         of milliseconds
		 */
		private static final Unit fromMillis(long millis){
			for(Unit unit : values()){
				if(millis % unit.unit.toMillis(1) == 0 && (unit.up == null || millis < unit.up.unit.toMillis(1))){
					return unit;
				}
			}
			return null;
		}
		
		@Override
		public String toString(){
			return name;
		}
	}
}
