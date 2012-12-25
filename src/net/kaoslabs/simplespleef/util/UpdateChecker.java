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
package net.kaoslabs.simplespleef.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import net.kaoslabs.simplespleef.SimpleSpleef;

/**
 * @author mkalus Helper class that checks for updates.
 */
public class UpdateChecker implements Listener {
	/**
	 * actually check for an update
	 * @param String version version of current system to check
	 * 
	 * @return new version number, if there is an update or null, if there is no new version available
	 */
	public String checkForUpdate(String version) throws Exception {
		// open HTTP connection
		URL url = new URL("https://raw.github.com/Kaosvf/SimpleSpleef-Evolution/master/check.version");
		HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
		BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		// just read first line
		String inputLine = in.readLine();
		in.close();
		
		if (inputLine != null && Double.valueOf(inputLine) <= Double.valueOf(version)) return null;

		return inputLine; // new version
	}
	
	/**
	 * convert version string to long
	 * @param version
	 * @return
	 */
	protected long versionToNumber(String versionString) {
		String[] noPoints = versionString.split("\\.");
		if (noPoints.length != 3) return 0;
		long version = 0;
		for (int i = 0; i < noPoints.length; i++)
			version = version * 100 + Integer.valueOf(noPoints[i]);
		
		return version;
	}

	/**
	 * Update configuration files, if needed.
	 * @param simpleSpleef
	 */
	public void updateConfigurationVersion(SimpleSpleef simpleSpleef) {
		Configuration config = SimpleSpleef.getPlugin().getConfig();
		// check version number in config file
		int version = config.getInt("version", 1);
		// changed?
		boolean changed = false;
		
		// update stuff
		if (version <= 1) {
			config.set("allowDiggingOutsideArena", null); // delete obsolete setting allowDiggingOutsideArena
			changed = true;
		}
		if (version <= 2) {
			//update loose => lose
			if (config.contains("announceLoose")) {
				config.set("announceLose", config.getBoolean("announceLoose", true));
				config.set("announceLoose", null);
			}
			ConfigHelper configHelper = new ConfigHelper();
			// for each arena
			for (String arena : config.getConfigurationSection("arenas").getKeys(false)) {
				ConfigurationSection myConfig = config.getConfigurationSection("arenas." + arena);
				if (myConfig.contains("looseOnTouchBlocks")) {
					myConfig.set("loseOnTouchBlocks", myConfig.getBoolean("looseOnTouchBlocks", true));
					myConfig.set("looseOnTouchBlocks", null);
				}
				if (myConfig.contains("looseBlocks")) {
					if (myConfig.isList("looseBlocks"))
						myConfig.set("loseBlocks", myConfig.getStringList("looseBlocks"));
					else myConfig.set("loseBlocks", myConfig.get("looseBlocks"));
					myConfig.set("looseBlocks", null);
				}
				if (myConfig.contains("looseOnDeath")) {
					myConfig.set("loseOnDeath", myConfig.getBoolean("looseOnDeath", true));
					myConfig.set("looseOnDeath", null);
				}
				if (myConfig.contains("looseOnLogout")) {
					myConfig.set("loseOnLogout", myConfig.getBoolean("looseOnLogout", true));
					myConfig.set("looseOnLogout", null);
				}
				if (myConfig.contains("playersLooseShovelAtGameEnd")) {
					myConfig.set("playersLoseShovelAtGameEnd", myConfig.getBoolean("playersLooseShovelAtGameEnd", true));
					myConfig.set("playersLooseShovelAtGameEnd", null);
				}
				if (myConfig.contains("loose") && myConfig.isConfigurationSection("loose")) {
					// copy loose to lose
					ConfigurationSection newSection = myConfig.createSection("lose");
					configHelper.copySection(newSection, myConfig.getConfigurationSection("loose"));
					myConfig.set("loose", null); // delete old section
				}
				if (myConfig.contains("looseSpawn") && myConfig.isConfigurationSection("looseSpawn")) {
					// copy looseSpawn to loseSpawn
					ConfigurationSection newSection = myConfig.createSection("loseSpawn");
					configHelper.copySection(newSection, myConfig.getConfigurationSection("looseSpawn"));
					myConfig.set("looseSpawn", null); // delete old section
				}
			}
			changed = true;
		}
		// add new update versions here
		
		// increase version number
		if (changed) {
			SimpleSpleef.log.info("[SimpleSpleef] Updating configuration from version " + version + " to version 3.");
			config.set("version", 3);
			SimpleSpleef.getPlugin().saveConfig();
		}
	}
	
	/**
	 * Checks if a player logs in and checks for updates
	 * @param event
	 */
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		// update checker activated
		if(!SimpleSpleef.BETA_MODE){
			if (SimpleSpleef.getPlugin().getConfig().getBoolean("settings.updateNotificationOnLogin", true)) {
				final Player player = event.getPlayer();
				// Check for updates whenever an operator or user with the right simplespleef.admin joins the game
				if (player != null && (player.isOp() || SimpleSpleef.checkPermission(player, "simplespleef.admin"))) {
					SimpleSpleef.getPlugin().getServer().getScheduler().scheduleSyncDelayedTask(SimpleSpleef.getPlugin(), new Runnable() { // create a new anonymous task/thread that will check the version asyncronously
						@Override
						public void run() {
							try {
								// compare versions
								String oldVersion = SimpleSpleef.getPlugin().getDescription().getVersion();
								String newVersion = checkForUpdate(oldVersion);
								if (newVersion != null) // do we have a version update? => notify player
									player.sendMessage(SimpleSpleef.ll("feedback.update", "[OLDVERSION]", oldVersion, "[NEWVERSION]", newVersion));
							} catch (Exception e) {
								player.sendMessage("SimpleSpleef could not get version update - see log for details.");
								SimpleSpleef.log.warning("[SimpleSpleefEvo] Could not connect to remote server to check for update. Exception said: " + e.getMessage());
							}
						}
					}, 0L);
				}
			}
		}
	}
}
