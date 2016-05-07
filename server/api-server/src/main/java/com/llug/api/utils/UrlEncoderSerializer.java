package com.llug.api.utils;

import java.io.IOException;
import java.net.URLEncoder;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.ser.std.SerializerBase;

import com.wch.commons.utils.Utils;

// http://stackoverflow.com/questions/7161638/how-do-i-use-a-custom-serializer-with-jackson
public class UrlEncoderSerializer extends SerializerBase<String> {
    public UrlEncoderSerializer() {
        super(String.class, true);
    }

    @Override
    public void serialize(String arg0, JsonGenerator arg1, SerializerProvider arg2) throws IOException, JsonGenerationException {
        if (!Utils.isNullOrEmptyString(arg0)) {
            // a double-encode is necessary due to the fact that hashes appear rest-fully in the URL.
            arg1.writeString(URLEncoder.encode(URLEncoder.encode(arg0, "UTF-8"), "UTF-8"));
        }
    }
}
