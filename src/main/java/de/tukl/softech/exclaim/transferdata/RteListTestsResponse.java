package de.tukl.softech.exclaim.transferdata;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class RteListTestsResponse {
    private List<String> tests;
    private boolean success;

    @JsonProperty("Tests")
    public List<String> getTests() {
        return tests;
    }

    public void setTests(List<String> tests) {
        this.tests = tests;
    }

    @JsonProperty("Success")
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }
}
