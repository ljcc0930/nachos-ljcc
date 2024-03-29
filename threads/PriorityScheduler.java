package nachos.threads;

import nachos.machine.*;

import java.util.TreeSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Comparator;

/**
 * A scheduler that chooses threads based on their priorities.
 *
 * <p>
 * A priority scheduler associates a priority with each thread. The next thread
 * to be dequeued is always a thread with priority no less than any other
 * waiting thread's priority. Like a round-robin scheduler, the thread that is
 * dequeued is, among all the threads of the same (highest) priority, the
 * thread that has been waiting longest.
 *
 * <p>
 * Essentially, a priority scheduler gives access in a round-robin fassion to
 * all the highest-priority threads, and ignores all other threads. This has
 * the potential to
 * starve a thread if there's always a thread waiting with higher priority.
 *
 * <p>
 * A priority scheduler must partially solve the priority inversion problem; in
 * particular, priority must be donated through locks, and through joins.
 */
public class PriorityScheduler extends Scheduler {
    /**
     * Allocate a new priority scheduler.
     */
    public PriorityScheduler() {
    }
    
    /**
     * Allocate a new priority thread queue.
     *
     * @param	transferPriority	<tt>true</tt> if this queue should
     *					transfer priority from waiting threads
     *					to the owning thread.
     * @return	a new priority thread queue.
     */
    public ThreadQueue newThreadQueue(boolean transferPriority) {
	return new PriorityQueue(transferPriority);
    }

    public int getPriority(KThread thread) {
	Lib.assertTrue(Machine.interrupt().disabled());
		       
	return getThreadState(thread).getPriority();
    }

    public int getEffectivePriority(KThread thread) {
	Lib.assertTrue(Machine.interrupt().disabled());
		       
	return getThreadState(thread).getEffectivePriority();
    }

    public void setPriority(KThread thread, int priority) {
	Lib.assertTrue(Machine.interrupt().disabled());
		       
	Lib.assertTrue(priority >= priorityMinimum &&
		   priority <= priorityMaximum);
	
	getThreadState(thread).setPriority(priority);
    }

    public boolean increasePriority() {
	boolean intStatus = Machine.interrupt().disable();
		       
	KThread thread = KThread.currentThread();

	int priority = getPriority(thread);
	if (priority == priorityMaximum)
	    return false;

	setPriority(thread, priority+1);

	Machine.interrupt().restore(intStatus);
	return true;
    }

    public boolean decreasePriority() {
	boolean intStatus = Machine.interrupt().disable();
		       
	KThread thread = KThread.currentThread();

	int priority = getPriority(thread);
	if (priority == priorityMinimum)
	    return false;

	setPriority(thread, priority-1);

	Machine.interrupt().restore(intStatus);
	return true;
    }

    /**
     * The default priority for a new thread. Do not change this value.
     */
    public static final int priorityDefault = 1;
    /**
     * The minimum priority that a thread can have. Do not change this value.
     */
    public static final int priorityMinimum = 0;
    /**
     * The maximum priority that a thread can have. Do not change this value.
     */
    public static final int priorityMaximum = 7;    

    /**
     * Return the scheduling state of the specified thread.
     *
     * @param	thread	the thread whose scheduling state to return.
     * @return	the scheduling state of the specified thread.
     */
    protected ThreadState getThreadState(KThread thread) {
	if (thread.schedulingState == null)
	    thread.schedulingState = new ThreadState(thread);

	return (ThreadState) thread.schedulingState;
    }

    /**
     * A <tt>ThreadQueue</tt> that sorts threads by priority.
     */
    protected class PriorityQueue extends ThreadQueue {
	PriorityQueue(boolean transferPriority) {
	    this.transferPriority = transferPriority;
	}

	public void waitForAccess(KThread thread) {
	    Lib.assertTrue(Machine.interrupt().disabled());
	    getThreadState(thread).waitForAccess(this);
	}

	public void acquire(KThread thread) {
	    Lib.assertTrue(Machine.interrupt().disabled());
	    getThreadState(thread).acquire(this);
	}

	public KThread nextThread() {
	    Lib.assertTrue(Machine.interrupt().disabled());
	    // implement me
        if (waitQueue.isEmpty())
	        return null;
        
        KThread next = waitQueue.poll().thread;
        this.acquire( next );

        return this.queueHolder;
	}

	/**
	 * Return the next thread that <tt>nextThread()</tt> would return,
	 * without modifying the state of this queue.
	 *
	 * @return	the next thread that <tt>nextThread()</tt> would
	 *		return.
	 */
	protected ThreadState pickNextThread() {
	    // implement me
        return waitQueue.peek();
	}

    protected void remove(ThreadState TS) {
        waitQueue.remove(TS);
    }

    protected void add(ThreadState TS) {
        waitQueue.add(TS);
    }
	
	public void print() {
	    Lib.assertTrue(Machine.interrupt().disabled());
	    // implement me (if you want)
	}

	/**
	 * <tt>true</tt> if this queue should transfer priority from waiting
	 * threads to the owning thread.
	 */
	public boolean transferPriority;

    private java.util.PriorityQueue<ThreadState> waitQueue = 
        new java.util.PriorityQueue<ThreadState>(11, new ThreadStateComparator<ThreadState>());
    private KThread queueHolder = null;

    protected class ThreadStateComparator<T extends ThreadState> implements Comparator<T> {
        protected ThreadStateComparator() {
        }

