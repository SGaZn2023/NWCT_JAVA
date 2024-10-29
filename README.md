# NWCT_JAVA

一款内网穿透工具（Java版），可以帮助你将内网计算机端口映射到外网中，暂仅支持TCP连接。

## 使用方法

注意：该工具需要提前安装 **Java17**

1. 下载 Client（客户端）与 Server（服务端）的 zip
2. 将 Client客户端 放在内网的计算机上并解压，
修改配置文件 client_config.json

```json
{
    "client_id": "一次连接的id，随意输入",
    "public_ip": "127.0.0.1",   // 服务器IP
    "connect_port": 5103    // 连接服务器端口
}
```

3. 在相应文件夹下的终端中输入以下命令

```bash
java -jar Client.jar ./Client.yaml
```

4. 将 Server（服务端）放在服务器上并解压，修改配置文件server_config.json

```json
[
    {
		"listen_port": 5103 // 监听端口，这里需要与上面的 connect_port 一致
	},
	{
		"client_id": "这里需要与上面一致",
		"public_port": 5102, // 外界访问端口号
		"public_protocol": "tcp", // 内网穿透协议
		"internal_port": 5101, // 内网被访问端口号
		"internal_ip": "127.0.0.1", // 内网被访问IP，一般为 127.0.0.1
		"internal_protocol": "tcp"  // 访问内网协议
	}
]
```

5. 在相应文件夹下的终端中输入以下命令

```bash
java -jar Server.jar ./Server.yaml
```

## 其它配置文件

1. Server.yaml

   ```yaml
   config: "这是 server_config.json 的路径"
   ```
   
3. Client.yaml

   ```yaml
   config: "这是 client_config.json 的路径"
   ```

## 反馈

可发送至邮箱 zshenwei510@163.com
