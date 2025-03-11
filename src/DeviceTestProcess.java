import java.util.Arrays;

public class DeviceTestProcess extends UserlandProcess {
    @Override
    public void main() throws InterruptedException {
        // open random device with seed 12345
        int deviceSlot = OS.Open("random 12345");
        System.out.println("DeviceTestProcess: Opened random device slot " + deviceSlot);
        // read 10 bytes from random device
        byte[] data = OS.Read(deviceSlot, 10);
        System.out.println("DeviceTestProcess: Read random data " + Arrays.toString(data));
        // perform a dummy seek call
        OS.Seek(deviceSlot, 5);
        // attempt to write to random device (should return 0)
        int bytesWritten = OS.Write(deviceSlot, new byte[] {1, 2, 3});
        System.out.println("DeviceTestProcess: Write returned " + bytesWritten);
        // close random device
        OS.Close(deviceSlot);
        System.out.println("DeviceTestProcess: Closed random device");
        // exit process
        OS.Exit();
    }
}
