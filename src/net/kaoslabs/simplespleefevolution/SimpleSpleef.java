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

package net.kaoslabs.simplespleefevolution;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import com.sk89q.worldedit.bukkit.WorldEditAPI;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;

import net.kaoslabs.simplespleefevolution.command.SimpleSpleefCommandExecutor;
import net.kaoslabs.simplespleefevolution.command.SimpleSpleefCommandWhitelist;
import net.kaoslabs.simplespleefevolution.command.SimpleSpleefSignCommandExecutor;
import net.kaoslabs.simplespleefevolution.game.GameHandler;
import net.kaoslabs.simplespleefevolution.gamehelpers.OriginalPositionKeeper;
import net.kaoslabs.simplespleefevolution.listeners.PluginListener;
import net.kaoslabs.simplespleefevolution.statistics.StatisticsFactory;
import net.kaoslabs.simplespleefevolution.statistics.StatisticsModule;
import net.kaoslabs.simplespleefevolution.util.ConfigHelper;
import net.kaoslabs.simplespleefevolution.util.Translator;
import net.kaoslabs.simplespleefevolution.util.UpdateChecker;
import net.kaoslabs.simplespleefevolution.util.Metrics;

/**
 * @author mkalus
 *
 */
public class SimpleSpleef extends JavaPlugin {
	public static final Logger log = Logger.getLogger("Minecraft");

	public static final boolean DEBUG_MODE = false;
	
	/**
	 * reference to Vault economy
	 */
	public static Economy economy = null;
	
	/**
	 * reference to Vault permissions
	 */
	public static Permission permission = null;
	
	/**
	 * checks permission, either Vault-based or using builtin system
	 * @param sender
	 * @return
	 */
	public static boolean checkPermission(CommandSender sender, String permission) {
		if (SimpleSpleef.permission != null) { // use Vault to check permissions - actually does nothing different than sender.hasPermission at the moment, but might change
			try {
				return SimpleSpleef.permission.has(sender, permission);
			} catch (NoSuchMethodError e) {
				// if for some reason there is a problem with Vault, fall back to default permissions
				SimpleSpleef.log.warning("[SimpleSpleefEvo] Checking Vault permission threw an exception. Are you using the most recent version? Falling back to to default permission checking.");
			}
		}
		// fallback to default Bukkit permission checking system
		if(SimpleSpleef.DEBUG_MODE)
			System.out.println("[DEBUGpd] Player: "+sender.getName()+" | permessi per "+permission+"? -> "+ sender.hasPermission(permission)+ "| Controllo: "+sender.isPermissionSet(permission));
		return sender.hasPermission(permission) || sender.hasPermission("simplespleef.*");
	}

	/**
	 * self reference to singleton
	 */
	private static SimpleSpleef simpleSpleef;
	
	/**
	 * get instance
	 * @return
	 */
	public static SimpleSpleef getPlugin() {
		return simpleSpleef;
	}

	/**
	 * reference to game handler
	 */
	private static GameHandler gameHandler;

	/**
	 * @return the gameHandler
	 */
	public static GameHandler getGameHandler() {
		return gameHandler;
	}
	
	/**
	 * reference to statistics handler
	 */
	private static StatisticsModule statisticsModule;

	/**
	 * @return the statisticsModule
	 */
	public static StatisticsModule getStatisticsModule() {
		return statisticsModule;
	}

	/**
	 * world edit API
	 */
	private static WorldEditAPI worldEditAPI = null;

	/**
	 * sets the world edit API for this plugin
	 * @param worldEditAPI
	 */
	public static void setWorldEditAPI(WorldEditAPI worldEditAPI) {
		SimpleSpleef.worldEditAPI = worldEditAPI;
	}
	
	/**
	 * return WorldEditAPI
	 * @return
	 */
	public static WorldEditAPI getWorldEditAPI() {
		return SimpleSpleef.worldEditAPI;
	}

	/**
	 * keeper of original positions
	 */
	private static OriginalPositionKeeper originalPositionKeeper;

	/**
	 * get originalPositionKeeper instance (singleton)
	 * @return
	 */
	public static OriginalPositionKeeper getOriginalPositionKeeper() {
		if (originalPositionKeeper == null) originalPositionKeeper = new OriginalPositionKeeper();
		return originalPositionKeeper;
	}
	
	/**
	 * Teleport a player to a location - fixes people falling through the floor
	 * @param player
	 * @param location
	 */
	public static void simpleSpleefTeleport(Player player, Location location) {
		player.teleport(location.add(0, 0.25, 0), TeleportCause.PLUGIN); // add a quarter block to the height of the location to let the player "fall" onto the block
	}

