import java.util.ArrayList;
import java.util.List;

// Gateway between the userland thread and the kernel thread.
public class OS {

    private static boolean initProcessFinished = false;

    private static Kernel ki; // The one and only one instance of the kernel.

    // A static array list of parameters to the function; we don’t know what they will be, so we will make it
    // an arraylist of Object.
    public static List<Object> parameters = new ArrayList<>();

    // The return value. In a similar way, we don’t know what the return value type will be,
    // so make it a static Object
    public static Object retVal;

    // An enum of what function to call
    public enum CallType {SwitchProcess,SendMessage, Open, Close, Read, Seek, Write, GetMapping, CreateProcess, Sleep, GetPID, AllocateMemory, FreeMemory, GetPIDByName, WaitForMessage, Exit}

    public static boolean isInitProcessFinished() {
        return initProcessFinished;
    }

    public static void setInitProcessFinished() {
        System.out.println( "OS.setInitProcessFinished: Setting init process finished.");
        initProcessFinished = true;
    }

    // A static instance of that enum
    public static CallType currentCall;
    private static void startTheKernel() {
        System.out.println("OS.startTheKernel: Starting kernel initiation sequence.");
        if (!ki.thread.isAlive()) {
            System.out.println("OS.startTheKernel: Kernel thread is not alive, starting kernel thread.");
            ki.thread.start();
        } else {
            System.out.println("OS.startTheKernel: Kernel thread is already alive.");
        }
        if (OS.currentCall != null) {
            System.out.println("OS.startTheKernel: Starting kernel instance for system call: " + OS.currentCall);
            ki.start();
        }
        Scheduler scheduler = ki.getScheduler();
        if (scheduler.hasRunningProcess()) {
            System.out.println("OS.startTheKernel: Stopping running process: " + scheduler.runningProcess.userlandProcess.getClass().getSimpleName());
            scheduler.runningProcess.stop();
        } else {
            System.out.println("OS.startTheKernel: No running process to stop.");
        }
        System.out.println("OS.startTheKernel: Kernel start sequence complete.");
    }




    public static void switchProcess() {
        System.out.println("OS.switchProcess: Switch process requested."); // Debug print
        parameters.clear();
        currentCall = CallType.SwitchProcess;
        startTheKernel();
        System.out.println("OS.switchProcess: startTheKernel() returned."); // Debug print
    }

    public static void Startup(UserlandProcess init) throws InterruptedException {
        System.out.println("OS.Startup: OS Startup initiated.");
        System.out.println("OS.Startup: Initializing Kernel...");
        ki = new Kernel();
        System.out.println("OS.Startup: Kernel initialized.");

        // Create the idle process first.
        System.out.println("OS.Startup: Creating IdleProcess...");
        int idleProcessPID = CreateProcess(new IdleProcess(), PriorityType.background);
        System.out.println("OS.Startup: CreateProcess for IdleProcess returned PID: " + idleProcessPID);
        if (idleProcessPID == -1) {
            System.err.println("OS.Startup: Error. CreateProcess for IdleProcess failed.");
        }
        System.out.println("OS.Startup: IdleProcess creation requested.");

        // Then create the init process.
        System.out.println("OS.Startup: Creating InitProcess...");
        CreateProcess(init, PriorityType.interactive);
        System.out.println("OS.Startup: InitProcess creation requested.");

        // Wait for InitProcess to complete before continuing
        while (!isInitProcessFinished()) {
            try {
                Thread.sleep(10); // Give time for InitProcess to complete
            } catch (InterruptedException e) {
                System.out.println("OS.Startup: Interrupted while waiting for InitProcess.");
            }
        }
    }


    public enum PriorityType {realtime, interactive, background}

    public static int CreateProcess(UserlandProcess up) throws InterruptedException {
        return CreateProcess(up,PriorityType.interactive);
    }


    public static int CreateProcess(UserlandProcess up, PriorityType priority) throws InterruptedException {
        System.out.println("OS.CreateProcess: Request to create process of name: " + up.getClass().getSimpleName() + ", priority: " + priority); // Debug print
        parameters.clear();
        parameters.add(up);
        parameters.add(priority);
        currentCall = CallType.CreateProcess;
        System.out.println("OS.CreateProcess: Calling startTheKernel()."); // Debug print
        startTheKernel();
        System.out.println("OS.CreateProcess: startTheKernel() returned, waiting for retVal."); // Debug print
        // Ensures that Startup() waits for the kernel to complete the system call before returning.
        while (OS.retVal == null) {
            Thread.sleep(10);
        }
        System.out.println("OS.CreateProcess: retVal received: " + OS.retVal); // Debug print
        int result = (int) OS.retVal;
        OS.retVal = null; // Reset retVal for next syscall
        System.out.println("OS.CreateProcess: Returning PID: " + result); // Debug print
        return result;
    }

    public static int GetPID() {
        parameters.clear();
        currentCall = CallType.GetPID;
        startTheKernel();

        while (retVal == null) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException _) {}
        }
        int pid = (int) retVal;
        retVal = null;
        return pid;
    }

    public static void Exit() {
        System.out.println("OS.Exit: Exit system call requested."); // Debug print
        parameters.clear();
        currentCall = CallType.Exit;
        startTheKernel();
        System.out.println("OS.Exit: startTheKernel() returned."); // Debug print
    }

    public static void Sleep(int mills) {
        parameters.clear();
        parameters.add(mills);
        currentCall = CallType.Sleep;
        startTheKernel();
    }

    // Devices
    public static int Open(String s) {
        return 0;
    }

    public static void Close(int id) {
    }

    public static byte[] Read(int id, int size) {
        return null;
    }

    public static void Seek(int id, int to) {
    }

    public static int Write(int id, byte[] data) {
        return 0;
    }

    // Messages
    public static void SendMessage(KernelMessage km) {
    }

    public static KernelMessage WaitForMessage() {
        return null;
    }

    public static int GetPidByName(String name) {
        return 0; // Change this
    }

    // Memory
    public static void GetMapping(int virtualPage) {
    }

    public static int AllocateMemory(int size ) {
        return 0; // Change this
    }

    public static boolean FreeMemory(int pointer, int size) {
        return false; // Change this
    }
}