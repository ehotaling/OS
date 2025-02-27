import java.util.ArrayList;
import java.util.List;

// Gateway between the userland thread and the kernel thread.
public class OS {
    private static Kernel ki; // The one and only one instance of the kernel.

    // A static array list of parameters to the function; we don’t know what they will be, so we will make it
    // an arraylist of Object.
    public static List<Object> parameters = new ArrayList<>();

    // The return value. In a similar way, we don’t know what the return value type will be,
    // so make it a static Object
    public static Object retVal;

    // An enum of what function to call
    public enum CallType {SwitchProcess,SendMessage, Open, Close, Read, Seek, Write, GetMapping, CreateProcess, Sleep, GetPID, AllocateMemory, FreeMemory, GetPIDByName, WaitForMessage, Exit}

    // A static instance of that enum
    public static CallType currentCall;



    // For switching to the Kernal
    private static void startTheKernel() {
        // Can only have one kernel
        if (!ki.thread.isAlive()) {
            ki.thread.start();
        }
        ki.start();
        // If the scheduler has a currentlyRunning process, stop it.
        Scheduler scheduler = ki.getScheduler();
        if (scheduler.hasRunningProcess()) {
            scheduler.runningProcess.stop();
        }
    }


    public static void switchProcess() {
        parameters.clear();
        currentCall = CallType.SwitchProcess;
        startTheKernel();
    }

    public static void Startup(UserlandProcess init) throws InterruptedException {
        ki = new Kernel();
        CreateProcess(init, PriorityType.interactive);
        // CreateProcess(new IdleProcess(), PriorityType.background); A dedicated PCB is created in Schedulers constructor
    }

    public enum PriorityType {realtime, interactive, background}

    public static int CreateProcess(UserlandProcess up) throws InterruptedException {
        return CreateProcess(up,PriorityType.interactive);
    }


    public static int CreateProcess(UserlandProcess up, PriorityType priority) throws InterruptedException {
        parameters.clear();
        parameters.add(up);
        parameters.add(priority);
        currentCall = CallType.CreateProcess;
        startTheKernel();
        // Ensures that Startup() waits for the kernel to complete the system call before returning.
        while (OS.retVal == null) {
            Thread.sleep(10);
        }
        return (int) retVal;
    }

    public static int GetPID() {
        parameters.clear();
        currentCall = CallType.GetPID;
        startTheKernel();
        // Wait until the kernel has processed the call.
        while (retVal == null) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return (int) retVal;
    }


    public static void Exit() {
        parameters.clear();
        currentCall = CallType.Exit;
        startTheKernel();
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
