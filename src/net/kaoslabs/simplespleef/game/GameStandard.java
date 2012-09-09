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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import net.milkbowl.vault.economy.EconomyResponse;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
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
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;


import net.kaoslabs.simplespleef.SimpleSpleef;
import net.kaoslabs.simplespleef.game.arenarestoring.*;
import net.kaoslabs.simplespleef.game.trackers.*;
import net.kaoslabs.simplespleef.gamehelpers.*;
import net.kaoslabs.simplespleef.util.StringUtils;

/**
 * @author mkalus
 *
 */
public class GameStandard extends Game {
	/**
	 * static reference to random generator
	 */
	private static Random generator = new Random();
	
	/**
	 * Reference to spleefer list
	 */
	protected SpleeferList spleefers;
	
	/**
	 * Reference to spectators
	 */
	protected List<Player> spectators;
	
	/**
	 * Reference to configuration
	 */
	protected ConfigurationSection configuration;
	
	/**
	 * list of trackers called in each tick
	 */
	protected List<Tracker> trackers;

	/**
	 * to make game faster:
	 */
	protected Set<Material> loseOnTouchMaterial;

	/**
	 * to make game faster:
	 */
	protected Set<Material> winOnTouchMaterial;
	
	/**
	 * list of players which may be teleported - used by teleportPlayer and playerMayTeleport
	 */
	private Set<Player> teleportOkList;
	
	/**
	 * list of players which may change their game mode - used by onGameModeChange
	 */
	private Set<Player> gamemodeChangeOkList;
	
	/**
	 * arena cuboid
	 */
	private Cuboid arena;

	/**
	 * floor cuboid
	 */
	private Cuboid floor;

	/**
	 * lose cuboid
	 */
	private Cuboid lose;

	/**
	 * win cuboid
	 */
	private Cuboid win;
	
	/**
	 * set to false, if disallowDigBlocks has been chosen in config
	 */
	private boolean allowDigBlocks = true;
	
	/**
	 * inventory keeper for this game
	 */
	protected InventoryKeeper inventoryKeeper;
	
	/**
	 * shortcuts for digging settings
	 */
	protected static final int DIGGING_NONE = 0;
	protected static final int DIGGING_EVERYWHERE = 1;
	protected static final int DIGGING_FLOOR_ONLY = 2;
	protected static final int DIGGING_IN_ARENA = 3;
	protected static final int DIGGING_OUTSIDE_ARENA = 4;
	
	private int diggingIfArenaUndefined;
	private int diggingIfFloorUndefined;
	
	/**
	 * blocks that can be dug or (if allowDigBlocks is false) cannot
	 */
	private LinkedList<ItemStack> digBlocks;
	
	/**
	 * start timestamp
	 */
	protected long startTime = -1;

	/**
	 * finish timestamp
	 */
	protected long finishTime = -1;

	/**
	 * Constructor
	 * @param name
	 */
	public GameStandard(String name) {
		super(name);
		this.spleefers = new SpleeferList();
		this.spectators = new LinkedList<Player>();
		this.teleportOkList = new HashSet<Player>();
		this.gamemodeChangeOkList = new HashSet<Player>();
		this.trackers = new LinkedList<Tracker>();
	}

	@Override
	public String getName() {
		// get name from config
		String name = configuration.getString("name");
		if (name != null) return name;
		// otherwise return non-fancy name
		return super.getName();
	}

	@Override
	public String getType() {
		return "standard";
	}
	
	@Override
	public SpleeferList getSpleefers() {
		return spleefers;
	}

	@Override
	public synchronized void tick() {
		if (!isEnabled() || !isActive()) return; //ignore disabled and inactive arenas

		// call trackers in each tick
		try{
	        for (Iterator<Tracker> iterator = trackers.iterator(); iterator.hasNext();) {
	            Tracker tracker = iterator.next();
	            if (tracker.tick()) { // returned true - this means the tracker signalled its end - remove from list
	            	if(SimpleSpleef.DEBUG_MODE)
	            		System.out.println("[SpleefArenaDEBUG] tracker fottuto: "+tracker.getClass().getName()+" toString:"+tracker);
	                iterator.remove(); // remove element
	            }
	            
	        }
		} catch(Exception e) {
			//System.out.println("[SpleefArenaDEBUG] Concurrent modification, ignoring.. (LOL)");
		}
	    if(SimpleSpleef.DEBUG_MODE)
	    	System.out.println("[SpleefArenaDEBUG] tracker rimasti: "+StringUtils.listToString(trackers, "", ", ", ""));
		
		//System.out.println(trackers.size());

		// check game end and there are not open trackers any more
		
		if (this.status == Game.STATUS_FINISHED && trackers.size() == 0) {
			this.status = Game.STATUS_INACTIVE; // reset game to inactive status
			renewTrackers(); // renew the trackers for the next game
		}
	}

	@Override
	public void addTracker(Tracker tracker) {
		
			if (!trackers.contains(tracker)) { // only add the same tracker once
				trackers.add(tracker);
				tracker.initialize(this);
			}
		
	}

	@Override
	public void interruptTracker(Tracker tracker) {
		
		if (trackers.contains(tracker))
			tracker.interrupt();
		
	}

	@Override
	public void interruptAllTrackers() {
		for (Tracker tracker : trackers) {
			tracker.interrupt();
		}
	}

	@Override
	public void trackersUpdateBlock(Block block, BlockState oldState) {
		if (isEnabled() && isActive()) { // only call on active games
			for(Tracker tracker : trackers)
				tracker.updateBlock(block, oldState);
		}
	}
	
	@Override
	public void notifyChangedBlock(Block block, BlockState oldState, Tracker caller) {
		if (isEnabled() && isActive()) { // only call on active games
			for(Tracker tracker : trackers)
				if (tracker != caller) // caller is not updated - has to do this itself - this avoids endless callbacks
					tracker.updateBlock(block, oldState);
		}
	}

	@Override
	public void defineSettings(ConfigurationSection conf) {
		this.configuration = conf;
		
		// define defaults/shortcuts
		if (conf.getBoolean("loseOnTouchBlocks", true)) {
			// is loseBlocks a valid list?
			if (conf.isList("loseBlocks")) { //yes
				this.loseOnTouchMaterial = new HashSet<Material>();
				for (String material : conf.getStringList("loseBlocks")) {
					Material m = Material.getMaterial(material);
					// check existence of material
					if (m == null) SimpleSpleef.log.warning("[SimpleSpleef] " + getId() + " configuration warning! Unknown Material in loseBlocks: " + material);
					else this.loseOnTouchMaterial.add(m);
				}
				if (this.loseOnTouchMaterial.size() == 0) this.loseOnTouchMaterial = null; //no
			} else this.loseOnTouchMaterial = null; //no
		} else this.loseOnTouchMaterial = null; // reset
		
		if (conf.getBoolean("winOnTouchBlocks", false)) {
			// is winBlocks a valid list?
			if (conf.isList("winBlocks")) { //yes
				this.winOnTouchMaterial = new HashSet<Material>();
				for (String material : conf.getStringList("winBlocks")) {
					Material m = Material.getMaterial(material);
					// check existence of material
					if (m == null) SimpleSpleef.log.warning("[SimpleSpleef] " + getId() + " configuration warning! Unknown Material in winBlocks: " + material);
					else this.winOnTouchMaterial.add(m);
				}
				if (this.winOnTouchMaterial.size() == 0) this.winOnTouchMaterial = null; //no
			} else this.winOnTouchMaterial = null; //no
		} else this.winOnTouchMaterial = null; // reset

		// define arena, floor and lose cuboids
		arena = SimpleSpleef.getGameHandler().configToCuboid(getId(), "arena");
		floor = SimpleSpleef.getGameHandler().configToCuboid(getId(), "floor");
		lose = SimpleSpleef.getGameHandler().configToCuboid(getId(), "lose");
		win = SimpleSpleef.getGameHandler().configToCuboid(getId(), "win");
		// block destruction/keep hashes
		if (conf.isList("allowDigBlocks")) {
			allowDigBlocks = true;
			digBlocks = new LinkedList<ItemStack>();
			for (String line : conf.getStringList("allowDigBlocks")) {
				digBlocks.add(MaterialHelper.getItemStackFromString(line, true));
			}
		} else if (conf.isList("disallowDigBlocks")) {
			allowDigBlocks = false;
			digBlocks = new LinkedList<ItemStack>();
			for (String line : conf.getStringList("disallowDigBlocks")) {
				digBlocks.add(MaterialHelper.getItemStackFromString(line, true));
			}
		} else digBlocks = null; // delete previous settings

		// get dig settings
		String dig = conf.getString("diggingIfArenaUndefined", "floorOnly").toLowerCase();
		if (dig.equals("none")) diggingIfArenaUndefined = GameStandard.DIGGING_NONE;
		else if (dig.equals("everywhere")) diggingIfArenaUndefined = GameStandard.DIGGING_EVERYWHERE;
		else if (floor == null) diggingIfArenaUndefined = GameStandard.DIGGING_EVERYWHERE;
		else diggingIfArenaUndefined = GameStandard.DIGGING_FLOOR_ONLY;
		
		dig = conf.getString("diggingIfFloorUndefined", "inArena").toLowerCase();
		if (dig.equals("none")) diggingIfFloorUndefined = GameStandard.DIGGING_NONE;
		else if (dig.equals("everywhere")) diggingIfFloorUndefined = GameStandard.DIGGING_EVERYWHERE;
		else if (arena == null) diggingIfFloorUndefined = GameStandard.DIGGING_EVERYWHERE;
		else if (dig.equals("outsidearena")) diggingIfFloorUndefined = GameStandard.DIGGING_OUTSIDE_ARENA;
		else diggingIfFloorUndefined = GameStandard.DIGGING_IN_ARENA;

		// initialize inventory keeper if needed
		if (configuration.getBoolean("clearInventory", false))
			this.inventoryKeeper = new InventoryKeeper();
		else this.inventoryKeeper = null;
		
		// floor changes might be registered by the floor tracker
		renewTrackers();

		//TODO: more definitions/shortcuts
	}

