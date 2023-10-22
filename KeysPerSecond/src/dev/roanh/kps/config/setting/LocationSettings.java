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
package dev.roanh.kps.config.setting;

import java.util.Map;

import dev.roanh.kps.config.SettingGroup;

public class LocationSettings implements SettingGroup{
	/**
	 * The x position of the panel.
	 */
	private IntSetting x = new IntSetting("x", -1, Integer.MAX_VALUE, -1);
	/**
	 * The y position of the panel.
	 */
	private IntSetting y = new IntSetting("y", -1, Integer.MAX_VALUE, 0);
	/**
	 * The width of the panel.
	 */
	private IntSetting width = new IntSetting("width", 0, Integer.MAX_VALUE, 2);
	/**
	 * The height of the panel.
	 */
	private IntSetting height = new IntSetting("height", 0, Integer.MAX_VALUE, 3);
	
	/**
	 * Gets the x position for this panel
	 * @return The x position for this panel
	 */
	public int getX(){
		return x.getValue();
	}

	/**
	 * Gets the y position for this panel
	 * @return The y position for this panel
	 */
	public int getY(){
		return y.getValue();
	}

	/**
	 * Gets the width for this panel
	 * @return The width for this panel
	 */
	public int getWidth(){
		return width.getValue();
	}

	/**
	 * Gets the height for this panel
	 * @return The height for this panel
	 */
	public int getHeight(){
		return height.getValue();
	}
	
	@Override
	public boolean parse(Map<String, String> data){
		return findAndParse(data, x) | findAndParse(data, y) | findAndParse(data, width) | findAndParse(data, height);
	}
}
