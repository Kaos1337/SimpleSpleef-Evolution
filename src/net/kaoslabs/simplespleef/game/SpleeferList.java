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

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;

import org.bukkit.entity.Player;

import net.kaoslabs.simplespleef.SimpleSpleef;

/**
 * @author mkalus
 *
 */
public class SpleeferList {
	/**
	 * Get a compiled list of players with commas and and at the end
	 * @param players
	 * @return
	 */
	public static String getPrintablePlayerList(List<Spleefer> players) {
		// build list of winners
		StringBuilder builder = new StringBuilder();
		int i = 0;
		String comma = SimpleSpleef.ll("feedback.infoComma");
		
		// Sort by display name
		TreeSet<String> ordered = new TreeSet<String>();
		for (Spleefer spleefer : players) {
			ordered.add(spleefer.getPlayer().getDisplayName());
		}
		
		// compile list of spleefers
		for (String playerName : ordered) {
			if (i > 0 && i == players.size() - 1) builder.append(SimpleSpleef.ll("feedback.infoAnd")); // last element with end
			else if (i > 0) builder.append(comma); // other elements with ,
			builder.append(playerName);
		}
		return builder.toString();
	}
	
	/**
	 *  list of spleefers currently spleefing
	 *  - Since we have to iterate the list most of the time anyway, we simply use a linked list
	 */
	private HashMap<String, Spleefer> spleefers;

	/**
	 * Constructor
	 */
	public SpleeferList() {
		spleefers = new HashMap<String, Spleefer>();
	}

	/**
	 * Get a spleefer from the list
	 * @param player
	 * @return spleefer or null
	 */
	public Spleefer getSpleefer(Player player) {
		return spleefers.get(player.getName());
	}

	/**
	 * Add a spleefer to the list
	 * @param player
	 * @return boolean, true if successful
	 */
	public boolean addSpleefer(Player player) {
		if (hasSpleefer(player)) return false;
		spleefers.put(player.getName(), new Spleefer(player));
		return true;
	}
	
	/**
	 * Remove spleefer from list
	 * @param player
	 * @return boolean, true if successful
	 */
	public boolean removeSpleefer(Player player) {
		if (spleefers.remove(player.getName()) != null) return true;
		
		return false;
	}

	/**
	 * checks if a player is in the list
	 * @param player
	 * @return
	 */
	public boolean hasSpleefer(Player player) {
		return spleefers.containsKey(player.getName());
	}
	
	/**
	 * checks if a player has lost
	 * @param player
	 * @return
	 */
	public boolean hasLost(Player player) {
		Spleefer spleefer = getSpleefer(player);
		if (spleefer != null) return spleefer.hasLost();

		return false;
	}

	/**
	 * set player to lost
	 * @param player
	 */
	public void setLost(Player player) {
		Spleefer spleefer = getSpleefer(player);
		if (spleefer != null) spleefer.setLost(true);
	}
	
	/**
	 * counts the spleefers still in the game
	 * @return
	 */
	public int inGame() {
		int inGame = 0;
		for (Spleefer spleefer : spleefers.values()) {
			if (!spleefer.hasLost()) inGame++;
		}
		return inGame;
	}
	
	/**
	 * counts the spleefers still in the game (team version)
	 * @param team
	 * @return
	 */
	public int inGame(int team) {
		int inGame = 0;
		for (Spleefer spleefer : spleefers.values()) {
			if (spleefer.getTeam() == team && !spleefer.hasLost()) inGame++;
		}
		return inGame;
	}
	
	/**
	 * counts the size of the spleefers
	 * @return
	 */
	public int size() {
		return spleefers.size();
	}
	
	/**
	 * gets a list of spleefers
	 * @return
	 */
	public List<Spleefer> get() {
		LinkedList<Spleefer> list = new LinkedList<Spleefer>();
		for (Spleefer spleefer : spleefers.values()) {
			list.add(spleefer);
		}
		return list;
	}
	
	/**
	 * get list of players that have lost
	 */
	public List<Spleefer> getLost() {
		LinkedList<Spleefer> list = new LinkedList<Spleefer>();
		for (Spleefer spleefer : spleefers.values()) {
			if (spleefer.hasLost()) list.add(spleefer);
		}
		return list;
	}
	
	/**
	 * get list of players that have not lost yet
	 */
	public List<Spleefer> getNotLost() {
		LinkedList<Spleefer> list = new LinkedList<Spleefer>();
		for (Spleefer spleefer : spleefers.values()) {
			if (!spleefer.hasLost()) list.add(spleefer);
		}
		return list;
	}
	
	/**
	 * get list of players that are not ready yet
	 */
	public List<Spleefer> getUnready() {
		LinkedList<Spleefer> list = new LinkedList<Spleefer>();
		for (Spleefer spleefer : spleefers.values()) {
			if (!spleefer.isReady()) list.add(spleefer);
		}
		return list;
	}
	
	/**
	 * return iterator of spleefers list
	 * @return
	 */
	public Iterator<Spleefer> iterator() {
		return spleefers.values().iterator();
	}
	
	/**
	 * return a specific team list
	 * @param team
	 * @return
	 */
	public List<Spleefer> getTeam(int team) {
		LinkedList<Spleefer> compiledTeam = new LinkedList<Spleefer>();
		for (Spleefer spleefer : spleefers.values()) {
			if (spleefer.getTeam() == team) compiledTeam.add(spleefer);
		}
		
		// empty teams return null
		return compiledTeam;
	}
	
	/**
	 * count unready spleefers
	 * @return
	 */
	public int countUnreadyPlayers() {
		int unready = 0;
		for (Spleefer spleefer : spleefers.values()) {
			if (!spleefer.isReady()) unready++;
		}
		
		return unready;
	}
	
	/**
	 * count lost spleefers
	 * @return
	 */
	public int countLostPlayers() {
		int lost = 0;
		for (Spleefer spleefer : spleefers.values()) {
			if (!spleefer.hasLost()) lost++;
		}
		return lost;
	}
}
