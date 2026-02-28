package com.company.figmaintegrationservice.utils;

//import tools.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;

public class JsonUtils {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static byte[] toJsonBytes(Object obj) throws IOException {
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(obj);
    }
}
