package org.example.core.dto;

public class Response {
    private String status;
    private Object data;

    public Response() {}

    public Response(String status, Object data) {
        this.status = status;
        this.data = data;
    }
    public void setStatus(String status) {this.status = status;}
    public String getStatus() {
        return status;
    }
    public void setData(Object data) {this.data = data;}
    public Object getData() {return data;}

    public String getMessage() {
        return data != null ? data.toString() : "";
    }
}
