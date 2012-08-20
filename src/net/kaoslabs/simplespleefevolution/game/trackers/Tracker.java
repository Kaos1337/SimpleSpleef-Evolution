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

package net.kaoslabs.simplespleefevolution.game.trackers;

import org.bukkit.block.Block;
import org.bukkit.block.BlockState;

import net.kaoslabs.simplespleefevolution.game.Game;

/**
 * @author mkalus
 *
 */
public interface Tracker {
	/**
	 * Initialize a tracker
	 * @param game reference
	 */
	public void initialize(Game game);

	/**
	 * Interrupt a tracker
	 */
	public void interrupt();

	/**
	 * Call tracker tick
	 * @return true, if tracker has ended
	 */
	public boolean tick();

	/**
	 * Called when a block in the arena is changed
	 * @return true if tracker did something with the block
	 */
	public boolean updateBlock(Block block, BlockState oldState);
}
