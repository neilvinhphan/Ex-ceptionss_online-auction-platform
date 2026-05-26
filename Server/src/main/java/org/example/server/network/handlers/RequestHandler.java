package org.example.server.network.handlers;

import org.example.core.dto.Request;
import org.example.server.network.ClientHandler;

public interface RequestHandler {
    void handle(Request request, ClientHandler client) throws Exception;
}