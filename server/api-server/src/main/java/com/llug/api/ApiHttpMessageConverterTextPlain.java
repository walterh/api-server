package com.llug.api;

import org.springframework.http.MediaType;

public class ApiHttpMessageConverterTextPlain extends ApiHttpMessageConverterBase {
    public ApiHttpMessageConverterTextPlain() {
        super(new MediaType("text", "plain", DEFAULT_CHARSET));
    }
}
