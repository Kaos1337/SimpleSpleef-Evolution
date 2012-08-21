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

import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import net.kaoslabs.simplespleef.SimpleSpleef;
import net.kaoslabs.simplespleef.game.trackers.Tracker;

/**
 * @author mkalus
 *
 */
public abstract class Game {
	/**
	 * game status constants
	 */
	protected static final int STATUS_INACTIVE = 0; // no game started
	protected static final int STATUS_NEW = 1; // game initialized, players may join
	protected static final int STATUS_READY = 2; // game is ready to be started
	protected static final int STATUS_COUNTDOWN = 3; // game countdown is running
	protected static final int STATUS_STARTED = 4; // game is actually running
	protected static final int STATUS_FINISHED = 5; // game has finished, now cleaning up
	protected static final int STATUS_INTERRUPTED = 10; // game has been interrupted

	/**
	 * name of the game/arena
	 */
	private final String name;

	/**
	 * game status
	 */
	protected int status;

	/**
	 * Constructor
	 * @param name
	 */
	public Game(String name) {
		this.name = name;
		this.status = STATUS_INACTIVE;
	}

	/**
	 * @return the id (normaly name to lower case)
	 */
	public String getId() {
		return this.name.toLowerCase();
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * @return the name
	 */
	public int getStatus() {
		return this.status;
	}
	
	/**
	 * get type of arena
	 * @return
	 */
	public abstract String getType();
	
	/**
	 * get the list of spleefers
	 * @return
	 */
	public abstract SpleeferList getSpleefers();

	// read the configuration settings of this arena
	public abstract void defineSettings(ConfigurationSection conf);
	
	/**
	 * call game ticker periodically
	 * @return
	 */
	public abstract void tick();

	/**
	 * Player issues announce command
	 * @param player
	 * @return boolean successful?
	 */
	public abstract boolean announce(CommandSender sender);

	/**
	 * Player issues join command
	 * @param player
	 * @return boolean successful?
	 */
	public abstract boolean join(Player player);
	
	/**
	 * Player issues leave command
	 * @param player
	 * @return boolean successful?
	 */
	public abstract boolean leave(Player player);

	/**
	 * Attempt to join a team
	 * @return boolean successful?
	 */
	public abstract boolean team(Player player, String team);

	/**
	 * Mark player as ready
	 * @return boolean successful?
	 */
	public abstract boolean ready(Player player, boolean hitBlock);

	/**
	 * check whether a certain block may be broken
	 * => player has been checked before this, so this does only concern block breaks
	 * and interactions by spleefers
	 * @param block broken/interacted (on instant-break) by spleefer
	 * @param player - player or null for automatic changes (e.g. trackers)
	 * @return true, if block may be destroyed
	 */
	public abstract boolean checkMayBreakBlock(Block block, Player player);
	
	/**
	 * Is this arena enabled?
	 * @return
	 */
	public abstract boolean isEnabled();

	/**
	 * Arena contains active game?
	 * @return
	 */
	public boolean isActive() {
		return this.status != STATUS_INACTIVE;
	}
	
	/**
	 * is game joinable?
	 * @return true for joinable
	 */
	public boolean isJoinable() {
		return this.status <= STATUS_READY;
	}

	/**
	 * is game ready?
	 * @return true for game ready
	 */
	public boolean isReady() {
		// game must be readied?
		if (supportsReady())
			return this.status == STATUS_READY;
		return this.status <= STATUS_READY; // without using ready, game is ready automatically
	}

	/**
	 * is game in progress?
	 * @return true/false
	 */
	public boolean isInProgress() {
		return this.status == STATUS_COUNTDOWN || this.status == STATUS_STARTED;
	}

	/**
	 * has game started (not countdown)?
	 * @return true/false
	 */
	public boolean isInGame() {
		return this.status == STATUS_STARTED;
	}

	/**
	 * has game finished?
	 * @return true/false
	 */
	public boolean isFinished() {
		return this.status == STATUS_FINISHED;
	}

	/**
	 * has game been interrupted?
	 * @return true/false
	 */
	public boolean isInterrupted() {
		return this.status == STATUS_INTERRUPTED;
	}

	/**
	 * set game status to interrupted - mostly for trackers to teleport players back
	 * @return true/false
	 */
	public void setInterrupted() {
		this.status = STATUS_INTERRUPTED;
	}
	
	/**
	 * Return true, if game supports a "ready" players list. Override for your own inventions.
	 * @return
	 */
	public abstract boolean supportsReady();
	
	/**
	 * Return true, if game supports a "ready" players list. Override for your own inventions.
	 * @return
	 */
	public abstract boolean supportsCommandReady();
	
	/**
	 * Return true, if game supports a "ready" players list. Override for your own inventions.
	 * @return
	 */
	public abstract boolean supportsBlockReady();


	/**
	 * Countdown started
	 * @param sender
	 * @return boolean successful?
	 */
	public abstract boolean countdown(CommandSender sender);

	/**
	 * Start command issued
	 * @return boolean successful?
	 */
	public abstract boolean start();

	/**
	 * Stop command issued
	 * @param player
	 * @return boolean successful?
	 */
	public abstract boolean stop(Player player);

	/**
	 * Delete/reset command issued
	 * @param sender
	 * @return boolean successful?
	 */
	public abstract boolean delete(CommandSender sender);

	/**
	 * Watch command issued
	 * @param player
	 * @return
	 */
	public abstract boolean watch(Player player);
	
	/**
	 * Back command issued
	 * @param player
	 * @return
	 */
	public abstract boolean back(Player player);

	/**
	 * Check if player is in arena
	 * @return boolean
	 */
	public abstract boolean hasPlayer(Player player);

	/**
	 * Check if player is spectator in arena
	 * @return boolean
	 */
	public abstract boolean hasSpectator(Player player);

	/**
	 * Remove spectator from arena
	 * @return boolean
	 */
	public abstract boolean removeSpectator(Player player);
	
	/**
	 * check the readiness status of a game and possibly start it
	 */
	public abstract void checkReadyAndStartGame();
	
	/**
	 * Called when a spleefer moves in this game
	 * @param event
	 */
	public abstract boolean onPlayerMove(PlayerMoveEvent event);
	
	/**
	 * Called when a spleefer tries to teleport in this game
	 * may be called by the game itself in a way, so it should be able to
	 * teleport the player in certain cases
	 * @param player
	 */
	public abstract boolean playerMayTeleport(Player player);
	
	/**
	 * Called when a spleefer interacts with something in this game
	 * @param event
	 */
	public abstract boolean onPlayerInteract(PlayerInteractEvent event);
	
	/**
	 * Called when a spleefer quits
	 * @param event
	 */
	public abstract boolean onPlayerQuit(PlayerQuitEvent event);

	/**
	 * Called when a spleefer gets kicked
	 * @param event
	 */
	public abstract boolean onPlayerKick(PlayerKickEvent event);
	
	/**
	 * Called when a player joins - game can check if the player has quit recently, for example
	 * @param event
	 * @return 
	 */
	public abstract boolean onPlayerJoin(PlayerJoinEvent event);
	
	/**
	 * Called when a spleefer dies during this game
	 * @param event
	 * @return 
	 */
	public abstract boolean onPlayerDeath(Player player);
	
	/**
	 * Called when a spleefer breaks a block
	 * @param event
	 * @return 
	 */
	public abstract boolean onBlockBreak(BlockBreakEvent event);
	
	/**
	 * Called when a spleefer places a block
	 * @param event
	 * @return 
	 */
	public abstract boolean onBlockPlace(BlockPlaceEvent event);

	/**
	 * Called when a players food level changes
	 * @param event
	 */
	public abstract boolean onFoodLevelChange(FoodLevelChangeEvent event);

	/**
	 * Called when a player is damaged
	 * @param event
	 */
	public abstract boolean onEntityDamage(EntityDamageEvent event);

	/**
	 * Called when something explodes 
	 * @param event
	 */
	public abstract boolean onEntityExplode(EntityExplodeEvent event);

	/**
	 * Called when a player teleports
	 * @param event
	 */
	public abstract boolean onPlayerTeleport(PlayerTeleportEvent event);

	/**
	 * Called when a player changes game mode
	 * @param event
	 * @return 
	 */
	public abstract boolean onPlayerGameModeChange(PlayerGameModeChangeEvent event);
	
	/**
	 * Called when a creature spawns
	 * @param event
	 * @return
	 */
	public abstract boolean onCreatureSpawn(CreatureSpawnEvent event);
	
	/**
	 * Called when an entity picks/places a block
	 * @param event
	 * @return
	 */
	public abstract boolean onEntityChangeBlock(EntityChangeBlockEvent event);

	/**
	 * Send a message to broadcast, or to players and spectators
	 * @param message
	 * @param broadcast
	 */
	public abstract void sendMessage(String message, boolean broadcast);
	
	/**
	 * Send a message to broadcast, or to players and spectators
	 * @param message
	 * @param exception exception - this player does not receive message
	 */
	public abstract void sendMessage(String message, Player exception);

	/**
	 * Get number of players
	 * @return something like (1/2)
	 */
	public abstract String getNumberOfPlayers();
	
	/**
	 * return a comma separated list of spleefers (or null)
	 * @return
	 */
	public abstract String getListOfSpleefers();
	
	/**
	 * get a list of unready spleefers
	 * @return
	 */
	public abstract String getListOfUnreadySpleefers();
	
	/**
	 * return a comma separated list of spectators (or null)
	 * @return
	 */
	public abstract String getListOfSpectators();
	
	/**
	 * add a tracker
	 * @param tracker
	 */
	public abstract void addTracker(Tracker tracker);

	/**
	 * interrupt a tracker
	 * @param tracker
	 */
	public abstract void interruptTracker(Tracker tracker);

	/**
	 * interrupt all tracker
	 * @param tracker
	 */
	public abstract void interruptAllTrackers();
	
	/**
	 * get time when game started
	 * @return timestamp or -1
	 */
	public abstract long getStartTime();

	/**
	 * get time when game finished
	 * @return timestamp or -1
	 */
	public abstract long getFinishTime();

	/**
	 * update a certain block location - notify trackers of this change
	 * @param block
	 * @param oldState old state of block
	 */
	public abstract void trackersUpdateBlock(Block block, BlockState oldState);
	
	/**
	 * Called by tick()-methods of trackers when they change a block,
	 * so other trackers can update their block database. 
	 * @param block
	 * @param oldState old state of block
	 * @param caller
	 */
	public abstract void notifyChangedBlock(Block block, BlockState oldState, Tracker caller);

	/**
	 * interrupt/end game
	 */
	public abstract boolean endGame();

	/**
	 * sends a list of players and spectators of a specific game to a sender
	 * @param sender
	 * @param game
	 */
	public void printGamePlayersAndSpectators(CommandSender sender) {

		// list of spleefers and spectators
		if (sender != null) {
			String spleefers = getListOfSpleefers();
			String spectators = getListOfSpectators();

			//info if there are no spleefers and spectators at all
			if (spleefers == null && spectators == null) {
				sender.sendMessage(SimpleSpleef.ll("feedback.infoEmpty"));
				return;
			}

			// otherwise list separately
			if (spleefers != null)
				sender.sendMessage(SimpleSpleef.ll("feedback.infoSpleefers", "[SPLEEFERS]", spleefers));
			if (supportsCommandReady()) {
				String unready = getListOfUnreadySpleefers();
				if (unready != null)
					sender.sendMessage(SimpleSpleef.ll("feedback.infoUnreadySpleefers", "[SPLEEFERS]", ChatColor.RED + unready));
				else if (getListOfUnreadySpleefers() != null) sender.sendMessage(ChatColor.GREEN + SimpleSpleef.ll("feedback.infoAllReady"));
			}
			if (spectators != null)
				sender.sendMessage(SimpleSpleef.ll("feedback.infoSpectators", "[SPECTATORS]", spectators));
		}		
	}
	
	/**
	 * save arena to disk
	 * @param sender (can be null for console)
	 * @param temporaryStore save in temporary store
	 */
	public abstract void saveArena(CommandSender sender, boolean temporaryStore);

	/**
	 * restore arena from disk
	 * @param sender (can be null for console)
	 * @param temporaryStore restore from temporary store
	 */
	public abstract void restoreArena(CommandSender sender, boolean temporaryStore);
}
