package com.llug.api;

import com.llug.api.domain.ApiResponse.ResultCode;

public class ApiServerException extends Exception {
    private static final long serialVersionUID = -2027957627611547557L;

    private ResultCode resultCode;

    public ApiServerException(ResultCode resultCode) {
        this(resultCode, null);
    }

    public ApiServerException(ResultCode resultCode, String detail) {
        super(detail);

        this.resultCode = resultCode;
    }

    public ResultCode getResultCode() {
        return resultCode;
    }
}
