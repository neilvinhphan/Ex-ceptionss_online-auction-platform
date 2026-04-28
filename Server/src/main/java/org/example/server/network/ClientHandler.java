package org.example.server.network;

import com.google.gson.Gson;

import org.example.core.dto.LoginRequestDTO;
import org.example.core.dto.RegisterRequestDTO;
import org.example.core.dto.Request;

import org.example.core.dto.Response;
import org.example.core.models.users.User;
import org.example.server.services.AuthService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private BufferedReader in;
    private PrintWriter out;
    private final Gson gson = new Gson();

    public ClientHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
        try {
            this.in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            this.out = new PrintWriter(clientSocket.getOutputStream(), true);
        } catch (Exception e) {
            throw new RuntimeException("Error initializing client handler: " + e.getMessage(), e);
        }
    }

    @Override
    public void run() {
        try {
            String requestJson;
            while ((requestJson = in.readLine()) != null) {
                Request request = gson.fromJson(requestJson, Request.class);
                if (request != null && request.getAction()!= null) {
                    switch (request.getAction()) {
                        case "REGISTER":
                            handleRegister(request);
                            break;
                        case "LOGIN":
                            handleLogin(request);
                            break;
                        default:
                            System.out.println("Unknown action: " + request.getAction());
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void handleRegister(Request request) {
        try {
            RegisterRequestDTO registerRequest;

            if (request.getData() instanceof RegisterRequestDTO) {
                registerRequest = (RegisterRequestDTO) request.getData();
            } else {
                String dataJson = gson.toJson(request.getData());
                registerRequest = gson.fromJson(dataJson, RegisterRequestDTO.class);
            }

            System.out.println("Registering user: " + registerRequest.getUsername());
            User newUser = AuthService.register(registerRequest);

            Response response;
            if(newUser != null) {
                response = new Response("SUCCESS", "Registration successful");
            } else {
                response = new Response("ERROR", "Registration failed");
            }
            sendMessage(gson.toJson(response));
        } catch (Exception e) {
            e.printStackTrace();
            Response errorRespone = new Response("ERROR", e.getMessage());
            sendMessage(gson.toJson(errorRespone));
        }
    }

    private void handleLogin(Request request) {
        try{
            LoginRequestDTO loginRequest;

            if (request.getData() instanceof LoginRequestDTO) {
                loginRequest = (LoginRequestDTO) request.getData();
            } else {
                String dataJson = gson.toJson(request.getData());
                loginRequest = gson.fromJson(dataJson, LoginRequestDTO.class);
            }
            User newUser = AuthService.login(loginRequest);

            Response response;
            if(newUser != null) {
                response = new Response("SUCCESS", newUser);
            } else {
                response = new Response("ERROR", "Login failed");
            }
            // Dòng này lúc nãy bị thiếu khiến Client đợi mòn mỏi nè!
            sendMessage(gson.toJson(response));
        } catch (Exception e) {
            e.printStackTrace();
            Response errorRespone = new Response("ERROR", e.getMessage());
            sendMessage(gson.toJson(errorRespone));
        }
    }

    public synchronized void sendMessage(String message) {
        out.println(message);
    }

    private void closeConnection() {
        try{
        if(in!=null) in.close();
        if(out!=null) out.close();
        if(clientSocket!=null) clientSocket.close();
    } catch (IOException e) {
        e.printStackTrace();
    }}
}
