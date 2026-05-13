package org.example.core.dto;

public class Response {
    private String status;
    private Object data;
    private String message;

    public Response() {}

    public Response(String status, String message, Object data) {
        this.status = status;
        this.data = data;
        this.message = message;
    }

    public Response(String status, String message) {
        this.status = status;
        this.message = message;
    }

    public String getStatus() {return status;}

    public Object getData() {return data;}

    public String getMessage() {return message;}
}
