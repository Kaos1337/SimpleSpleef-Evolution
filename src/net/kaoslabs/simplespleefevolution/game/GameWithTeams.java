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

package net.kaoslabs.simplespleefevolution.game;

import java.util.LinkedList;
import java.util.Random;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import net.kaoslabs.simplespleefevolution.SimpleSpleef;
import net.kaoslabs.simplespleefevolution.gamehelpers.LocationHelper;
import net.kaoslabs.simplespleefevolution.gamehelpers.MaterialHelper;

/**
 * @author mkalus
 *
 */
public class GameWithTeams extends GameStandard {
	/**
	 * Constructor
	 * @param name
	 */
	public GameWithTeams(String name) {
		super(name);
	}

	@Override
	public String getType() {
		return "randomteams";
	}

	@Override
	public String getNumberOfPlayers() {
		// if in progress - tell about teams
		if (isInProgress()) {
			// no spleefers - return empty string
			if (spleefers == null || spleefers.size() == 0) return "";

			int max = spleefers.size();
			return new StringBuilder().append(ChatColor.WHITE).append('(')
				.append(ChatColor.BLUE).append(spleefers.inGame(Spleefer.TEAM_BLUE))
				.append(ChatColor.WHITE).append(": ")
				.append(ChatColor.RED).append(spleefers.inGame(Spleefer.TEAM_RED))
				.append(ChatColor.WHITE).append('/')
				.append((max>0?max:'-')).append(')').toString();
		} else return super.getNumberOfPlayers();
	}

	@Override
	public String getListOfSpleefers() {
		// no spleefers - return null
		if (spleefers == null || spleefers.size() == 0) return null;
		// create list of spleefers
		String comma = SimpleSpleef.ll("feedback.infoComma");
		StringBuilder builder = new StringBuilder();
		for (int team = Spleefer.TEAM_RED; team >= Spleefer.TEAM_NONE; team--) {
			// color
			ChatColor teamColor;
			switch (team) {
			case Spleefer.TEAM_BLUE: teamColor = ChatColor.BLUE; break;
			case Spleefer.TEAM_RED: teamColor = ChatColor.RED; break;
			default: teamColor = ChatColor.WHITE;
			}

			// append team
			builder.append(SimpleSpleef.ll("feedback.infoTeam",
					"[TEAM]", teamColor + SimpleSpleef.ll("feedback.team" + Spleefer.getTeamId(team)) + ChatColor.WHITE)).append(' ');
			int i = 0;
			for (Spleefer spleefer : spleefers.get()) {
				if (i > 0) builder.append(comma);  // no ands here...
				// in team?
				if (spleefer.getTeam() == team) {
					// lost or in game?
					if (spleefer.hasLost()) builder.append(ChatColor.RED);
					else builder.append(ChatColor.GREEN);
					builder.append(spleefer.getPlayer().getDisplayName());
					builder.append(ChatColor.WHITE);
					i++;
				}
			}
			if (i == 0) builder.append("---"); // no players in this team
			if (team != Spleefer.TEAM_NONE) builder.append("; "); // separate both team strings
		}
		return builder.toString();		
	}
	
