public class testDemotion extends UserlandProcess {
    public void main() throws InterruptedException {
        // Simulate non-cooperative behavior for several iterations.
        // In each iteration, the process runs longer than the quantum (250ms)
        // so the TimerTask will mark it as expired and increment its timeout count.
        for (int i = 0; i < 6; i++) {
            System.out.println("Iteration " + i + ": Running without yielding.");
            try {
                // Sleep longer than the quantum to force a timeout
                Thread.sleep(300);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // Intentionally do not call cooperate() here.
            // The process does not voluntarily yield control,
            // The timer will keep counting timeouts and eventually trigger a demotion.
        }

        // Process should have been demoted at this point.
        // Now yielding control once to let the scheduler do a switch.
        System.out.println("Now yielding control after potential demotion.");
        cooperate();

        // After being demoted, run in a cooperative manner:
        // the process now calls cooperate() promptly to avoid further timeout penalties.
        while (true) {
            System.out.println("Process running at demoted priority.");
            try {
                Thread.sleep(50); // simulate some work
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // Cooperate immediately to prevent additional timeouts.
            cooperate();
        }
    }
}
