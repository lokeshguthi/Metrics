package de.tukl.softech.exclaim.utils;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.StringWriter;

public class Json {


    public static String toJson(Object o) {
        ObjectMapper m = new ObjectMapper();
        StringWriter w = new StringWriter();
        try {
            m.writeValue(w, o);
            return w.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
