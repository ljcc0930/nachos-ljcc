package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;

import java.io.EOFException;

import java.util.LinkedList;
import java.util.HashSet;
import java.util.Iterator;

/**
 * Encapsulates the state of a user process that is not contained in its
 * user thread (or threads). This includes its address translation state, a
 * file table, and information about the program being executed.
 *
 * <p>
 * This class is extended by other classes to support additional functionality
 * (such as additional syscalls).
 *
 * @see	nachos.vm.VMProcess
 * @see	nachos.network.NetProcess
 */
public class UserProcess {
    /**
     * Allocate a new process.
     */
    public UserProcess() {
    	pid = UserKernel.newProcess();
    	UserKernel.mapProcess(pid, this);
    	finished = new Semaphore(0);
    	
    	openFiles = new OpenFile[16];
    	openFiles[0] = UserKernel.console.openForReading();
    	openFiles[1] = UserKernel.console.openForWriting();
    }
    
    /**
     * Allocate and return a new process of the correct class. The class name
     * is specified by the <tt>nachos.conf</tt> key
     * <tt>Kernel.processClassName</tt>.
     *
     * @return	a new process of the correct class.
     */
    public static UserProcess newUserProcess() {
	return (UserProcess)Lib.constructObject(Machine.getProcessClassName());
    }

    /**
     * Execute the specified program with the specified arguments. Attempts to
     * load the program, and then forks a thread to run it.
     *
     * @param	name	the name of the file containing the executable.
     * @param	args	the arguments to pass to the executable.
     * @return	<tt>true</tt> if the program was successfully executed.
     */
    public boolean execute(String name, String[] args) {
	if (!load(name, args))
	    return false;
	
	new UThread(this).setName(name).fork();

	return true;
    }

    /**
     * Save the state of this process in preparation for a context switch.
     * Called by <tt>UThread.saveState()</tt>.
     */
    public void saveState() {
    }

    /**
     * Restore the state of this process after a context switch. Called by
     * <tt>UThread.restoreState()</tt>.
     */
    public void restoreState() {
	Machine.processor().setPageTable(pageTable);
    }

    /**
     * Read a null-terminated string from this process's virtual memory. Read
     * at most <tt>maxLength + 1</tt> bytes from the specified address, search
     * for the null terminator, and convert it to a <tt>java.lang.String</tt>,
     * without including the null terminator. If no null terminator is found,
     * returns <tt>null</tt>.
     *
     * @param	vaddr	the starting virtual address of the null-terminated
     *			string.
     * @param	maxLength	the maximum number of characters in the string,
     *				not including the null terminator.
     * @return	the string read, or <tt>null</tt> if no null terminator was
     *		found.
     */
    public String readVirtualMemoryString(int vaddr, int maxLength) {
	Lib.assertTrue(maxLength >= 0);

	byte[] bytes = new byte[maxLength+1];

	int bytesRead = readVirtualMemory(vaddr, bytes);

	for (int length=0; length<bytesRead; length++) {
	    if (bytes[length] == 0)
		return new String(bytes, 0, length);
	}

	return null;
    }

    /**
     * Transfer data from this process's virtual memory to all of the specified
     * array. Same as <tt>readVirtualMemory(vaddr, data, 0, data.length)</tt>.
     *
     * @param	vaddr	the first byte of virtual memory to read.
     * @param	data	the array where the data will be stored.
     * @return	the number of bytes successfully transferred.
     */
    public int readVirtualMemory(int vaddr, byte[] data) {
	return readVirtualMemory(vaddr, data, 0, data.length);
    }

    /**
     * Transfer data from this process's virtual memory to the specified array.
     * This method handles address translation details. This method must
     * <i>not</i> destroy the current process if an error occurs, but instead
     * should return the number of bytes successfully copied (or zero if no
     * data could be copied).
     *
     * @param	vaddr	the first byte of virtual memory to read.
     * @param	data	the array where the data will be stored.
     * @param	offset	the first byte to write in the array.
     * @param	length	the number of bytes to transfer from virtual memory to
     *			the array.
     * @return	the number of bytes successfully transferred.
     */
    public int readVirtualMemory(int vaddr, byte[] data, int offset,
				 int length) {
		Lib.assertTrue(offset >= 0 && length >= 0 && offset+length <= data.length);

		byte[] memory = Machine.processor().getMemory();
	
		if (vaddr < 0)
			return 0;

		int total = 0;
        int phyOffset = Machine.processor().offsetFromAddress(vaddr);
        int vpn = Machine.processor().pageFromAddress(vaddr);
		
		for(; length > 0 && vpn < numPages; vpn++) {
			TranslationEntry entry = pageTable[vpn];
			entry.used = true;
			
			int ppn = entry.ppn;
			int paddr = (ppn * pageSize) + phyOffset;
			
			int amount = Math.min(length, pageSize - phyOffset);
            System.arraycopy(memory, paddr, data, offset, amount);
            offset += amount;
            length -= amount;
            total += amount;
            phyOffset = 0;
		}

		return total;
    }

