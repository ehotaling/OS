import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

public class OS {

    // kernel instance
    private static Kernel ki;
    // list for system call parameters
    public static List<Object> parameters = new ArrayList<>();
    // return value for system calls
    public static Object retVal;
    // enum for system call types
    public enum CallType {SwitchProcess, SendMessage, Open, Close, Read, Seek, Write, GetMapping, CreateProcess, Sleep, GetPID, AllocateMemory, FreeMemory, GetPIDByName, WaitForMessage, Exit}
    // current system call type
    public static CallType currentCall;



    // start kernel
    private static void startTheKernel() throws InterruptedException {
        if (ki != null) {
            ki.start();
            if (ki.getScheduler().getCurrentlyRunning() != null) {
                ki.getScheduler().getCurrentlyRunning().stop();
            }
        }
    }

    // startup OS with init process and initialize kernel and scheduler
    public static void Startup(UserlandProcess init) throws InterruptedException {
        System.out.println("OS.Startup: OS Startup initiated");
        System.out.println("OS.Startup: Initializing Kernel");
        ki = new Kernel();
        // pass kernel reference to scheduler
        ki.getScheduler().setKernel(ki);
        System.out.println("OS.Startup: Kernel initialized");
        System.out.println("OS.Startup: Creating InitProcess");
        CreateProcess(init, PriorityType.interactive);
        // Ensure kernel has time to start properly
        while (retVal == null) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        System.out.println("OS.Startup: InitProcess created");
    }

    public enum PriorityType {realtime, interactive, background}

    public static int CreateProcess(UserlandProcess up) throws InterruptedException {
        return CreateProcess(up, PriorityType.interactive);
    }

    public static int CreateProcess(UserlandProcess up, PriorityType priority) throws InterruptedException {
        System.out.println("OS.CreateProcess: Request to create process " + up.getClass().getSimpleName() + " with priority " + priority);
        parameters.clear();
        parameters.add(up);
        parameters.add(priority);
        currentCall = CallType.CreateProcess;
        startTheKernel();
        System.out.println("OS.CreateProcess: Waiting for retVal for process " + up.getClass().getSimpleName());
        while (retVal == null) {
            Thread.sleep(10);
        }
        System.out.println("OS.CreateProcess: retVal received: " + retVal + " for process " + up.getClass().getSimpleName());
        int result = (int) retVal;
        retVal = null;
        System.out.println("OS.CreateProcess: Returning PID " + result + " for process " + up.getClass().getSimpleName());
        return result;
    }


    public static void switchProcess() throws InterruptedException {
        System.out.println("OS.switchProcess: Switch process requested."); // Debug print
        parameters.clear();
        currentCall = CallType.SwitchProcess;
        startTheKernel();
        System.out.println("OS.switchProcess: startTheKernel() returned."); // Debug print
    }


    public static int GetPID() throws InterruptedException {
        parameters.clear();
        currentCall = CallType.GetPID;
        startTheKernel();
        while (retVal == null) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException _) {}
        }
        return (int) retVal;
    }

    public static void Exit() throws InterruptedException {
        System.out.println("OS.Exit: Exit system call requested");
        parameters.clear();
        currentCall = CallType.Exit;
        startTheKernel();
        System.out.println("OS.Exit: startTheKernel returned");
    }

    public static void Sleep(int mills) throws InterruptedException {
        parameters.clear();
        parameters.add(mills);
        currentCall = CallType.Sleep;
        startTheKernel();
    }

    // Device calls

    public static int Open(String s) throws InterruptedException {
        parameters.clear();
        parameters.add(s);
        currentCall = CallType.Open;
        startTheKernel();
        while (retVal == null) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {}
        }
        return (int) retVal;
    }

    public static void Close(int id) throws InterruptedException {
        parameters.clear();
        parameters.add(id);
        currentCall = CallType.Close;
        startTheKernel();
    }

    public static byte[] Read(int id, int size) throws InterruptedException {
        parameters.clear();
        parameters.add(id);
        parameters.add(size);
        currentCall = CallType.Read;
        startTheKernel();
        while (retVal == null) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException _) {
            }
        }
        return (byte[]) retVal;
    }

    public static void Seek(int id, int to) throws InterruptedException {
        parameters.clear();
        parameters.add(id);
        parameters.add(to);
        currentCall = CallType.Seek;
        startTheKernel();
    }

    public static int Write(int id, byte[] data) throws InterruptedException {
        parameters.clear();
        parameters.add(id);
        parameters.add(data);
        currentCall = CallType.Write;
        startTheKernel();
        while (retVal == null) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {}
        }
        return (int) retVal;
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