	/**
	 * renew the trackers
	 */
	protected synchronized void renewTrackers() {
		// renew the tracking list if needed
		
		if (trackers.size() > 0)
			trackers = new LinkedList<Tracker>();
		
		
		// floor trackers that are only used with working floor
		if (floor != null) {
			List<Block> diggableBlocks = floor.getDiggableBlocks(this);

			if (diggableBlocks != null) {
				int arenaFloorDissolvesAfter = configuration.getInt("arenaFloorDissolvesAfter", -1);
				if (arenaFloorDissolvesAfter > -1) {
					addTracker(new FloorDissolveWorker(arenaFloorDissolvesAfter, configuration.getInt("arenaFloorDissolveTick", 5), diggableBlocks));
				}
				int arenaFloorRepairsAfter = configuration.getInt("arenaFloorRepairsAfter", -1);
				if (arenaFloorRepairsAfter > -1) {
					addTracker(new FloorRepairWorker(arenaFloorRepairsAfter, configuration.getInt("arenaFloorRepairTick", 10), diggableBlocks));
				}
				int blockDegeneration = configuration.getInt("blockDegeneration", -1);
				if (blockDegeneration > -1) {
					addTracker(new PlayerOnBlockDegenerator(blockDegeneration, configuration.getStringList("degeneratingBlocks")));
				}
			}
		}
		
		// arena timeout
		int arenaTimeout = configuration.getInt("arenaTimeout", -1);
		if (arenaTimeout > 0) {
			addTracker(new ArenaTimeout(arenaTimeout));
		}
		
		if (configuration.getBoolean("useReady", false)) {
			int readyTimeout = configuration.getInt("readyTimeout", 0);
			if (readyTimeout > 0) {
				addTracker(new ReadyTimeout(readyTimeout));
			}
		}
		
		// determine type of arena
		String type;
		if (configuration.isBoolean("restoreArenaAfterGame")) type = "soft";
		else if (configuration.isString("restoreArenaAfterGame")) type = configuration.getString("restoreArenaAfterGame");
		else type = "soft"; // fall back
		
		ArenaRestorer arenaRestorer = null;
		if (type.equalsIgnoreCase("arenahard")) { // hard arena restorer
			arenaRestorer = new HardArenaRestorer(configuration.getInt("restoreArenaAfterGameTimer", 0));
			arenaRestorer.setArena(arena);
		} else if (type.equalsIgnoreCase("floorhard") || type.equalsIgnoreCase("hard")) { // hard floor restorer
			arenaRestorer = new HardArenaRestorer(configuration.getInt("restoreArenaAfterGameTimer", 0));
			arenaRestorer.setArena(floor);
		} else if (type.equalsIgnoreCase("simple")) { // simple floor restorer
			arenaRestorer = new SimpleRestorer(configuration.getInt("restoreArenaAfterGameTimer"), configuration.getString("simpleRestorerBlock", "GRASS"));
			arenaRestorer.setArena(floor);
		} else { // soft restorer
			Cuboid possibleFloor = floor==null?arena:floor;
			if (possibleFloor == null) {
				arenaRestorer = null;
				return; // freestyle spleefing does not implement any floor
				// TODO: we could relax this, though...
			}

			// create soft restorer
			arenaRestorer = new SoftRestorer(configuration.getInt("restoreArenaAfterGameTimer", 0));
			arenaRestorer.setArena(possibleFloor);
		}
		if (arenaRestorer != null)
			addTracker(arenaRestorer);
	}

	@Override
	public boolean announce(CommandSender sender) {
		// prechecks
		if (preCheckArenaActive(sender)) return false;
		if (preCheckArenaDisabled(sender)) return false;

		// activate game
		this.status = Game.STATUS_READY;
		
		// announce new game globally?
		if (SimpleSpleef.getPlugin().getConfig().getBoolean("settings.announceGame", true))
			broadcastMessage(ChatColor.GOLD + SimpleSpleef.ll("broadcasts.announce", "[PLAYER]", sender.getName(), "[ARENA]", getName()));
		else
			sender.sendMessage(ChatColor.GOLD + SimpleSpleef.ll("feedback.announce", "[ARENA]", getName()));

		return true;
	}

	@Override
	public boolean join(Player player) {
		// prechecks
		if (preCheckArenaDisabled(player)) return false;
		// announce before join activated for new game?
		if (!isActive()) {
			if (configuration.getBoolean("announceOnJoin", true))
				announce(player); // announce game, too
			else { // tell player that he/she may not announce game
				player.sendMessage(ChatColor.DARK_RED + SimpleSpleef.ll("errors.announceBeforeJoin", "[ARENA]", getName()));
				return false;
			}
		}
		
		// game is not joinable any more?
		if (!isJoinable()) {
			player.sendMessage(ChatColor.DARK_RED + SimpleSpleef.ll("errors.join", "[ARENA]", getName()));
			return false;
		}
		
		// has player already joined this game?
		if (hasPlayer(player)) {
			player.sendMessage(ChatColor.DARK_RED + SimpleSpleef.ll("errors.joinDouble", "[ARENA]", getName()));
			return false;
		}

		// max number of players?
		int maximumPlayers = configuration.getInt("maximumPlayers", 0);
		if (spleefers == null) spleefers = new SpleeferList(); //restore spleefers list if it was null for some reason - strange that this would happen...
		if (maximumPlayers > 0 && spleefers.size() >= maximumPlayers) {
			player.sendMessage(ChatColor.DARK_RED + SimpleSpleef.ll("errors.joinMax", "[ARENA]", getName(), "[NUMBER]", String.valueOf(maximumPlayers)));
			return false;
		}

		// player has join with empty inventory?
		if (configuration.getBoolean("emptyInventoryToJoin", false)) {
			if (!InventoryKeeper.inventoryIsEmpty(player)) {
				player.sendMessage(ChatColor.DARK_RED + SimpleSpleef.ll("errors.joinEmptyInventory"));
				return false;
			}
		}

		// check funds of player...
		if (SimpleSpleef.economy != null) {
			double entryFee = configuration.getDouble("entryFee", 0.0);
			if (entryFee > 0.0) {
				EconomyResponse response = SimpleSpleef.economy.withdrawPlayer(player.getName(), entryFee);
				if (response.type == EconomyResponse.ResponseType.SUCCESS) { // ok, tell the player about the amount charged
					player.sendMessage(SimpleSpleef.ll("feedback.joinFee", "[AMOUNT]", SimpleSpleef.economy.format(entryFee)));
				} else { //insufficient funds
					player.sendMessage(ChatColor.DARK_RED + SimpleSpleef.ll("errors.joinFee", "[AMOUNT]", SimpleSpleef.economy.format(entryFee)));
					return false;
				}
				// ok, is a part of the fee paid to a player?
				String playerNameGettingFees = configuration.getString("entryFeeGoesToPlayer", "none");
				if (playerNameGettingFees != null && !playerNameGettingFees.equalsIgnoreCase("none") &&  SimpleSpleef.economy.hasAccount(playerNameGettingFees))
					SimpleSpleef.economy.depositPlayer(playerNameGettingFees, configuration.getDouble("entryFeeAmountToPlayer", entryFee));
			}
		}
		
		// check gamemode and change it if needed
		boolean upgradeModeToCreative = false; // needed for OriginalPositionKeeper
		if (player.getGameMode() != GameMode.SURVIVAL) {
			player.setGameMode(GameMode.SURVIVAL);
			player.sendMessage(ChatColor.YELLOW + SimpleSpleef.ll("feedback.gamemodeChanged"));
			upgradeModeToCreative = true;
		}

		// actually add player
		if (!spleefers.addSpleefer(player)) { // some weird error
			player.sendMessage(ChatColor.DARK_RED + "Internal error while joining occured! Please tell the SimpleSpleef creator!");
			return false;
		}
		
		// unready game, if needed - this is needed because the game can have the status "ready", if all players are ready, but it has not started
		if (supportsReady()) {
			this.status = Game.STATUS_NEW;
		}

		// remember player's last position
		if (configuration.getBoolean("enableBackCommand", true))
			SimpleSpleef.getOriginalPositionKeeper().keepPosition(player, upgradeModeToCreative);
		// teleport player to lounge
		teleportPlayer(player, "lounge");

		// now we announce the joining of the player...
		String broadcastMessage = ChatColor.GREEN + SimpleSpleef.ll("broadcasts.join", "[PLAYER]", player.getName(), "[ARENA]", getName());
		if (SimpleSpleef.getPlugin().getConfig().getBoolean("settings.announceJoin", true)) { // broadcast
			broadcastMessage(broadcastMessage);
		} else { // player only
			player.sendMessage(ChatColor.GREEN + SimpleSpleef.ll("feedback.join", "[ARENA]", getName()));
			sendMessage(broadcastMessage, player); // notify players and spectators
		}
		return true;
	}

	@Override
	public boolean team(Player player, String team) {
		// no team games possible in this arena
		player.sendMessage(ChatColor.DARK_RED + SimpleSpleef.ll("errors.teamNotPossible", "[ARENA]", getName()));
		return false;
	}

