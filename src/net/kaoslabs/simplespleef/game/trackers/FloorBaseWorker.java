/**
 * 
 */
package net.kaoslabs.simplespleef.game.trackers;

import net.kaoslabs.simplespleef.game.Game;

/**
 * @author mkalus
 *
 */
public abstract class FloorBaseWorker implements Tracker {
	/**
	 * flag to toggle interrupts
	 */
	protected boolean interrupted = false;

	/**
	 * game reference
	 */
	protected Game game;

	/**
	 * seconds after which task actually starts doing something
	 */
	protected int startAfter;
	
	/**
	 * seconds to wait each tick of the task
	 */
	protected int tickTime;
	
	/**
	 * system time to start task at
	 */
	protected long startCounter = 0;
	
	/**
	 * system time for next tick
	 */
	protected long tickCounter = 0;

	/**
	 * Constructor
	 * @param startAfter
	 * @param tickTime
	 */
	public FloorBaseWorker(int startAfter, int tickTime) {
		this.startAfter = startAfter;
		this.tickTime = tickTime;
	}

	@Override
	public void initialize(Game game) {
		this.game = game;
		startCounter = startAfter; // initial counter
	}

	@Override
	public void interrupt() {
		this.interrupted = true;
	}

	@Override
	public boolean tick() {
		if (interrupted || game.isFinished()) return true; // stop at the end of the game

		// wait for game to start
		if (!game.isInGame()) return false;
		
		// game started, initial countdown in progress?
		if (startCounter > 0) {
			startCounter--;
			return false;
		}
		
		// count down ticks
		if (tickCounter > 0) {
			tickCounter--;
		} else { // it's time!
			executeTick();
			tickCounter = tickTime; // renew counter
		}

		return false;
	}

	/**
	 * do a tick within the task
	 */
	public abstract void executeTick();
}
