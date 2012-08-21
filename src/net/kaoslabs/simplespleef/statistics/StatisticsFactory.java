/**
 * This file is part of the SimpleSpleef bukkit plugin.
 * Copyright (C) 2012 Maximilian Kalus
 * See http://dev.bukkit.org/server-mods/simple-spleef/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 **/

package net.kaoslabs.simplespleef.statistics;

import org.bukkit.configuration.ConfigurationSection;

/**
 * Factory class to create statistics modules.
 * @author mkalus
 */
public class StatisticsFactory {
	/**
	 * Create a new statistics module
	 * @param moduleName
	 * @return
	 */
	public StatisticsModule getStatisticsModuleByName(String moduleName, ConfigurationSection configuration) throws Exception {
		if (moduleName == null) throw new Exception(); // no NPEs here!
		StatisticsModule module = null;
		
		// file module?
		if (moduleName.equalsIgnoreCase("file")) {
			// just create file statistics - do nothing else
			module = new FileStatisticsModule();
		} else if (moduleName.equalsIgnoreCase("beardstat")) {
			// just create file statistics - do nothing else
			module = new BeardStatisticsModule();
		} else if (moduleName.equalsIgnoreCase("hawkeye")) {
			// just create file statistics - do nothing else
			module = new HawkEyeStatisticsModule();
		}
		// TODO: more module types here
		
		// end by throwing an exception
		if (module == null)
			throw new Exception();
		// initialize module and return reference
		module.initialize();
		return module;
	}
}