	@Override
	public boolean ready(Player player, boolean hitBlock) {
		// readying is not used in this game
		if (!supportsReady()) {
			player.sendMessage(ChatColor.DARK_RED + SimpleSpleef.ll("errors.readyNotUsed", "[ARENA]", getName()));
			return false;
		}

		// game started already?
		if (!isJoinable()) { // avoid possible memory leak
			player.sendMessage(ChatColor.DARK_RED + SimpleSpleef.ll("errors.readyAlreadyStarted", "[ARENA]", getName()));
			return false;
		}

		// right command?
		if (!hitBlock && !supportsCommandReady()) {
			player.sendMessage(ChatColor.DARK_RED + SimpleSpleef.ll("errors.readyBlock", "[ARENA]", getName()));
			return false;
		}

		// get spleefer
		Spleefer spleefer = spleefers.getSpleefer(player);
		if (spleefer == null) { // internal error
			player.sendMessage(ChatColor.DARK_RED + "Internal error: Player " + player.getName() + " should be in spleefers list, but isn't!");
			return false;
		}

		// player already ready?
		if (spleefer.isReady()) {
			player.sendMessage(ChatColor.DARK_RED + SimpleSpleef.ll("errors.readyAlready", "[ARENA]", getName()));
			return false;
		}
		
		// ok, ready player now
		spleefer.setReady(true);
		
		// send messages
		player.sendMessage(ChatColor.GREEN + SimpleSpleef.ll("feedback.ready", "[ARENA]", getName(), "[PLAYER]", player.getDisplayName()));
		// broadcast message of somebody readying
		String broadcastMessage = ChatColor.DARK_PURPLE + SimpleSpleef.ll("broadcasts.ready", "[PLAYER]", player.getDisplayName(), "[ARENA]", getName());
		if (SimpleSpleef.getPlugin().getConfig().getBoolean("settings.announceReady", false)) {
			broadcastMessage(broadcastMessage); // broadcast message
		} else {
			// send message to all receivers
			sendMessage(broadcastMessage, player);
		}

		// is the game ready?
		checkReadyAndStartGame();
		return true;
	}

	@Override
	public boolean countdown(CommandSender sender) {
		// game started already?
		if (!isJoinable()) { // avoid possible memory leak
			sender.sendMessage(ChatColor.DARK_RED + SimpleSpleef.ll("errors.startDouble", "[ARENA]", getName()));
			return false;
		}
		
		// minimum number of players?
		int minimumPlayers = configuration.getInt("minimumPlayers", 0);
		if (minimumPlayers > 0 && spleefers.size() < minimumPlayers) {
			sender.sendMessage(ChatColor.DARK_RED + SimpleSpleef.ll("errors.startMin", "[ARENA]", getName(), "[NUMBER]", String.valueOf(minimumPlayers)));
			return false;
		}
		
		// game is not ready yet?
		if (!isReady()) {
			//get list of unready spleefers
			String unreadyList = getListOfUnreadySpleefers();
			if (unreadyList == null) unreadyList = "---"; // in any case...
			// send error message to player
			sender.sendMessage(ChatColor.DARK_RED + SimpleSpleef.ll("errors.startNotReady", "[ARENA]", getName(), "[PLAYERS]", unreadyList));
			return false;
		}

		// start game or countdown
		startGameOrCountdown();
		
		return true;
	}

	@Override
	public void checkReadyAndStartGame() {
		if (spleefers.countUnreadyPlayers() == 0) {
			// autostart game once all are ready
			if (configuration.getBoolean("readyAutoStart", false)) {
				// test for minumum players
				int minimumPlayers = configuration.getInt("minimumPlayers", 0);
				if (minimumPlayers > 0 && spleefers.size() < minimumPlayers) {
					// wait for more players to join and ready
					sendMessage(SimpleSpleef.ll("broadcasts.ready", "[NUMBER]", String.valueOf(spleefers.size() - minimumPlayers)), false);
					this.status = Game.STATUS_READY;
				}
				else startGameOrCountdown(); // start game right away
			}
			// otherwise, just set game as ready
			else this.status = Game.STATUS_READY;
		}
	}

	/**
	 * helper to start game or countdown - used by ready and countdown methods
	 */
	private void startGameOrCountdown() {
		// teleport players to arena
		teleportPlayersAtGameStart();
		// start countdown, if setting is 0 or higher
		int countdown = configuration.getInt("countdownFrom", 10);
		if (countdown == 0) {
			start(); // if countdown is null, start game right away
		} else {
			// initiate countdown - tick() will do the rest
			this.status = Game.STATUS_COUNTDOWN;
			addTracker(new Countdown(countdown)); // add countdown tracker
		}
	}
	
	/**
	 * teleport players to arena
	 */
	protected void teleportPlayersAtGameStart() {
		for (Spleefer spleefer : spleefers.get()) {
			teleportPlayer(spleefer.getPlayer(), "game");
		}
	}

	@Override
	public boolean start() {
		// change game status
		status = STATUS_STARTED;
		startTime = new Date().getTime();

		// notify statistics
		if (SimpleSpleef.getStatisticsModule() != null)
			SimpleSpleef.getStatisticsModule().gameStarted(this);

		// possibly clear inventory
		clearInventories();

		// optionally add to inventory
		addToInventories();

		// and/or give shovels
		addShovelItems();

		return true;
	}

	@Override
	public boolean leave(Player player) {
		// is the player a spectator?
		if (hasSpectator(player)) {
			return back(player); // redirect to back command instead
		}
		
		// check, if player is not a spleefer
		if (!spleefers.hasSpleefer(player)) {
			player.sendMessage(ChatColor.DARK_RED + "Internal error while leave occured! Please tell the SimpleSpleef creator!");
			return false;
		}
		
		// inform player
		player.sendMessage(ChatColor.GREEN + SimpleSpleef.ll("feedback.leave"));
		// broadcast message of somebody loosing
		String broadcastMessage = ChatColor.DARK_PURPLE + SimpleSpleef.ll("broadcasts.leave", "[PLAYER]", player.getDisplayName(), "[ARENA]", getName());
		if (SimpleSpleef.getPlugin().getConfig().getBoolean("settings.announceJoin", true)) {
			broadcastMessage(broadcastMessage); // broadcast message
		} else {
			// send message to all receivers
			sendMessage(broadcastMessage, player);
		}
		
		// teleport him/her back to original position, if supported
		if (configuration.getBoolean("enableBackCommand", true)) {
			// get original position
			boolean wasInCreativeBefore = SimpleSpleef.getOriginalPositionKeeper().wasInCreativeBefore(player);
			Location originalLocation = SimpleSpleef.getOriginalPositionKeeper().getOriginalPosition(player);
			if (originalLocation == null) { // no position
				player.sendMessage(ChatColor.DARK_RED + SimpleSpleef.ll("errors.backNoLocation"));
				return false;
			}
			// add player to teleport ok list
			this.teleportOkList.add(player);
			// teleport player to original position
			SimpleSpleef.simpleSpleefTeleport(player, originalLocation);
			player.sendMessage(ChatColor.GREEN + SimpleSpleef.ll("feedback.back"));
			// change game mode back, if needed
			if (wasInCreativeBefore)
				changeGameModeToCreative(player);
		}
		
		// check game status
		if (isJoinable() || isReady() || isFinished()) { //still joinable or ready or finished state - not so bad!
			// just remove spleefer
			spleefers.removeSpleefer(player);
			refundPlayer(player);
		} else if (status == Game.STATUS_COUNTDOWN) { // during countdown - end the game...
			// set player to lost, so that the player is not teleported twice - see endGame() for more info
			spleefers.setLost(player);
			endGame(); // actually end the game
		} else { // game is in progress - player loses - simple as that
			// player loses
			playerLoses(player, false); // do not teleport leaving players...
		}
		return true;
	}

	@Override
	public boolean stop(Player player) {
		// game is not in progress!
		if (!isInProgress()) {
			player.sendMessage(ChatColor.DARK_RED + SimpleSpleef.ll("errors.stop", "[ARENA]", getName()));
			return false;
		}

		// actually end the game
		if (!endGame()) return false;

		// send message
		sendMessage(SimpleSpleef.ll("feedback.stop", "[ARENA]", getName(), "[PLAYER]", player.getDisplayName()),
				SimpleSpleef.getPlugin().getConfig().getBoolean("settings.announceStop", true));
		return true;
	}

	@Override
	public boolean delete(CommandSender sender) {
		// refund players, if possible
		if (configuration.getDouble("refundMoney", 0.0) > 0) {
			for (Spleefer spleefer : spleefers.get())
				refundPlayer(spleefer.getPlayer());
		}
		
		// end the game first, if game is started
		if (!endGame()) return false;
		// send message
		sendMessage(SimpleSpleef.ll("feedback.delete", "[ARENA]", getName(), "[PLAYER]", sender.getName()),
				SimpleSpleef.getPlugin().getConfig().getBoolean("settings.announceStop", true));
		return true;
	}

	@Override
	public boolean watch(Player player) {
		if (!isEnabled() || !isActive()) { // game is not active
			player.sendMessage(ChatColor.DARK_RED + SimpleSpleef.ll("errors.watchNoGame", "[ARENA]", getName()));
			return false;
		}
		
		// check, if player is spleefer
		if (spleefers.hasSpleefer(player)) {
			player.sendMessage(ChatColor.DARK_RED + SimpleSpleef.ll("errors.watchSpleefer", "[ARENA]", getName()));
			return false;
		}

		// check, if player is in spectator list already
		if (spectators.contains(player)) {
			player.sendMessage(ChatColor.DARK_RED + SimpleSpleef.ll("errors.watchAlready", "[ARENA]", getName()));
			return false;
		}

		// check, if we have a spectator spawn defined
		if (!configuration.isConfigurationSection("spectatorSpawn") || !configuration.getBoolean("spectatorSpawn.enabled", false)) {
			player.sendMessage(ChatColor.DARK_RED + SimpleSpleef.ll("errors.watchNoSpawnDefined", "[ARENA]", getName()));
			return false;
		}

		// player has to watch with empty inventory?
		if (configuration.getBoolean("emptyInventoryToSpectate", false)) {
			if (!InventoryKeeper.inventoryIsEmpty(player)) {
				player.sendMessage(ChatColor.DARK_RED + SimpleSpleef.ll("errors.watchEmptyInventory"));
				return false;
			}
		}

		// save spectator's original position
		if (configuration.getBoolean("enableBackCommand", true))
			SimpleSpleef.getOriginalPositionKeeper().keepPosition(player, false); // never change game mode here

		// teleport spectator
		Location teleportTo = LocationHelper.configToExactLocation(configuration.getConfigurationSection("spectatorSpawn"));
		SimpleSpleef.simpleSpleefTeleport(player, teleportTo);

		// add to spectator list
		spectators.add(player);

		// send message to player
		player.sendMessage(ChatColor.GREEN + SimpleSpleef.ll("feedback.watch", "[ARENA]", getName()));
		return true;
	}

