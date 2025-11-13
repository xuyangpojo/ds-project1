# NTP客户端
##### 编译与运行命令
```bash
# 编译
javac -d build src/ntp_client/*.java
# 运行
java -cp build ntp_client.GUI
```

##### 可用的NTP服务器列表

| 服务器名称 | 服务器地址/URL | 位置 | 备注 |
|----------|--------------|------|------|
| pool.ntp.org | pool.ntp.org | 全球 | NTP Pool Project，自动选择最近的服务器 |
| time.nist.gov | time.nist.gov | 美国 | 美国国家标准与技术研究院 |
| time.windows.com | time.windows.com | 全球 | 微软Windows时间服务器 |
| time.apple.com | time.apple.com | 全球 | 苹果时间服务器 |
| ntp.aliyun.com | ntp.aliyun.com | 中国 | 阿里云NTP服务器 |
| ntp1.aliyun.com | ntp1.aliyun.com | 中国 | 阿里云NTP服务器（备用） |
| cn.pool.ntp.org | cn.pool.ntp.org | 中国 | NTP Pool中国区域 |
| time.cloudflare.com | time.cloudflare.com | 全球 | Cloudflare时间服务器 |
| 0.pool.ntp.org | 0.pool.ntp.org | 全球 | NTP Pool（Stratum 0） |
| 1.pool.ntp.org | 1.pool.ntp.org | 全球 | NTP Pool（Stratum 1） |
| 2.pool.ntp.org | 2.pool.ntp.org | 全球 | NTP Pool（Stratum 2） |
| 3.pool.ntp.org | 3.pool.ntp.org | 全球 | NTP Pool（Stratum 3） |

##### 技术说明
NTP（Network Time Protocol）是一种用于分布式计算机网络中同步系统时钟的协议。它的目的是确保网络中各个计算机的时钟保持一致，以便协调事件和数据的发生顺序。

NTP起源于1985年，经过多次升级和改进，已成为全球广泛使用的时间同步协议，特别是在互联网和其他网络环境中。

NTP的基本原理是通过分层的时间服务器体系来提供高精度的时间同步。这个体系由多个层级的NTP服务器组成，每个层级的服务器都有其特定的任务和精度水平。NTP服务器可以分为以下几类：

(1)Stratum 0：Stratum 0服务器通常是原子钟或GPS接收器，它们提供高精度的时间参考。

(2)Stratum 1：Stratum 1服务器是直接与Stratum 0服务器同步的服务器，通常是高精度的计算机或专用设备。它们从Stratum 0服务器获取时间信息并分发给下一级的Stratum 2服务器。

(3)Stratum 2及更高：Stratum 2及更高级别的服务器是网络中的普通计算机，它们从更高级别的Stratum服务器同步时间，并为其他设备提供时间信息。这些服务器可能分布在全球各地，构成了分布式的时间同步体系。

NTP使用时间戳和精确的算法来计算和调整系统时钟，以确保时间同步的准确性。它采用一种分层的体系结构，允许每个服务器在不同精度级别上提供时间信息，从而满足各种应用的需求。

NTP的基本流程如下：

(1) 客户端计算机定期向NTP服务器请求时间信息。同时在请求的信息中附上自身发送请求的时间戳

(2) NTP服务器收到请求后，标记上自身收到和发送返回体的时间戳

(3) 客户端计算机根据NTP服务器的相应信息来调整自身的本地时钟

(4) 时间同步过程会周期性地进行，以保持时钟的准确性。

NTP的核心原则是不断地校正本地系统时钟，以保持与NTP服务器的同步。这使得NTP成为互联网和其他分布式网络中时间同步的重要工具，特别是在需要高精度时间信息的应用中，如金融交易、网络日志记录等。