	/**
	 * reference to command handler
	 */
	private SimpleSpleefCommandExecutor commandExecutor;

	/**
	 * reference to translator
	 */
	private static Translator lang;

	/**
	 * Called when enabling plugin
	 */
	@Override
	public void onEnable() {
		// initialize plugin
		log.info(this.toString() + " is loading.");	
		
		// set reference
		SimpleSpleef.simpleSpleef = this;
		
		

		// configure plugin (configuration stuff)
		configurePlugin();
		
		// check updates, if turned on
		checkForUpdate();
		
		// create new handler
		SimpleSpleef.gameHandler = new GameHandler();
		SimpleSpleef.gameHandler.updateGameHandlerData();
		// start tracking
		SimpleSpleef.getPlugin().getServer().getScheduler().scheduleSyncRepeatingTask(this, gameHandler, 0L, 20L);
		
		// register vault stuff
		setupEconomy();
		setupPermission();
		
		// add event listeners
		registerEvents();
		
		// check for WorldEdit 
		checkForWorldEdit();
		// create statistics handler
		createStatisticsHander();
		
		startMetrics();
		
		PluginManager pm = getServer().getPluginManager();
		if (pm.getPlugin("Vault") == null) {
			getLogger().info("Permission plugin not found... shutting down!");
			pm.disablePlugin(this);
		}
	}

	/**
	 * Called when disabling plugin
	 */
	public void onDisable() {
		log.info(this.toString() + " is shutting down.");
		// cancel all tasks for this plugin
		SimpleSpleef.getPlugin().getServer().getScheduler().cancelTasks(this);
		// clean memory
		SimpleSpleef.worldEditAPI = null;
		// stop game handler
		SimpleSpleef.gameHandler = null;
		SimpleSpleef.economy = null;
		SimpleSpleef.originalPositionKeeper = null;
		SimpleSpleef.lang = null;
		SimpleSpleef.statisticsModule = null;
		//TODO add if more stuff comes along

		//save config to disk
		//this.saveConfig();
		
		// derefer self reference
		SimpleSpleef.simpleSpleef = null;
	}

	
	/**
	 * Run some configuration stuff at the initialization of the plugin
	 */
	public void configurePlugin() {
		// define that default config should be copied
		this.getConfig().options().copyDefaults(true);

		// check for the existance of plugin folder
		if (!getDataFolder().exists()) {
			getDataFolder().mkdir(); // create it!
		}
		
		// create config helper
		ConfigHelper configHelper = new ConfigHelper();
		// update defaults of existing arenas
		configHelper.updateDefaults();
		// update sample config, if needed
		configHelper.updateSampleConfig();
		// update language files
		configHelper.updateLanguageFiles();
		
		// initialize the translator
		lang = new Translator(this, this.getConfig().getString("language", "en"));
	}

	/**
	 * reload configuration and translation
	 */
	public void reloadSimpleSpleefConfiguration() {
		// reload the config file
		this.reloadConfig();
		// have the game loader reload, too
		SimpleSpleef.gameHandler.updateGameHandlerData();

		// re-initialize the translator
		lang = new Translator(this, this.getConfig().getString("language", "en"));
	}

	/**
	 * Check for updates
	 */
	protected void checkForUpdate() {
		// create update checker
		final UpdateChecker checker = new UpdateChecker();
		
		// possibly check for updates in the internet on startup
		this.getServer().getScheduler().scheduleAsyncDelayedTask(this, new Runnable() {
		    public void run() {
		    	try {
					String newVersion = checker.checkForUpdate(SimpleSpleef.simpleSpleef.getDescription().getVersion());
					if (newVersion != null)
						log.info("[SimpleSpleefEvo] Update found for SimpleSpleef - please go to http://dev.bukkit.org/server-mods/simple-spleef/ to download version " + newVersion + "!");
				} catch (Exception e) {
					log.warning("[SimpleSpleefEvo] Could not connect to remote server to check for update. Exception said: " + e.getMessage());
				}
		    }
		}, 0L);
		
		// also check for updates in the configuration files and update them, if needed
		checker.updateConfigurationVersion(this);
	}

	/**
	 * Configure event listeners
	 */
	protected void registerEvents() {
		// let my command handler take care of commands
		this.commandExecutor = new SimpleSpleefCommandExecutor();
		this.getCommand("spleef").setExecutor(commandExecutor);

		// Prepare listeners
		PluginManager pm = getServer().getPluginManager();
		
		// add listener for other plugins
		pm.registerEvents(new PluginListener(), this);
		// Register our events
		pm.registerEvents(gameHandler, this);
		pm.registerEvents(new UpdateChecker(), this); // check updates
		pm.registerEvents(new SimpleSpleefSignCommandExecutor(), this); // check sign actions
		pm.registerEvents(new SimpleSpleefCommandWhitelist(), this); // check command whitelist
	}

