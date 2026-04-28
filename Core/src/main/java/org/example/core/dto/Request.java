package org.example.core.dto;

public class Request {
    private String action;
    private Object data;

    public Request() {}

    public Request(String action, Object data) {
        this.action = action;
        this.data = data;
    }

    public void setAction(String action) {this.action = action;}
    public String getAction() {return action;}
    public void setData(Object data) {this.data = data;}
    public Object getData() {return data;}
}
