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

package net.kaoslabs.simplespleef.command;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import net.kaoslabs.simplespleef.SimpleSpleef;

/**
 * @author mkalus
 *
 */
public class SimpleSpleefCommandWhitelist implements Listener {
	/**
	 * list of precompiled command patterns
	 */
	private List<Pattern> patterns;
	
	/**
	 * Constructor
	 */
	public SimpleSpleefCommandWhitelist() {
		compileCommandPatterns(); // compile patterns
	}

	/**
	 * compile the command patterns
	 */
	public void compileCommandPatterns() {
		FileConfiguration config = SimpleSpleef.getPlugin().getConfig();

		// compile command patterns, if settings are set to do so
		if (config.getBoolean("checkCommands.activateWhitelist", true) && config.isList("checkCommands.whitelist")) {
			// renew pattern list
			patterns = new LinkedList<Pattern>();

			// compile
			for (String pattern : config.getStringList("checkCommands.whitelist")) {
				patterns.add(Pattern.compile(pattern));
			}
		} else patterns = null;
	}

	/**
	 * Listener: Command preprocessor for the whitelister
	 * @param event
	 */
	@EventHandler(priority=EventPriority.HIGHEST)
	public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
		// some prechecks
		if (patterns == null || event == null || event.isCancelled()) return;
		// Configuration active for checking commands?
		if (!SimpleSpleef.getPlugin().getConfig().getBoolean("checkCommands.activateWhitelist", true)) return;

		Player player = event.getPlayer();
		
		// sanity check
		if(player == null) return;
		
		// is player taking part in a game
		if (SimpleSpleef.getGameHandler().checkPlayerInGame(player) == null) return; // no
		
		// ok, we checked all stuff, now check command itself
		for (Pattern pattern : patterns) {
			// match command
			Matcher m = pattern.matcher(event.getMessage());
			if(m.find()) { // found a matching pattern!
				return; // command was whitelisted, everything ok
			}
		}
		
		// command was not found - cancel command and tell player
		player.sendMessage(ChatColor.DARK_RED + SimpleSpleef.ll("errors.commandNotWhitelisted"));
		event.setCancelled(true);
	}
}