	@Override
	public boolean back(Player player) {
		// not allowed
		if (!configuration.getBoolean("enableBackCommand", true)) {
			player.sendMessage(ChatColor.DARK_RED + SimpleSpleef.ll("errors.commandNotAllowed", "[ARENA]", getName()));
			return false;
		}

		// check, if player is spleefer
		if (spleefers.hasSpleefer(player)) {
			player.sendMessage(ChatColor.DARK_RED + SimpleSpleef.ll("errors.backSpleefer", "[ARENA]", getName()));
			return false;
		}

		// check, if player is not a spectator
		if (spectators == null || !spectators.contains(player)) {
			player.sendMessage(ChatColor.DARK_RED + "Internal error while back occured! Please tell the SimpleSpleef creator!");
			return false;
		}

		// get original position
		Location originalLocation = SimpleSpleef.getOriginalPositionKeeper().getOriginalPosition(player);
		if (originalLocation == null) { // no position
			player.sendMessage(ChatColor.DARK_RED + SimpleSpleef.ll("errors.backNoLocation"));
			return false;
		}

		// remove player from watch list
		removeSpectator(player);
		// teleport player to original position
		SimpleSpleef.simpleSpleefTeleport(player, originalLocation);

		player.sendMessage(ChatColor.GREEN + SimpleSpleef.ll("feedback.back"));
		return true;
	}

	@Override
	public boolean onPlayerQuit(PlayerQuitEvent event) {
		if (!isEnabled() || !hasPlayer(event.getPlayer())) return false; // ignore disabled arenas and if player not here
		
		// in game?
		if (hasPlayer(event.getPlayer())) {
			// call helper method
			loseOnQuitOrKick(event.getPlayer());
			return true;
		}
		return false;
	}

	@Override
	public boolean onPlayerKick(PlayerKickEvent event) {
		if (!isEnabled()) return false; // ignore disabled arenas
		
		// in game?
		if (hasPlayer(event.getPlayer())) {
			// call helper method
			loseOnQuitOrKick(event.getPlayer());
			
			// delete original position, because player is banned anyhow
			SimpleSpleef.getOriginalPositionKeeper().deleteOriginalPosition(event.getPlayer());
			return true;
		}
		// also delete position if spectator
		if (hasSpectator(event.getPlayer())) {
			// delete original position, because player is banned anyhow
			SimpleSpleef.getOriginalPositionKeeper().deleteOriginalPosition(event.getPlayer());
			return true;
		}

		return false;
	}
	
	/**
	 * helper method for onPlayerQuit and onPlayerKick
	 * @param player
	 */
	protected void loseOnQuitOrKick(Player player) {
		if (configuration.getBoolean("loseOnLogout", true)) {
			// broadcast message of somebody loosing
			String broadcastMessage = ChatColor.GREEN + SimpleSpleef.ll("broadcasts.lostByQuitting", "[PLAYER]", player.getName(), "[ARENA]", getName());
			
			if (SimpleSpleef.getPlugin().getConfig().getBoolean("settings.announceLose", true)) {
				broadcastMessage(broadcastMessage); // broadcast message
			} else {
				// send message to all receivers
				sendMessage(broadcastMessage, player);
			}

			// update player's game mode
			if (SimpleSpleef.getOriginalPositionKeeper().wasInCreativeBefore(player))
				changeGameModeToCreative(player);

			// player loses, if set to true
			playerLoses(player, true); // do not teleport dead players...
		}
	}

	@Override
	public boolean onPlayerJoin(PlayerJoinEvent event) {
		if (!isEnabled()) return false; // ignore disabled arenas
		return true;
	}
	

	@Override
	public boolean onPlayerDeath(Player player) {
		if (!isEnabled()) return false; // ignore disabled arenas
		
		if (hasPlayer(player)) {
			// delete original position, because player spawns somewhere else anyhow
			boolean creativeMode = SimpleSpleef.getOriginalPositionKeeper().wasInCreativeBefore(player);
			SimpleSpleef.getOriginalPositionKeeper().deleteOriginalPosition(player);
			
			// add respawn location, if set
			Location teleportTo = LocationHelper.configToExactLocation(configuration.getConfigurationSection("loseSpawn"));
			if (teleportTo != null) {
				SimpleSpleef.getGameHandler().addPlayerRespawnLocation(player, teleportTo, creativeMode);
			}
	
			if (configuration.getBoolean("loseOnDeath", true)) {
				// broadcast message of somebody loosing
				String broadcastMessage = ChatColor.GREEN + SimpleSpleef.ll("broadcasts.lostByDeath", "[PLAYER]", player.getName(), "[ARENA]", getName());
				if (SimpleSpleef.getPlugin().getConfig().getBoolean("settings.announceLose", true)) {
					broadcastMessage(broadcastMessage); // broadcast message
				} else {
					// send message to all receivers
					sendMessage(broadcastMessage, player);
				}

				// change game mode
				// update player's game mode
				if (SimpleSpleef.getOriginalPositionKeeper().wasInCreativeBefore(player))
					changeGameModeToCreative(player);

				// player loses, if set to true
				playerLoses(player, false); // do not teleport dead players...
			} // else - do nothing...
			return true;
		}
		return false;
	}

	@Override
	public boolean onPlayerMove(PlayerMoveEvent event) {
		if (!isEnabled()) return false; // ignore disabled arenas
		Player player = event.getPlayer();
		if (!hasPlayer(player) || !isInGame() || spleefers.hasLost(player)) return false; // if not in game or game is not in progress or player has lost, return
		
		//player touched certain block (setting loseOnTouchBlocks)
		if (loseOnTouchMaterial != null) {
			Material touchedBlock = event.getTo().getBlock().getType();
			Material onBlock = player.getLocation().getBlock().getRelative(BlockFace.DOWN).getType();
			// what happened exactly?
			boolean lostByTouching = loseOnTouchMaterial.contains(touchedBlock);
			boolean lostByStandingOn = loseOnTouchMaterial.contains(onBlock);
			if (lostByTouching || lostByStandingOn) {
				// Block name
				String blockName;
				if (lostByTouching) blockName = touchedBlock.name();
				else blockName = onBlock.name();
				// translate block name
				String translatedBlockName = SimpleSpleef.ll("material." + blockName);
				if (translatedBlockName != null) blockName = translatedBlockName;
				// broadcast message of somebody loosing
				String broadcastMessage = ChatColor.GREEN + SimpleSpleef.ll("broadcasts.lostByTouching", "[PLAYER]", player.getName(), "[ARENA]", getName(), "[MATERIAL]", blockName);
				if (SimpleSpleef.getPlugin().getConfig().getBoolean("settings.announceLose", true)) {
					broadcastMessage(broadcastMessage); // broadcast message
				} else {
					// send message to all receivers
					sendMessage(broadcastMessage, player);
				}
				// Ha, lost!
				playerLoses(player, true);
				return true;
			}
		}
		
		//player touched certain block (setting winOnTouchBlocks)
		if (winOnTouchMaterial != null) {
			Material touchedBlock = event.getTo().getBlock().getType();
			Material onBlock = player.getLocation().getBlock().getRelative(BlockFace.DOWN).getType();
			// what happened exactly?
			boolean wonByTouching = winOnTouchMaterial.contains(touchedBlock);
			boolean wonByStandingOn = winOnTouchMaterial.contains(onBlock);
			if (wonByTouching || wonByStandingOn) {
				// broadcast message of somebody loosing
				String broadcastMessage = ChatColor.GOLD + SimpleSpleef.ll("broadcasts.winByTouching", "[PLAYER]", player.getName(), "[ARENA]", getName());
				if (SimpleSpleef.getPlugin().getConfig().getBoolean("settings.announceWin", true)) {
					broadcastMessage(broadcastMessage); // broadcast message
				} else {
					// send message to all receivers
					sendMessage(broadcastMessage, player);
				}
				//player wins
				winByTouching(player);
				return true;
			}
		}
		
		// check location within "lose" cuboid (setting lose)
		if (lose != null && lose.contains(player.getLocation())) {
			// broadcast message of somebody loosing
			String broadcastMessage = ChatColor.GREEN + SimpleSpleef.ll("broadcasts.lostByCuboid", "[PLAYER]", player.getName(), "[ARENA]", getName());
			if (SimpleSpleef.getPlugin().getConfig().getBoolean("settings.announceLose", true)) {
				broadcastMessage(broadcastMessage); // broadcast message
			} else {
				// send message to all receivers
				sendMessage(broadcastMessage, player);
			}
			// Ha, lost!
			playerLoses(player, true);
			//return true; //redundant right now		
		}
		
		// check location within "win" cuboid (setting win)
		if (win != null && win.contains(player.getLocation())) {
			// broadcast message of somebody loosing
			String broadcastMessage = ChatColor.GOLD + SimpleSpleef.ll("broadcasts.winByTouching", "[PLAYER]", player.getName(), "[ARENA]", getName());
			if (SimpleSpleef.getPlugin().getConfig().getBoolean("settings.announceWin", true)) {
				broadcastMessage(broadcastMessage); // broadcast message
			} else {
				// send message to all receivers
				sendMessage(broadcastMessage, player);
			}
			// Player wins
			winByTouching(player);
			//return true; //redundant right now		
		}
		return true;
	}

	@Override
	public boolean onPlayerInteract(PlayerInteractEvent event) {
		if (!isEnabled()) return false; // ignore disabled arenas
		Block block = event.getClickedBlock();
		Player player = event.getPlayer();
		if (block == null || player == null || !hasPlayer(player) || spleefers.getSpleefer(player).hasLost()) return false; // ignore null blocks and null players and players not in game
		
		// check instant dig and block may be broken
		if (event.getAction() == Action.LEFT_CLICK_BLOCK && configuration.getBoolean("instantDig", true) && checkMayBreakBlock(block, player)) {
			// cancel event
			event.setCancelled(true);
			// get block data
			Block clickedBlock = event.getClickedBlock();
			BlockState state = clickedBlock.getState();
			// set block to air
			clickedBlock.setType(Material.AIR);
			// update chunk information
			clickedBlock.getWorld().refreshChunk(clickedBlock.getChunk().getX(), clickedBlock.getChunk().getZ());
			// notify trackers
			trackersUpdateBlock(clickedBlock, state);
		} else
		//check if player clicked on a "ready" block (e.g. iron block) and the game is readyable
			if (supportsBlockReady() && isJoinable()) {
				ItemStack readyBlockMaterial;
				try {
					readyBlockMaterial = MaterialHelper.getItemStackFromString(configuration.getString("readyBlockMaterial", null), true);
				} catch (Exception e) {
					return true; // ignore exceptions
				}
				if (readyBlockMaterial == null) return true; // ignore null materials
				// material has been checked, now test, if clicked block is of the same material
				if (readyBlockMaterial.getTypeId() == block.getTypeId() && MaterialHelper.isSameBlockType(block, readyBlockMaterial)) {
					ready(player, true);
				}
			}
		return true;
	}

