package com.jeiqi;

import com.jeiqi.network.TcpGameServer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class JeiqiApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext ctx = SpringApplication.run(JeiqiApplication.class, args);
        TcpGameServer tcpServer = ctx.getBean(TcpGameServer.class);
        tcpServer.start();
    }
}
