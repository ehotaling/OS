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

    public static void startTheKernel() {
        ki.start();
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
        // Create the init process.
        System.out.println("OS.Startup: Creating InitProcess...");
        CreateProcess(init, PriorityType.interactive);
        System.out.println("OS.Startup: InitProcess created.");
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