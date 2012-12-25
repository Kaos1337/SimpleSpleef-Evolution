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
package net.kaoslabs.simplespleef.command;

import java.lang.reflect.Method;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import net.kaoslabs.simplespleef.SimpleSpleef;
import net.kaoslabs.simplespleef.game.Game;

/**
 * @author mkalus
 * Simple Spleef command handler
 */
public class SimpleSpleefCommandExecutor implements CommandExecutor {
	/**
	 * list of all commands
	 */
	private final static String[] commands = {"help", "announce", "join", "arenas", "info", "list", "ready", "team", "start", "countdown", "leave", "stop", "delete", "reset", "watch", "back", "admin", "me", "player", "topten" };
	/**
	 * commands possible from the console
	 */
	private final static String[] consoleCommands = {"help", "announce", "arenas", "info", "list", "countdown", "reset", "admin", "player", "topten" };

	//private final static String NoPermission = "[SSEvo] You do not have permission.";

	/**
	 * Print command string from settings line
	 * @param sender
	 * @param commandString comand|description...
	 */
	public static void printCommandString(CommandSender sender, String commandString) {
		int pos = commandString.indexOf('|');
		if (pos == -1) sender.sendMessage(ChatColor.GOLD + commandString);
		else
			sender.sendMessage(ChatColor.GOLD + commandString.substring(0, pos)
					+ ": " + ChatColor.WHITE + commandString.substring(pos + 1));
	}
	
	/**
	 * reference to command executor for admin class
	 */
	private static SimpleSpleefAdmin simpleSpleefAdmin = new SimpleSpleefAdmin();
	
	/**
	 * Executed whenever simplespeef is hit by a command
	 */
	public boolean onCommand(CommandSender sender, Command command, String label,
			String[] args) {
		boolean signCommand = false;
		// spleef admin short command?
		
		if (label.equalsIgnoreCase("spla")) {
			// expand arguments by prepending admin
			String[] expandargs = new String[args.length+1];
			expandargs[0] = "admin";
			for (int i = 0; i < args.length; i++)
				expandargs[i+1] = args[i];
			args = expandargs;
		} // spleef sign command?
		
		else if (label.equalsIgnoreCase("spleefsigncmd")) {
			if (!isConsole(sender) || args == null || args.length == 0) return false; //ignore sign commands not dispatched from the console sender or malformed - prevents players to exploit this pseudo command
			// real sender is first argument
			Player player = SimpleSpleef.getPlugin().getServer().getPlayer(args[0]);
			if (player == null) return false; // no such player
			sender = player; // new sender for this command
			// shorten args by copying and shortening
			String[] newArgs;
			if (args.length == 0) newArgs = new String[]{}; // empty array
			else {
				newArgs = new String[args.length-1];
				for (int i = 1; i < args.length; i++)
					newArgs[i-1] = args[i];
			}
			args = newArgs;
			// yes, we have a sign command!
			signCommand = true;
		}
			
		// how many arguments?
		if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
			if (signCommand) return false; //empty sign - ignore command!
			else return helpCommand(sender);
		}
		
		// first argument is subcommand
		String subcommand = args[0].toLowerCase();

		// parse commands - does it exist?
		boolean found = false;
		for (String checkCommand : commands) {
			if (checkCommand.equalsIgnoreCase(subcommand)) {
				found = true;
				break;
			}
		}
		// command not found?
		if (!found) return unknownCommand(sender, subcommand);
		
		// is it a console command and it cannot be executed from the console?
		if (isConsole(sender) && !isConsoleCommand(subcommand)) {
			sender.sendMessage("This command cannot be executed from the console.");
			return true;
		}

		// check permissions
		if(SimpleSpleef.DEBUG_MODE)
			System.out.println("[DEBUG] SignCommand: "+signCommand + "| Comando usato: "+"simplespleef." + subcommand+" | hasPermission ritorna "+sender.hasPermission("simplespleef.sign." + subcommand));
		if (signCommand) { // check sign permission for sign commands
			if (!SimpleSpleef.checkPermission(sender, "simplespleef.sign." + subcommand))
				return permissionMissing(sender, subcommand, signCommand);
			
		} else if (!SimpleSpleef.checkPermission(sender,"simplespleef." + subcommand)) // check normal permissions for normal commands
			return permissionMissing(sender, subcommand, signCommand);

