package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;

import java.util.LinkedList;

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
            public void run() {
                exceptionHandler();
            }
        });

        offsetLen = 0;
        for (offsetLen = 0; ; ++offsetLen)
            if ((Processor.pageSize >> offsetLen) == 1) {
                offsetMask = (1 << offsetLen) - 1;
                break;
            }

        pageLock = new Lock();

        int numPhysPages = Machine.processor().getNumPhysPages();
        availablePages = new LinkedList<Integer>();
        for (int i = 0; i < numPhysPages; ++i)
            availablePages.add(new Integer(i));
    }

    /**
     * Test the console device.
     */
    public void selfTest() {
        super.selfTest();

        System.out.println("Testing the console device. Typed characters");
        System.out.println("will be echoed until q is typed.");

        char c;

        do {
            c = (char) console.readByte(true);
            console.writeByte(c);
        } while (c != 'q');

        System.out.println("");
    }

    /**
     * Returns the current process.
     * 
     * @return the current process, or <tt>null</tt> if no process is current.
     */
    public static UserProcess currentProcess() {
        if (!(KThread.currentThread() instanceof UThread))
            return null;

        return ((UThread) KThread.currentThread()).process;
    }

    /**
     * The exception handler. This handler is called by the processor whenever a
     * user instruction causes a processor exception.
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
     * @see nachos.machine.Machine#getShellProgramName
     */
    public void run() {
        super.run();

        UserProcess process = UserProcess.newUserProcess();
        root = process;

        String shellProgram = Machine.getShellProgramName();
        Lib.assertTrue(process.execute(shellProgram, new String[] {}));

        KThread.finish();
    }

    protected UserProcess root;

    /**
     * Terminate this kernel. Never returns.
     */
    public void terminate() {
        super.terminate();
    }

    public static int getVirtualPageNumber(int vaddr) {
        return Machine.processor().pageFromAddress(vaddr);
    }

    public static int getOffset(int vaddr) {
        return vaddr & offsetMask;
    }

    public static int makeAddress(int pn, int offset) {
        return Machine.processor().makeAddress(pn, offset);
    }

    public static int newPage() {
        int ret = -1;

        pageLock.acquire();
        if (availablePages.size() > 0)
            ret = availablePages.removeFirst().intValue();
        pageLock.release();

        return ret;
    }

    public static boolean deletePage(int ppn) {
        boolean ret = false;

        pageLock.acquire();
        availablePages.add(new Integer(ppn));
        ret = true;
        pageLock.release();

        return ret;
    }

    public String absoluteFileName(String s) {
        return s;
    }

    /** Globally accessible reference to the synchronized console. */
    public static SynchConsole console;

    private static int offsetLen, offsetMask;

    private static Lock pageLock;
    private static LinkedList<Integer> availablePages;
}
