public class RealTimeTestProcess extends UserlandProcess {
    @Override
    public void main() throws InterruptedException {
        // int pid = OS.GetPID();
        // System.out.println("RealTimeTestProcess started with PID: " + pid);
        System.out.println("Realtimetest process started for the first time.");
        cooperate();
        while (true) {
            for (double i = 0; i < 100_000; i++) {
                double dummy = Math.sin(i);
            }
            cooperate();
        }
    }
}
