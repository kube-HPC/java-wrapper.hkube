package hkube.communication.streaming;



public class Message {

    public Message(byte[] data, byte[] header, Flow flow) {
        this.data = data;
        this.header = header;
        this.flow = flow;
    }

    public byte[] getData() {
        return data;
    }

    public byte[] getHeader() {
        return header;
    }

    public Flow getFlow() {
        return flow;
    }

    byte[] data;
    byte[] header;
    Flow flow;


}
