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
package dev.roanh.kps.ui.dialog;

import java.awt.*;
import java.awt.event.ItemEvent;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.prefs.BackingStoreException;

import javax.swing.*;

import dev.roanh.kps.config.ConfigLoader;
import dev.roanh.kps.config.Configuration;
import dev.roanh.kps.translation.Translator;
import dev.roanh.kps.ui.model.FilePathFormatterFactory;
import dev.roanh.kps.utils.MapUtils;
import dev.roanh.util.Dialog;

/**
 * Dialog used to configure the default configuration.
 *
 * @author Roan
 */
public class DefaultConfigDialog extends JPanel {
	/**
	 * Serial ID.
	 */
	private static final long serialVersionUID = 7089667269158157654L;
	/**
	 * The text field holding the default config.
	 */
	private JTextField selectedFile = new JFormattedTextField(new FilePathFormatterFactory(), Objects.toString(ConfigLoader.getDefaultConfig(), ""));

    /*
    * Currently available languages
    * */
	private static Map<String, String> availableLanguageMaps = Translator.getAvailableLanguageMaps();

    /*
    * The dropdown list of default language.
    * */
	private JComboBox<Map.Entry<String, String>> languageSelections = new JComboBox<>();

	/*
	* Whether to refresh the interface when exiting
	* */
	private boolean reloadWindowOnClose = false;

	/**
	 * Constructs a new default config dialog.
	 */
	private DefaultConfigDialog() {
		super(new BorderLayout(0, 5));

		add(new JLabel("You can configure a default configuration to be opened automatically on launch."), BorderLayout.PAGE_START);

		JPanel vecPanel = new JPanel();
		vecPanel.setLayout(new BoxLayout(vecPanel, BoxLayout.Y_AXIS));

		JPanel configLayout = new JPanel();
		configLayout.setLayout(new BoxLayout(configLayout, BoxLayout.X_AXIS));

		JButton select = new JButton("Select");
		configLayout.add(new JLabel("Config: "));
		configLayout.add(selectedFile);
		configLayout.add(select);

		JPanel languagePanel = new JPanel();
		languagePanel.setLayout(new BoxLayout(languagePanel, BoxLayout.X_AXIS));

		languageSelections.setRenderer(new CustomCellRenderer());
		languageSelections.addItemListener(e -> {
			if (e.getStateChange() == ItemEvent.SELECTED) {
				reloadWindowOnClose = true;
			}
		});

		// add the current language to make it the first
		String currentLanguageTag = Translator.getCurrentUsingLocale().toString();
		languageSelections.addItem(MapUtils.getEntryByKey(availableLanguageMaps, currentLanguageTag));

		// add other language
		for (Map.Entry<String, String> entry : availableLanguageMaps.entrySet()) {
			if (entry.getKey().equals(currentLanguageTag)) {
				continue;
			}
			languageSelections.addItem(entry);
		}

		languagePanel.add(new Label("Default Language: "));
		languagePanel.add(languageSelections);

		vecPanel.add(configLayout);
		vecPanel.add(Box.createVerticalStrut(8));
		vecPanel.add(languagePanel);
		add(vecPanel, BorderLayout.CENTER);

		select.addActionListener(e -> {
			Path file = Dialog.showFileOpenDialog(Configuration.KPS_NEW_EXT);
			if (file != null) {
				selectedFile.setText(file.toAbsolutePath().toString());
			}
		});
	}

	public Locale getSelectedLocale() {
        Map.Entry<String, String> languageEntry = ((Map.Entry<String, String>) languageSelections.getSelectedItem());
        return Locale.forLanguageTag(languageEntry.getKey().replace("_", "-"));
    }

	/**
	 * Shows a dialog to configure the default configuration file to use.
	 * @param current Provides an interface that can be used to rebuild the original GUI
	 */
	public static final void showDefaultConfigDialog(OnChangeLocale current) {
		DefaultConfigDialog dialog = new DefaultConfigDialog();
		try {
			switch (Dialog.showDialog(dialog, new String[]{"Save", "Remove Default Config", "action.cancel"})) {
				case 0:
					ConfigLoader.setDefaultConfig(Paths.get(dialog.selectedFile.getText()));

					if (dialog.reloadWindowOnClose) {
						Locale selectedLocale = dialog.getSelectedLocale();
						ConfigLoader.setDefaultLanguage(selectedLocale);
						Translator.loadTranslation(selectedLocale);
						current.onChangeLocale(selectedLocale);
                    }

					break;
				case 1:
					ConfigLoader.setDefaultConfig(null);
					break;
				case 2:
				default:
					break;
			}
		} catch (BackingStoreException | InvalidPathException e) {
			e.printStackTrace();
			Dialog.showErrorDialog("Failed to save default config, cause: " + e.getMessage());
		}
	}

	static class CustomCellRenderer extends DefaultListCellRenderer {
		@Override
		public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
			Component component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			if (value instanceof Map.Entry) {
				String label = ((Map.Entry<String, String>) value).getValue();
				setText(label);
			}
			return component;
		}
	}
}
