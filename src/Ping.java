public class Ping extends UserlandProcess {
    public void main() throws InterruptedException {
        try {
            // Get our own PID and the PID of Pong.
            int myPid = OS.GetPID();
            int pongPid = OS.GetPidByName("Pong");
            System.out.println("I am PING, Pong PID is " + pongPid);

            int messageType = 0;
            while (true) {
                // Create a message for Pong.
                String data = "Ping message " + messageType;
                KernelMessage km = new KernelMessage(myPid, pongPid, messageType, data.getBytes());
                OS.SendMessage(km); // System call to send the message.
                System.out.println("PING: Sent -> " + km);

                // Wait for a reply from Pong.
                KernelMessage reply = OS.WaitForMessage();
                System.out.println("PING: Received -> " + reply);

                messageType++;
                // Sleep a bit to simulate processing.
                Thread.sleep(500);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
