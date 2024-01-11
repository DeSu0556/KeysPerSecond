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

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import dev.roanh.kps.Main;
import dev.roanh.kps.config.group.GraphSettings;

public class GraphEditor extends Editor{
	/**
	 * Serial ID.
	 */
	private static final long serialVersionUID = 2459364509023481281L;

	public GraphEditor(GraphSettings config, boolean live){
		super("Graph Settings");

		labels.add(new JLabel("Backlog (seconds / " + (1000 / Main.config.getUpdateRateMs()) + "): "));
		JSpinner backlog = new JSpinner(new SpinnerNumberModel(config.getBacklog(), 1, Short.MAX_VALUE, 1));
		backlog.addChangeListener(e->config.setBacklog((int)backlog.getValue()));
		fields.add(backlog);
		
		labels.add(new JLabel("Show average: "));
		JCheckBox avg = new JCheckBox("", config.isAverageVisible());
		fields.add(avg);
		avg.addActionListener(e->{
			config.setAverageVisible(avg.isSelected());
			Main.frame.repaint();
		});
	}
}
