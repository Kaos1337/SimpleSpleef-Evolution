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

/*import me.tehbeard.BeardStat.BeardStat;
import me.tehbeard.BeardStat.containers.*;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import net.kaoslabs.SimpleSpleef;
import net.kaoslabs.game.Game; */

/**
 * @author mkalus
 *
 */
public class BeardStatisticsModule extends FileStatisticsModule {
	/**
	 * Manager of player statistics
	 */
	//protected PlayerStatManager beardStatManager;

	/* (non-Javadoc)
	 * @see net.kaoslabs.statistics.StatisticsModule#initialize()
	 */
	/*@Override
	public void initialize() throws Exception {
		// call superclass
		super.initialize();
		
		// initialize beard stats manager
		Plugin plugin = SimpleSpleef.getPlugin().getServer().getPluginManager().getPlugin("BeardStat");
		 
	    // WorldGuard may not be loaded
	    if (plugin == null || !(plugin instanceof BeardStat)) {
	    	beardStatManager = null;
	    	SimpleSpleef.log.warning("[SimpleSpleef] Could not initiate BeardStat statistics.");
	    } else {
	    	beardStatManager = ((BeardStat) plugin).getStatManager();
	    	SimpleSpleef.log.info("[SimpleSpleef] Using BeardStat for statistics.");
	    }
	}

	/* (non-Javadoc)
	 * @see net.kaoslabs.statistics.StatisticsModule#playerWonGame(org.bukkit.entity.Player, net.kaoslabs.game.Game)
	 */
	/*@Override
	public void playerWonGame(Player player, Game game) {
		// call superclass
		super.playerWonGame(player, game);
		
		// get games won
		String key = "players." + player.getName() + ".";
		int gamesCount = statistics.getInt(key + "gamesWon", 0);
		
		// set stats
		if (beardStatManager != null) {
			PlayerStatBlob blob = beardStatManager.findPlayerBlob(player.getName());
			PlayerStat stat = blob.getStat("spleef", "won");
			if (stat == null) stat = new PlayerStat("spleef", "won", gamesCount);
			else stat.setValue(gamesCount);
		
			blob.addStat(stat);
		}
	}

	/* (non-Javadoc)
	 * @see net.kaoslabs.statistics.StatisticsModule#playerLostGame(org.bukkit.entity.Player, net.kaoslabs.game.Game)
	 */
	/*@Override
	public void playerLostGame(Player player, Game game) {
		// call superclass
		super.playerLostGame(player, game);
		// get games won
		String key = "players." + player.getName() + ".";
		int gamesCount = statistics.getInt(key + "gamesLost", 0);

		// set stats
		if (beardStatManager != null) {
			PlayerStatBlob blob = beardStatManager.findPlayerBlob(player.getName());
			PlayerStat stat = blob.getStat("spleef", "lost");
			if (stat == null) stat = new PlayerStat("spleef", "lost", gamesCount);
			else stat.setValue(gamesCount);
			
			blob.addStat(stat);
		}
	} */
}