    /**
     * Transfer all data from the specified array to this process's virtual
     * memory.
     * Same as <tt>writeVirtualMemory(vaddr, data, 0, data.length)</tt>.
     *
     * @param	vaddr	the first byte of virtual memory to write.
     * @param	data	the array containing the data to transfer.
     * @return	the number of bytes successfully transferred.
     */
    public int writeVirtualMemory(int vaddr, byte[] data) {
	return writeVirtualMemory(vaddr, data, 0, data.length);
    }

    /**
     * Transfer data from the specified array to this process's virtual memory.
     * This method handles address translation details. This method must
     * <i>not</i> destroy the current process if an error occurs, but instead
     * should return the number of bytes successfully copied (or zero if no
     * data could be copied).
     *
     * @param	vaddr	the first byte of virtual memory to write.
     * @param	data	the array containing the data to transfer.
     * @param	offset	the first byte to transfer from the array.
     * @param	length	the number of bytes to transfer from the array to
     *			virtual memory.
     * @return	the number of bytes successfully transferred.
     */
    public int writeVirtualMemory(int vaddr, byte[] data, int offset,
				  int length) {
		Lib.assertTrue(offset >= 0 && length >= 0 && offset+length <= data.length);

		byte[] memory = Machine.processor().getMemory();
	
		if (vaddr < 0)
			return 0;

		int total = 0;
        int phyOffset = Machine.processor().offsetFromAddress(vaddr);
        int vpn = Machine.processor().pageFromAddress(vaddr);
		
		for(; length > 0 && vpn < numPages; vpn++) {
			TranslationEntry entry = pageTable[vpn];
			if(entry.readOnly) {
                Lib.debug(dbgProcess, 
                	"\tUserProcess.writeVirtualMemory: \n" + 
                	"\t\t write on read-only page with vpn = " + vpn);
			}
			else {
				entry.used = true;
		        entry.dirty = true;
			
				int ppn = entry.ppn;
				int paddr = (ppn * pageSize) + phyOffset;
			
				int amount = Math.min(length, pageSize - phyOffset);
		        System.arraycopy(data, offset, memory, paddr, amount);
		        offset += amount;
		        length -= amount;
		        total += amount;
		        phyOffset = 0;
            }
		}

		return total;
    }

    /**
     * Load the executable with the specified name into this process, and
     * prepare to pass it the specified arguments. Opens the executable, reads
     * its header information, and copies sections and arguments into this
     * process's virtual memory.
     *
     * @param	name	the name of the file containing the executable.
     * @param	args	the arguments to pass to the executable.
     * @return	<tt>true</tt> if the executable was successfully loaded.
     */
    private boolean load(String name, String[] args) {
	Lib.debug(dbgProcess, "UserProcess.load(\"" + name + "\")");
	
	OpenFile executable = ThreadedKernel.fileSystem.open(name, false);
	if (executable == null) {
	    Lib.debug(dbgProcess, "\topen failed");
	    return false;
	}

	try {
	    coff = new Coff(executable);
	}
	catch (EOFException e) {
	    executable.close();
	    Lib.debug(dbgProcess, "\tcoff load failed");
	    return false;
	}

	// make sure the sections are contiguous and start at page 0
	numPages = 0;
	for (int s=0; s<coff.getNumSections(); s++) {
	    CoffSection section = coff.getSection(s);
	    if (section.getFirstVPN() != numPages) {
		coff.close();
		Lib.debug(dbgProcess, "\tfragmented executable");
		return false;
	    }
	    numPages += section.getLength();
	}

	// make sure the argv array will fit in one page
	byte[][] argv = new byte[args.length][];
	int argsSize = 0;
	for (int i=0; i<args.length; i++) {
	    argv[i] = args[i].getBytes();
	    // 4 bytes for argv[] pointer; then string plus one for null byte
	    argsSize += 4 + argv[i].length + 1;
	}
	if (argsSize > pageSize) {
	    coff.close();
	    Lib.debug(dbgProcess, "\targuments too long");
	    return false;
	}

	// program counter initially points at the program entry point
	initialPC = coff.getEntryPoint();	

	// next comes the stack; stack pointer initially points to top of it
	numPages += stackPages;
	initialSP = numPages*pageSize;

	// and finally reserve 1 page for arguments
	numPages++;

	if (!loadSections())
	    return false;

	// store arguments in last page
	int entryOffset = (numPages-1)*pageSize;
	int stringOffset = entryOffset + args.length*4;

	this.argc = args.length;
	this.argv = entryOffset;
	
	for (int i=0; i<argv.length; i++) {
	    byte[] stringOffsetBytes = Lib.bytesFromInt(stringOffset);
	    Lib.assertTrue(writeVirtualMemory(entryOffset,stringOffsetBytes) == 4);
	    entryOffset += 4;
	    Lib.assertTrue(writeVirtualMemory(stringOffset, argv[i]) ==
		       argv[i].length);
	    stringOffset += argv[i].length;
	    Lib.assertTrue(writeVirtualMemory(stringOffset,new byte[] { 0 }) == 1);
	    stringOffset += 1;
	}

	return true;
    }

