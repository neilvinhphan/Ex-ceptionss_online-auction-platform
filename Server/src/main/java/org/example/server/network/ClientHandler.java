package org.example.server.network;

import java.io.BufferedReader;
import java.net.Socket;

public class ClientHandler implements Runnable {
    public ClientHandler(Socket clientSocket) {
        BufferedReader reader = null;

    }
    @Override
    public void run() {
        while (true) {
            // read messages from the client and process them
        }
    }
}


