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

package net.kaoslabs.simplespleef.game.trackers;

import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;

import net.kaoslabs.simplespleef.SimpleSpleef;
import net.kaoslabs.simplespleef.game.Game;

/**
 * Implements countdown tracker
 * @author mkalus
 *
 */
public class Countdown implements Tracker {
	/**
	 * flag to toggle interrupts
	 */
	private boolean interrupted = false;
	
	/**
	 * counter
	 */
	private int count = -1;
	
	/**
	 * should coundown be broadcast?
	 */
	private boolean broadcast;
	
	/**
	 * game reference
	 */
	private Game game;
	
	/**
	 * Constructor
	 * @param count
	 */
	public Countdown(int count) {
		this.count = count;
	}

	/* (non-Javadoc)
	 * @see net.kaoslabs.game.trackers.Tracker#initialize(net.kaoslabs.game.Game)
	 */
	@Override
	public void initialize(Game game) {
		this.game = game;

		// announce countdown?
		broadcast = SimpleSpleef.getPlugin().getConfig().getBoolean("settings.announceCountdown", true);
		// start announcing
		game.sendMessage(ChatColor.BLUE + SimpleSpleef.ll("feedback.countdownStart"), broadcast);
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
		if (interrupted) {
			// send message
			game.sendMessage(ChatColor.RED + SimpleSpleef.ll("feedback.countdownInterrupted"), broadcast);
			game.endGame();
			return true;
		}
		
		// do countdown
		if (count > 0) {
			game.sendMessage(ChatColor.BLUE + SimpleSpleef.ll("feedback.countdown", "[COUNT]", String.valueOf(count), "[ARENA]", game.getName()), broadcast);
			count--;
		} else {
			// send message
			game.sendMessage(ChatColor.BLUE + SimpleSpleef.ll("feedback.countdownGo"), broadcast);
			// start the game itself!
			game.start();
			// cancel countdown
			return true;
		}
		
		return false;
	}

	@Override
	public boolean updateBlock(Block block, BlockState oldState) { // ignore
		return false;
	}
}