    /**
     * Allocates memory for this process, and loads the COFF sections into
     * memory. If this returns successfully, the process will definitely be
     * run (this is the last step in process initialization that can fail).
     *
     * @return	<tt>true</tt> if the sections were successfully loaded.
     */
    protected boolean loadSections() {
    	int[] ppns = UserKernel.allocPhyPage(numPages);
        if (ppns == null) {
            coff.close();
            Lib.debug(dbgProcess, "\tUserProcess.loadSections:\n" + 
            					  "\t\tinsufficient physical memory");
            return false;
        }

        pageTable = new TranslationEntry[numPages];
        int it = 0;

        for(int s=0; s<coff.getNumSections(); s++) {
            CoffSection section = coff.getSection(s);
            
            Lib.debug(dbgProcess, "\tinitializing " + section.getName()
                      + " section (" + section.getLength() + " pages)");

            for(int i=0; i < section.getLength(); i++) {
                int vpn = section.getFirstVPN()+i;
                int ppn = ppns[it++];
                pageTable[vpn] = new TranslationEntry(vpn, ppn, true,
                	section.isReadOnly(), false, false);
                section.loadPage(i, ppn);
            }
        }

        for(int i = numPages - stackPages - 1; i < numPages; i++) {
            pageTable[i] = new TranslationEntry(i, ppns[i], true, false, false, false);
        }
	
		return true;
    }

    /**
     * Release any resources allocated by <tt>loadSections()</tt>.
     */
    protected void unloadSections() {
    	coff.close();
    	for(int i = 0; i < numPages; i++)
    		UserKernel.releasePhyPage(pageTable[i].ppn);
    	pageTable = null;
    }    

    /**
     * Initialize the processor's registers in preparation for running the
     * program loaded into this process. Set the PC register to point at the
     * start function, set the stack pointer register to point at the top of
     * the stack, set the A0 and A1 registers to argc and argv, respectively,
     * and initialize all other registers to 0.
     */
    public void initRegisters() {
	Processor processor = Machine.processor();

	// by default, everything's 0
	for (int i=0; i<processor.numUserRegisters; i++)
	    processor.writeRegister(i, 0);

	// initialize PC and SP according
	processor.writeRegister(Processor.regPC, initialPC);
	processor.writeRegister(Processor.regSP, initialSP);

	// initialize the first two argument registers to argc and argv
	processor.writeRegister(Processor.regA0, argc);
	processor.writeRegister(Processor.regA1, argv);
    }

    /**
     * Handle the halt() system call. 
     */
    private int handleHalt() {

	Machine.halt();
	
	Lib.assertNotReached("Machine.halt() did not halt machine!");
	return 0;
    }

   public int handleExit(int status) {
		this.status = status;
		
		for(int i = 0; i < 16; i++)
			if(openFiles[i] != null) {
				openFiles[i].close();
				openFiles[i] = null;
			}
			
		while(childrenPid != null && !childrenPid.isEmpty()) {
			int pid = childrenPid.removeFirst();
			UserProcess process = UserKernel.getProcess(pid);
			process.parentPid = 1;
		}
		
		finished.V();
		unloadSections();
		
		if(pid == 1)
			Machine.halt();
			
		UThread.finish();
		UserKernel.deleteProcess(pid);
		
		return 0;
    }

