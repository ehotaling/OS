// VFS (Virtual File System) maps user-level device calls to actual devices
// Supports RandomDevice and FakeFileSystem by interpreting input strings

public class VFS implements Device {
    private static final int MAX_VFS_ENTRIES = 10; // Maximum number of active device mappings
    private DeviceMapping[] mappings; // Holds device-id mappings
    private RandomDevice randomDevice; // Instance of the Random device
    private FakeFileSystem fakeFileSystem; // Instance of the Fake File System

    // Initializes the VFS and its supported devices
    public VFS() {
        mappings = new DeviceMapping[MAX_VFS_ENTRIES];
        for (int i = 0; i < MAX_VFS_ENTRIES; i++) {
            mappings[i] = null;
        }
        randomDevice = new RandomDevice();
        fakeFileSystem = new FakeFileSystem();
    }

    // Inner class that stores a mapping between a VFS id and a device/id pair
    private class DeviceMapping {
        Device device;
        int deviceId;

        public DeviceMapping(Device device, int deviceId) {
            this.device = device;
            this.deviceId = deviceId;
        }
    }

    // Finds the first available slot in the mappings array
    private int findEmptySlot() {
        for (int i = 0; i < MAX_VFS_ENTRIES; i++) {
            if (mappings[i] == null) {
                return i;
            }
        }
        return -1;
    }

    // Opens a device based on input, extracting the device type and parameter
    public int open(String s) {
        if (s == null || s.trim().isEmpty()) {
            return -1;
        }
        String[] parts = s.trim().split("\\s+", 2);
        String deviceType = parts[0].toLowerCase();
        String parameter = parts.length > 1 ? parts[1] : "";
        Device device;

        // Determines which device to open based on the first word
        switch (deviceType) {
            case "random":
                device = randomDevice;
                break;
            case "file":
                device = fakeFileSystem;
                break;
            default:
                return -1;
        }

        // Opens the device and retrieves a device-specific id
        int deviceId = device.open(parameter);
        if (deviceId == -1) {
            return -1;
        }

        // Finds an available mapping slot
        int vfsId = findEmptySlot();
        if (vfsId == -1) {
            device.close(deviceId); // Ensures the device is closed if VFS is full
            return -1;
        }

        // Stores the mapping and returns the VFS id
        mappings[vfsId] = new DeviceMapping(device, deviceId);
        return vfsId;
    }

    // Closes the device associated with the VFS id
    public void close(int id) {
        if (id < 0 || id >= MAX_VFS_ENTRIES || mappings[id] == null) {
            return;
        }
        DeviceMapping mapping = mappings[id];
        mapping.device.close(mapping.deviceId);
        mappings[id] = null;
    }

    // Reads data from the device associated with the VFS id
    public byte[] read(int id, int size) {
        if (id < 0 || id >= MAX_VFS_ENTRIES || mappings[id] == null) {
            return new byte[0];
        }
        DeviceMapping mapping = mappings[id];
        return mapping.device.read(mapping.deviceId, size);
    }

    // Moves the file pointer or seeks in the device
    public void seek(int id, int to) {
        if (id < 0 || id >= MAX_VFS_ENTRIES || mappings[id] == null) {
            return;
        }
        DeviceMapping mapping = mappings[id];
        mapping.device.seek(mapping.deviceId, to);
    }

    // Writes data to the device associated with the VFS id
    public int write(int id, byte[] data) {
        if (id < 0 || id >= MAX_VFS_ENTRIES || mappings[id] == null) {
            return 0;
        }
        DeviceMapping mapping = mappings[id];
        return mapping.device.write(mapping.deviceId, data);
    }
}
