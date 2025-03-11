import java.util.Random;

// RandomDevice implements the Device interface to simulate a random number generator device
// It maintains an array of java.util.Random objects

public class RandomDevice implements Device {
    private static final int MAX_DEVICES = 10;
    private Random[] randomDevices;

    public RandomDevice() {
        randomDevices = new Random[MAX_DEVICES];
    }

    // Finds an available slot in the array for a new Random object
    private int findEmptySlot() {
        for (int i = 0; i < MAX_DEVICES; i++) {
            if (randomDevices[i] == null) {
                return i;
            }
        }
        return -1; // No available slots
    }

    public int open(String s) {
        int index = findEmptySlot();
        if (index == -1) {
            return -1; // No space available
        }
        if (s != null && !s.trim().isEmpty()) {
            try {
                long seed = Long.parseLong(s.trim());
                randomDevices[index] = new Random(seed);
            } catch (NumberFormatException e) {
                randomDevices[index] = new Random();
            }
        } else {
            randomDevices[index] = new Random();
        }
        return index;
    }

    public void close(int id) {
        if (id >= 0 && id < MAX_DEVICES) {
            randomDevices[id] = null; // Free up the slot
        }
    }

    public byte[] read(int id, int size) {
        if (id < 0 || id >= MAX_DEVICES || randomDevices[id] == null) {
            return new byte[0]; // Invalid ID, return empty array
        }
        byte[] data = new byte[size];
        randomDevices[id].nextBytes(data);
        return data;
    }

    public void seek(int id, int to) {
        if (id < 0 || id >= MAX_DEVICES || randomDevices[id] == null) {
            return;
        }
        byte[] dummy = new byte[to];
        randomDevices[id].nextBytes(dummy); // Simulates seeking
    }

    public int write(int id, byte[] data) {
        return 0; // Writing to a random device does nothing
    }
}

