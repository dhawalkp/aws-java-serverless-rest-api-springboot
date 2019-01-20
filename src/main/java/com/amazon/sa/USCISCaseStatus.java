package com.amazon.sa;

public class USCISCaseStatus {

    private final String id;
    private final String status;

    public USCISCaseStatus(String id, String status) {
        this.id = id;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public String getStatus() {
        return status;
    }
}
