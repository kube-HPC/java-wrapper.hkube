package hkube.model;

public class ObjectAndSize {
    Integer size;
    Object value;

    public ObjectAndSize( Object value,Integer size) {
        this.size = size;
        this.value = value;
    }

    public Integer getSize() {
        return size;
    }

    public Object getValue() {
        return value;
    }
}
