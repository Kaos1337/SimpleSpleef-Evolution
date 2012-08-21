/**
 * 
 */
package net.kaoslabs.simplespleef.gamehelpers;


import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * @author mkalus
 *
 */
public class InventoryKeeper {
	/**
	 * Check players inventory if it is empty
	 * @param player
	 * @return true if empty
	 */
	public static boolean inventoryIsEmpty(Player player) {
		// check inventory contents for nun-null entries
		for (ItemStack stack : player.getInventory().getContents())
			if (stack != null && stack.getType() != Material.AIR) return false;

		// check armor
		for (ItemStack stack : player.getInventory().getArmorContents()) {
			System.out.println(stack);
			if (stack != null && stack.getType() != Material.AIR) return false;
		}

		return true;
	}
	
	/**
	 * inventory positions of armor
	 */
	public static final int HELMET = -10;
	public static final int CHESTPLATE = -11;
	public static final int LEGGINGS = -12;
	public static final int BOOTS = -13;
	
	/**
	 * keeps the players' inventories
	 */
	private HashMap<Player, List<InventoryEntry>> playerInventories = new HashMap<Player, List<InventoryEntry>>();
	
	/**
	 * save a player's inventory to a file
	 * @param player
	 * @return
	 */
	public boolean saveInventory(Player player) {
		if (player == null || playerInventories == null) return false; // no NPEs
		
		// create inventory entry
		LinkedList<InventoryEntry> copiedInventory = new LinkedList<InventoryKeeper.InventoryEntry>();
		
		// cycle through entries of stack
		int i = 0;
		for (ItemStack stack : player.getInventory().getContents()) {
			if (stack != null) //add new clone of stack to inventory
				copiedInventory.add(new InventoryEntry(i, stack.clone()));
			i++;
		}
		// now get armor
		ItemStack stack = player.getInventory().getHelmet();
		if (stack != null)
			copiedInventory.add(new InventoryEntry(HELMET, stack.clone()));
		stack = player.getInventory().getChestplate();
		if (stack != null)
			copiedInventory.add(new InventoryEntry(CHESTPLATE, stack.clone()));
		stack = player.getInventory().getLeggings();
		if (stack != null)
			copiedInventory.add(new InventoryEntry(LEGGINGS, stack.clone()));
		stack = player.getInventory().getBoots();
		if (stack != null)
			copiedInventory.add(new InventoryEntry(BOOTS, stack.clone()));
		
		// remove old entry, if needed
		if (playerInventories.containsKey(player))
			playerInventories.remove(player);
		
		// add new entry to list
		playerInventories.put(player, copiedInventory);

		// finally, clear player's inventory
		player.getInventory().clear();
		player.getInventory().setHelmet(null);
		player.getInventory().setChestplate(null);
		player.getInventory().setLeggings(null);
		player.getInventory().setBoots(null);

		return true;
	}
	
	/**
	 * restore a player's inventory to a file
	 * @param player
	 * @return
	 */
	public boolean restoreInventory(Player player) {
		if (player == null || !player.isOnline() || playerInventories == null) return false; // no NPEs

		// entry not found -> return
		if (!playerInventories.containsKey(player)) return false;
			
		// clear player's inventory first
		Inventory inventory = player.getInventory();
		inventory.clear();
		player.getInventory().setHelmet(null);
		player.getInventory().setChestplate(null);
		player.getInventory().setLeggings(null);
		player.getInventory().setBoots(null);
		
		// get contents from list and put them into player inventory
		List<InventoryEntry> playerInventory = playerInventories.get(player);
		for (InventoryEntry inventoryEntry : playerInventory) {
			if (inventoryEntry.index >= 0)
				inventory.setItem(inventoryEntry.index, inventoryEntry.item.clone());
			else switch (inventoryEntry.index) {
			case HELMET:
				player.getInventory().setHelmet(inventoryEntry.item.clone());
				break;
			case CHESTPLATE:
				player.getInventory().setChestplate(inventoryEntry.item.clone());
				break;
			case LEGGINGS:
				player.getInventory().setLeggings(inventoryEntry.item.clone());
				break;
			case BOOTS:
				player.getInventory().setBoots(inventoryEntry.item.clone());
				break;
			}
		}
		
		// finally, remove old entry
		playerInventories.remove(player);

		return false;
	}
	
	/**
	 * private class to keep track of item stack entries
	 * @author mkalus
	 *
	 */
	private class InventoryEntry {
		private int index;
		private ItemStack item;
		
		/**
		 * Constructor
		 * @param index
		 * @param item
		 */
		public InventoryEntry(int index, ItemStack item) {
			this.index = index;
			this.item = item;
		}
	}
}
