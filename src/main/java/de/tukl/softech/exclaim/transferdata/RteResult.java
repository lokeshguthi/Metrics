package de.tukl.softech.exclaim.transferdata;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RteResult {

    private TestResultDetails test_result;
    private List<FileWarnings> file_warnings;
    private List<ClocResult> cloc_result;

    public static RteResult fromJson(String resultJson) {
        ObjectMapper mapper = new ObjectMapper();
        try {

            List<String> resJson = Arrays.asList(resultJson.split("\"cloc_result\":null}"));

            String test = resJson.get(1);
            test = test.replace("\n", "").replace("\r", "");
            return mapper.readValue(test, new TypeReference<RteResult>() {});

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public TestResultDetails getTest_result() {
        return test_result;
    }

    public void setTest_result(TestResultDetails test_result) {
        this.test_result = test_result;
    }

    public List<FileWarnings> getFile_warnings() {
        return file_warnings;
    }

    public void setFile_warnings(List<FileWarnings> file_warnings) {
        this.file_warnings = file_warnings;
    }

    public List<ClocResult> getCloc_result() {
        return cloc_result;
    }

    public void setCloc_result(List<ClocResult> cloc_result) {
        this.cloc_result = cloc_result;
    }
}
