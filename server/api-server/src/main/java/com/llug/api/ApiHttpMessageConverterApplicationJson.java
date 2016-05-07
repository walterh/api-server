package com.llug.api;

import org.springframework.http.MediaType;

public class ApiHttpMessageConverterApplicationJson extends ApiHttpMessageConverterBase {
    public ApiHttpMessageConverterApplicationJson() {
        super(new MediaType("application", "json", DEFAULT_CHARSET));
    }
}
