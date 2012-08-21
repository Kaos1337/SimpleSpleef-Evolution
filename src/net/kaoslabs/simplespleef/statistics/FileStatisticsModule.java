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

package net.kaoslabs.simplespleef.statistics;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import net.kaoslabs.simplespleef.SimpleSpleef;
import net.kaoslabs.simplespleef.game.Game;
import net.kaoslabs.simplespleef.game.Spleefer;

/**
 * Saves statistics to a flat file in the plugin directory
 * @author mkalus
 */
public class FileStatisticsModule implements StatisticsModule {
	/**
	 * statistics reference (YAML)
	 */
	protected YamlConfiguration statistics;
	
	/**
	 * file reference
	 */
	protected File statisticsFile;

	/* (non-Javadoc)
	 * @see net.kaoslabs.statistics.StatisticsModule#initialize()
	 */
	@Override
	public void initialize() throws Exception {
		statisticsFile = new File(SimpleSpleef.getPlugin().getDataFolder(), "statistics.yml");
		
		// does the file exist - no
		if (!statisticsFile.exists()) {
			SimpleSpleef.log.info("[SimpleSpleef] Using flat file statistics - creating new file.");
			statistics = new YamlConfiguration();
			try {
				statistics.save(statisticsFile);
			} catch (IOException e) {
				SimpleSpleef.log.severe("[SimpleSpleef] Could not create statistics file. Error: " + e.getMessage());
			}
		} else { // load file
			statistics = YamlConfiguration.loadConfiguration(statisticsFile);
			if (statistics == null)
				throw new Exception("Could not load YAML file " + statisticsFile + ".");
		}
	}

	/* (non-Javadoc)
	 * @see net.kaoslabs.statistics.StatisticsModule#playerWonGame(org.bukkit.entity.Player, net.kaoslabs.game.Game)
	 */
	@Override
	public void playerWonGame(Player player, Game game) {
		String key = "players." + player.getName() + ".";
		
		// increase win counter
		int gamesCount = statistics.getInt(key + "gamesWon", 0) + 1;
		statistics.set(key + "gamesWon", gamesCount);

		// increase arena winner count
		String gkey = "arenas." + game.getId() + ".players." + player.getName() + ".gamesWon";
		int arenaWon = statistics.getInt(gkey, 0) + 1;
		statistics.set(gkey, arenaWon);
		
		// add last game activity entry
		statistics.set(key + "lastGameTick", game.getStartTime());

		// save statistics
		saveStatistics();
	}

	/* (non-Javadoc)
	 * @see net.kaoslabs.statistics.StatisticsModule#playerLostGame(org.bukkit.entity.Player, net.kaoslabs.game.Game)
	 */
	@Override
	public void playerLostGame(Player player, Game game) {
		String key = "players." + player.getName() + ".";
		
		// increase win counter
		int gamesCount = statistics.getInt(key + "gamesLost", 0) + 1;
		statistics.set(key + "gamesLost", gamesCount);

		// increase arena loser count
		String gkey = "arenas." + game.getId() + ".players." + player.getName() + ".gamesLost";
		int arenaLost = statistics.getInt(gkey, 0) + 1;
		statistics.set(gkey, arenaLost);

		// add last game activity entry
		statistics.set(key + "lastGameTick", game.getStartTime());

		// save statistics
		saveStatistics();
	}

	/* (non-Javadoc)
	 * @see net.kaoslabs.statistics.StatisticsModule#gameStarted(net.kaoslabs.game.Game)
	 */
	@Override
	public void gameStarted(Game game) {
		String key = "arenas." + game.getId() + ".";
		
		// increase game counter
		int gamesCount = statistics.getInt(key + "gamesCount", 0) + 1;
		statistics.set(key + "gamesCount", gamesCount);
		
		// number of players in last game
		int playersCount = game.getSpleefers().get().size();
		statistics.set(key + "lastGamePlayers", playersCount);
		
		// get total players
		int totalPlayers = statistics.getInt(key + "gamesTotalPlayers");
		
		// add to total players and set average number of players
		statistics.set(key + "gamesTotalPlayers", totalPlayers + playersCount);
		//statistics.set(key + "gamesAveragePlayers", ((double) (totalPlayers + playersCount)) / (double) gamesCount);

		// add last game started entry
		statistics.set(key + "lastGameStartedAt", game.getStartTime());
		
		// reset last game finished (still in progress)
		statistics.set(key + "lastGameFinishedAt", -1);
		
		// increase game counter for players
		for (Spleefer spleefer : game.getSpleefers().get()) {
			String pkey = "players." + spleefer.getPlayer().getName() + ".";
			statistics.set(pkey + "gamesCount", statistics.getInt(pkey + "gamesCount", 0) + 1);

			// add last game activity entry
			statistics.set(pkey + "lastGameTick", game.getStartTime());
			
			// increase game counter for player in this arena
			pkey = key + pkey + "gamesCount";
			statistics.set(pkey, statistics.getInt(pkey, 0) + 1);
		}

		// save statistics
		saveStatistics();
	}

