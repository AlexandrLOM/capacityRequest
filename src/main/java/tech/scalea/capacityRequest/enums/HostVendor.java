package tech.scalea.capacityRequest.enums;

public enum HostVendor {
    Cisco("cisco"),
    Dell("dell"),
    Juniper("juniper"),
    NEC("nec");

    private String stringValue;

    HostVendor(String stringValue) {
        this.stringValue = stringValue;
    }

    public String getStringValue() {
        return stringValue;
    }

    public static HostVendor parseString(String stringValue) {
        for (HostVendor type : HostVendor.values()) {
            if (type.stringValue.equals(stringValue)) return type;
        }
        // throw new IllegalArgumentException(stringValue);
        return null;
    }


}
