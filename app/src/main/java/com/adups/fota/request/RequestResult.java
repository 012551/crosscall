package com.adups.fota.request;

public class RequestResult {

    private boolean isSuccess;
    private int status_code;
    private int error_code;
    private String error_message;
    private String result;
    private long start_time;
    private long end_time;

    public RequestResult() {
        error_message = "";
        result = "";
        start_time = 0;
        end_time = 0;

    }

    public RequestResult(boolean isOk, int status_code, String result, int error_code, String error_message) {
        this.isSuccess = isOk;
        this.status_code = status_code;
        this.error_code = error_code;
        this.error_message = error_message;
        this.result = result;
    }

    public int getError_code() {
        return error_code;
    }

    public RequestResult setError_code(int error_code) {
        this.error_code = error_code;
        return this;
    }

    public String getError_message() {
        return error_message;
    }

    public RequestResult setError_message(String error_message) {
        this.error_message = error_message;
        return this;
    }

    public boolean isSuccess() {
        return isSuccess;
    }

    public String getResult() {
        return result;
    }

    public RequestResult setResult(String result) {
        this.result = result;
        return this;
    }

    public int getStatus_code() {
        return status_code;
    }

    public RequestResult setStatus_code(int status_code) {
        this.status_code = status_code;
        return this;
    }

    public long getStart_time() {
        return start_time;
    }

    public RequestResult setStart_time(long start_time) {
        this.start_time = start_time;
        return this;
    }

    public long getEnd_time() {
        return end_time;
    }

    public RequestResult setEnd_time(long end_time) {
        this.end_time = end_time;
        return this;
    }

    public RequestResult setIsSuccess(boolean isSuccess) {
        this.isSuccess = isSuccess;
        return this;
    }


}



