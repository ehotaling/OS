public class RealTimeTestProcess extends UserlandProcess {
    @Override
    public void main() throws InterruptedException {

        // TODO this breaks the program
        // int pid = OS.GetPID();
        // System.out.println("RealTimeTestProcess started with PID: " + pid);
        System.out.println("RealTimeTestProcess started for the first time.");
        cooperate();
        double dummy = 0;
        while (true) {
            System.out.println("RealTimeTestProcess cooperating.");
            Thread.sleep(500);
            cooperate();
        }
    }
}
