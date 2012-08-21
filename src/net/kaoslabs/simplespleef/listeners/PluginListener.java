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
package net.kaoslabs.simplespleef.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginEnableEvent;

import com.sk89q.worldedit.bukkit.WorldEditAPI;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;

import net.kaoslabs.simplespleef.SimpleSpleef;

/**
 * @author mkalus Listens to plugin events
 */
public class PluginListener implements Listener {
	/**
	 * @param event
	 */
	@EventHandler
	public void onPluginEnable(PluginEnableEvent event) {
		// worldEdit - inspired by MultiversePortals
		if (event.getPlugin().getDescription().getName().equals("WorldEdit")) {
			SimpleSpleef.setWorldEditAPI(new WorldEditAPI((WorldEditPlugin) SimpleSpleef.getPlugin().getServer().getPluginManager().getPlugin("WorldEdit")));
			SimpleSpleef.log.info("[SimpleSpleef] Found WorldEdit. Using it for selections.");
		}
	}
}