	@Override
	public boolean onBlockBreak(BlockBreakEvent event) {
		if (!isEnabled()) return false; // ignore disabled arenas
		Player player = event.getPlayer();
		if (player == null) return false; // no NPEs
		
		// is player a spleefer?
		if (!isInProgress() || !hasPlayer(player) || spleefers.getSpleefer(player).hasLost()) {
			if (inProtectedArenaCube(event.getBlock())) { //check if block is in cube
				// cancel event
				event.setCancelled(true);
				player.sendMessage(ChatColor.DARK_RED + SimpleSpleef.ll("errors.noDig"));
				return true;
			}
		} else { // not in spleefer list, check arena area nevertheless
			blockBreakInGame(event);
			return true;			
		}
		return false;
	}

	/**
	 * execute a block break within the game
	 * @param event
	 */
	protected void blockBreakInGame(BlockBreakEvent event) {
		Block block = event.getBlock();
		if (block == null) return; // sanity check
		// get block data
		BlockState state = block.getState();
		boolean floorBroken = false;
		// may the block be broken?
		if (!checkMayBreakBlock(block, event.getPlayer())) {
			// cancel event
			event.setCancelled(true);
			// message to player
			event.getPlayer().sendMessage(ChatColor.DARK_RED + SimpleSpleef.ll("errors.noDig"));
		} else if (!configuration.getBoolean("blockDropping", true)) { // otherwise: block dropping set to false => destroy blocks
			// cancel event - because we will handle the block destruction ourselves
			event.setCancelled(true);
			floorBroken = true;
			// set block to air
			block.setType(Material.AIR);
			// update chunk information
			block.getWorld().refreshChunk(block.getChunk().getX(), block.getChunk().getZ());
		} else floorBroken = true; // block was broken
		
		if (floorBroken) // update trackers
			trackersUpdateBlock(block, state);
	}
	
	/**
	 * Check if a block is in protected arena
	 * @param block
	 * @return
	 */
	protected boolean inProtectedArenaCube(Block block) {
		if (arena == null) return false;
		
		// check block position
		Location blockLoc = block.getLocation();
		return arena.contains(blockLoc);
	}

	@Override
	public boolean onBlockPlace(BlockPlaceEvent event) {
		if (!isEnabled()) return false; // ignore disabled arenas
		Player player = event.getPlayer();
		if (player == null) return true; // no NPEs
		
		// is player a spleefer?
		if (isInProgress() && hasPlayer(player) && !spleefers.getSpleefer(player).hasLost()) {
			blockPlaceInGame(event);
			return true;
		} else { // not in spleefer list, check arena area nevertheless
			if (inProtectedArenaCube(event.getBlock())) { //check if block is in cube
				// cancel event
				event.setCancelled(true);
				player.sendMessage(ChatColor.DARK_RED + SimpleSpleef.ll("errors.noPlacement"));
				return true;
			}
		}
		return false;
	}
	
	/**
	 * execute a block place within the game
	 * @param event
	 */
	protected void blockPlaceInGame(BlockPlaceEvent event) {
		// joined players may not place blocks as long as game has not started
		// also, if allowBlockPlacing is false, disallow block placing during game
		if (!isInGame() || !configuration.getBoolean("allowBlockPlacing", false)) {
			// cancel event
			event.setCancelled(true);
			event.getPlayer().sendMessage(ChatColor.DARK_RED + SimpleSpleef.ll("errors.noPlacement"));
		}

		// if there is a floor tracker running, tell it about the change
		if (!event.isCancelled())
			trackersUpdateBlock(event.getBlock(), event.getBlockReplacedState());
	}

	@Override
	public boolean onFoodLevelChange(FoodLevelChangeEvent event) {
		if (!isEnabled()) return false; // ignore disabled arenas
		//if (event.getEntity() == null || !(event.getEntity() instanceof Player)) return;
		Player player = (Player) event.getEntity();

		if (hasPlayer(player) && configuration.getBoolean("noHunger", true)) {// if setting noHunger has been set for this arena, do not feel any hunger
			event.setCancelled(true);
			return true;
		}
		return false;
	}

