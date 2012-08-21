/**
 * 
 */
package net.kaoslabs.simplespleef.game.arenarestoring;

import net.kaoslabs.simplespleef.game.Game;
import net.kaoslabs.simplespleef.game.trackers.Tracker;
import net.kaoslabs.simplespleef.gamehelpers.Cuboid;

/**
 * @author mkalus
 * Abstract class for classes that restore the arena eventually - these classes also have to call the
 * handler's game over method to delete the game when finished.
 */
public abstract class ArenaRestorer implements Tracker {
	/**
	 * interrupted signal
	 */
	protected boolean interrupted = false;
	
	/**
	 * game reference
	 */
	protected Game game;
	
	/**
	 * wait seconds before restoring - implement wait in children of this class
	 */
	protected int waitBeforeRestoring;
	
	/**
	 * Constructor
	 * @param waitBeforeRestoring
	 */
	public ArenaRestorer(int waitBeforeRestoring) {
		this.waitBeforeRestoring = waitBeforeRestoring;
	}
	
	/**
	 * Set the arena for one game
	 * @param game
	 * @param cuboid
	 */
	public abstract void setArena(Cuboid cuboid);

	/**
	 * Store the arena for one game
	 */
	public abstract void saveArena();

	/**
	 * Restore the arena for one game - please call handler's game over method to delete the game when finished restoring.
	 * @param game
	 */
	public abstract void restoreArena();


	@Override
	public void initialize(Game game) {
		this.game = game;
	}

	@Override
	public void interrupt() {
		interrupted = true;
	}
}
