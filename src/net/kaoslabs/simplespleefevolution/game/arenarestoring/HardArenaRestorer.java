/**
 * 
 */
package net.kaoslabs.simplespleefevolution.game.arenarestoring;

import java.io.File;

import org.bukkit.block.Block;
import org.bukkit.block.BlockState;

import net.kaoslabs.simplespleefevolution.SimpleSpleef;
import net.kaoslabs.simplespleefevolution.game.Game;
import net.kaoslabs.simplespleefevolution.gamehelpers.Cuboid;

/**
 * @author mkalus
 *
 */
public class HardArenaRestorer extends ArenaRestorer {
	public static final int STATUS_NONE = 0;
	public static final int STATUS_SAVED = 1;
	public static final int STATUS_RESTORED = 2;
	
	private int status = HardArenaRestorer.STATUS_NONE;
	
	/**
	 * cuboid to store/restore
	 */
	private Cuboid cuboid;

	/**
	 * Constructor
	 * @param waitBeforeRestoring
	 */
	public HardArenaRestorer(int waitBeforeRestoring) {
		super(waitBeforeRestoring);
	}

	/* (non-Javadoc)
	 * @see net.kaoslabs.game.arenarestoring.ArenaRestorer#setArena(net.kaoslabs.game.Game)
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
		if (game == null || cuboid == null) return; //ignore invalid stuff

		game.saveArena(null, true);
	}

	/* (non-Javadoc)
	 * @see net.kaoslabs.game.arenarestoring.ArenaRestorer#restoreArena()
	 */
	@Override
	public void restoreArena() {
		if (game == null || cuboid == null) return; //ignore invalid stuff
		
		game.restoreArena(null, true);
	}

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

	@Override
	public boolean updateBlock(Block block, BlockState oldState) { // ignore
		return false;
	}
	
	@Override
	public void initialize(Game game) {
		this.game = game;

		// restore game arena if there is a temporary file present - very likely this is the result of a server crash in the middle of a game
		File file = new File(SimpleSpleef.getPlugin().getDataFolder(), "arena_" + game.getId() + "_temporary.save");
		if (file.exists()) {
			SimpleSpleef.log.info("[SimpleSpleef] Restoring arena for " + game.getId() + " - apparently there was a server crash during the game.");
			restoreArena();
		}
	}
}
