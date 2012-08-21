/**
 * 
 */
package net.kaoslabs.simplespleef.game.trackers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;

import net.kaoslabs.simplespleef.gamehelpers.SerializableBlockData;

/**
 * @author mkalus
 *
 */
public class FloorRepairWorker extends FloorBaseWorker {
	/**
	 * keeps the data of the original blocks
	 */
	private HashMap<Location, SerializableBlockData> originalBlocks = new HashMap<Location, SerializableBlockData>();
	
	/**
	 * areas of the floor that are air (changed areas)
	 */
	private HashSet<Location> air = new HashSet<Location>();

	/**
	 * Constructor
	 * @param startAfter
	 * @param tickTime
	 */
	public FloorRepairWorker(int startAfter, int tickTime, List<Block> floor) {
		super(startAfter, tickTime);
		for (Block block : floor) {
			if (block == null) continue; // no NPEs
			if (block.getType() != Material.AIR) // add location to original locations
				originalBlocks.put(block.getLocation(), new SerializableBlockData(block.getTypeId(), block.getData()));
		}
	}

	@Override
	public boolean updateBlock(Block block, BlockState oldState) {
		if (block == null) return false; // no NPEs

		Location loc = block.getLocation();
		if (originalBlocks.containsKey(loc)) { // only locations that are contained in the original block database
			if (block.getType() == Material.AIR) // dissolved to air?
				air.add(loc); //add location to repair it later on
			else air.remove(loc); // if not dissolved, remove from repair list to prevent repairs
			return true;
		}
		return false;
	}

	@Override
	public void executeTick() {
		// get a random entry
		Location location = getRandomEntry();
		//System.out.println("Tick FloorRepairWorker " + location);
		if (location != null) {
			SerializableBlockData blockData = originalBlocks.get(location); // get original block data
			if (blockData == null) return; // no NPEs, should not happen here, but just to make sure...
			// repair it
			Block block = location.getBlock();
			// get old data
			BlockState oldState = block.getState();
			block.setTypeIdAndData(blockData.getTypeId(), blockData.getData(), false);
			// update chunk information
			block.getWorld().refreshChunk(block.getChunk().getX(), block.getChunk().getZ());
			air.remove(location);				
			// notify others - and myself
			game.trackersUpdateBlock(block, oldState);
		}
	}
	
	/**
	 * picks a random entry from air set
	 */
	private Location getRandomEntry() {
		int size = air.size();
		if (size == 0) return null;
		int item = new Random().nextInt(size);
		int i = 0;
		for(Location location : air) {
			if (i == item) return location;
			i++;
		}
		
		return null; // if all locations have been dissolved
	}
}
