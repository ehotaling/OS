public class Main {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("Main: Starting OS with Memory Tests..."); // Debug print
        OS.Startup(new MemoryTestInitProcess());
        System.out.println("Main: OS Startup complete."); // Debug print
    }
}