public class InitProcess extends UserlandProcess {
    @Override
    public void main() throws InterruptedException {
        System.out.println("InitProcess.main: Starting message passing test.");

        // Create the Ping process.
        System.out.println("InitProcess.main: Creating Ping process.");
        OS.CreateProcess(new Ping(), OS.PriorityType.realtime);
        cooperate();

        // Create the Pong process.
        System.out.println("InitProcess.main: Creating Pong process.");
        OS.CreateProcess(new Pong(), OS.PriorityType.realtime);
        cooperate();

        System.out.println("InitProcess.main: Message passing test setup complete. Exiting InitProcess.");
        OS.Exit();
    }
}

