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

package net.kaoslabs.simplespleef.game.arenarestoring;

import java.util.LinkedList;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;

import net.kaoslabs.simplespleef.gamehelpers.Cuboid;
import net.kaoslabs.simplespleef.gamehelpers.MaterialHelper;

/**
 * @author mkalus
 *
 */
public class SimpleRestorer extends ArenaRestorer {
	public static final int STATUS_NONE = 0;
	public static final int STATUS_SAVED = 1;
	public static final int STATUS_RESTORED = 2;

	private int status = SimpleRestorer.STATUS_NONE;

	/**
	 * material to set the block as
	 */
	private Material blockType;
	
	/**
	 * cuboid to store/restore
	 */
	private Cuboid cuboid;

	/**
	 * Constructor
	 * @param waitBeforeRestoring
	 * @param blocktype block to fill the floor with
	 */
	public SimpleRestorer(int waitBeforeRestoring, String blockType) {
		super(waitBeforeRestoring);
		// set block type
		this.blockType = MaterialHelper.getMaterialFromString(blockType);
	}

	/* (non-Javadoc)
	 * @see net.kaoslabs.game.trackers.Tracker#tick()
	 */
	@Override
	public boolean tick() {
		if (interrupted == true) { // when interrupted, restore arena right awaw
			restoreArena(); // finish arena at once
			return true; // finish restorer
		}
		
		// wait for game to start
		if (status == HardArenaRestorer.STATUS_NONE && game.isInGame()) { // game started and not saved yet
			saveArena();
			status = HardArenaRestorer.STATUS_SAVED;
			return false;
		}
		
		// wait for game to finish - or return, if already restored
		if (!game.isFinished() || status == HardArenaRestorer.STATUS_RESTORED) return false;

		// before start restoring count restore ticker to 0
		if (this.waitBeforeRestoring > 0) {
			this.waitBeforeRestoring--;
			return false;
		}

		restoreArena(); // finish arena at once
		status = HardArenaRestorer.STATUS_RESTORED;
		return true; // finish restorer
	}

	/* (non-Javadoc)
	 * @see net.kaoslabs.game.trackers.Tracker#updateBlock(org.bukkit.block.Block, int, byte)
	 */
	@Override
	public boolean updateBlock(Block block, BlockState oldState) {
		return false;
	}

	/* (non-Javadoc)
	 * @see net.kaoslabs.game.arenarestoring.ArenaRestorer#setArena(net.kaoslabs.gamehelpers.Cuboid)
	 */
	@Override
	public void setArena(Cuboid cuboid) {
		this.cuboid = cuboid;
	}

	/* (non-Javadoc)
	 * @see net.kaoslabs.game.arenarestoring.ArenaRestorer#saveArena()
	 */
	@Override
	public void saveArena() {
		fillArenaFloor();
	}

	/* (non-Javadoc)
	 * @see net.kaoslabs.game.arenarestoring.ArenaRestorer#restoreArena()
	 */
	@Override
	public void restoreArena() {
		fillArenaFloor();
	}

	/**
	 * actually fill arena floor
	 */
	private void fillArenaFloor() {
		if (cuboid == null || game == null || blockType == null) return; // if something has not been set - return
		
		// get min/max of cuboid
		int[] coords = cuboid.getMinMaxCoords();
		World world = cuboid.getWorld();
		
		LinkedList<Chunk> chunksChanged = new LinkedList<Chunk>();
		
		for (int x = coords[0]; x <= coords[3]; x++)
			for (int y = coords[1]; y <= coords[4]; y++)
				for (int z = coords[2]; z <= coords[5]; z++) {
					Block block = world.getBlockAt(x, y, z);

					// load chunk if needed
					Chunk here = block.getChunk();
					if (!here.isLoaded()) here.load();

					// set type
					block.setTypeId(blockType.getId(), false);

					// add to list of changed chunks
					if (!chunksChanged.contains(here))
						chunksChanged.addFirst(here);
				}

		// refresh chunks
		for (Chunk chunk : chunksChanged)
			world.refreshChunk(chunk.getX(), chunk.getZ());
	}
}
