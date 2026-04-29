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

    public void setStatus(String status) {this.status = status;}

    public String getStatus() {return status;}

    public void setData(Object data) {this.data = data;}

    public Object getData() {return data;}

    public void setMessage(String message) {this.message = message;}

    public String getMessage() {return message;}
}