	@Override
	public boolean team(Player player, String team) {
		if (player == null || team == null) return false; // prevent NPEs
		// is spleefer?
		Spleefer spleefer = spleefers.getSpleefer(player);
		if (spleefer == null) return false; // should not happen, because the game handler should have checked that..

		// no teams during gaming
		if (!isJoinable()) {
			player.sendMessage(ChatColor.DARK_RED + SimpleSpleef.ll("errors.teamAlreadyStarted", "[ARENA]", getName()));
			return false;
		}
		// is team command disallowed in the arena?
		if (!configuration.getBoolean("teamCommand", true)) {
			player.sendMessage(ChatColor.DARK_RED + SimpleSpleef.ll("errors.teamNoTeamCommand", "[ARENA]", getName()));
			return false;
		}
		// parse team string
		int teamId;
		if (team.equalsIgnoreCase("red")) teamId = Spleefer.TEAM_RED;
		else if (team.equalsIgnoreCase("blue")) teamId = Spleefer.TEAM_BLUE;
		else { // no valid team name
			player.sendMessage(ChatColor.DARK_RED + SimpleSpleef.ll("errors.teamNoValidTeam", "[NAME]", team));
			return false;			
		}
		// localize team name
		String teamName = SimpleSpleef.ll("feedback." + Spleefer.getTeamId(teamId));
		
		// is the player already part of this team?
		if (spleefer.getTeam() == teamId) {
			player.sendMessage(ChatColor.DARK_RED + SimpleSpleef.ll("errors.teamAlreadyInTeam", "[TEAM]", teamName));
			return false;
		}

		// clear: join the team
		spleefer.setTeam(teamId);
		player.sendMessage(ChatColor.GREEN + SimpleSpleef.ll("feedback.team", "[TEAM]", teamName));

		// broadcast message of somebody joining a team
		String broadcastMessage = ChatColor.DARK_PURPLE + SimpleSpleef.ll("broadcasts.team", "[PLAYER]", player.getDisplayName(), "[ARENA]", getName(), "[TEAM]", teamName);
		if (SimpleSpleef.getPlugin().getConfig().getBoolean("settings.announceTeam", false)) {
			broadcastMessage(broadcastMessage); // broadcast message
		} else {
			// send message to all receivers
			sendMessage(broadcastMessage, player);
		}

		// if teamJoiningAlsoReadies is true, also ready player
		if (configuration.getBoolean("teamJoiningAlsoReadies", true)) {
			spleefer.setReady(true);
			checkReadyAndStartGame(); // check status and possibly start the game
		}
		return true;
	}
	
	@Override
	public boolean onPlayerInteract(PlayerInteractEvent event) {
		if (!isEnabled()) return false; // ignore disabled arenas
		Block block = event.getClickedBlock();
		if (block == null || event.getPlayer() == null || !hasPlayer(event.getPlayer())) return false; // ignore null blocks and null players and players not in game

		if (isJoinable() && configuration.getBoolean("teamCommand", true)) {
			// get blocks
			ItemStack redBlockMaterial;
			try {
				redBlockMaterial = MaterialHelper.getItemStackFromString(configuration.getString("teamBlockMaterialRed", null), true);
			} catch (Exception e) {
				SimpleSpleef.log.warning("[SimpleSpleef] Could not parse teamBlockMaterialRed in arena " + getId());
				return true; // ignore exceptions
			}
			ItemStack blueBlockMaterial;
			try {
				blueBlockMaterial = MaterialHelper.getItemStackFromString(configuration.getString("teamBlockMaterialBlue", null), true);
			} catch (Exception e) {
				SimpleSpleef.log.warning("[SimpleSpleef] Could not parse teamBlockMaterialBlue in arena " + getId());
				return true; // ignore exceptions
			}

			// blocks are ok, now check touched material and join team
			if (redBlockMaterial != null && redBlockMaterial.getTypeId() == block.getTypeId() && MaterialHelper.isSameBlockType(block, redBlockMaterial)) {
				if (team(event.getPlayer(), "red")) return true;
			}
			if (blueBlockMaterial != null && blueBlockMaterial.getTypeId() == block.getTypeId() && MaterialHelper.isSameBlockType(block, blueBlockMaterial)) {
				if (team(event.getPlayer(), "blue")) return true;
			}
		}
		return super.onPlayerInteract(event); // call parent
	}
	
	@Override
	protected boolean checkGameOver() {
		// check if teams are still here
		boolean[] stillHere = {false, false};
		// check spleefer's status
		for (Spleefer spleefer : spleefers.getNotLost()) {
			int team = spleefer.getTeam(); //out-of-bounds check
			if (team == Spleefer.TEAM_BLUE || team == Spleefer.TEAM_RED)
				stillHere[spleefer.getTeam() - 1] = true; // team is still here
		}
		// do we have a team that isn't here?
		for (int i = 0; i < stillHere.length; i++)
			if (!stillHere[i]) return true; // one team has lost
		return false;
	}

	@Override
	protected void winByTouching(Player player) {
		// get team
		int team = -99;
		Spleefer s;
		if ((s = spleefers.getSpleefer(player)) != null) {
			team = s.getTeam();
		}
		if (team == -99) {
			SimpleSpleef.log.warning("[SimpleSpleef] Could not determine winning team while touching a block.");
		}
		
		for (Spleefer spleefer : spleefers.get()) {
			// if not the winning team and not already lost and not in same team
			if (spleefer.getTeam() != team && !spleefer.hasLost())
				playerLoses(spleefer.getPlayer(), true);
		}
	}
	
