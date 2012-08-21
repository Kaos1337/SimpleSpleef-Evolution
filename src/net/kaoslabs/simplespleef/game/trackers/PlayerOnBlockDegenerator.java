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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import net.kaoslabs.simplespleef.game.Game;
import net.kaoslabs.simplespleef.game.Spleefer;
import net.kaoslabs.simplespleef.gamehelpers.MaterialHelper;

/**
 * @author mkalus
 *
 */
public class PlayerOnBlockDegenerator implements Tracker {
	/**
	 * flag to toggle interrupts
	 */
	private boolean interrupted = false;
	
	/**
	 * game reference
	 */
	private Game game;

	/**
	 * count block degeneration
	 */
	private int blockDegeneration;
	
	/**
	 * list of blocks that will degenerate when standing on them for too long
	 */
	private List<ItemStack> degeneratingBlocks;

	/**
	 * list of DegenerationKeeper to players
	 */
	private Map<Player, DegenerationKeeper> degenerationList;

	/**
	 * Constructor
	 * @param blockDegeneration
	 */
	public PlayerOnBlockDegenerator(int blockDegeneration, List<String> degeneratingBlocks) {
		this.blockDegeneration = blockDegeneration;
		this.degenerationList = new HashMap<Player, PlayerOnBlockDegenerator.DegenerationKeeper>();
		
		// fill with degenerating blocks
		this.degeneratingBlocks = new LinkedList<ItemStack>();
		for (String string : degeneratingBlocks) {
			ItemStack stack = MaterialHelper.getItemStackFromString(string, true);
			if (stack != null) this.degeneratingBlocks.add(stack);
		}
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
		interrupted = true;
	}

	/* (non-Javadoc)
	 * @see net.kaoslabs.game.trackers.Tracker#tick()
	 */
	@Override
	public boolean tick() {
		// if interrupted or finished - finish tracker, too
		if (interrupted || game.isFinished()) {
			return true;
		}
		
		// wait for game to start
		if (!game.isInGame()) return false;
		
		// run through active spleefers and check their positions
		for (Spleefer spleefer : game.getSpleefers().get()) {
			if (!spleefer.hasLost() && spleefer.getPlayer().isOnline())
				updatePosition(spleefer.getPlayer());
		}
		
		return false;
	}

	/**
	 * update player position in list
	 * @param player
	 */
	private void updatePosition(Player player) {
		// try to get from list
		DegenerationKeeper keeper = degenerationList.get(player);
		
		if (keeper == null) { // new entry?
			addNewBlock(player);
		} else {
			// check player position
			Block playerOnBlock = player.getLocation().getBlock().getRelative(BlockFace.DOWN);

			// did the player move?
			if (playerOnBlock.getX() != keeper.checkedBlock.getX() || playerOnBlock.getY() != keeper.checkedBlock.getY()
					 || playerOnBlock.getZ() != keeper.checkedBlock.getZ()) {
				degenerationList.remove(player); // remove player from list
				addNewBlock(player); // new block for player
				return;
			}

			// decrement counter and check
			keeper.counter--;
			if (keeper.counter <= 0) { // reset counter
				// if for some reason, block has already dissolved into air
				if (keeper.checkedBlock.getType() == Material.AIR) {
					degenerationList.remove(player); // remove player from list
					return;
				}
				
				// is the block not within the arena? ignore
				if (!game.checkMayBreakBlock(playerOnBlock, player)) {
					degenerationList.remove(player); // remove player from list
					return;
				}
				
				// get original data
				BlockState oldState = keeper.checkedBlock.getState();

				keeper.checkedBlock.setType(Material.AIR); // block dissolves into thin air

				// update chunk information
				keeper.checkedBlock.getWorld().refreshChunk(keeper.checkedBlock.getChunk().getX(), keeper.checkedBlock.getChunk().getZ());
				
				degenerationList.remove(player); // remove player from list
				
				// update trackers
				game.trackersUpdateBlock(keeper.checkedBlock, oldState);
			}
		}
	}
	
	/**
	 * get the block player is standing on - if is is degeneratable
	 * @param player
	 * @return block or null, if block is not degeneratable
	 */
	private Block getPlayerOnBlock(Player player) {
		// get block player is standing on
		Block block = player.getLocation().getBlock().getRelative(BlockFace.DOWN);
		
		// do not degenerate air
		if (block.getType() == Material.AIR) return null;
		
		// only if in list of blocks
		if (degeneratingBlocks == null || degeneratingBlocks.size() == 0 || MaterialHelper.isSameBlockType(block, degeneratingBlocks)) {
			return block;
		}
		
		return null;
	}
	
	/**
	 * helper method to add new block for player
	 * @param player
	 */
	private Block addNewBlock(Player player) {
		Block block = getPlayerOnBlock(player);
		if (block != null) { // create new keeper
			DegenerationKeeper keeper = new DegenerationKeeper();
			keeper.checkedBlock = block;
			keeper.counter = blockDegeneration;
			degenerationList.put(player, keeper);
		}
		return block;
	}

	/* (non-Javadoc)
	 * @see net.kaoslabs.game.trackers.Tracker#updateBlock(org.bukkit.block.Block, int, byte)
	 */
	@Override
	public boolean updateBlock(Block block, BlockState oldState) {
		return false; // ignore
	}

	/**
	 * Keeps track of player's block and its degeneration
	 * @author mkalus
	 *
	 */
	private class DegenerationKeeper {
		/**
		 * keeps track of the checked block
		 */
		private Block checkedBlock = null;
		
		private int counter = 0;
	}
}
