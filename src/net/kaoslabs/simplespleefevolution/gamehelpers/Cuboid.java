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
package net.kaoslabs.simplespleefevolution.gamehelpers;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

import net.kaoslabs.simplespleefevolution.game.Game;

/**
 * @author mkalus Represents cube in the world - inspired by Cuboid plugin
 */
public interface Cuboid {
	/**
	 * checks whether coordinates are within this cuboid
	 * 
	 * @param location
	 * @return
	 */
	public boolean contains(Location location);
	
	/**
	 * return array of serializable blocks
	 * return serializable block data
	 * @return
	 */
	public SerializableBlockData[][][] getSerializedBlocks();
	
	/**
	 * restore array of serializable blocks
	 * @param blockData
	 */
	public void setSerializedBlocks(SerializableBlockData[][][] blockData);
	
	/**
	 * retrieve a list of blocks from the cuboid that can be dug by a particular game
	 * @param game
	 * @return
	 */
	public List<Block> getDiggableBlocks(Game game);
	
	/**
	 * return center of cuboid
	 * @return
	 */
	public Location getCenter();
	
	/**
	 * return minimum location
	 * @return
	 */
	public Location getMinimumLocation();
	
	/**
	 * return maximum location
	 * @return
	 */
	public Location getMaximumLocation();
	
	/**
	 * return min/max coordinates
	 * @return
	 */
	public int[] getMinMaxCoords();
	
	/**
	 * return world the cuboid is in
	 * @return
	 */
	public World getWorld();
}
