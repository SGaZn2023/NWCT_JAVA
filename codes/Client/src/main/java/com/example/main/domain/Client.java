package com.example.main.domain;

import com.example.main.obj.ProxyProtocol;
//import com.example.main.util.HeartBeat;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


public class Client {
    private static int sleepTime = 3000;
    private String clientId;    // 不得长于 36 字节
    private String publicIp;    // 不带端口
    private int connectPort;
    private static final byte CMD_END = 0x08;
    private static final String endMessage = "#EELSEYLJTHECONNECTEDISEND#";

    public static int getSleepTime() {
        return sleepTime;
    }

    public static ThreadPoolExecutor pool = new ThreadPoolExecutor(
            3,  // 核心线程数
            16, // 线程池总大小
            60, // 空闲时间
            TimeUnit.SECONDS,   // 空闲时间单位
            new ArrayBlockingQueue<>(2),    // 队列
            Executors.defaultThreadFactory(),   // 线程工厂，让线程池如何创建对象
            new ThreadPoolExecutor.AbortPolicy()    // 阻塞队列
    );

    private static final byte CMD_HANDSHAKE = 0x01;


    public Client(String clientId, String publicIp, int connectPort) {
        this.clientId = clientId;
        this.publicIp = publicIp;
        this.connectPort = connectPort;
    }

    public void run() throws InterruptedException {
        while(true) {
            try {
                this.Run();
            } catch (Exception e) {
                System.out.println(this.publicIp + ":" + this.connectPort +" 连接错误: " + e.getMessage());
                Thread.sleep(sleepTime);
            }
            System.out.println("正在重新连接 " + this.publicIp + ":" + this.connectPort);
        }
    }

    public void Run() throws Exception {
        Socket socket = new Socket(publicIp, connectPort);
        socket.setKeepAlive(true);
        System.out.println("服务器成功连接 " + publicIp + ":" + connectPort);
        socket.setSoTimeout(3000);
        byte[] clientIdBytes = encodeClientId();
        socket.getOutputStream().write(clientIdBytes);
        socket.getOutputStream().flush();
        socket.setSoTimeout(0);

        // 内网穿透
        while (true) {
            InputStream inputStream = socket.getInputStream();
            ProxyProtocol pProtocol;
            pProtocol = ProxyProtocol.decode(inputStream);

            // 与本地端口建立连接
            Socket localSocket = null;
            switch (pProtocol.internalProtocol) {
                case "tcp":
                    try {
                        localSocket = new Socket(pProtocol.internalIp, pProtocol.internalPort);
                    } catch (Exception e) {
                        System.out.println("本地连接失败: " + e.getMessage());
                        Thread.sleep(sleepTime);
                        continue;
                    }
                    System.out.println("已与本地 " + pProtocol.internalIp + ":" + pProtocol.internalPort + " 建立连接");
                    break;
                default:
                    throw new IOException("unsupported protocol : " + pProtocol.internalProtocol);
            }

            Socket finalLocalSocket = localSocket;
            System.out.println("--开始穿透--");

            // 接收数据
            pool.submit(() -> {
                try {
                    OutputStream localOutputStream = finalLocalSocket.getOutputStream();

                    byte[] buffer = new byte[1024];

                    int bytesRead;
                    while((bytesRead = inputStream.read(buffer)) != -1) {
                        if (bytesRead == 1024 && buffer[1] == CMD_END) {
                            System.out.println("get end");
                            // 检测结束标志
                            ByteBuffer byteBuffer = ByteBuffer.wrap(buffer, 2, 2);
                            byteBuffer.order(ByteOrder.BIG_ENDIAN);
                            short length = byteBuffer.getShort();
                            byte[] endMessageBytes = endMessage.getBytes();
                            if (length == endMessageBytes.length) {
                                byte[] message = new byte[length];
                                for (int i = 0; i < endMessageBytes.length; i++)
                                    message[i] = buffer[i + 4];
                                String messageStr = new String(message);
                                if (endMessage.equals(messageStr)) {
                                    System.out.println("?");
                                    break;
                                }
                            }
                        }
                        // 传输数据
                        localOutputStream.write(buffer, 0, bytesRead);
                        localOutputStream.flush();
                    }

                    // 关闭与本地的连接
                    finalLocalSocket.close();
                    System.out.println("??");

                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

            // 发送数据
            try {
                InputStream localInputStream = finalLocalSocket.getInputStream();
                byte[] buffer = new byte[1024];
                int bytesRead;
                OutputStream outputStream = socket.getOutputStream();
                while((bytesRead = localInputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    outputStream.flush();
                }
            } catch (IOException e) {
                System.out.println("???");
                // 发送结束标志
                byte[] end = new byte[1024];
                end[0] = 0x0;
                end[1] = CMD_END;
                byte[] messageBytes = endMessage.getBytes();
                ByteBuffer byteBuffer = ByteBuffer.wrap(end);
                byteBuffer.order(ByteOrder.BIG_ENDIAN);
                byteBuffer.putShort(2, (short) messageBytes.length);

                for (int i = 0; i < messageBytes.length; i++) {
                    end[i + 4] = messageBytes[i];
                }

                socket.setSoTimeout(sleepTime);
                socket.getOutputStream().write(end);
                socket.getOutputStream().flush();
                socket.setSoTimeout(0);

                System.out.println("--穿透结束--");
            }

        }
    }

    public byte[] encodeClientId() throws JsonProcessingException {
        byte[] hdr = new byte[4];
        hdr[0] = 0x0;
        hdr[1] = CMD_HANDSHAKE;

        ObjectMapper objectMapper = new ObjectMapper();
        byte[] body = objectMapper.writeValueAsBytes(clientId);

        ByteBuffer buffer = ByteBuffer.wrap(hdr);
        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.putShort(2, (short) body.length);

        if (hdr.length + body.length > 40) throw new RuntimeException("Client Id too long");
        byte[] result = new byte[40];
        System.arraycopy(hdr, 0, result, 0, hdr.length);
        System.arraycopy(body, 0, result, hdr.length, body.length);

        return result;
    }
}
