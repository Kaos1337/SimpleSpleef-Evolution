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

import java.util.LinkedList;

import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;

import net.kaoslabs.simplespleef.SimpleSpleef;
import net.kaoslabs.simplespleef.game.Game;
import net.kaoslabs.simplespleef.game.Spleefer;

/**
 * Implements countdown tracker
 * @author mkalus
 *
 */
public class ReadyTimeout implements Tracker {
	/**
	 * flag to toggle interrupts
	 */
	private boolean interrupted = false;

	/**
	 * counter - keeps original count
	 */
	private int originalCount = -1;

	/**
	 * counter
	 */
	private int count = -1;
	
	/**
	 * keep track of counter status
	 */
	private boolean counterStarted = false;
	
	/**
	 * game reference
	 */
	private Game game;
	
	/**
	 * Constructor
	 * @param count
	 */
	public ReadyTimeout(int count) {
		this.originalCount = count;
		this.count = count;
		if(SimpleSpleef.DEBUG_MODE)
			System.out.println("[SpleefArenaDEBUG] tracker creato: "+this);
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
		if (interrupted || game.isInProgress() || game.isFinished()) return true;
		
		if(SimpleSpleef.DEBUG_MODE)
			System.out.println("[SpleefArenaDEBUG]"+ this + " called a tick | interrupted="+interrupted+", game="+game.getStatus()+", count="+count);
		
		// wait as long as game is joinable
		if (!game.isJoinable()) return false;
		
		// check, if there are ready and unready spleefers
		boolean someAreReady = false;
		boolean someAreUnready = false;
		for (Spleefer spleefer : game.getSpleefers().get()) {
			if (someAreReady && someAreUnready) break; // both flags set

			if (spleefer.isReady()) someAreReady = true;
			else someAreUnready = true;
		}
		
		// start timer
		if (someAreReady && !counterStarted) {
			game.sendMessage(ChatColor.GREEN + SimpleSpleef.ll("feedback.readyTimeoutStart", "[ARENA]", game.getName()), false);
			counterStarted = true;
		} else if (counterStarted && !someAreReady) { // counter started and there are no ready spleefers any more
			// reset timer and notify spleefers
			counterStarted = false;
			count = originalCount;
			game.sendMessage(ChatColor.GREEN + SimpleSpleef.ll("feedback.readyTimeoutStop", "[ARENA]", game.getName()), false);
		} else if (counterStarted && someAreUnready) { // counter has started and there are still unready persons
			// decrease timer
			count--;
			
			// zero?
			if (count == 0) {
				game.sendMessage(ChatColor.GREEN + SimpleSpleef.ll("feedback.readyTimeout", "[ARENA]", game.getName(), "[PLAYERS]", game.getListOfUnreadySpleefers()), false);
				// kick out unready spleefers
				LinkedList<Player> kickPlayers = new LinkedList<Player>();
				// aggregate players to be kicked
				for (Spleefer spleefer : game.getSpleefers().get())
					if (!spleefer.isReady())
						kickPlayers.add(spleefer.getPlayer());
				// make them leave
				for (Player player : kickPlayers)
					game.leave(player);
				// check readiness of game
				game.checkReadyAndStartGame();
				// game not started yet?
				if (!game.isInProgress()) {
					// reset timer
					counterStarted = false;
					count = originalCount;
				} else
					return true; // game in progress now - kill timer
			}
			
			if (count % 60 == 0) { // announce full minutes
				int minutes = count / 60;
				if (minutes > 1) game.sendMessage(ChatColor.GREEN + SimpleSpleef.ll("feedback.readyMinutes", "[COUNT]", String.valueOf(minutes), "[ARENA]", game.getName()), false);
				else game.sendMessage(ChatColor.GREEN + SimpleSpleef.ll("feedback.readyMinute", "[ARENA]", game.getName()), false);
			} else if (count == 30) { // 30 secs
				game.sendMessage(ChatColor.GREEN + SimpleSpleef.ll("feedback.readyHalfMinute", "[ARENA]", game.getName()), false);
			} else if (count <= 10) { // last ten seconds
				game.sendMessage(ChatColor.GREEN + SimpleSpleef.ll("feedback.readyCountdown", "[COUNT]", String.valueOf(count), "[ARENA]", game.getName()), false);
			}
		}
		
		return false;
	}

	@Override
	public boolean updateBlock(Block block, BlockState oldState) { // ignore
		return false;
	}
}
