package com.rashidmayes.clairvoyance.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

public class ClairvoyanceObjectMapper {

    public static final ObjectMapper objectMapper;
    public static final ObjectWriter objectWriter;

    static {
        objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectWriter = objectMapper.writerWithDefaultPrettyPrinter();
    }

}
