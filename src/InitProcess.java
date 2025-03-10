// This is the class that manages what happens on startup
public class InitProcess extends UserlandProcess {
    public InitProcess() {}


    @Override
    public void main() throws InterruptedException {
        System.out.println("InitProcess.main: Init process started.");

        System.out.println("InitProcess.main: Creating HelloWorld.");
        OS.CreateProcess(new HelloWorld(), OS.PriorityType.realtime);
        System.out.println("InitProcess.main: HelloWorld created.");

        System.out.println("InitProcess.main: Creating GoodByeWorld.");
        OS.CreateProcess(new GoodbyeWorld(), OS.PriorityType.realtime);
        System.out.println("InitProcess.main: GoodByeWorld created.");

        System.out.println("InitProcess.main: Creating testDemotion.");
        OS.CreateProcess(new testDemotion(), OS.PriorityType.realtime);
        System.out.println("InitProcess.main: testDemotion created.");

        System.out.println("InitProcess.main: Creating SleepTestProcess.");
        OS.CreateProcess(new SleepTestProcess(), OS.PriorityType.realtime);
        System.out.println("InitProcess.main: SleepTestProcess created.");

        System.out.println("InitProcess.main: Creating IdleProcess...");
        OS.CreateProcess(new IdleProcess(), OS.PriorityType.background);
        System.out.println("InitProcess.main: IdleProcess created.");

        // Give time for all startup processes to be before exiting (which will lead to scheduler.switchProcess().
        Thread.sleep(1000);
        System.out.println("InitProcess.main: Exiting.");
        OS.Exit();
    }
}
