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

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

/**
 * @author mkalus
 *
 */
public abstract class MaterialHelper {
	/**
	 * Returns material for entry (simple material like DIAMOND_SPADE or 277 - no damage numbers here!)
	 * @param material
	 * @return
	 */
	public static Material getMaterialFromString(String material) {
		if (material == null) return null; // ignore null strings

		Material m;
		// try to convert material from number
		try {
			m = Material.getMaterial(Integer.valueOf(material));
		} catch (Exception e) {
			m = Material.getMaterial(material); // try to convert it from name instead
		}

		return m;
	}
	
	/**
	 * Get a stack from a config line
	 * @param line
	 * @param considerAny true if you want simple lines (not containing damage number) to be of type "any"
	 * @return
	 */
	public static ItemStack getItemStackFromString(String line, boolean considerAny) {
		if (line == null) return null; // ignore null strings
		// split string to stack
		String[] parts = line.split(":");
		int number;
		String item;
		short dmg = 0;
		if (parts.length > 3) return null; //syntax error
		else if (parts.length == 3) { // with damage number!
			try {
				number = Integer.valueOf(parts[0]);
			} catch (Exception e) {
				return null; //syntax error
			}
			item = parts[1];
			try { // get damage number
				dmg = Short.valueOf(parts[2]);
			} catch (Exception e) {
				return null; //syntax error
			}
		} else if (parts.length == 2) {
			try {
				number = Integer.valueOf(parts[0]);
				item = parts[1];
			} catch (Exception e) {
				// ok, then this is the material
				number = 1;
				item = parts[0];
				// second should be damage number
				try { // get damage number
					dmg = Short.valueOf(parts[1]);
				} catch (Exception ex) {
					return null; //syntax error
				}
			}
		} else {
			number = 1;
			item = line;
			if (considerAny) dmg = -1; // consider this to be "any" type
		}
		// get material
		Material material = getMaterialFromString(item);
		if (material == null) return null; //syntax error
		// create stack
		ItemStack stack;
		stack = new ItemStack(material, number, dmg);
		//else stack = new ItemStack(material, number, (short) -1); //-1 to define a generic material (like all wool, not wool:0 = white wool)
		//else stack = new ItemStack(material, number); //-1 to define a generic material (like all wool, not wool:0 = white wool)

		return stack;
	}
	
	/**
	 * Checks a block an returns true if similar to compareBlock
	 * @param block
	 * @param compareBlock
	 * @return
	 */
	public static boolean isSameBlockType(Block block, ItemStack compareBlock) {
		if (block == null || compareBlock == null) return false; // no NPEs
		
		if (block.getTypeId() != compareBlock.getTypeId()) return false;
		try {
			// either type is -1 or data value matches
			if (compareBlock.getData().getData() == -1 || block.getData() == compareBlock.getData().getData())
				return true; // found -> return state						
		} catch (NullPointerException e) { // possibly thrown by compareBlock.getData() if data was -1
			return true;
		}
		
		return false;
	}
	
	/**
	 * Checks a block an returns true if similar to one of the compareBlocks
	 * @param block
	 * @param compareBlocks
	 * @return
	 */
	public static boolean isSameBlockType(Block block, List<ItemStack> compareBlocks) {
		for (ItemStack compareBlock : compareBlocks) { // cycle through blocks to compare stuff
			if (isSameBlockType(block, compareBlock)) return true;
		}
		// not found
		return false; // negate
		
	}
}
