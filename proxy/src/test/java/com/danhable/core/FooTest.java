package com.danhable.core;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.HashMap;


public class FooTest {

    private String input = "{\n" +
                           "  \"data\": {\n" +
                           "    \"regions\": null,\n" +
                           "    \"cluster\": null,\n" +
                           "    \"org\": {\n" +
                           "      \"details\": null\n" +
                           "    }\n" +
                           "  },\n" +
                           "  \"errors\": [\n" +
                           "    {\n" +
                           "      \"message\": \"Internal Server Error(s) while executing query\",\n" +
                           "      \"path\": null,\n" +
                           "      \"extensions\": null\n" +
                           "    }\n" +
                           "  ]\n" +
                           "}";

    @Test
    void testIt() throws Exception {
        var mapper = new ObjectMapper();

//        var factory = new JsonFactory();
//        var parser = factory.createParser(input);

        var foo = mapper.readValue(input, HashMap.class);

        System.err.println("s");

    }

}
