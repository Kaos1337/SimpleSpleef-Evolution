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

package net.kaoslabs.simplespleefevolution.game.trackers;

import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;

import net.kaoslabs.simplespleefevolution.SimpleSpleef;
import net.kaoslabs.simplespleefevolution.game.Game;

/**
 * Implements countdown tracker
 * @author mkalus
 *
 */
public class ArenaTimeout implements Tracker {
	/**
	 * flag to toggle interrupts
	 */
	private boolean interrupted = false;
	
	/**
	 * counter
	 */
	private int count = -1;
	
	/**
	 * game reference
	 */
	private Game game;
	
	/**
	 * Constructor
	 * @param count
	 */
	public ArenaTimeout(int count) {
		this.count = count;
	}

	/* (non-Javadoc)
	 * @see net.kaoslabs.game.trackers.Tracker#initialize(net.kaoslabs.game.Game)
	 */
	@Override
	public void initialize(Game game) {
		this.game = game;
	}

	/* (non-Javadoc)
	 * @see net.kaoslabs.game.trackers.Tracker#interrupt()
	 */
	@Override
	public void interrupt() {
		this.interrupted = true;
	}

	/* (non-Javadoc)
	 * @see net.kaoslabs.game.trackers.Tracker#tick()
	 */
	@Override
	public boolean tick() {
		// if interrupted or finished - kill timeout
		if (interrupted || game.isFinished()) return true;
		
		// wait until in game
		if (!game.isInGame()) return false;
		
		// in game - decrease timer
		count--;
		
		// zero?
		if (count == 0) {
			// send message
			game.sendMessage(ChatColor.GOLD + SimpleSpleef.ll("broadcasts.winTimeout", "[ARENA]", game.getName()), SimpleSpleef.getPlugin().getConfig().getBoolean("settings.announceWin", true));
			game.setInterrupted(); // set game status to interrupted
			game.endGame();
			return true;
		}
		
		if (count % 60 == 0) { // announce full minutes
			int minutes = count / 60;
			if (minutes > 1) game.sendMessage(ChatColor.BLUE + SimpleSpleef.ll("feedback.timeoutMinutes", "[COUNT]", String.valueOf(minutes), "[ARENA]", game.getName()), false);
			else game.sendMessage(ChatColor.BLUE + SimpleSpleef.ll("feedback.timeoutMinute", "[ARENA]", game.getName()), false);
		} else if (count == 30) { // 30 secs
			game.sendMessage(ChatColor.BLUE + SimpleSpleef.ll("feedback.timeoutHalfMinute", "[ARENA]", game.getName()), false);
		} else if (count <= 10) { // last ten seconds
			game.sendMessage(ChatColor.BLUE + SimpleSpleef.ll("feedback.timeoutCountdown", "[COUNT]", String.valueOf(count), "[ARENA]", game.getName()), false);
		}
		
		return false;
	}

	@Override
	public boolean updateBlock(Block block, BlockState oldState) { // ignore
		return false;
	}
}
