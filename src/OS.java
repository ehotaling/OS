// OS.java
import java.util.ArrayList;
import java.util.List;

public class OS {

    // kernel instance
    private static Kernel ki;
    // list for system call parameters
    public static List<Object> parameters = new ArrayList<>();

    // A lock for all system calls and a reference to the current result
    public static final Object sysCallLock = new Object();
    public static SysCallResult currentSysCallResult;

    // enum for system call types
    public enum CallType {
        SwitchProcess, SendMessage, Open, Close, Read, Seek, Write,
        GetMapping, CreateProcess, Sleep, GetPID, AllocateMemory,
        FreeMemory, GetPIDByName, WaitForMessage, Exit
    }
    // current system call type
    public static CallType currentCall;

    public enum PriorityType { realtime, interactive, background }

    // start the kernel (if not null)
    private static void startTheKernel() throws InterruptedException {
        if (ki != null) {
            ki.start();
        }
    }

    // Startup: initialize the kernel and create the init process
    public static void Startup(UserlandProcess init) throws InterruptedException {
        System.out.println("OS.Startup: Initializing Kernel");
        ki = new Kernel();
        // pass kernel reference to scheduler
        ki.getScheduler().setKernel(ki);
        System.out.println("OS.Startup: Kernel initialized");
        System.out.println("OS.Startup: Creating InitProcess");
        int pid = CreateProcess(init, PriorityType.interactive);
        System.out.println("OS.Startup: InitProcess created with PID " + pid);
    }

    // CreateProcess without explicit priority uses interactive
    public static int CreateProcess(UserlandProcess up) throws InterruptedException {
        return CreateProcess(up, PriorityType.interactive);
    }

    // CreateProcess: use a per‑call result object so each call waits for its own result.
    public static int CreateProcess(UserlandProcess up, PriorityType priority) throws InterruptedException {
        SysCallResult result = new SysCallResult();
        synchronized(sysCallLock) {
            parameters.clear();
            parameters.add(up);
            parameters.add(priority);
            currentCall = CallType.CreateProcess;
            currentSysCallResult = result;
            startTheKernel();
        }
        System.out.println("OS.CreateProcess: Waiting for retVal for process " + up.getClass().getSimpleName());
        result.latch.await();  // block until the kernel sets the value
        int pid = (int) result.value;
        synchronized(sysCallLock) {
            currentSysCallResult = null;
        }
        System.out.println("OS.CreateProcess: Returning PID " + pid + " for process " + up.getClass().getSimpleName());
        return pid;
    }

    public static void switchProcess() throws InterruptedException {
        System.out.println("OS.switchProcess: Switch process requested.");
        synchronized(sysCallLock) {
            parameters.clear();
            currentCall = CallType.SwitchProcess;
            startTheKernel();
        }
        System.out.println("OS.switchProcess: startTheKernel() returned.");
    }

    public static int GetPID() throws InterruptedException {
        SysCallResult result = new SysCallResult();
        synchronized(sysCallLock) {
            parameters.clear();
            currentCall = CallType.GetPID;
            currentSysCallResult = result;
            startTheKernel();
        }
        result.latch.await();
        int pid = (int) result.value;
        synchronized(sysCallLock) {
            currentSysCallResult = null;
        }
        return pid;
    }

    public static void Exit() throws InterruptedException {
        System.out.println("OS.Exit: Exit system call requested");
        synchronized(sysCallLock) {
            parameters.clear();
            currentCall = CallType.Exit;
            startTheKernel();
        }
        System.out.println("OS.Exit: startTheKernel returned");
    }

    public static void Sleep(int mills) throws InterruptedException {
        synchronized(sysCallLock) {
            parameters.clear();
            parameters.add(mills);
            currentCall = CallType.Sleep;
            startTheKernel();
        }
    }

    // Device calls

    public static int Open(String s) throws InterruptedException {
        SysCallResult result = new SysCallResult();
        synchronized(sysCallLock) {
            parameters.clear();
            parameters.add(s);
            currentCall = CallType.Open;
            currentSysCallResult = result;
            startTheKernel();
        }
        result.latch.await();
        int ret = (int) result.value;
        synchronized(sysCallLock) {
            currentSysCallResult = null;
        }
        return ret;
    }

    public static void Close(int id) throws InterruptedException {
        synchronized(sysCallLock) {
            parameters.clear();
            parameters.add(id);
            currentCall = CallType.Close;
            startTheKernel();
        }
    }

    public static byte[] Read(int id, int size) throws InterruptedException {
        SysCallResult result = new SysCallResult();
        synchronized(sysCallLock) {
            parameters.clear();
            parameters.add(id);
            parameters.add(size);
            currentCall = CallType.Read;
            currentSysCallResult = result;
            startTheKernel();
        }
        result.latch.await();
        byte[] ret = (byte[]) result.value;
        synchronized(sysCallLock) {
            currentSysCallResult = null;
        }
        return ret;
    }

    public static void Seek(int id, int to) throws InterruptedException {
        synchronized(sysCallLock) {
            parameters.clear();
            parameters.add(id);
            parameters.add(to);
            currentCall = CallType.Seek;
            startTheKernel();
        }
    }

    public static int Write(int id, byte[] data) throws InterruptedException {
        SysCallResult result = new SysCallResult();
        synchronized(sysCallLock) {
            parameters.clear();
            parameters.add(id);
            parameters.add(data);
            currentCall = CallType.Write;
            currentSysCallResult = result;
            startTheKernel();
        }
        result.latch.await();
        int ret = (int) result.value;
        synchronized(sysCallLock) {
            currentSysCallResult = null;
        }
        return ret;
    }

    // Message calls (stubs)

    public static void SendMessage(KernelMessage km) {
        // implementation pending
    }

    public static KernelMessage WaitForMessage() {
        return null;
    }

    public static int GetPidByName(String name) {
        return 0; // implementation pending
    }

    // Memory calls (stubs)

    public static void GetMapping(int virtualPage) {
        // implementation pending
    }

    public static int AllocateMemory(int size) {
        return 0; // implementation pending
    }

    public static boolean FreeMemory(int pointer, int size) {
        return false; // implementation pending
    }
}
