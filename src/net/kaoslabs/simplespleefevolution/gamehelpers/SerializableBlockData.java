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

import java.io.Serializable;

/**
 * @author mkalus
 * Keep block data of a cuboid for saving it to disk
 */
public class SerializableBlockData implements Serializable {
	private static final long serialVersionUID = 8057033565492517528L;

	/**
	 * type of block
	 */
	private int typeId;
	/**
	 * date value of block
	 */
	private byte data;
	
	/**
	 * Constructor
	 * @param typeId
	 * @param data
	 */
	public SerializableBlockData(int typeId, byte data) {
		this.typeId = typeId;
		this.data = data;
	}

	/**
	 * @return the typeId
	 */
	public int getTypeId() {
		return typeId;
	}

	/**
	 * @return the data
	 */
	public byte getData() {
		return data;
	}
}
