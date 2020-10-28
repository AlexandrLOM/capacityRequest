package tech.scalea.capacityRequest.enums;

public enum HostStatus {
    Up("1"),
    Down("0");

    private String value;

    HostStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