	/**
	 * @param key of translation file
	 * @param replacers an even number of key/value pairs to replace key entries
	 * @return translated string
	 */
	public static String ll(String key, String... replacers) {
		return lang.ll(key, replacers);
	}
	
	/**
	 * Get translation section list
	 * @param key
	 * @return
	 */
	public static Map<String, String> lls(String section) {
		return lang.lls(section);
	}

	/**
	 * set up economy - see http://dev.bukkit.org/server-mods/vault/
	 * @return
	 */
	private boolean setupEconomy() {
		if (this.getServer().getPluginManager().getPlugin("Vault") != null) {
			RegisteredServiceProvider<Economy> economyProvider = getServer()
					.getServicesManager().getRegistration(
							net.milkbowl.vault.economy.Economy.class);
			if (economyProvider != null) {
				economy = economyProvider.getProvider();
			}
	
			SimpleSpleef.log.info("[SimpleSpleefEvo] Vault hooked as economy plugin.");
			return (economy != null);
		}
		economy = null; // if the plugin is reloaded during play, possibly kill economy
		SimpleSpleef.log.info("[SimpleSpleefEvo] Vault plugin not found - not using economy.");
		return false;
	}
	
	/**
	 * set up permissions - see http://dev.bukkit.org/server-mods/vault/
	 * @return
	 */
	private boolean setupPermission() {
		if (this.getServer().getPluginManager().getPlugin("Vault") != null) {
			RegisteredServiceProvider<Permission> permissionProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.permission.Permission.class);
	        if (permissionProvider != null) {
	            permission = permissionProvider.getProvider();
	        }
	
			SimpleSpleef.log.info("[SimpleSpleefEvo] Vault hooked as permission plugin.");
	        return (permission != null);
		}
		permission = null; // if the plugin is reloaded during play, possibly kill permissions
		SimpleSpleef.log.info("[SimpleSpleefEvo] Vault plugin not found - defaulting to Bukkit permission system.");
		return false;
	}

	/**
	 * make sure worldEdit is loaded - inspired by MultiversePortals
	 */
	private void checkForWorldEdit() {
		Plugin plugin = this.getServer().getPluginManager().getPlugin("WorldEdit");
		if (plugin != null) {
			// check version
			try {
				double version = Double.parseDouble(plugin.getDescription().getVersion());
				if (version < 5.0) {
					SimpleSpleef.log
					.warning("[SimpleSpleefEvo] Found WorldEdit, but must be at least version 5.0 to work! Ignoring it.");
					return;
				}
			} catch (Exception e) {} // no number, should be fine...
			
			SimpleSpleef.setWorldEditAPI(new WorldEditAPI((WorldEditPlugin) plugin));
			SimpleSpleef.log
					.info("[SimpleSpleefEvo] Found WorldEdit " + plugin.getDescription().getVersion() + ". Using it for selections.");
		}
	}

	/**
	 * get a world guard instance if it exists - see http://wiki.sk89q.com/wiki/WorldGuard/Regions/API for source
	 * @return
	 */
	public WorldGuardPlugin getWorldGuard() {
	    Plugin plugin = getServer().getPluginManager().getPlugin("WorldGuard");
	 
	    // WorldGuard may not be loaded
	    if (plugin == null || !(plugin instanceof WorldGuardPlugin)) {
	        return null; // Maybe you want throw an exception instead
	    }
	 
	    return (WorldGuardPlugin) plugin;
	}
	
	public void startMetrics() {
		try {
		    Metrics metrics = new Metrics(this);
		    metrics.start();
		    SimpleSpleef.log.info("[SimpleSpleefEvo] Metrics connection started.");
		} catch (IOException e) {
			SimpleSpleef.log.warning("[SimpleSpleefEvo] Failed to submit the stats :-("); // Failed to submit the stats :-(
		}
	}


	/**
	 * create new statistics handler instance
	 */
	private void createStatisticsHander() {
		if (getConfig().getBoolean("settings.enableStatistics", true))
			try {
				StatisticsFactory statisticsHandler = new StatisticsFactory();
				statisticsModule = statisticsHandler.getStatisticsModuleByName(getConfig().getString("settings.statisticsModule", "file"),
						getConfig().getConfigurationSection("settings.statisticsSettings"));
			} catch (Exception e) {
				SimpleSpleef.log.warning("[SimpleSpleefEvo] Could not initiate statistics module - settings bugged somehow or database error. Disabling statistics for now.");
				statisticsModule = null;
			}
		else statisticsModule = null;
	}
}