		// Ok, we are clear...
		try {
			// inflect method
			Method myCommand = this.getClass().getDeclaredMethod(subcommand + "Command", CommandSender.class, String[].class);
			Object[] xargs = { sender, args };
			if(SimpleSpleef.DEBUG_MODE){
				System.out.print("Sender: "+sender+" ha il permesso: "+sender.hasPermission("simplespleef."+subcommand)+" | ValutPermission: "+SimpleSpleef.permission.has(sender, "simplespleef."+subcommand)+"| Nome permission: "+SimpleSpleef.permission.getName()+" | args: ");
				for(int i=0; i<args.length; i++)
					System.out.print(args[i]+", ");
				System.out.println();
			}
			// invoke it
			myCommand.invoke(this, xargs);
		} catch (Exception e) {
			e.printStackTrace();
			sender.sendMessage("Error in command execution - command was not found in executor. Contact SimpleSpleef programmer!");
		}
		return true;
	}

	/**
	 * handle help commands
	 * @param sender
	 * @return
	 */
	protected boolean helpCommand(CommandSender sender) {
		// last option...
		if (!SimpleSpleef.checkPermission(sender, "simplespleef.help")) return false;
		
		// get all commands in language file
		Map<String, String> commands = SimpleSpleef.lls("command");
		// control if there is any output:
		boolean output = false;
		// either commands for player or for console
		String[] commandList = isConsole(sender)?SimpleSpleefCommandExecutor.consoleCommands:SimpleSpleefCommandExecutor.commands;
		
		// cycle through commands
		for (String command : commandList) {
			// check permission on command
			if (!SimpleSpleef.checkPermission(sender, "simplespleef." + command)) continue;
			// check if we are on the console and check command in that case
			if (isConsole(sender) && !isConsoleCommand(command)) continue;
			// we are cleared and may output the command nicely
			output = true;
			// print command
			printCommandString(sender, commands.get(command));
		}
		// was there output? if not show response
		if (!output)
			sender.sendMessage(ChatColor.DARK_RED + SimpleSpleef.ll("errors.noPermissionAtAll"));
		return true;
	}
	
	/**
	 * Announce a new game (in arena)
	 * @param sender
	 * @param args
	 */
	protected void announceCommand(CommandSender sender, String[] args) {
		// too many arguments?
		if (tooManyArguments(sender, args, 1)) return;
		// get game from 2nd argument
		Game arena = this.getArenaFromArgument(sender, args, 1);
		if (arena != null) // no errors - then try to announce new game
			SimpleSpleef.getGameHandler().announce(sender, arena);
		
	}
	
	/**
	 * Join a game (in arena)
	 * @param sender
	 * @param args
	 */
	protected void joinCommand(CommandSender sender, String[] args) {
		// too many arguments?
		if (tooManyArguments(sender, args, 1)) return;
		// get game from 2nd argument
		Game arena = this.getArenaFromArgument(sender, args, 1);
		if (arena != null)
			SimpleSpleef.getGameHandler().join(sender, arena);
                   
	}
	
	/**
	 * List available arenas
	 * @param sender
	 * @param args
	 */
	protected void arenasCommand(CommandSender sender, String[] args) {

			SimpleSpleef.getGameHandler().arenas(sender);
		
		
	}
	
	/**
	 * Show information (on arena)
	 * @param sender
	 * @param args
	 */
	protected void infoCommand(CommandSender sender, String[] args) {
		// remember, we have 20 chat lines maximum...
		// get game from 2nd argument
		Game game = this.getArenaFromArgument(sender, args, 1);
		if (game == null) return; // no arena found
		SimpleSpleef.getGameHandler().info(sender, game);
		
		
		
	}
	
	/**
	 * List spleefers in arena
	 * @param sender
	 * @param args
	 */
	protected void listCommand(CommandSender sender, String[] args) {
		// remember, we have 20 chat lines maximum...
		// get game from 2nd argument
		Game game = this.getArenaFromArgument(sender, args, 1);
		if (game == null) return; // no arena found
		SimpleSpleef.getGameHandler().list(sender, game);
		
		
	}
	
	/**
	 * Join a team (joined spleefers only)
	 * @param sender
	 * @param args
	 */
	protected void teamCommand(CommandSender sender, String[] args) {
		// too many or to few arguments?
		if (tooFewArguments(sender, args, 1)) return;
		if (tooManyArguments(sender, args, 1)) return;
		SimpleSpleef.getGameHandler().team(sender, args[1]);
	}
	
	/**
	 * Ready for the game (spleefers only)
	 * @param sender
	 * @param args
	 */
	protected void readyCommand(CommandSender sender, String[] args) {
		// too many arguments?
		if (tooManyArguments(sender, args, 0)) return;
		SimpleSpleef.getGameHandler().ready(sender);
	}
	
	/**
	 * Start game (spleefers only)
	 * @param sender
	 * @param args
	 */
	protected void startCommand(CommandSender sender, String[] args) {
		// too many arguments?
		if (tooManyArguments(sender, args, 0)) return;
		SimpleSpleef.getGameHandler().start(sender);
	}
	
	/**
	 * Start count down (of arena)
	 * @param sender
	 * @param args
	 */
	protected void countdownCommand(CommandSender sender, String[] args) {
		// too many arguments?
		if (tooManyArguments(sender, args, 1)) return;
		// get game from 2nd argument
		Game arena = this.getArenaFromArgument(sender, args, 1);
		// if player and no arena defined - arena is unset, so player can start current arena
		if (arena != null && args.length < 2 && !isConsole(sender))
			SimpleSpleef.getGameHandler().start(sender);
		else if (arena != null) { // no errors - then try to announce new game
			SimpleSpleef.getGameHandler().countdown(sender, arena);
		}
	}
	
	/**
	 * Leave a game
	 * @param sender
	 * @param args
	 */
	protected void leaveCommand(CommandSender sender, String[] args) {
		// too many arguments?
		if (tooManyArguments(sender, args, 0)) return;
		SimpleSpleef.getGameHandler().leave(sender);
	}
	
	/**
	 * Stop a game
	 * @param sender
	 * @param args
	 */
	protected void stopCommand(CommandSender sender, String[] args) {
		// too many arguments?
		if (tooManyArguments(sender, args, 0)) return;
		SimpleSpleef.getGameHandler().stop(sender);
	}
	
	/**
	 * Delete a game
	 * @param sender
	 * @param args
	 */
	protected void deleteCommand(CommandSender sender, String[] args) {
		// too many arguments?
		if (tooManyArguments(sender, args, 0)) return;
		SimpleSpleef.getGameHandler().delete(sender, null);
	}
	
	/**
	 * Reset a game (admin/moderator delete game)
	 * @param sender
	 * @param args
	 */
	protected void resetCommand(CommandSender sender, String[] args) {
		// too many arguments?
		if (tooManyArguments(sender, args, 1)) return;
		// too few arguments?
		if (tooFewArguments(sender, args, 1)) return;
		Game arena = this.getArenaFromArgument(sender, args, 1);
		if (arena != null) {
			SimpleSpleef.getGameHandler().delete(sender, arena);
		}
	}
	
	/**
	 * Watch a game
	 * @param sender
	 * @param args
	 */
	protected void watchCommand(CommandSender sender, String[] args) {
		// too many arguments?
		if (tooManyArguments(sender, args, 1)) return;
		// get game from 2nd argument
		Game arena = this.getArenaFromArgument(sender, args, 1);
		if (arena != null) { // no errors - then try watch a game
			SimpleSpleef.getGameHandler().watch(sender, arena);
		}
	}
	
	/**
	 * Admin commands
	 * @param sender
	 * @param args
	 */
	protected void adminCommand(CommandSender sender, String[] args) {
		// delegate all further stuff to admin class
		SimpleSpleefCommandExecutor.simpleSpleefAdmin.executeCommand(sender, args);
	}
	
	/**
	 * Back command
	 * @param sender
	 * @param args
	 */
	protected void backCommand(CommandSender sender, String[] args) {
		// too many arguments?
		if (tooManyArguments(sender, args, 0)) return;
		SimpleSpleef.getGameHandler().back(sender);
	}
	
	/**
	 * Me command
	 * @param sender
	 * @param args
	 */
	protected void meCommand(CommandSender sender, String[] args) {
		// too many arguments?
		if (tooManyArguments(sender, args, 0)) return;
		SimpleSpleef.getGameHandler().playerStatistics(sender, sender.getName());
	}
	
	/**
	 * Player command
	 * @param sender
	 * @param args
	 */
	protected void playerCommand(CommandSender sender, String[] args) {
		// too many arguments?
		if (tooManyArguments(sender, args, 1)) return;
		// too few arguments?
		if (tooFewArguments(sender, args, 1)) return;
		SimpleSpleef.getGameHandler().playerStatistics(sender, args[1]);
	}
	
	/**
	 * Topten command
	 * @param sender
	 * @param args
	 */
	protected void toptenCommand(CommandSender sender, String[] args) {
		// too many arguments?
		if (tooManyArguments(sender, args, 1)) return;
		// get game from 2nd argument
		Game arena;
		if (args.length < 2) arena = null;
		else arena = this.getArenaFromArgument(sender, args, 1);
		SimpleSpleef.getGameHandler().toptenStatistics(sender, arena);
	}
	
	//TODO: add further commands here...
	
	/**
	 * Shows unknown command error
	 * @param sender
	 * @param command
	 * @return true
	 */
	protected boolean unknownCommand(CommandSender sender, String command) {
		sender.sendMessage(ChatColor.DARK_RED + SimpleSpleef.ll("errors.unknownCommand", "[COMMAND]", command));
		return true;
	}

	/**
	 * Shows unknown arena error
	 * @param sender
	 * @param command
	 * @return true
	 */
	protected boolean unknownArena(CommandSender sender, String arena) {
		sender.sendMessage(ChatColor.DARK_RED + SimpleSpleef.ll("errors.unknownArena", "[ARENA]", arena));
		return true;
	}
	
	/**
	 * tells the sender that a particular permission is missing
	 * @param sender
	 * @param command
	 * @param signcommand is this a sign command?
	 * @return
	 */
	protected boolean permissionMissing(CommandSender sender, String command, boolean signcommand) {
		String msg = signcommand?"errors.signPermissionMissing":"errors.permissionMissing";
		sender.sendMessage(ChatColor.DARK_RED + SimpleSpleef.ll(msg, "[COMMAND]", command));
		return true;
	}

	/**
	 * Checks if sender is console
	 * @param sender
	 * @return
	 */
	protected boolean isConsole(CommandSender sender) {
		if (sender instanceof ConsoleCommandSender) return true;
		return false;
	}
	
	/**
	 * checks whether given command is allowed on the console
	 * @param command
	 * @return
	 */
	protected boolean isConsoleCommand(String command) {
		for (String c : SimpleSpleefCommandExecutor.consoleCommands) {
			if (c.equals(command)) return true;
		}
		return false;
	}
	
	/**
	 * checks for a minimum number of arguments
	 * @param sender
	 * @param args
	 * @param min index
	 * @return
	 */
	protected boolean tooFewArguments(CommandSender sender, String[] args, int min) {
		if (args.length < min+1) {
			sender.sendMessage(ChatColor.DARK_RED + SimpleSpleef.ll("errors.tooFewArguments", "[MIN]", String.valueOf(min)));
			String commandString = SimpleSpleef.ll("command." + args[0]);
			if (commandString != null)
				printCommandString(sender, commandString);
			return true;
		}
		return false;
	}
	
	/**
	 * checks for a maximum number of arguments
	 * @param args
	 * @param max
	 * @return
	 */
	protected boolean tooManyArguments(CommandSender sender, String[] args, int max) {
		if (args.length > max+1) {
			sender.sendMessage(ChatColor.DARK_RED + SimpleSpleef.ll("errors.tooManyArguments", "[MAX]", String.valueOf(max)));
			String commandString = SimpleSpleef.ll("command." + args[0]);
			if (commandString != null)
				printCommandString(sender, commandString);
			return true;
		}
		return false;
	}
	
	/**
	 * get game name from string
	 * @param args
	 * @param index
	 * @return null, if arena name is not found
	 */
	protected Game getArenaFromArgument(CommandSender sender, String[] args, int index) {
		String name;

		// too short, get the default arena
		if (args.length <= index) name = SimpleSpleef.getGameHandler().getDefaultArena();
		else name = args[index];

		// check for the existence of the arena
		Game game = SimpleSpleef.getGameHandler().getGameByName(name);
		if (game == null) // error if no arena has been found
			unknownArena(sender, name);
		
		return game;
	}
}
