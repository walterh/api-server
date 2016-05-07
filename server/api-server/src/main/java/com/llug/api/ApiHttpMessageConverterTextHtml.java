package com.llug.api;

import org.springframework.http.MediaType;

public class ApiHttpMessageConverterTextHtml extends ApiHttpMessageConverterBase {
    public ApiHttpMessageConverterTextHtml() {
        super(new MediaType("text", "html", DEFAULT_CHARSET));
    }
}
