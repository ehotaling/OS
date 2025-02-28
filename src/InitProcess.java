// This is the class that manages what happens on startup
public class InitProcess extends UserlandProcess {
    public InitProcess() {}


    @Override
    public void main() throws InterruptedException {
        System.out.println("InitProcess.main: Init process started.");
        Thread.sleep(1000); // Give time for initialization to complete

        // Ensure InitProcess gets CPU time and isn't preempted before finishing
        System.out.println("InitProcess.main: Creating HelloWorld.");
        OS.CreateProcess(new HelloWorld(), OS.PriorityType.realtime);
        System.out.println("InitProcess.main: HelloWorld created.");

        System.out.println("InitProcess.main: Creating GoodByeWorld.");
        OS.CreateProcess(new GoodbyeWorld(), OS.PriorityType.realtime);
        System.out.println("InitProcess.main: GoodByeWorld created.");


        System.out.println("InitProcess.main: Creating RealTimeTestProcess.");
        OS.CreateProcess(new RealTimeTestProcess(), OS.PriorityType.realtime);
        System.out.println("InitProcess.main: RealTimeTestProcess created.");


        OS.setInitProcessFinished(); //  Mark InitProcess as finished before exiting
        // Ensure InitProcess actually exits and is not suspended
        System.out.println("InitProcess.main: Exiting.");
        OS.Exit();
    }
}
