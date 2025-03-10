// This is the class that manages what happens on startup
public class InitProcess extends UserlandProcess {
    public InitProcess() {}


    @Override
    public void main() throws InterruptedException {
        System.out.println("InitProcess.main: Init process started.");

        System.out.println("InitProcess.main: Creating HelloWorld.");
        OS.CreateProcess(new HelloWorld(), OS.PriorityType.realtime);
        System.out.println("InitProcess.main: HelloWorld created.");
        cooperate();

        System.out.println("InitProcess.main: Creating GoodByeWorld.");
        OS.CreateProcess(new GoodbyeWorld(), OS.PriorityType.realtime);
        System.out.println("InitProcess.main: GoodByeWorld created.");
        cooperate();

        System.out.println("InitProcess.main: Creating testDemotion.");
        OS.CreateProcess(new testDemotion(), OS.PriorityType.realtime);
        System.out.println("InitProcess.main: testDemotion created.");
        cooperate();

        System.out.println("InitProcess.main: Creating SleepTestProcess.");
        OS.CreateProcess(new SleepTestProcess(), OS.PriorityType.realtime);
        System.out.println("InitProcess.main: SleepTestProcess created.");
        cooperate();

        System.out.println("InitProcess.main: Creating IdleProcess...");
        OS.CreateProcess(new IdleProcess(), OS.PriorityType.background);
        System.out.println("InitProcess.main: IdleProcess created.");
        cooperate();

        System.out.println("InitProcess.main: Exiting.");
        OS.setInitProcessDone(true);
        OS.Exit();
    }
}
