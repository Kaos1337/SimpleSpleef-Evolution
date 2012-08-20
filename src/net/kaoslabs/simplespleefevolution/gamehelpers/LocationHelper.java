/**
 * This file is part of the SimpleSpleef bukkit plugin.
 * Copyright (C) 2011 Maximilian Kalus
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
package net.kaoslabs.simplespleefevolution.gamehelpers;

import java.util.Map;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;

import net.kaoslabs.simplespleefevolution.SimpleSpleef;

/**
 * @author mkalus
 * Class helping with locations
 */
public abstract class LocationHelper {
	/**
	 * get location including yaw and pitch from location
	 * @param location
	 * @param enabled
	 * @return
	 */
	public static Map<String, Object> getExactLocation(Location location, boolean enabled) {
		// create configuration
		ConfigurationSection conf = new MemoryConfiguration();
		// define stuff
		conf.set("enabled", enabled);
		conf.set("world", location.getWorld().getName());
		conf.set("x", location.getX());
		conf.set("y", location.getY());
		conf.set("z", location.getZ());
		conf.set("yaw", location.getYaw());
		conf.set("pitch", location.getPitch());
		
		return conf.getValues(true);
	}

	/**
	 * get location of a block from location
	 * @param location
	 * @return
	 */
	public static Map<String, Object> getXYZLocation(Location location) {
		// create configuration
		ConfigurationSection conf = new MemoryConfiguration();
		// define stuff
		conf.set("world", location.getWorld().getName());
		conf.set("x", location.getBlockX());
		conf.set("y", location.getBlockY());
		conf.set("z", location.getBlockZ());
		
		return conf.getValues(true);
	}

	/**
	 * get exact location from config section
	 * @param config
	 * @return
	 */
	public static Location configToExactLocation(ConfigurationSection config) {
		if (!config.getBoolean("enabled", false)) return null; // disabled position
		try {
			World world = SimpleSpleef.getPlugin().getServer().getWorld(config.getString("world"));
			return new Location(world, config.getDouble("x"), config.getDouble("y"), config.getDouble("z"), (float) config.getDouble("yaw"), (float) config.getDouble("pitch"));
		} catch (Exception e) {
			return null;
		}
	}
}
