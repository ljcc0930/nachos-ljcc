package nachos.threads;

import nachos.machine.*;

/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>,
 * and multiple threads can be waiting to <i>listen</i>. But there should never
 * be a time when both a speaker and a listener are waiting, because the two
 * threads can be paired off at this point.
 */
public class Communicator {
    /**
     * Allocate a new communicator.
     */
    public Communicator() {
    	communicatorLock = new Lock();
    	waitingListener = new Condition2(communicatorLock);
    	waitingSpeaker = new Condition2(communicatorLock);
    	dialog = null;
    	listeners = 0;
    }

    /**
     * Wait for a thread to listen through this communicator, and then transfer
     * <i>word</i> to the listener.
     *
     * <p>
     * Does not return until this thread is paired up with a listening thread.
     * Exactly one listener should receive <i>word</i>.
     *
     * @param	word	the integer to transfer.
     */
    public void speak(int word) {
    	communicatorLock.acquire();
    	
    	while(listeners == 0 || dialog != null) {
    		waitingSpeaker.sleep();
    	}
    	dialog = new Integer(word);
    	waitingListener.wake();
    	listeners--;
    	
    	communicatorLock.release();
    }

    /**
     * Wait for a thread to speak through this communicator, and then return
     * the <i>word</i> that thread passed to <tt>speak()</tt>.
     *
     * @return	the integer transferred.
     */    
    public int listen() {
    	communicatorLock.acquire();
    	
	    listeners++;
    	while(dialog == null) {
    		waitingSpeaker.wake();
	    	waitingListener.sleep();
    	}
    	
    	int ret = dialog;
    	dialog = null;
    	waitingSpeaker.wake();
    	
    	communicatorLock.release();
    	
		return ret;
    }
    private Lock communicatorLock;
    private Condition2 waitingListener;
    private Condition2 waitingSpeaker;
    private Integer dialog;
    private int listeners;
}
