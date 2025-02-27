// This is the class that manages what happens on startup

public class InitProcess extends UserlandProcess {
    public InitProcess() {}

    @Override
    public void main() throws InterruptedException {
        System.out.println("InitProcess.main: Init process started."); // Debug print
        // Init should create new processes: HelloWorld and GoodbyeWorld.
//        OS.CreateProcess(new HelloWorld());
//        OS.CreateProcess(new GoodbyeWorld());
        System.out.println("InitProcess.main: Creating RealTimeTestProcess."); // Debug print
        OS.CreateProcess(new RealTimeTestProcess(), OS.PriorityType.realtime);

        Thread.sleep(10);
        System.out.println("InitProcess.main: RealTimeTestProcess created, exiting."); // Debug print

        // Create a real-time process that intentionally calls Sleep()
//        System.out.println("InitProcess.main: Creating SleepTestProcess..");
//        OS.CreateProcess(new SleepTestProcess(), OS.PriorityType.realtime);
//        System.out.println("InitProcess.main: SleepTestProcess created, exiting.");

        OS.Exit();
    }
}