    public int handleExec(int file, int argc, int argv) {
		String name = readVirtualMemoryString(file, 256);
		if(name == null || !name.endsWith(".coff") || argc < 0)
			return -1;
			
		String[] arg = new String[argc];
		byte argAddr[] = new byte[4];
		for(int i = 0; i<argc; i++) {
			if(readVirtualMemory(argv + 4 * i, argAddr) != 4)
				return -1;
			arg[i] = readVirtualMemoryString(Lib.bytesToInt(argAddr, 0), 256);
			if(arg[i] == null)
				return -1;
		}
		
		UserProcess childProcess = UserProcess.newUserProcess();
		childProcess.parentPid = pid;
		
		if(!childProcess.execute(name, arg))
			return -1;
		childrenPid.add(childProcess.pid);
		
		return childProcess.pid;
    }
     
   public int handleJoin(int childPid, int childAddr) {
		UserProcess process = null;
		Iterator<Integer> it = childrenPid.iterator();
		
		while(it.hasNext())
			if(childPid == it.next()) {
				process = UserKernel.getProcess(childPid);
				it.remove();
				break;
			}
		
        if(process == null)
        	return -1;
        	
		process.finished.P();
		writeVirtualMemory(childAddr, Lib.bytesFromInt(process.status));

		if(process.error)
			return 0;
		return 1;
    }

    private int handleCreate(int fileAddr) {
        String filename = readVirtualMemoryString(fileAddr, 256);
        if (filename == null || removed.contains(filename))
        	return -1;
        
        int id = getEmptyOpenFile();
        if (id == -1)
        	return -1;
        
        openFiles[id] = ThreadedKernel.fileSystem.open(filename, true);
        if (openFiles[id] == null)
        	return -1;
        
        return id;
    }
    
    private int handleOpen(int fileAddr) {
        String filename = readVirtualMemoryString(fileAddr, 256);
        
        if (filename == null || removed.contains(filename))
        	return -1;
        
        int id = getEmptyOpenFile();
        if (id == -1)
        	return -1;
        
        openFiles[id] = ThreadedKernel.fileSystem.open(filename, false);
        if (openFiles[id] == null)
        	return -1;
        
        return id;
    }
    
    private int handleRead(int id, int buffer, int cnt) {
        if(id >=0 && id < 16 && openFiles[id] != null && 
        	buffer >= 0 && cnt >= 0) {
            byte buf[] = new byte[cnt];
            int len = openFiles[id].read(buf, 0, cnt);
            
            if (len == -1) return -1;
            
            return writeVirtualMemory(buffer, buf, 0, len);
        }
        
        return -1;
    }
    
    private int handleWrite(int id, int buffer, int cnt) {
    
        if(id > -1 && id < 16 && openFiles[id] != null &&
        	buffer >= 0 && cnt >= 0) {
            byte buf[] = new byte[cnt];
            int len = readVirtualMemory(buffer, buf, 0, cnt);
            return openFiles[id].write(buf, 0, len);
        }
        
        return -1;
    }
    
    private int handleClose(int id) {
        if(id < 0 || id > 15 || openFiles[id] == null)
        	return -1;
        
        String filename = openFiles[id].getName();
        openFiles[id].close();
        openFiles[id] = null;
        
        if(removed.contains(filename)) {
            for(int i = 0; i < 16; i++)
                if(openFiles[i] != null && openFiles[i].getName() == filename)
                	return 0;
            
            removed.remove(filename);
            ThreadedKernel.fileSystem.remove(filename);
        }
        
        return 0;
    }
    
    private int handleUnlink(int fileAddr) {
        String filename = readVirtualMemoryString(fileAddr, 256);
        
        if(filename == null)
        	return -1;
        
        for(int i=0; i<16; i++)
            if(openFiles[i] != null && openFiles[i].getName() == filename) {
		        removed.add(filename);
		        return 0;
            }
        
        if(!ThreadedKernel.fileSystem.remove(filename))
        	return -1;
        
        return 0;
        
    }

    private int getEmptyOpenFile() {
        for(int i = 0; i < 16; i++)
            if(openFiles[i] == null)
            	return i;
        return -1;
    }