	@Override
	public boolean onEntityDamage(EntityDamageEvent event) {
		//if (event.getEntity() == null || !(event.getEntity() instanceof Player)) return;
		Player player = (Player) event.getEntity();
		
		// player in game and in PVP world? Consider settings and only damage by other entities
		if (hasPlayer(player) && player.getWorld().getPVP() && configuration.getBoolean("noPvP", true) && event instanceof EntityDamageByEntityEvent) {
			EntityDamageByEntityEvent damageEvent = (EntityDamageByEntityEvent) event;
			// only consider player damage
			if (damageEvent.getDamager() instanceof Player) {
				event.setCancelled(true); // cancel damage event by caused by other players
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean onEntityExplode(EntityExplodeEvent event) {
		if (!isEnabled() || !configuration.getBoolean("protectArena", true)
				|| (arena == null && floor == null)) return false; // ignore disabled arenas, as well as unprotected ones or ones without blocks to protect

		// check if any exploded blocks were within the arena perimeter
		if (isInGame() && configuration.getBoolean("enableExplosionsDuringGame", false)) {
			// check for blocks exploded within the reach of a tracker...
			for (Block block : event.blockList()) {
				Cuboid checkCuboid = arena==null?floor:arena;
				if (checkCuboid.contains(block.getLocation())) { 
					trackersUpdateBlock(block, block.getState());
				}
			}
			return true;
		} else { // not in game, cancel explosions close to the arena
			for (Block block : event.blockList()) {
				Cuboid checkCuboid = arena==null?floor:arena;
				if (checkCuboid.contains(block.getLocation())) {
					event.setCancelled(true);
					return true;
				}
			}
		}

		return false;
	}

	@Override
	public boolean onPlayerTeleport(PlayerTeleportEvent event) {
		if (!isEnabled()) return false; // ignore disabled arenas
		if (hasPlayer(event.getPlayer())) {
			// check, if arena allows the player's teleportation
			if (!playerMayTeleport(event.getPlayer())) {
				event.getPlayer().sendMessage(ChatColor.DARK_RED + SimpleSpleef.ll("errors.teleport", "[ARENA]", getName()));
				event.setCancelled(true); //cancel event
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean onPlayerGameModeChange(PlayerGameModeChangeEvent event) {
		if (!isEnabled()) return false; // ignore disabled arenas
		Player player = event.getPlayer();
		
		// players and spectators may not change game mode
		if (hasPlayer(player) || hasSpectator(player)) {
			// ... if they are not in the game mode change ok list
			if (gamemodeChangeOkList.contains(player)) {
				gamemodeChangeOkList.remove(player);
			} else {
				event.getPlayer().sendMessage(ChatColor.DARK_RED + SimpleSpleef.ll("errors.gamemodeChange"));
				event.setCancelled(true); //cancel event
				return true;
			}
		}
		return false;
	}
	
	@Override
	public boolean onCreatureSpawn(CreatureSpawnEvent event) {
		if (!isEnabled() || arena == null || !configuration.getBoolean("disallowCreatureSpawns", true)) return false; // ignore disabled and undefined arenas, as well as those that allow spawning
		
		// check location of spawn event
		Location loc = event.getLocation();
		if (arena.contains(loc)) { // if within arena...
			event.setCancelled(true); //..cancel spawn event
		}
		
		return false;
	}
	
	@Override
	public boolean onEntityChangeBlock(EntityChangeBlockEvent event) {
		if (!isEnabled() || (arena == null && floor == null) || !configuration.getBoolean("protectArena", true)) return false; // ignore disabled and undifined arenas, as well as those that are not protected
		if (!(event.getEntity() instanceof Enderman)) return false; // only consider Endermen for now

		// check location of enderman event
		Location loc = event.getBlock().getLocation();
		if ((arena != null && arena.contains(loc)) || (floor != null && floor.contains(loc))) { // if within arena or floor...
			event.setCancelled(true); //..cancel spawn event
		}
		
		return false;
	}

	/**
	 * Do end game maintenance
	 * @return
	 */
	public boolean endGame() {
		//check what the current status is and end game then
		boolean wasInProgress = isInProgress(); 
		//isJoinable()
		//isReady()
		// + other cases?
		int oldStatus = status;
		
		// change game status
		status = STATUS_FINISHED;
		finishTime = new Date().getTime();

		// notify statistics
		if (SimpleSpleef.getStatisticsModule() != null)
			SimpleSpleef.getStatisticsModule().gameFinished(this);

		// only do this when game was in progress
		if (wasInProgress) {
			// possibly take away shovel items
			removeShovelItems();
			// possibly restore inventories
			restoreAllInventories();
		}
		
		// do we need to teleport players?
		if (oldStatus == Game.STATUS_COUNTDOWN || oldStatus == Game.STATUS_INTERRUPTED) { // game ended during countdown - teleport players back to lounge
			for (Spleefer spleefer : spleefers.getLost()) {
				teleportPlayer(spleefer.getPlayer(), "lounge");
			}
		}
		
		// remove players from game
		spleefers = new SpleeferList();
		
		return true;
	}

	/**
	 * check whether a certain block may be broken
	 * => player has been checked before this, so this does only concern block breaks
	 * and interactions by spleefers
	 * @param block broken/interacted (on instant-break) by spleefer
	 * @param player - player or null for automatic changes (e.g. trackers)
	 * @return true, if block may be destroyed
	 */
	@Override
	public boolean checkMayBreakBlock(Block block, Player player) {
		// sanity check
		if (block == null) return true;
		// joined players may not break blocks as long as game has not started
		if (player != null && !isInGame())
			return false;
		// joined players may not break blocks as long as game has not started
		return checkMayBreakBlockMaterial(block) && checkMayBreakBlockLocation(block);
	}
	
	/**
	 * helper function for the above checkMayBreakBlock method to check the material of a block
	 * @param block
	 * @return
	 */
	protected boolean checkMayBreakBlockMaterial(Block block) {
		// allowed blocks? => allowDigBlocks
		// alternatively: disallowDigBlocks
		if (digBlocks != null) {
			if (MaterialHelper.isSameBlockType(block, digBlocks)) return allowDigBlocks; // found -> return state
			else // not found
				return !allowDigBlocks; // negate
		}
		return true;
	}
	
	/**
	 * helper function for the above checkMayBreakBlock method to check the location of a block
	 * @param block
	 * @return
	 */
	protected boolean checkMayBreakBlockLocation(Block block) {
		Location blockLocation = block.getLocation();
		// is arena undefined?
		if (arena == null) {
			switch (diggingIfArenaUndefined) {
			case GameStandard.DIGGING_NONE: return false; // digging is not allowed
			case GameStandard.DIGGING_EVERYWHERE: return true; // digging is allowed everywhere
			default: // allowed in the arena floor in all other cases
				return this.floor.contains(blockLocation);
			}
		} else { // arena defined
			// is floor undefined?
			if (floor == null) {
				switch (diggingIfFloorUndefined) {
				case GameStandard.DIGGING_NONE: return false; // digging is not allowed
				case GameStandard.DIGGING_EVERYWHERE: return true; // digging is allowed everywhere
				case GameStandard.DIGGING_OUTSIDE_ARENA: return !this.arena.contains(blockLocation); // digging is allowed outside of arena
				default: // allowed within the arena in other cases
					return this.arena.contains(blockLocation);
				}
			} else { // floor and arena defined
				return this.floor.contains(blockLocation); // only arena floor can be broken during game
			}
		}
	}

	@Override
	public boolean hasPlayer(Player player) {
		if (spleefers == null || player == null) return false;
		return spleefers.hasSpleefer(player);
	}
	
	@Override
	public boolean hasSpectator(Player player) {
		if (spectators == null) return false;
		return spectators.contains(player);
	}
	
	@Override
	public boolean removeSpectator(Player player) {
		return spectators.remove(player);
	}

	@Override
	public String getNumberOfPlayers() {
		// no spleefers - return empty string
		if (spleefers == null || spleefers.size() == 0) return "";

		int active;
		int max;
		// shows information depending on state
		if (isInProgress()) {
			active = spleefers.inGame();
			max = spleefers.size();
		} else { // joinable -> show number of players joined, open places
			active = spleefers.size();
			max = configuration.getInt("maximumPlayers", 0);
		}
		return "(" + active + "/" + (max>0?max:"-") + ")";
	}

	@Override
	public String getListOfSpleefers() {
		// no spleefers - return null
		if (spleefers == null || spleefers.size() == 0) return null;
		// create list of spleefers
		String comma = SimpleSpleef.ll("feedback.infoComma");
		StringBuilder builder = new StringBuilder();
		int i = 0;
		for (Spleefer spleefer : spleefers.get()) {
			if (i > 0 && i == spleefers.size() - 1) builder.append(SimpleSpleef.ll("feedback.infoAnd")); // last element with end
			else if (i > 0) builder.append(comma);  // other elements with ,
			// lost or in game?
			if (spleefer.hasLost()) builder.append(ChatColor.RED);
			else builder.append(ChatColor.GREEN);
			builder.append(spleefer.getPlayer().getDisplayName());
			builder.append(ChatColor.GRAY);
			i++;
		}
		return builder.toString();
	}

	@Override
	public String getListOfUnreadySpleefers() {
		// no spleefers - return null
		if (spleefers == null || spleefers.size() == 0) return null;
		// get unready spleefers
		List<Spleefer> list = spleefers.getUnready();
		// is the list empty?
		if (list.size() == 0) return null; // no unready spleefes
		// compile list
		return SpleeferList.getPrintablePlayerList(list);
	}

	@Override
	public String getListOfSpectators() {
		// no spectators - return null
		if (spectators == null || spectators.size() == 0) return null;
		// create list of spectators
		String comma = SimpleSpleef.ll("feedback.infoComma");
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < spectators.size(); i++) {
			if (i > 0 && i == spectators.size() - 1) builder.append(SimpleSpleef.ll("feedback.infoAnd")); // last element with end
			else if (i > 0) builder.append(comma);  // other elements with ,
			builder.append(spectators.get(i).getDisplayName());
		}
		return builder.toString();
	}


	@Override
	public boolean isEnabled() {
		return configuration.getBoolean("enabled", true);
	}

	@Override
	public boolean supportsReady() {
		return supportsReady(false, false);
	}

	@Override
	public boolean supportsCommandReady() {
		return supportsReady(false, true);
	}

	@Override
	public boolean supportsBlockReady() {
		return supportsReady(true, false);
	}

	/**
	 * Helper method to actually check useReady element
	 * @param noCommand
	 * @param noBlock
	 * @return
	 */
	protected boolean supportsReady(boolean noCommand, boolean noBlock) {
		if (configuration.isBoolean("useReady")) {
			return configuration.getBoolean("useReady", false);
		}
		if (configuration.isString("useReady")) {
			String ready = configuration.getString("useReady");
			if (noCommand == false && ready.equalsIgnoreCase("command")) return true;
			if (noBlock == false && ready.equalsIgnoreCase("block")) return true;
		}
		return false;	
	}	


	/**
	 * Send a message to broadcast, or to players and spectators
	 * @param message
	 * @param broadcast
	 */
	@Override
	public void sendMessage(String message, boolean broadcast) {
		if (message == null) {
			SimpleSpleef.log.warning("[SimpleSpleef] Message was null and could not be broadcasted!");
			return;
		}
		// global broadcast
		if (broadcast)
			broadcastMessage(message);
		else { // only players and specators
			// players
			for (Spleefer spleefer : spleefers.get()) {
				spleefer.getPlayer().sendMessage(message);
			}
			// spectators
			for (Player player : this.spectators) {
				player.sendMessage(message);
			}
			// send to console, too?
			if (SimpleSpleef.getPlugin().getConfig().getBoolean("settings.sendMessagesToConsole", true)) {
				SimpleSpleef.getPlugin().getServer().getConsoleSender().sendMessage(message);
			}
		}
	}

	/**
	 * Send a message to broadcast, or to players and spectators
	 * @param message
	 * @param exception exception - this player does not receive message
	 */
	@Override
	public void sendMessage(String message, Player exception) {
		// players
		for (Spleefer spleefer : spleefers.get()) {
			if (exception != spleefer.getPlayer())
				spleefer.getPlayer().sendMessage(message);
		}
		// spectators
		for (Player player : this.spectators) {
			if (exception != player)
				player.sendMessage(message);
		}
		// send to console, too
		SimpleSpleef.log.info(message);
	}
	
	/**
	 * broadcast message considering announcementRadius
	 * @param message
	 */
	protected void broadcastMessage(String message) {
		// get announcement radius
		int announcementRadius = SimpleSpleef.getPlugin().getConfig().getInt("settings.announcementRadius", -1);
		if (announcementRadius <= 0 || arena == null) {
			SimpleSpleef.getPlugin().getServer().broadcastMessage(message); // broadcast globally, of radius set to -1 or arena undefined
			return; // do not send to console twice
		} else {
			// get (rough) center of arena
			Location center = arena.getCenter();

			//get players and calculate distance - only players in distance or in the game receive a message
			if (center != null)
				for (Player player : SimpleSpleef.getPlugin().getServer().getOnlinePlayers()) {
					if ((player.getWorld() == center.getWorld() && player.getLocation().distance(center) <= announcementRadius) || spleefers.hasSpleefer(player) || spectators.contains(player))
						player.sendMessage(message);
				}
		}
	}

	@Override
	public boolean playerMayTeleport(Player player) {
		// at the start of game, teleportation is ok
		if (isJoinable() && (spleefers == null || spleefers.size() == 0)) return true;
		// check if the player is on the teleport-ok-list, delete him/her from list and return true
		if (this.teleportOkList.contains(player)) {
			this.teleportOkList.remove(player);
			player.setFallDistance(0.0f); // set fall distance to 0 to negate damager
			return true;
		}
		// otherwise return preventTeleportingDuringGames for this arena
		return !configuration.getBoolean("preventTeleportingDuringGames", true);
	}

	/**
	 * teleport a player to named spawn, if it exists and it is enabled
	 * @param player
	 * @param string
	 */
	protected void teleportPlayer(Player player, String spawn) {
		if (!configuration.isConfigurationSection(spawn + "Spawn") || !configuration.getBoolean(spawn + "Spawn.enabled", false))
			return; // just ignore, if not set or not enabled
		// everything ok -> teleport player
		Location teleportTo = LocationHelper.configToExactLocation(configuration.getConfigurationSection(spawn + "Spawn"));
		if (teleportTo == null) SimpleSpleef.log.warning("[SimpleSpleef] Teleport error - location was null!");
		// load chunk, if needed
		teleportTo.getWorld().loadChunk(teleportTo.getChunk());
		// add player to teleport ok list
		this.teleportOkList.add(player);
		SimpleSpleef.simpleSpleefTeleport(player, teleportTo);
	}


	/**
	 * called when player loses a game
	 * (broadcast message has to be sent by calling method)
	 * @param player
	 * @param teleport true, if player should be teleported to lose spawn, if possible
	 */
	protected void playerLoses(Player player, boolean teleport) {
		// set player to lost
		spleefers.setLost(player);
		// notify statistics
		if (SimpleSpleef.getStatisticsModule() != null)
			SimpleSpleef.getStatisticsModule().playerLostGame(player, this);
		// message to player
		player.sendMessage(ChatColor.RED + SimpleSpleef.ll("feedback.lost"));
		// broadcast message has to be sent by calling method
		// shovel lost, too
		removeShovelItem(player, true);
		// restore inventory
		restoreInventory(player);
		// teleport player to lose spawn
		if (teleport)
			teleportPlayer(player, "lose");
		// update player's game mode
		if (SimpleSpleef.getOriginalPositionKeeper().wasInCreativeBefore(player))
			changeGameModeToCreative(player);
		// determine if game is over...
		if (checkGameOver()) gameOver();
	}
	
	/**
	 * checks whether the game is over
	 * @return true for game over conditions are all met
	 */
	protected boolean checkGameOver() {
		// check for number of players that have to remain on the field to tell it a win
		if (spleefers.inGame() <= configuration.getInt("remainingPlayersWin", 1)) return true;
		return false;
	}
	
	/**
	 * Player wins by touching or reaching a certain part of the arena
	 * @param player
	 */
	protected void winByTouching(Player player) {
		for (Spleefer spleefer : spleefers.getNotLost()) {
			// if not the winning player and not already lost
			if (spleefer.getPlayer() != player)
				playerLoses(spleefer.getPlayer(), true);
		}
		// check game over manually, if there was no winner yet
		if (!isFinished() && checkGameOver()) gameOver();
	}
	
	/**
	 * called when game is over - get winners, etc.
	 */
	protected void gameOver() {
		// possibly restore inventories
		restoreAllInventories(); // done before paying prizes...

		// determine winners
		LinkedList<Spleefer> winners = new LinkedList<Spleefer>();
		for (Spleefer spleefer : spleefers.get()) {
			Player player = spleefer.getPlayer();
			if (!spleefer.hasLost()) { // not lost?
				//this guy is a winner - send a message
				player.sendMessage(ChatColor.DARK_GREEN + SimpleSpleef.ll("feedback.won"));
				winners.add(spleefer); // aggregate the winners to broadcast them later on
				// pay prizes
				payPrizeMoney(player);
				payPrizeExperience(player);
				payPrizeItems(player);

				// notify statistics
				if (SimpleSpleef.getStatisticsModule() != null)
					SimpleSpleef.getStatisticsModule().playerWonGame(player, this);
				
				// change player's game mode, if it was creative before
				if (SimpleSpleef.getOriginalPositionKeeper().wasInCreativeBefore(player))
					changeGameModeToCreative(player);

				// teleport winners back to winner's point or to lounge
				if (configuration.isConfigurationSection("winnerSpawn") && configuration.getBoolean("winnerSpawn.enabled", false))
					teleportPlayer(player, "winner");
				else teleportPlayer(player, "lounge");
			}
			// update original positions
			if (configuration.getBoolean("enableBackCommand", true))
				SimpleSpleef.getOriginalPositionKeeper().updateOriginalLocationTimestamp(player);
		}
		broadcastWinners(winners);
		
		// clean up game and end it
		endGame();
	}
	
	/**
	 * broadcast the winners
	 * @param winners
	 */
	protected void broadcastWinners(LinkedList<Spleefer> winners) {
		// get the total winners from winners list
		String broadcastKey;
		String replacePlayer = "";
		// no winner?
		if (winners.size() == 0) broadcastKey = "None";
		else if (winners.size() == 1) { // one winner?
			broadcastKey = "One";
			replacePlayer = winners.getFirst().getPlayer().getDisplayName();
		} else { // multiple winners
			broadcastKey = "Multi";
			// build list of winners
			replacePlayer = SpleeferList.getPrintablePlayerList(winners);
		}
		// broadcast message
		String broadcastMessage = ChatColor.GOLD + SimpleSpleef.ll("broadcasts.win" + broadcastKey, "[PLAYER]", replacePlayer, "[ARENA]", getName());
		sendMessage(broadcastMessage, SimpleSpleef.getPlugin().getConfig().getBoolean("settings.announceWin", true));		
	}

	/**
	 * Pay a prize in money
	 * @param player
	 */
	protected void payPrizeMoney(Player player) {
		// get prizes
		double prizeMoneyFixed = configuration.getDouble("prizeMoneyFixed", 0.0);
		double prizeMoneyPerPlayer = configuration.getDouble("prizeMoneyPerPlayer", 5.0);
		double win = prizeMoneyFixed + prizeMoneyPerPlayer * spleefers.size();
		if (win == 0) return; // if no prize money is payed, return without telling anybody
		if (SimpleSpleef.economy != null) {
			String formated = SimpleSpleef.economy.format(win);
			// give money to player
			SimpleSpleef.economy.depositPlayer(player.getName(), win);
			// player gets message
			player.sendMessage(ChatColor.AQUA + SimpleSpleef.ll("feedback.prizeMoney", "[ARENA]", getName(), "[MONEY]", formated));
			// broadcast prize?
			String broadcastMessage = ChatColor.AQUA + SimpleSpleef.ll("broadcasts.prizeMoney", "[PLAYER]", player.getName(), "[ARENA]", getName(), "[MONEY]", formated);
			if (SimpleSpleef.getPlugin().getConfig().getBoolean("settings.announcePrize", true)) {
				broadcastMessage(broadcastMessage); // broadcast message
			} else {
				sendMessage(broadcastMessage, player); // send message to all receivers
			}
		}
	}
	
	/**
	 * Pay a prize in experience
	 * @param player
	 */
	protected void payPrizeExperience(Player player) {
		// get configuration and look, if we actually add experience
		int prizeExperienceFixed = configuration.getInt("prizeExperienceFixed", 0);
		int prizeExperiencePerPlayer = configuration.getInt("prizeExperiencePerPlayer", 0);
		int win = prizeExperienceFixed + prizeExperiencePerPlayer * spleefers.size();
		if (win == 0) return; // if no prize xps is payed, return without telling anybody
		// add player experience
		player.giveExp(win);
		// player gets message
		player.sendMessage(ChatColor.AQUA + SimpleSpleef.ll("feedback.prizeExperience", "[ARENA]", getName(), "[EXPERIENCE]", String.valueOf(win)));
		// broadcast prize?
		String broadcastMessage = ChatColor.AQUA + SimpleSpleef.ll("broadcasts.prizeExperience", "[PLAYER]", player.getName(), "[ARENA]", getName(), "[EXPERIENCE]", String.valueOf(win));
		if (SimpleSpleef.getPlugin().getConfig().getBoolean("settings.announcePrize", true)) {
			broadcastMessage(broadcastMessage); // broadcast message
		} else {
			sendMessage(broadcastMessage, player); // send message to all receivers
		}
	}

	/**
	 * Pay a prize in items
	 * @param player
	 */
	protected void payPrizeItems(Player player) {
		if (!configuration.getBoolean("giveItemPrizes", true)) return; // disabled?
		if (!configuration.isList("itemPrizes")) { // check if this is a list
			SimpleSpleef.log.warning("[SimpleSpleef] Item winning list is arena " + getId() + " is not a valid list!");
			return;
		}
		// get list
		List<String> prizes = configuration.getStringList("itemPrizes");
		// random entry...
		String prize;
		if (prizes.size() == 1) { // if there is only one entry...
			prize = prizes.get(0);
		} else prize = prizes.get(generator.nextInt(prizes.size() - 1));
		// interpret line to Item Stack
		ItemStack itemStack = MaterialHelper.getItemStackFromString(prize, false);
		if (itemStack == null) { // fix NPE
			SimpleSpleef.log.warning("[SimpleSpleef] Item prize error: NPE for prize index #" + prize + "! Ignoring prize.");
			return;
		}
		// give prizes to player
		player.getInventory().addItem(itemStack);
		// player gets message
		player.sendMessage(ChatColor.AQUA + SimpleSpleef.ll("feedback.prizeItems", "[ARENA]", getName(), "[ITEM]", itemStack.getType().toString(), "[AMOUNT]", String.valueOf(itemStack.getAmount())));
		// broadcast prize?
		String broadcastMessage = ChatColor.AQUA + SimpleSpleef.ll("broadcasts.prizeItems", "[PLAYER]", player.getName(), "[ARENA]", getName(), "[ITEM]", itemStack.getType().toString(), "[AMOUNT]", String.valueOf(itemStack.getAmount()));
		if (SimpleSpleef.getPlugin().getConfig().getBoolean("settings.announcePrize", true)) {
			broadcastMessage(broadcastMessage); // broadcast message
		} else {
			sendMessage(broadcastMessage, player); // send message to all receivers
		}
	}

	/**
	 * if "clearInventory" setting of area is true, clear inventory of player and remember it
	 * @return
	 */
	protected boolean clearInventories() {
		// check setting in configuration
		if (!configuration.getBoolean("clearInventory", false)) return false;
		if (spleefers == null || inventoryKeeper == null) return false; // no NPE

		for (Spleefer spleefer : spleefers.get()) {
			Player player = spleefer.getPlayer();
			if (!inventoryKeeper.saveInventory(player)) {
				// log error
				SimpleSpleef.log.severe("[SimpleSpleef] Could not clear inventory of " + player.getName() + ". Keeping it.");
			}
		}
		return true;
	}
	
	/**
	 * if "clearInventory" setting of area is true, restore saved inventory of all players
	 * @return
	 */
	protected boolean restoreAllInventories() {
		// check setting in configuration
		if (!configuration.getBoolean("clearInventory", false)) return false;
		if (spleefers == null) return false; // no NPE

		for (Spleefer spleefer : spleefers.get()) {
			restoreInventory(spleefer.getPlayer());
		}
		return true;
	}
	
	/**
	 * if "clearInventory" setting of area is true, restore saved inventory of single player
	 * @return
	 */
	protected boolean restoreInventory(Player player) {
		// check setting in configuration
		if (!configuration.getBoolean("clearInventory", false)) return false;
		if (player == null || inventoryKeeper == null) return false; // no NPE

		return inventoryKeeper.restoreInventory(player);
	}
	
	/**
	 * if "addToInventory" setting of area is true, add items to inventory of player
	 * @return
	 */
	protected boolean addToInventories() {
		// check setting in configuration
		if (!configuration.getBoolean("addToInventory", false) || !configuration.isList("addInventoryItems")) return false;
		
		// create items for the inventory
		List<String> addInventoryItems = configuration.getStringList("addInventoryItems");
		LinkedList<ItemStack> itemStack = new LinkedList<ItemStack>();
		for (String item : addInventoryItems) {
			ItemStack stack = MaterialHelper.getItemStackFromString(item, false);
			if (stack == null)
				SimpleSpleef.log.warning("[SimpleSpleef] Could not parse addInventoryItems to item stack in arena " + getId() + ", line: " + item);
			else itemStack.add(stack);
		}
		// only continue if stack is greater 0
		if (itemStack.size() == 0) return false;
		addInventoryItems = null; // save memory
		
		// now give spleefers stack items
		for (Spleefer spleefer : spleefers.get()) {
			Player player = spleefer.getPlayer();
			for (ItemStack item : itemStack) {
				player.getInventory().addItem(item.clone());
			}
		}

		return true;
	}
	
	/**
	 * if "playersReceiveShovelAtGameStart" setting of area is true, add shovel to inventory of player
	 * @return
	 */
	protected boolean addShovelItems() {
		// check setting first
		if (!configuration.getBoolean("playersReceiveShovelAtGameStart", true)) return false; // no shovels lost
		// if yes, add shovels to all players
		for (Spleefer spleefer : spleefers.get()) {
			addShovelItem(spleefer.getPlayer(), false); // add shovel without checking setting again
		}
		return true;
	}
	
	/**
	 * remove shovel item from single player
	 * @param player
	 * @param checkSetting - if true, setting playersReceiveShovelAtGameStart is checked before addition
	 * @return
	 */
	protected boolean addShovelItem(Player player, boolean checkSetting) {
		// should setting be checked first?
		if (checkSetting && !configuration.getBoolean("playersReceiveShovelAtGameStart", true)) return false; // no shovel added
		// get material
		ItemStack shovelItem = MaterialHelper.getItemStackFromString(configuration.getString("shovelItem", "DIAMOND_SPADE"), false);
		if (shovelItem == null) {
			SimpleSpleef.log.warning("[SimpleSpleef] shovelItem of arena " + getId() + " is not a correct item id/name!");
			return false;
		}
		// give it to the player
		player.getInventory().addItem(shovelItem);
		
		return true;
	}
	
	/**
	 * if "playersLoseShovelAtGameEnd" setting of area is true, remove shovel from inventory of player
	 * @return
	 */
	protected boolean removeShovelItems() {
		// check setting first
		if (!configuration.getBoolean("playersLoseShovelAtGameEnd", true)) return false; // no shovels lost
		// if yes, remove shovels from remaining players
		for (Spleefer spleefer : spleefers.getNotLost()) {
			removeShovelItem(spleefer.getPlayer(), false); // remove shovel without checking setting again
		}
		return true;
	}
	
	/**
	 * remove shovel item from single player
	 * @param player
	 * @param checkSetting - if true, setting playersLoseShovelAtGameEnd is checked before removal
	 * @return
	 */
	protected boolean removeShovelItem(Player player, boolean checkSetting) {
		// ignore offline players
		if (player == null || !player.isOnline()) return false;

		// should setting be checked first?
		if (checkSetting && !configuration.getBoolean("playersLoseShovelAtGameEnd", true)) return false; // no shovel lost
		// get material
		ItemStack shovelItem = MaterialHelper.getItemStackFromString(configuration.getString("shovelItem", "DIAMOND_SPADE"), false);
		if (shovelItem == null) {
			SimpleSpleef.log.warning("[SimpleSpleef] shovelItem of arena " + getId() + " is not a correct item id/name!");
			return false;
		}
		// take if, if player still has it - yes, there are exploits, but I do
		// not want to handle them ;-)
		// We have to find the shovel item - it might have been used and its damage number be lowered due to use... So we have to
		// find the shovel that has been used most and delete that one...
		Inventory inventory = player.getInventory();
		Material shovelMaterial = MaterialHelper.getMaterialFromString(configuration.getString("shovelItem", "DIAMOND_SPADE"));
		// find all shovel items, if there are more than one to be removed
		for (int i = 0; i < shovelItem.getAmount(); i++) {
			int indexFound = -1; // keeps found shovels
			short durability = Short.MIN_VALUE; // keeps status of worst shovel found
			// get all possible shovel items
			for (Integer index : inventory.all(shovelMaterial).keySet()) {
				ItemStack current = inventory.getItem(index);
				short currentDurability = current.getDurability();
				if (currentDurability > durability) { // is this the worst shovel found?
					indexFound = index;
					durability = currentDurability;
				}
			}
			
			// worst shovel found?
			if (indexFound > -1) {
				// get item
				ItemStack stack = inventory.getItem(indexFound);
				if (stack.getAmount() > 1) { // shovels should only come in packs of one, but who knows?
					stack.setAmount(stack.getAmount() - 1); // reduce amount by one
					inventory.setItem(indexFound, stack);
				} else
					inventory.setItem(indexFound, null); // remove item completely
			}
		}
		
		return true;
	}
	
	/**
	 * Check if arena is disabled
	 * @param sender
	 * @return true, if arena disabled
	 */
	private boolean preCheckArenaDisabled(CommandSender sender) {
		if (!isEnabled()) {
			sender.sendMessage(ChatColor.DARK_RED + SimpleSpleef.ll("errors.arenaDisabled", "[ARENA]", getName()));
			return true;
		}
		return false;
	}

	/**
	 * Check if game is already active
	 * @param sender
	 * @return true, if game is active
	 */
	private boolean preCheckArenaActive(CommandSender sender) {
		// already running?
		if (isActive()) {
			sender.sendMessage(ChatColor.DARK_RED + SimpleSpleef.ll("errors.arenaExistsAlready", "[ARENA]", getName()));
			return true;
		}
		return false;
	}

	/**
	 * refund player, if possible
	 * @param player
	 */
	private void refundPlayer(Player player) {
		// refund player?
		if (configuration.getDouble("refundMoney", 0.0) > 0 && SimpleSpleef.economy != null) {
			double money = configuration.getDouble("refundMoney", 0.0);
			String formated = SimpleSpleef.economy.format(money);
			// give money to player
			SimpleSpleef.economy.depositPlayer(player.getName(), money);
			// player gets message
			player.sendMessage(ChatColor.AQUA + SimpleSpleef.ll("feedback.refund", "[ARENA]", getName(), "[MONEY]", formated));
		}
	}

	/**
	 * change game mode to creative and add player to gamemodeChangeOkList
	 * @param player
	 */
	private void changeGameModeToCreative(Player player) {
		// change only if setting has been set to do so
		if (configuration.getBoolean("changeGameModeBackToCreative", true)) {
			gamemodeChangeOkList.add(player); // may change game mode
			player.setGameMode(GameMode.CREATIVE);
		}
	}

	@Override
	public long getStartTime() {
		return startTime;
	}

	@Override
	public  long getFinishTime() {
		return finishTime;
	}

	@Override
	public void saveArena(CommandSender sender, boolean temporaryStore) {
		if (sender == null) sender = SimpleSpleef.getPlugin().getServer().getConsoleSender();

		if (arena == null) {
			sender.sendMessage(ChatColor.DARK_RED + SimpleSpleef.ll("adminerrors.saverestore", "[ARENA]", getName()));
			return;
		}
		
		SerializableBlockData[][][] blockData = arena.getSerializedBlocks();
		
		// output file
		File file = new File(SimpleSpleef.getPlugin().getDataFolder(), "arena_" + getId() + (temporaryStore?"_temporary":"") + ".save");
		// delete old file
		if (file.exists() && !file.delete()) {
			sender.sendMessage("[SimpleSpleef] Could not delete file " + file.getName());
			return;
		}
		
		try {
			// serialize objects
			FileOutputStream fileStream = new FileOutputStream(file);
			ObjectOutputStream os = new ObjectOutputStream(fileStream);
			// write array itself
			os.writeObject(blockData);
			os.close();
		} catch (Exception e) {
			sender.sendMessage("[SimpleSpleef] Could not save arena file " + file.getName() + ". Reason: " + e.getMessage());
			e.printStackTrace();
		}
		
		//feedback, if not temporary
		if (!temporaryStore) {
			sender.sendMessage(ChatColor.GREEN + SimpleSpleef.ll("adminfeedback.savearena", "[ARENA]", getName()));
		}
	}

	@Override
	public void restoreArena(CommandSender sender, boolean temporaryStore) {
		if (sender == null) sender = SimpleSpleef.getPlugin().getServer().getConsoleSender();

		if (arena == null) {
			sender.sendMessage(ChatColor.DARK_RED + SimpleSpleef.ll("adminerrors.saverestore", "[ARENA]", getName()));
			return;
		}
		
		// input file
		File file = new File(SimpleSpleef.getPlugin().getDataFolder(), "arena_" + getId() + (temporaryStore?"_temporary":"") + ".save");
		if (!file.exists()) {
			sender.sendMessage("[SimpleSpleef] Could not find arena file " + file.getName());
			return;
		}
		SerializableBlockData[][][] blockData;
		try {
			// deserialize objects
			FileInputStream fileInputStream = new FileInputStream(file);
			ObjectInputStream oInputStream = new ObjectInputStream(fileInputStream);
			blockData = (SerializableBlockData[][][]) oInputStream.readObject();
			oInputStream.close();
		} catch (Exception e) {
			sender.sendMessage("[SimpleSpleef] Could not restore arena file " + file.getName() + ". Reason: " + e.getMessage());
			return;
		}
		// restore arena - this is quite heavy on the server...
		arena.setSerializedBlocks(blockData);
		// delete file at the end in teporary mode - cleanup work...
		if (temporaryStore && !file.delete()) sender.sendMessage("[SimpleSpleef] Could not delete file " + file.getName());

		//feedback, if not temporary
		if (!temporaryStore) {
			sender.sendMessage(ChatColor.GREEN + SimpleSpleef.ll("adminfeedback.restorearena", "[ARENA]", getName()));
		}
	}
}
