public class RealTimeTestProcess extends UserlandProcess {
    @Override
    public void main() throws InterruptedException {

        // TODO this breaks the program
        int pid = OS.GetPID();
        // System.out.println("RealTimeTestProcess started with PID: " + pid);
        System.out.println("Realtimetest process started for the first time.");
        cooperate();
        double dummy = 0;
        while (true) {
            for (double i = 0; i < 100_000_000; i++) {
                 dummy = Math.sin(i);
                 for (double j = 0; j < 100_000_000; j++) {
                     dummy = Math.cos(i+j);
                     for (double k = 0; k < 100_000_000; k++) {
                         dummy = Math.cos(i+k) * dummy;
                         for (double l = 0; l < 100_000_000; l++) {
                             dummy = Math.cos(i+l) * dummy;
                             for (double n = 0; n < 100_000_000; n++) {
                                 dummy = Math.cos(i+n) * dummy;
                             }
                         }
                     }
                 }
            }
            System.out.println("Realtimetest process cooperating.");
            cooperate();
        }
    }
}
