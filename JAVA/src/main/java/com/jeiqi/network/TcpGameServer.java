package com.jeiqi.network;

import com.jeiqi.protocol.ProtocolHandler;
import com.jeiqi.service.GameService;
import com.jeiqi.service.MatchmakingService;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class TcpGameServer implements DisposableBean {

    private final int port;
    private final ProtocolHandler protocolHandler;
    private final MatchmakingService matchmakingService;
    private final GameService gameService;
    private final ExecutorService executor;
    private ServerSocket serverSocket;
    private volatile boolean running;

    public TcpGameServer(@Value("${game.tcp-port:9090}") int port,
                         ProtocolHandler protocolHandler,
                         MatchmakingService matchmakingService,
                         GameService gameService) {
        this.port = port;
        this.protocolHandler = protocolHandler;
        this.matchmakingService = matchmakingService;
        this.gameService = gameService;
        this.executor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "tcp");
            t.setDaemon(true);
            return t;
        });
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            running = true;
            executor.submit(() -> {
                while (running) {
                    try {
                        Socket client = serverSocket.accept();
                        executor.submit(new TcpClientHandler(client, protocolHandler,
                            matchmakingService, gameService));
                    } catch (Exception e) {
                        if (running) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        } catch (Exception e) {
            throw new RuntimeException("Failed to start TCP server on port " + port, e);
        }
    }

    @Override
    public void destroy() {
        running = false;
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (Exception ignored) {}
        executor.shutdown();
    }
}
