package com.example.main.obj;

import com.example.main.util.HeartbeatUtil;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Server {
    private int port;
    private static int sleepTime = 3000;
    private SessionManager sessionManager;
    private ThreadPoolExecutor pool = new ThreadPoolExecutor(
            3,  // 核心线程数
            16, // 线程池总大小
            60, // 空闲时间
            TimeUnit.SECONDS,   // 空闲时间单位
            new ArrayBlockingQueue<>(2),    // 队列
            Executors.defaultThreadFactory(),   // 线程工厂，让线程池如何创建对象
            new ThreadPoolExecutor.AbortPolicy()    // 阻塞队列
    );

    public static int getSleepTime() {
        return sleepTime;
    }

    public Server(int port, SessionManager sessionManager) {
        this.port = port;
        this.sessionManager = sessionManager;
        pool.submit(new HeartbeatUtil(this.sessionManager));
    }

    public void listenAndServer() {
        try {
            ServerSocket ss = new ServerSocket(this.port);
            while (true) {
                System.out.println("服务器启动，等待内网连接");
                Socket s = ss.accept();

                pool.submit(() -> {
                    try {
                        this.sessionManager.createSession(s);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
