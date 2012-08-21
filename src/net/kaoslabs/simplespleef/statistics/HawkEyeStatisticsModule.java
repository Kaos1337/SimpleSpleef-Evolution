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

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import uk.co.oliwali.HawkEye.HawkEye;
import uk.co.oliwali.HawkEye.util.HawkEyeAPI;

import net.kaoslabs.simplespleef.SimpleSpleef;
import net.kaoslabs.simplespleef.game.Game;
import net.kaoslabs.simplespleef.game.Spleefer;

/**
 * @author mkalus
 *
 */
public class HawkEyeStatisticsModule extends FileStatisticsModule {
	/**
	 * Manager of player statistics
	 */
	protected boolean isEnabled;

	/* (non-Javadoc)
	 * @see net.kaoslabs.statistics.StatisticsModule#initialize()
	 */
	@Override
	public void initialize() throws Exception {
		// call superclass
		super.initialize();
		
		// initialize beard stats manager
		Plugin plugin = SimpleSpleef.getPlugin().getServer().getPluginManager().getPlugin("HawkEye");
		 
	    // WorldGuard may not be loaded
	    if (plugin == null || !(plugin instanceof HawkEye)) {
	    	isEnabled = false;
	    	SimpleSpleef.log.warning("[SimpleSpleef] Could not initiate HawkEye statistics.");
	    } else {
	    	isEnabled = true;
	    	SimpleSpleef.log.info("[SimpleSpleef] Using HawkEye for statistics.");
	    }
	}

	/* (non-Javadoc)
	 * @see net.kaoslabs.statistics.StatisticsModule#playerWonGame(org.bukkit.entity.Player, net.kaoslabs.game.Game)
	 */
	@Override
	public void playerWonGame(Player player, Game game) {
		// call superclass
		super.playerWonGame(player, game);

		// add stats
		if (isEnabled)
			HawkEyeAPI.addCustomEntry(SimpleSpleef.getPlugin(), "won", player, player.getLocation(), game.getId());
	}

	/* (non-Javadoc)
	 * @see net.kaoslabs.statistics.StatisticsModule#playerLostGame(org.bukkit.entity.Player, net.kaoslabs.game.Game)
	 */
	@Override
	public void playerLostGame(Player player, Game game) {
		// call superclass
		super.playerLostGame(player, game);

		// add stats
		if (isEnabled)
			HawkEyeAPI.addCustomEntry(SimpleSpleef.getPlugin(), "lost", player, player.getLocation(), game.getId());
	}
	
	@Override
	public void gameStarted(Game game) {
		super.gameStarted(game);
		
		// add stats
		if (isEnabled) {
			for (Spleefer spleefer : game.getSpleefers().get()) {
				HawkEyeAPI.addCustomEntry(SimpleSpleef.getPlugin(), "startgame", spleefer.getPlayer(), spleefer.getPlayer().getLocation(), game.getId());
			}
		}
	}
}
