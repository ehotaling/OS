
// I recommend you save console output to a file because there is a lot of output for this....

public class Main {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("Main: Starting OS with Memory Tests..."); // Debug print
        OS.Startup(new VirtualMemoryTestInitProcess());
        System.out.println("Main: OS Startup complete."); // Debug print
    }
}