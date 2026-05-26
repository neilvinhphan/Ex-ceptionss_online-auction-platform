package org.example.core.dto;

import org.example.core.shared.enums.ActionType;
public class Request {
    private ActionType action;
    private Object data;

    public Request() {}

    public Request(ActionType action, Object data) {
        this.action = action;
        this.data = data;
    }

    public ActionType getAction() {return action;}

    public Object getData() {return data;}
}
