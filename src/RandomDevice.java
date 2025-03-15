import java.util.Random;

// RandomDevice simulates a random number generator device by implementing the Device interface.
// It uses an array of java.util.Random objects to manage multiple "device" instances.
public class RandomDevice implements Device {
    // Maximum number of random device instances allowed.
    private static final int MAX_DEVICES = 10;
    // Array to hold Random objects. A null entry indicates an unused slot.
    private Random[] randomDevices;

    // Constructor initializes the randomDevices array.
    public RandomDevice() {
        randomDevices = new Random[MAX_DEVICES];
    }

    // Finds and returns the index of the first empty slot in the randomDevices array.
    // Returns -1 if no slot is available.
    private int findEmptySlot() {
        for (int i = 0; i < MAX_DEVICES; i++) {
            if (randomDevices[i] == null) {
                return i;
            }
        }
        return -1; // No available slots found.
    }

    // Opens a random device using the supplied string as a seed (if provided).
    // Returns the index (device id) in the array if successful, or -1 if no slot is available.
    public int open(String s) {
        int index = findEmptySlot();
        if (index == -1) {
            return -1; // No space available for a new device.
        }
        // If a seed is provided, try to parse it; otherwise, create a new Random without a seed.
        if (s != null && !s.trim().isEmpty()) {
            try {
                long seed = Long.parseLong(s.trim());
                randomDevices[index] = new Random(seed);
            } catch (NumberFormatException e) {
                // If seed parsing fails, create a Random without a seed.
                randomDevices[index] = new Random();
            }
        } else {
            randomDevices[index] = new Random();
        }
        return index;
    }

    // Closes the random device at the specified index by nullifying the slot.
    public void close(int id) {
        if (id >= 0 && id < MAX_DEVICES) {
            randomDevices[id] = null; // Free the slot for future use.
        }
    }

    // Reads a specified number of random bytes from the device identified by id.
    // Returns an array of bytes or an empty array if the id is invalid.
    public byte[] read(int id, int size) {
        if (id < 0 || id >= MAX_DEVICES || randomDevices[id] == null) {
            return new byte[0]; // Invalid id, so return an empty byte array.
        }
        byte[] data = new byte[size];
        // Fill the array with random bytes.
        randomDevices[id].nextBytes(data);
        return data;
    }

    // Simulates seeking in the device.
    // Since a Random device doesn't really support seeking, this method generates dummy data.
    public void seek(int id, int to) {
        if (id < 0 || id >= MAX_DEVICES || randomDevices[id] == null) {
            return; // Invalid id, do nothing.
        }
        // Create a dummy byte array of length 'to' and fill it with random bytes.
        byte[] dummy = new byte[to];
        randomDevices[id].nextBytes(dummy);
    }

    // Write operation does nothing for a random device, so it returns 0.
    public int write(int id, byte[] data) {
        return 0; // Writing is not supported on a random device.
    }
}
