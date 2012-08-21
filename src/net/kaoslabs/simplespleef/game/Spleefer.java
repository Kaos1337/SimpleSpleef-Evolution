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

import org.bukkit.entity.Player;

/**
 * Models spleefers
 * @author mkalus
 */
public class Spleefer {
	/**
	 * team ids
	 */
	public static final int TEAM_NONE = 0;
	public static final int TEAM_BLUE = 1;
	public static final int TEAM_RED = 2;
	
	/**
	 * get String id of team name (or "none" for errors)
	 * @param team
	 * @return
	 */
	public static String getTeamId(int team) {
		switch (team) {
		case TEAM_BLUE: return "blue";
		case TEAM_RED: return "red";
		}
		return "none";
	}
	
	/**
	 * reference to player object
	 */
	private Player player;
	
	/**
	 * has the player lost?
	 */
	private boolean lost = false;
	
	/**
	 * is player ready to play?
	 */
	private boolean ready = false;

	/**
	 * Team of this player
	 */
	private int team = TEAM_NONE;
	
	/**
	 * Constructor
	 * @param player
	 */
	public Spleefer(Player player) {
		this.player = player;
	}

	/**
	 * @return the player
	 */
	public Player getPlayer() {
		return player;
	}

	/**
	 * @param player the player to set
	 */
	public void setPlayer(Player player) {
		this.player = player;
	}

	/**
	 * @return the lost
	 */
	public boolean hasLost() {
		return lost;
	}

	/**
	 * @param lost the lost to set
	 */
	public void setLost(boolean lost) {
		this.lost = lost;
	}

	/**
	 * @return the ready
	 */
	public boolean isReady() {
		return ready;
	}

	/**
	 * @param ready the ready to set
	 */
	public void setReady(boolean ready) {
		this.ready = ready;
	}

	/**
	 * @return the team
	 */
	public int getTeam() {
		return team;
	}

	/**
	 * @param team the team to set
	 */
	public void setTeam(int team) {
		this.team = team;
	}
}