        @Override
        public int compare(T threadState1, T threadState2) {
            int effectivePriority1 = threadState1.getEffectivePriority(),
                effectivePriority2 = threadState2.getEffectivePriority();

            if (effectivePriority1 > effectivePriority2)
                return -1;
            else if (effectivePriority1 < effectivePriority2)
                return 1;
            else {
                long waitingTime1 = threadState1.waitingTime,
                     waitingTime2 = threadState2.waitingTime;
                if (waitingTime1 < waitingTime2)
                    return -1;
                else if (waitingTime1 > waitingTime2)
                    return 1;
                else return 0;
            }
        }
    }
    }

    /**
     * The scheduling state of a thread. This should include the thread's
     * priority, its effective priority, any objects it owns, and the queue
     * it's waiting for, if any.
     *
     * @see	nachos.threads.KThread#schedulingState
     */
    protected class ThreadState {
	/**
	 * Allocate a new <tt>ThreadState</tt> object and associate it with the
	 * specified thread.
	 *
	 * @param	thread	the thread this state belongs to.
	 */
	public ThreadState(KThread thread) {
	    this.thread = thread;
	    
        this.priority = priorityDefault;
        this.effectivePriority = priority;
	}

	/**
	 * Return the priority of the associated thread.
	 *
	 * @return	the priority of the associated thread.
	 */
	public int getPriority() {
	    return priority;
	}

	/**
	 * Return the effective priority of the associated thread.
	 *
	 * @return	the effective priority of the associated thread.
	 */
	public int getEffectivePriority() {
	    // implement me
	    return effectivePriority;
	}

	/**
	 * Set the priority of the associated thread to the specified value.
	 *
	 * @param	priority	the new priority.
	 */
	public void setPriority(int priority) {
	    if (this.priority == priority)
		return;
	    
	    this.priority = priority;
        updateEffectivePriority();
	    
	    // implement me
	}

    // waitingQueue: This thread is waiting in this queue
    // acquiredQueueSet: The threads in any queue in the set will be waiting for this thread.
    protected void updateEffectivePriority() {
        int maxPriority = priority;

        if (waitingQueue != null)
            waitingQueue.remove( this );

        for (PriorityQueue queue : acquiredQueueSet) 
            if (queue.transferPriority) {
                ThreadState priorThreadState = queue.pickNextThread();
                if (priorThreadState != null) {
                    int donatingPriority = priorThreadState.getEffectivePriority();

                    if (donatingPriority > maxPriority)
                        maxPriority = donatingPriority;
                }
            }

        boolean updatedFlag = (maxPriority != effectivePriority);

        effectivePriority = maxPriority;
        if (waitingQueue != null)
            waitingQueue.add( this );

        if (updatedFlag && waitingQueue != null &&
                (waitingQueue.transferPriority && waitingQueue.queueHolder != null))
            getThreadState( waitingQueue.queueHolder ).updateEffectivePriority();
    }

    void release(PriorityQueue priorityQueue) {
        if (acquiredQueueSet.remove(priorityQueue)) {
            priorityQueue.queueHolder = null;
            updateEffectivePriority();
        }
    }

	/**
	 * Called when <tt>waitForAccess(thread)</tt> (where <tt>thread</tt> is
	 * the associated thread) is invoked on the specified priority queue.
	 * The associated thread is therefore waiting for access to the
	 * resource guarded by <tt>waitQueue</tt>. This method is only called
	 * if the associated thread cannot immediately obtain access.
	 *
	 * @param	waitQueue	the queue that the associated thread is
	 *				now waiting on.
	 *
	 * @see	nachos.threads.ThreadQueue#waitForAccess
	 */
	public void waitForAccess(PriorityQueue waitQueue) {
        if (this.waitingQueue != waitQueue) {
            release( waitQueue );

            this.waitingQueue = waitQueue;
            this.waitingTime = Machine.timer().getTime();
            waitQueue.add(this);

            if (waitQueue.queueHolder != null)
                getThreadState( waitQueue.queueHolder ).updateEffectivePriority();
        }
	}

	/**
	 * Called when the associated thread has acquired access to whatever is
	 * guarded by <tt>waitQueue</tt>. This can occur either as a result of
	 * <tt>acquire(thread)</tt> being invoked on <tt>waitQueue</tt> (where
	 * <tt>thread</tt> is the associated thread), or as a result of
	 * <tt>nextThread()</tt> being invoked on <tt>waitQueue</tt>.
	 *
	 * @see	nachos.threads.ThreadQueue#acquire
	 * @see	nachos.threads.ThreadQueue#nextThread
	 */
	public void acquire(PriorityQueue waitQueue) {
        if (waitQueue.queueHolder != null)
            getThreadState(waitQueue.queueHolder).release( waitQueue );
        
        waitQueue.remove( this );

        waitQueue.queueHolder = this.thread;
        acquiredQueueSet.add( waitQueue );
        if (this.waitingQueue == waitQueue)
            this.waitingQueue = null;
        
        updateEffectivePriority();
	}

	/** The thread with which this object is associated. */	   
	protected KThread thread;
	/** The priority of the associated thread. */
	protected int priority;
    protected int effectivePriority;
    protected long waitingTime;

    private HashSet< PriorityQueue > acquiredQueueSet = new HashSet<PriorityQueue>();
    private PriorityQueue waitingQueue = null;
    }
}
