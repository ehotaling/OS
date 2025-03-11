public class TestMain {
    public static void main(String[] args) {
        try {
            // start the OS with TestInitProcess as the initial process
            OS.Startup(new TestInitProcess());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
