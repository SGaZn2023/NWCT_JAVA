package com.example.main.obj;

import com.example.main.util.HeartbeatUtil;

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
                Socket s2 = ss.accept();
                try {
                    this.sessionManager.createSession(ss, s, s2);
                } catch (Exception e) {
                    System.out.println("创建会话失败，关闭连接");
                    s.close();
                    s2.close();
                }
                while (true) {
                    try {
                        s2.getOutputStream().write(0);
                        Thread.sleep(3000);
                    } catch (Exception e) {
                        break;
                    }

//                    if (s.isClosed() || s2.isClosed()) {
//                        break;
//                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
