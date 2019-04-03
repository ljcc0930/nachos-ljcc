package nachos.threads;

import java.util.PriorityQueue;
import java.util.Comparator;
import javafx.util.Pair;

import nachos.machine.*;

/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {
    /**
     * Allocate a new Alarm. Set the machine's timer interrupt handler to this
     * alarm's callback.
     *
     * <p><b>Note</b>: Nachos will not function correctly with more than one
     * alarm.
     */
    public Alarm() {
	alarmQueue = new PriorityQueue<WaitingPair>(11, pairComp);
	Machine.timer().setInterruptHandler(new Runnable() {
		public void run() { timerInterrupt(); }
	    });
    }

    /**
     * The timer interrupt handler. This is called by the machine's timer
     * periodically (approximately every 500 clock ticks). Causes the current
     * thread to yield, forcing a context switch if there is another thread
     * that should be run.
     */
    public void timerInterrupt() {
    	
		KThread.currentThread().yield();
	
		boolean intStatus = Machine.interrupt().disable();
	
		while (!alarmQueue.isEmpty() && alarmQueue.peek().getKey() < Machine.timer().getTime()) {
			KThread thread = alarmQueue.poll().getValue();
			thread.ready();
		}
	
		Machine.interrupt().restore(intStatus);
    }

    /**
     * Put the current thread to sleep for at least <i>x</i> ticks,
     * waking it up in the timer interrupt handler. The thread must be
     * woken up (placed in the scheduler ready set) during the first timer
     * interrupt where
     *
     * <p><blockquote>
     * (current time) >= (WaitUntil called time)+(x)
     * </blockquote>
     *
     * @param	x	the minimum number of clock ticks to wait.
     *
     * @see	nachos.machine.Timer#getTime()
     */
    public void waitUntil(long x) {
		// for now, cheat just to get something working (busy waiting is bad)
		long wakeTime = Machine.timer().getTime() + x;
	
		while(wakeTime > Machine.timer().getTime()) {
			boolean intStatus = Machine.interrupt().disable();
			
			KThread tempTh = KThread.currentThread();
			
			WaitingPair temp_pair = new WaitingPair(wakeTime, tempTh);
			
			alarmQueue.add(temp_pair);
			KThread.sleep();

			Machine.interrupt().restore(intStatus);
		}
    }
    
    private class WaitingPair {
    	public WaitingPair(long key, KThread value) {
			this.key = key;
			this.value = value;
		}
		public long getKey() {
			return key;
		}
		public KThread getValue() {
			return value;
		}
		private long key;
		private KThread value;
	}
	
	private Comparator<WaitingPair> pairComp = new Comparator<WaitingPair>() {
		@Override
		public int compare(WaitingPair x, WaitingPair y) {
			return new Long(x.getKey()).compareTo(new Long(y.getKey()));
		}
	};
    
	private PriorityQueue<WaitingPair> alarmQueue;
}
