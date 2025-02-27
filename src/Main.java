public class Main {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("Main: Starting OS..."); // Debug print
        OS.Startup(new InitProcess());
        System.out.println("Main: OS Startup complete."); // Debug print
    }
}