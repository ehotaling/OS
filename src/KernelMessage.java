public class KernelMessage {
    private int senderPid;
    private int receiverPid;
    private int messageType;
    private byte[] data;

    // Constructor to initialize all fields
    public KernelMessage(int senderPid, int receiverPid, int messageType, byte[] data) {
        this.senderPid = senderPid;
        this.receiverPid = receiverPid;
        this.messageType = messageType;

        // Create deep copy to avoid different messages using same memory location
        if (data != null) {
            this.data = new byte[data.length];
            System.arraycopy(data, 0, this.data, 0, data.length);
        } else {
            this.data = null;
        }
    }

    // Copy constructor
    public KernelMessage(KernelMessage message) {
        // Can copy primitives directly because they are stored by value
        this.senderPid = message.senderPid;
        this.receiverPid = message.receiverPid;
        this.messageType = message.messageType;

        // Must deep copy array because arrays are objects and can point to same memory
        if (message.data != null) {
            this.data = new byte[message.data.length];
            System.arraycopy(message.data, 0, this.data, 0, message.data.length);
        } else {
            this.data = null;
        }
    }

    // Getters and Setters

    public int getSenderPid() {
        return senderPid;
    }

    public void setSenderPid(int senderPid) {
        this.senderPid = senderPid;
    }

    public int getReceiverPid() {
        return receiverPid;
    }

    public void setReceiverPid(int receiverPid) {
        this.receiverPid = receiverPid;
    }

    public int getMessageType() {
        return messageType;
    }

    public void setMessageType(int messageType) {
        this.messageType = messageType;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        if (data != null) {
            this.data = new byte[data.length];
            System.arraycopy(data, 0, this.data, 0, data.length);
        } else {
            this.data = null;
        }
    }

    // toString method for debugging and logging purposes
    // Assumes the data is text
    @Override
    public String toString() {
        // For simplicity, if data is non-null, we convert it to a string.
        // In a real system, you might want to represent binary data differently.
        return "KernelMessage [senderPid=" + senderPid + ", receiverPid=" + receiverPid
                + ", messageType=" + messageType + ", data=" + (data != null ? new String(data) : "null") + "]";
    }

}
