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

package net.kaoslabs.simplespleef.statistics;

import java.util.Comparator;

/**
 * Simple class to contain top ten entry
 * @author mkalus
 */
public class TopTenEntry {
	/**
	 * player name
	 */
	public String player;
	
	/**
	 * number of games total
	 */
	public int games;
	
	/**
	 * number of games won
	 */
	public int lost;
	
	/**
	 * number of games lost
	 */
	public int won;
	
	/**
	 * @return new ascending comparator for TopTenEntries
	 */
	public static Comparator<TopTenEntry> getAscendingComparator() {
		return new TopTenEntryAscendingComparator();
	}
	
	/**
	 * Ascending comparator class for sorted list using top ten entries
	 * @author mkalus
	 */
	private static class TopTenEntryAscendingComparator implements Comparator<TopTenEntry> {
		@Override
		public int compare(TopTenEntry entry1, TopTenEntry entry2) {
			// first compare games won
			if (entry1.won > entry2.won) return -1;
			if (entry1.won < entry2.won) return 1;
			
			// now compare lost games, too - but negatively
			if (entry1.lost < entry2.lost) return -1;
			if (entry1.lost > entry2.lost) return 1;
			
			// lastly, compare games total
			if (entry1.games > entry2.games) return -1;
			if (entry1.games < entry2.games) return 1;

			// compare name, if everything above fails
			return entry1.player.compareTo(entry2.player);
		}
		
	}
}
