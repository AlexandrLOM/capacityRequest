package tech.scalea.capacityRequest.enums;

import java.util.Arrays;

public enum HostType {
    SR_IOV("SR_IOV"),
    DPDK("DPDK"),
    DCGW("DCGW"),
    Leaf("Leaf"),
    Mgt("Mgt"),
    Spine("Spine"),
    Server("Server"),
    vrouter("vrouter");


    private String stringValue;

    HostType(String stringValue) {
        this.stringValue = stringValue;
    }

    public String getStringValue() {
        return stringValue;
    }

    public static HostType parseString(String stringValue) {
        return Arrays.stream(values()).filter(v -> v.stringValue.equals(stringValue)).findFirst().orElse(null);
    }

    public boolean isComputeNode() {
        return this == SR_IOV || this == DPDK;
    }

    public boolean isNetEquipment() {
        return this == DCGW || this == Leaf || this == Spine || this == Mgt;
    }
}
