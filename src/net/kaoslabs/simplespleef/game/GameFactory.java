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
package net.kaoslabs.simplespleef.game;

import java.lang.reflect.Constructor;


import net.kaoslabs.simplespleef.SimpleSpleef;

/**
 * @author mkalus
 * Factory class to create games
 */
public abstract class GameFactory {
	/**
	 * factory create a new game instance
	 * @param type
	 * @param name
	 * @return game or null, if game type does not exist
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static Game createGame(String type, String name) {
		// standard games
		if (type == null || type.equalsIgnoreCase("standard"))
			return new GameStandard(name);
		// randomteams games
		if (type.equalsIgnoreCase("randomteams") || type.equalsIgnoreCase("teams") || type.equalsIgnoreCase("randomteam") || type.equalsIgnoreCase("team"))
			return new GameWithTeams(name);
		// inject other game types
		try {
			// define constructor parameters
			Class[] argsClass = new Class[] { String.class };
			// define arguments for this constructor
			Object[] args = new Object[] { new String(name) };
			// use reflection to create new instance
			Class gameClass = Class.forName(name); // get class
			Constructor constructor = gameClass.getConstructor(argsClass); // get constructor
			// call constructor and return game instance
			return (Game) constructor.newInstance(args);
		} catch (Exception e) {
			SimpleSpleef.log.severe("[SimpleSpleef] Could not load class " + name);
			e.printStackTrace();
		}
		return null;
	}
}
