package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;


import java.util.LinkedList;
import java.util.HashMap;

/**
 * A kernel that can support multiple user processes.
 */
public class UserKernel extends ThreadedKernel {
    /**
     * Allocate a new user kernel.
     */
    public UserKernel() {
	super();
    }

    /**
     * Initialize this kernel. Creates a synchronized console and sets the
     * processor's exception handler.
     */
    public void initialize(String[] args) {
		super.initialize(args);

		console = new SynchConsole(Machine.console());
	
		Machine.processor().setExceptionHandler(new Runnable() {
			public void run() { exceptionHandler(); }
			});
			
        int numPhyPages = Machine.processor().getNumPhysPages();
        for(int i = 0; i < numPhyPages; i++)
            pageList.add(i);
        memoryLock = new Lock();
        processLock = new Lock();
    }

    /**
     * Test the console device.
     */	
    public void selfTest() {
		/*
		super.selfTest();

		System.out.println("Testing the console device. Typed characters");
		System.out.println("will be echoed until q is typed.");

		char c;

		do {
			c = (char) console.readByte(true);
			console.writeByte(c);
		}
		while (c != 'q');

		System.out.println("");
		*/
    }

    /**
     * Returns the current process.
     *
     * @return	the current process, or <tt>null</tt> if no process is current.
     */
    public static UserProcess currentProcess() {
	if (!(KThread.currentThread() instanceof UThread))
	    return null;
	
	return ((UThread) KThread.currentThread()).process;
    }

    /**
     * The exception handler. This handler is called by the processor whenever
     * a user instruction causes a processor exception.
     *
     * <p>
     * When the exception handler is invoked, interrupts are enabled, and the
     * processor's cause register contains an integer identifying the cause of
     * the exception (see the <tt>exceptionZZZ</tt> constants in the
     * <tt>Processor</tt> class). If the exception involves a bad virtual
     * address (e.g. page fault, TLB miss, read-only, bus error, or address
     * error), the processor's BadVAddr register identifies the virtual address
     * that caused the exception.
     */
    public void exceptionHandler() {
	Lib.assertTrue(KThread.currentThread() instanceof UThread);

	UserProcess process = ((UThread) KThread.currentThread()).process;
	int cause = Machine.processor().readRegister(Processor.regCause);
	process.handleException(cause);
    }

    /**
     * Start running user programs, by creating a process and running a shell
     * program in it. The name of the shell program it must run is returned by
     * <tt>Machine.getShellProgramName()</tt>.
     *
     * @see	nachos.machine.Machine#getShellProgramName
     */
    public void run() {
	super.run();

	UserProcess process = UserProcess.newUserProcess();
	
	String shellProgram = Machine.getShellProgramName();	
	Lib.assertTrue(process.execute(shellProgram, new String[] { }));

	KThread.currentThread().finish();
    }

    /**
     * Terminate this kernel. Never returns.
     */
    public void terminate() {
	super.terminate();
    }
    
    public static int[] allocPhyPage(int n) {
		boolean intStatus = Machine.interrupt().disable();
		
    	memoryLock.acquire();
		
        int[] pageNumber = null;
        if(n <= pageList.size()) {
		    pageNumber = new int[n];
		    
		    for(int i = 0; i < n; i++)
				pageNumber[i] = pageList.removeFirst();
		}
		
    	memoryLock.release();
		
		Machine.interrupt().restore(intStatus);
		
		return pageNumber;
    }
    public static void releasePhyPage(int ppn) {
        Lib.assertTrue(ppn >= 0 && ppn < Machine.processor().getNumPhysPages());
        
		boolean intStatus = Machine.interrupt().disable();
		
    	memoryLock.acquire();

        pageList.add(ppn);
        
    	memoryLock.release();
        
		Machine.interrupt().restore(intStatus);
    }
    
    public static int getNumPhyPages() {
    	memoryLock.acquire();
    	
    	int temp = pageList.size();
    	
    	memoryLock.release();
    	return temp;
    }
    
    public static int newProcess() {
    	processLock.acquire();
    	
    	int temp = ++nProcess;
    	
    	processLock.release();
    	
    	return temp;
    }
    
    public static UserProcess getProcess(int pid) {
		boolean intStatus = Machine.interrupt().disable();
		
    	processLock.acquire();
		
    	UserProcess temp = pidMap.get(pid);
    	
    	processLock.release();
        
		Machine.interrupt().restore(intStatus);
		
		return temp;
    }
    
    public static void mapProcess(int pid, UserProcess process) {
		boolean intStatus = Machine.interrupt().disable();
		
    	processLock.acquire();
		
		pidMap.put(pid,process);
		
    	processLock.release();
        
		Machine.interrupt().restore(intStatus);
    }
	 
	public static void deleteProcess(int pid) {
		boolean intStatus = Machine.interrupt().disable();
		
    	processLock.acquire();
    	
		pidMap.remove(pid);
        
    	processLock.release();
    	
		Machine.interrupt().restore(intStatus);
	}
    
    

    /** Globally accessible reference to the synchronized console. */
    public static SynchConsole console;

    // dummy variables to make javac smarter
    private static Coff dummy1 = null;
    
    private static int nProcess = 0;
    private static HashMap<Integer, UserProcess> pidMap =
    	new HashMap<Integer, UserProcess>();
    
    private static LinkedList<Integer> pageList = new LinkedList<Integer>();
    private static Lock processLock;
    private static Lock memoryLock;
}
