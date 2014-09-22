package com.raytheon.uf.ooi.plugin.instrumentagent;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class JsonHelper {
    private final static ObjectMapper mapper = new ObjectMapper();
    protected static final TypeReference<Map<String, Object>> mapType =
    		new TypeReference<Map<String, Object>>() {};
	protected static final TypeReference<List<Object>> listType =
    		new TypeReference<List<Object>>() {};
    		
    static {
    	// TODO - this is needed to handle NaN, but not present in jackson 1.7.3
        //mapper.configure(JsonParser.Feature.ALLOW_NON_NUMERIC_NUMBERS, true);
        //mapper.configure(JsonGenerator.Feature.QUOTE_NON_NUMERIC_NUMBERS, false);
    }
    private JsonHelper() {}

    public static String toJson(Object obj) throws IOException {
        return mapper.writeValueAsString(obj);
    }

	public static Map<String, Object> toMap(String json) throws IOException {
        return mapper.readValue(json, mapType);
    }

	public static List<Object> toList(String json) throws IOException {
        JsonNode node = mapper.readTree(json);
        if (node.isArray())
            return mapper.readValue(json, listType);
        return null;
    }

    public static Object toObject(String json) throws IOException {
        JsonNode node = mapper.readTree(json);
        if (node.isArray()) {
            return toList(json);
        } else if (node.isObject()) {
            return toMap(json);
        } else {
            return node.getTextValue();
        }
    }
}