	@Override
	protected void broadcastWinners(LinkedList<Spleefer> winners) {
		// get the total winners from winners list
		String broadcastKey;
		String replacePlayer = "";
		String team = "---";
		// no winner?
		if (winners.size() == 0) broadcastKey = "None";
		else { // winning team
			broadcastKey = ""; // just winTeam
			// get winning team
			team = SimpleSpleef.ll("feedback.team" + Spleefer.getTeamId(winners.getFirst().getTeam()));
			replacePlayer = SpleeferList.getPrintablePlayerList(winners);
		}
		// broadcast message
		String broadcastMessage = ChatColor.GOLD + SimpleSpleef.ll("broadcasts.winTeam" + broadcastKey, "[PLAYER]", replacePlayer, "[ARENA]", getName(), "[TEAM]", team);
		sendMessage(broadcastMessage, SimpleSpleef.getPlugin().getConfig().getBoolean("settings.announceWin", true));		
	}

	@Override
	protected void teleportPlayersAtGameStart() {
		//create teams at game start
		createTeams();

		// do red or blue spawns exist?
		Location blue = LocationHelper.configToExactLocation(configuration.getConfigurationSection("blueSpawn"));
		Location red = LocationHelper.configToExactLocation(configuration.getConfigurationSection("redSpawn"));
		for (Spleefer spleefer : spleefers.get()) {
			int team = spleefer.getTeam();
			// if spawn of player is not defined, teleport to normal game spawn
			if ((team == Spleefer.TEAM_BLUE && blue == null)
					|| (team == Spleefer.TEAM_RED && red == null))
				teleportPlayer(spleefer.getPlayer(), "game");
			else teleportPlayer(spleefer.getPlayer(), Spleefer.getTeamId(team));
		}
	}

	/**
	 * even out the players to two teams and tell everybody about it
	 */
	protected void createTeams() {
		// npe checks
		if (spleefers == null || spleefers.size() == 0) return;
		
		// list of spleefers of the teams
		LinkedList<Spleefer> blueTeam = new LinkedList<Spleefer>();
		LinkedList<Spleefer> redTeam = new LinkedList<Spleefer>();
		
		// fill teams with spleefers
		for (Spleefer spleefer : spleefers.get()) {
			if (spleefer.getTeam() == Spleefer.TEAM_BLUE) redTeam.add(spleefer);
			else redTeam.add(spleefer); // move all others to the red team
		}
		
		// are the teams unevenly mixed? If yes, even them out on a random basis.
		if (redTeam.size() > blueTeam.size()) evenOutTeamLists(blueTeam, redTeam);
		else if (blueTeam.size() > redTeam.size()) evenOutTeamLists(redTeam, blueTeam);
		
		// we have the teams, now set teams
		for (Spleefer spleefer : blueTeam) spleefer.setTeam(Spleefer.TEAM_BLUE);
		for (Spleefer spleefer : redTeam) spleefer.setTeam(Spleefer.TEAM_RED);

		// broadcast message of player teams, so everyone knows this...
		String broadcastMessage = ChatColor.WHITE + SimpleSpleef.ll("broadcasts.teams", "[TEAMS]", getListOfSpleefers());
		sendMessage(broadcastMessage, SimpleSpleef.getPlugin().getConfig().getBoolean("settings.announceTeam", false));
	}
	
	/**
	 * even out the teams
	 * @param smaller
	 * @param larger
	 */
	protected void evenOutTeamLists(LinkedList<Spleefer> smaller, LinkedList<Spleefer> larger) {
		// if the size difference is 1, ignore evening out - also ignore 1 member teams
		if (larger.size()-1 == smaller.size() || larger.size() <= 1) return;
		
		Random generator = new Random();
		while (smaller.size() < larger.size()) { // take one of the red team members from team red to blue until they even out
			int index = generator.nextInt(larger.size() -1);
			smaller.add(larger.get(index));
			larger.remove(index);
		}
	}
}