    private static final int
        syscallHalt = 0,
	syscallExit = 1,
	syscallExec = 2,
	syscallJoin = 3,
	syscallCreate = 4,
	syscallOpen = 5,
	syscallRead = 6,
	syscallWrite = 7,
	syscallClose = 8,
	syscallUnlink = 9;

    /**
     * Handle a syscall exception. Called by <tt>handleException()</tt>. The
     * <i>syscall</i> argument identifies which syscall the user executed:
     *
     * <table>
     * <tr><td>syscall#</td><td>syscall prototype</td></tr>
     * <tr><td>0</td><td><tt>void halt();</tt></td></tr>
     * <tr><td>1</td><td><tt>void exit(int status);</tt></td></tr>
     * <tr><td>2</td><td><tt>int  exec(char *name, int argc, char **argv);
     * 								</tt></td></tr>
     * <tr><td>3</td><td><tt>int  join(int pid, int *status);</tt></td></tr>
     * <tr><td>4</td><td><tt>int  creat(char *name);</tt></td></tr>
     * <tr><td>5</td><td><tt>int  open(char *name);</tt></td></tr>
     * <tr><td>6</td><td><tt>int  read(int fd, char *buffer, int size);
     *								</tt></td></tr>
     * <tr><td>7</td><td><tt>int  write(int fd, char *buffer, int size);
     *								</tt></td></tr>
     * <tr><td>8</td><td><tt>int  close(int fd);</tt></td></tr>
     * <tr><td>9</td><td><tt>int  unlink(char *name);</tt></td></tr>
     * </table>
     * 
     * @param	syscall	the syscall number.
     * @param	a0	the first syscall argument.
     * @param	a1	the second syscall argument.
     * @param	a2	the third syscall argument.
     * @param	a3	the fourth syscall argument.
     * @return	the value to be returned to the user.
     */
    public int handleSyscall(int syscall, int a0, int a1, int a2, int a3) {
		switch (syscall) {
			case syscallHalt: return handleHalt();
			case syscallExit: return handleExit(a0);
			case syscallExec: return handleExec(a0, a1, a2);
			case syscallJoin: return handleJoin(a0, a1);
		    case syscallCreate: return handleCreate(a0);
		    case syscallOpen: return handleOpen(a0);
		    case syscallRead: return handleRead(a0, a1, a2);
		    case syscallWrite: return handleWrite(a0, a1, a2);
		    case syscallClose: return handleClose(a0);
		    case syscallUnlink: return handleUnlink(a0);
		    
			default:
				Lib.debug(dbgProcess, "Unknown syscall " + syscall);
				Lib.assertNotReached("Unknown system call!");
		}
		return 0;
    }

    /**
     * Handle a user exception. Called by
     * <tt>UserKernel.exceptionHandler()</tt>. The
     * <i>cause</i> argument identifies which exception occurred; see the
     * <tt>Processor.exceptionZZZ</tt> constants.
     *
     * @param	cause	the user exception that occurred.
     */
    public void handleException(int cause) {
		Processor processor = Machine.processor();

		switch (cause) {
			case Processor.exceptionSyscall:
				int result = handleSyscall(processor.readRegister(Processor.regV0),
							   processor.readRegister(Processor.regA0),
							   processor.readRegister(Processor.regA1),
							   processor.readRegister(Processor.regA2),
							   processor.readRegister(Processor.regA3)
							   );
				processor.writeRegister(Processor.regV0, result);
				processor.advancePC();
				break;				       
							   
			default: {
				Lib.debug(dbgProcess, "Unexpected exception: " +
					  Processor.exceptionNames[cause]);
				Lib.assertNotReached("Unexpected exception");
				handleExit(-1);
				error = true;
			}
		}
    }

    /** The program being run by this process. */
    protected Coff coff;

    /** This process's page table. */
    protected TranslationEntry[] pageTable;
    /** The number of contiguous pages occupied by the program. */
    protected int numPages;

    /** The number of pages in the program's stack. */
    protected final int stackPages = 8;
    
    private int initialPC, initialSP;
    private int argc, argv;
	
    private static final int pageSize = Processor.pageSize;
    private static final char dbgProcess = 'a';
    
    
    
    protected static HashSet<String> removed = new HashSet<String>();
    private OpenFile openFiles[] = new OpenFile[16];
    
    protected Semaphore finished;
    private int pid;
    
    protected boolean error = false;
    protected int status;
    private int parentPid;
    private LinkedList<Integer> childrenPid = new LinkedList<Integer>();
}
