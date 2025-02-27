// This is the class that manages what happens on startup

public class InitProcess extends UserlandProcess {
    public InitProcess() {}

    @Override
    public void main() throws InterruptedException {
        // Init should create new processes: HelloWorld and GoodbyeWorld.
//        OS.CreateProcess(new HelloWorld());
//        OS.CreateProcess(new GoodbyeWorld());
        OS.CreateProcess(new RealTimeTestProcess(), OS.PriorityType.realtime);
        OS.Exit();
    }
}
