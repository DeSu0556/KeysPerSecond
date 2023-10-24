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
package dev.roanh.kps.config.group;

import java.util.List;
import java.util.Map;

import dev.roanh.kps.config.Setting;
import dev.roanh.kps.config.setting.PrecisionSetting;

public class AveragePanelSettings extends PanelSettings{
	private final PrecisionSetting precision = new PrecisionSetting("precision", 0, 3, 0);
	
	public AveragePanelSettings(){
		super("AVG");
	}
	
	public void setPrecision(int value){
		precision.update(value);
	}
	
	public int getPrecision(){
		return precision.getValue();
	}
	
	public String formatAvg(double value){
		return precision.format(value);
	}
	
	@Override
	public boolean parse(Map<String, String> data){
		return super.parse(data) | findAndParse(data, precision);
	}
	
	@Override
	public List<Setting<?>> collectSettings(){
		List<Setting<?>> data = super.collectSettings();
		data.add(precision);
		return data;
	}
}
