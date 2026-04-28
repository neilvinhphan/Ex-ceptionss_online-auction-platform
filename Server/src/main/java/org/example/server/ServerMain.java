package org.example.server;

import org.example.server.network.AuctionServer;

public class ServerMain {
    public static void main(String[] args){
        AuctionServer server = new AuctionServer();
        server.start();
    }
}
