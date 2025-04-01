public class Pong extends UserlandProcess {
    public void main() throws InterruptedException {
        try {
            // Get our own PID and the PID of Ping.
            int myPid = OS.GetPID();
            int pingPid = OS.GetPidByName("Ping");
            System.out.println("I am PONG, Ping PID is " + pingPid);

            while (true) {
                // Wait for a message from Ping.
                System.out.println("PONG: Waiting for message...");
                KernelMessage incoming = OS.WaitForMessage();
                System.out.println("PONG: Received -> " + incoming);

                // Create a reply message.
                int responseType = incoming.getMessageType() + 1;
                String data = "Pong response " + responseType;
                KernelMessage reply = new KernelMessage(myPid, pingPid, responseType, data.getBytes());
                System.out.println("PONG: Sending reply -> " + reply);
                OS.SendMessage(reply);
                System.out.println("PONG: Sent -> " + reply);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