	/* (non-Javadoc)
	 * @see net.kaoslabs.statistics.StatisticsModule#gameFinished(net.kaoslabs.game.Game)
	 */
	@Override
	public void gameFinished(Game game) {
		String key = "arenas." + game.getId() + ".";
		
		// add last game started entry
		statistics.set(key + "lastGameFinishedAt", game.getFinishTime());
		
		// length of last game
		long length = game.getFinishTime() - game.getStartTime();
		statistics.set(key + "lastGameLength", length);
		
		// get total length
		long totalLength = statistics.getLong(key + "gamesTotalLength");
		
		// add to total length and set average length
		statistics.set(key + "gamesTotalLength", totalLength + length);
		//int gamesCount = statistics.getInt(key + "gamesCount");
		//statistics.set(key + "gamesAverageLength", (totalLength + length) / gamesCount);
		
		// save statistics
		saveStatistics();
	}

	/* (non-Javadoc)
	 * @see net.kaoslabs.statistics.StatisticsModule#getStatistics(org.bukkit.entity.Player)
	 */
	@Override
	public HashMap<String, Object> getStatistics(String player) {
		// get config section
		ConfigurationSection playerSection = statistics.getConfigurationSection("players." + player);
		
		// is it null?
		if (playerSection == null) return null;
		
		// create hashmap
		HashMap<String, Object> map = new HashMap<String, Object>();
		// populate with values
		map.put("gamesCount", playerSection.getInt("gamesCount", 0));
		map.put("gamesLost", playerSection.getInt("gamesLost", 0));
		map.put("gamesWon", playerSection.getInt("gamesWon", 0));
		map.put("lastGameTick", playerSection.getLong("lastGameTick", -1));

		return map;
	}

	/* (non-Javadoc)
	 * @see net.kaoslabs.statistics.StatisticsModule#getStatistics(net.kaoslabs.game.Game)
	 */
	@Override
	public HashMap<String, Object> getStatistics(Game game) {
		// get config section
		ConfigurationSection arenaSection = statistics.getConfigurationSection("arenas." + game.getId());
		
		// is it null?
		if (arenaSection == null) return null;

		// create hashmap
		HashMap<String, Object> map = new HashMap<String, Object>();
		// populate with values
		map.put("gamesCount", arenaSection.getInt("gamesCount", 0));
		map.put("lastGamePlayers", arenaSection.getInt("lastGamePlayers", 0));
		map.put("gamesTotalPlayers", arenaSection.getInt("gamesTotalPlayers", 0));
		map.put("lastGameStartedAt", arenaSection.getLong("lastGameStartedAt", 0));
		map.put("lastGameFinishedAt", arenaSection.getLong("lastGameFinishedAt", 0));
		map.put("lastGameLength", arenaSection.getInt("lastGameLength", 0));
		map.put("gamesTotalLength", arenaSection.getInt("gamesTotalLength", 0));

		return map;
	}

	@Override
	public List<TopTenEntry> getTopTen() {
		return getTopTen("players");
	}

	@Override
	public List<TopTenEntry> getTopTen(Game game) {
		return getTopTen("arenas." + game.getId() + ".players");
	}
	
	/**
	 * actual worker method to get top 10 players
	 * @param key
	 * @return
	 */
	private List<TopTenEntry> getTopTen(String key) {
		TreeSet<TopTenEntry> topTenList = new TreeSet<TopTenEntry>(TopTenEntry.getAscendingComparator());
		if (statistics == null || statistics.getConfigurationSection(key) == null) return null; // avoid NPEs
		// get list of all gamers
		for (String playerName : statistics.getConfigurationSection(key).getKeys(false)) {
			ConfigurationSection playerSection = statistics.getConfigurationSection(key + "." + playerName);

			TopTenEntry topTenEntry = new TopTenEntry();
			topTenEntry.player = playerName;
			topTenEntry.games = playerSection.getInt("gamesCount", 0);
			topTenEntry.lost = playerSection.getInt("gamesLost", 0);
			topTenEntry.won = playerSection.getInt("gamesWon", 0);
			
			topTenList.add(topTenEntry);
		}

		// get top 10
		int count = 0;
		LinkedList<TopTenEntry> topten = new LinkedList<TopTenEntry>();
		for (TopTenEntry entry : topTenList) {
			topten.add(entry);
			if (++count >= 10) break;
		}

		return topten;
	}

	/**
	 * save statistics to disk
	 */
	private void saveStatistics() {
		try {
			statistics.save(statisticsFile);
		} catch (Exception e) {
			SimpleSpleef.log.severe("[SimpleSpleef] Could not save statistics file " +  statisticsFile + ". Reason: " + e.getMessage());
		}
	}
}
