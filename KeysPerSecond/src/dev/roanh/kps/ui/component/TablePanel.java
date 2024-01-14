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
package dev.roanh.kps.ui.component;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.function.IntConsumer;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SwingConstants;

import dev.roanh.kps.Main;
import dev.roanh.kps.config.SettingList;
import dev.roanh.kps.config.group.GraphSettings;
import dev.roanh.kps.config.group.KeyPanelSettings;
import dev.roanh.kps.config.group.LocationSettings;
import dev.roanh.kps.config.group.PanelSettings;
import dev.roanh.kps.layout.LayoutValidator;
import dev.roanh.kps.ui.editor.Editor;
import dev.roanh.kps.ui.editor.GraphEditor;
import dev.roanh.kps.ui.model.EndNumberModel;
import dev.roanh.kps.ui.model.MaxNumberModel;
import dev.roanh.kps.ui.model.SpecialNumberModel.ValueChangeListener;
import dev.roanh.kps.ui.model.SpecialNumberModelEditor;

public class TablePanel extends JPanel{
	/**
	 * Serial ID.
	 */
	private static final long serialVersionUID = 4467273936432261623L;
	private JPanel rows = new JPanel(new GridLayout(0, 1, 0, 2));
	private boolean live;
	private boolean location;
	
	public TablePanel(String name, boolean location, boolean live){
		super(new BorderLayout());
		this.live = live;
		this.location = location;
		
		addHeaders(name);

		setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
		add(rows, BorderLayout.PAGE_START);
		add(new JPanel(), BorderLayout.CENTER);
	}
	
	private void addHeaders(String name){
		JPanel row = new JPanel(new GridLayout(0, location ? 7 : 3, 2, 0));
		row.add(new JLabel(name, SwingConstants.CENTER));
		
		if(location){
			row.add(new JLabel("X", SwingConstants.CENTER));
			row.add(new JLabel("Y", SwingConstants.CENTER));
			row.add(new JLabel("Width", SwingConstants.CENTER));
			row.add(new JLabel("Height", SwingConstants.CENTER));
		}
		
		row.add(new JLabel("Settings", SwingConstants.CENTER));
		row.add(new JLabel("Delete", SwingConstants.CENTER));
		rows.add(row);
	}
	
	public boolean isLive(){
		return live;
	}
	
	public void addGraphs(SettingList<GraphSettings> graphs){
		for(GraphSettings graph : graphs){
			addGraphRow(graphs, graph);
		}
	}
	
	public void addPanels(SettingList<? extends PanelSettings> panels){
		for(PanelSettings panel : panels){
			addPanelRow(panels, panel);
		}
	}
	
	public void addKeys(SettingList<KeyPanelSettings> keys){
		for(KeyPanelSettings key : keys){
			addPanelRow(keys, key);
		}
	}
	
//	/**
//	 * Creates a editable list item for the
//	 * layout configuration dialog
//	 * @param info The positionable that links the 
//	 *        editor to the underlying data
//	 * @param fields The GUI panel that holds all the fields
//	 * @param live Whether or not edits should be displayed in real time
//	 */
	public void addPanelRow(SettingList<? extends PanelSettings> panels, PanelSettings info){
		JPanel row = new JPanel(new GridLayout(0, location ? 7 : 3, 2, 0));
		
		JLabel nameLabel = new JLabel(info.getName(), SwingConstants.CENTER);
		row.add(nameLabel);

		if(location){
			addLocationFields(row, info);
		}

		JButton edit = new JButton("Edit");
		row.add(edit);
		edit.addActionListener(e->{
			info.showEditor(live);
			nameLabel.setText(info.getName());
		});
		
		addDeleteButton(row, panels, info);
		
		rows.add(row);
	}
	
	public void addGraphRow(SettingList<GraphSettings> panels, GraphSettings info){
		JPanel row = new JPanel(new GridLayout(0, location ? 7 : 3, 2, 0));
		row.add(new JLabel("KPS", SwingConstants.CENTER));
		
		if(location){
			addLocationFields(row, info);
		}
		
		JButton edit = new JButton("Edit");
		row.add(edit);
		edit.addActionListener(e->Editor.showEditor(new GraphEditor(info, live)));
		
		addDeleteButton(row, panels, info);

		rows.add(row);
	}
	
	private void addDeleteButton(JPanel row, SettingList<? extends LocationSettings> panels, LocationSettings info){
		JButton delete = new JButton("Remove");
		row.add(delete);
		delete.addActionListener(e->{
			panels.remove(info);
			rows.remove(row);
			revalidate();
			if(live){
				if(info instanceof KeyPanelSettings){
					Main.removeKey(((KeyPanelSettings)info).getKeyCode());
				}
				
				Main.reconfigure();
			}
		});
	}
	
	private void addLocationFields(JPanel row, LocationSettings info){
		LayoutValidator validator = new LayoutValidator();
		validator.getXField().setModel(new EndNumberModel(info.getLayoutX(), validator.getXField(), update(info::setX)));
		validator.getYField().setModel(new EndNumberModel(info.getLayoutY(), validator.getYField(), update(info::setY)));
		validator.getWidthField().setModel(new MaxNumberModel(info.getLayoutWidth(), validator.getWidthField(), update(info::setWidth)));
		validator.getHeightField().setModel(new MaxNumberModel(info.getLayoutHeight(), validator.getHeightField(), update(info::setHeight)));

		JSpinner x = new JSpinner(validator.getXField().getModel());
		x.setEditor(new SpecialNumberModelEditor(x));
		row.add(x);

		JSpinner y = new JSpinner(validator.getYField().getModel());
		y.setEditor(new SpecialNumberModelEditor(y));
		row.add(y);

		JSpinner w = new JSpinner(validator.getWidthField().getModel());
		w.setEditor(new SpecialNumberModelEditor(w));
		row.add(w);

		JSpinner h = new JSpinner(validator.getHeightField().getModel());
		h.setEditor(new SpecialNumberModelEditor(h));
		row.add(h);
	}
	
	/**
	 * Construct a value change listener that sets new values
	 * to the given field and optionally updates the main GUI.
	 * @param field The field to update with new values.
	 * @return The newly constructed change listener.
	 */
	private ValueChangeListener update(IntConsumer field){
		return val->{
			field.accept(val);
			if(live){
				Main.reconfigure();
			}
		};
	}
}
