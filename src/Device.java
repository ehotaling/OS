// Device interface for all devices on the os. Provides standard operations for opening a device, closing a device
// reading bytes from a device, seeking to a position on the device, and writing data to a device


public interface Device {
    // Opens the device with a configuration string, returning a unique device ID or -1 on failure
    int open(String s);

    // Closes the device using its unique device ID
    void close(int id);

    // Reads a number of bytes from the device, returning them in a byte array
    byte[] read(int id, int size);

    // Seeks to a position within the device
    void seek(int id, int to);

    // Writes data to the device, returning the number of bytes written
    int write(int id, byte[] data);
}
