public class MessagesInitProcess extends UserlandProcess {
    @Override
    public void main() throws InterruptedException {
        System.out.println("MessagesInitProcess.main: Starting message passing test.");

        // Create the Ping process.
        System.out.println("MessagesInitProcess.main: Creating Ping process.");
        OS.CreateProcess(new Ping(), OS.PriorityType.realtime);
        cooperate();

        // Create the Pong process.
        System.out.println("MessagesInitProcess.main: Creating Pong process.");
        OS.CreateProcess(new Pong(), OS.PriorityType.realtime);
        cooperate();

        System.out.println("MessagesInitProcess.main: Message passing test setup complete. Exiting MessagesInitProcess.");
        OS.Exit();
    }
}

