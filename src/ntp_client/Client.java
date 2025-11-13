package ntp_client;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;

/**
 * @name NTP客户端
 * @discirption 进行NTP协议的发送与接收解析
 * @author xuyangpojo@icloud.com
 * @date 2025-11-13
 */
public class Client
{
    private static final int NTP_Port = 123;
    private static final int NTP_PACKET_SIZE = 48;
    private static final long SeventyYears = 2208988800L;
    private static final int SOCKET_TIMEOUT_MS = 5000; // 增加到5秒，避免网络延迟导致的超时
    
    private DatagramSocket m_TimeService_Socket;
    private InetAddress m_TimeService_IPAddress;
    private Boolean m_bNTP_Client_Started;
    private Boolean m_bTimeServiceAddressSet;
    
    public enum NTP_Client_ResultCode {
        NTP_Success,
        NTP_ServerAddressNotSet,
        NTP_SendFailed,
        NTP_ReceiveFailed 
    }
    
    public final class NTP_Timestamp_Data
    {
        public NTP_Client_ResultCode eResultCode;
        public long lUnixTime;
        public long lHour;
        public long lMinute;
        public long lSecond;
        public long lRoundTripTime;
        public long lTimeOffset;
        
        NTP_Timestamp_Data()
        {
            eResultCode = NTP_Client_ResultCode.NTP_ServerAddressNotSet;
            lHour = 0;
            lMinute = 0;
            lSecond = 0;
            lUnixTime = 0;
            lRoundTripTime = 0;
            lTimeOffset = 0;
        }
    }

    public Client()
    {
        m_bTimeServiceAddressSet = false;
        m_bNTP_Client_Started = false;
        m_TimeService_Socket = null;
        m_TimeService_IPAddress = null;
    }
    
    /**
     * 创建UDP套接字
     * @return true表示成功，false表示失败
     */
    public Boolean CreateSocket()
    {
        try
        {
            m_TimeService_Socket = new DatagramSocket();
            m_TimeService_Socket.setSoTimeout(SOCKET_TIMEOUT_MS);
        }
        catch (SocketException Ex)
        {
            System.err.println("创建套接字失败: " + Ex.getMessage());
            return false;
        }
        return true;
    }
    
    /**
     * 设置时间服务器地址
     * @param sURL 服务器URL（域名）
     * @return 解析后的IP地址，失败返回null
     */
    public InetAddress SetUp_TimeService_AddressStruct(String sURL)
    {
        try
        {
            String sFullURL = sURL.startsWith("http://") || sURL.startsWith("https://") 
                ? sURL : "http://" + sURL;
            URL url = new URL(sFullURL);
            m_TimeService_IPAddress = InetAddress.getByName(url.getHost());
            m_bTimeServiceAddressSet = true;
            return m_TimeService_IPAddress;
        }
        catch (Exception Ex)
        {
            System.err.println("解析服务器地址失败: " + sURL + " - " + Ex.getMessage());
            m_bTimeServiceAddressSet = false;
            return null;
        }
    }
    
    /**
     * 获取NTP端口号
     * @return NTP端口号(123)
     */
    public int GetPort()
    {
        return NTP_Port;
    }
    
    /**
     * 获取一次NTP时间戳
     * @return 携带结果码与时间数据的NTP_Timestamp_Data
     */
    public NTP_Timestamp_Data Get_NTP_Timestamp()
    {
        NTP_Timestamp_Data NTP_Timestamp = new NTP_Timestamp_Data();
        if (!m_bTimeServiceAddressSet)
        {
            NTP_Timestamp.eResultCode = NTP_Client_ResultCode.NTP_ServerAddressNotSet;
            return NTP_Timestamp;
        }
        long lSendTime = System.currentTimeMillis();
        if (!Send_TimeService_Request())
        {
            NTP_Timestamp.eResultCode = NTP_Client_ResultCode.NTP_SendFailed;
            return NTP_Timestamp;
        }
        NTP_Timestamp = Receive(NTP_Timestamp);
        long lReceiveTime = System.currentTimeMillis();
        NTP_Timestamp.lRoundTripTime = lReceiveTime - lSendTime;
        if (NTP_Timestamp.lUnixTime != 0)
        {
            long lLocalTime = System.currentTimeMillis() / 1000;
            NTP_Timestamp.lTimeOffset = (lLocalTime - NTP_Timestamp.lUnixTime) * 1000;
            
            NTP_Timestamp.eResultCode = NTP_Client_ResultCode.NTP_Success;
            return NTP_Timestamp;
        }
        else
        {
            NTP_Timestamp.eResultCode = NTP_Client_ResultCode.NTP_ReceiveFailed;
            return NTP_Timestamp;
        }
    }
    
    /**
     * 发送NTP请求报文（客户端模式）
     * NTP报文格式：
     * - 第1字节：LI (Leap Indicator, 2位) + VN (Version Number, 3位) + Mode (3位)
     * - 0xE3 = 0b11100011
     *   - LI = 3 (时钟未同步)
     *   - VN = 4 (NTP版本4)
     *   - Mode = 3 (客户端模式)
     * @return true表示发送成功，false表示失败
     */
    private Boolean Send_TimeService_Request()
    {
        byte[] bSendBuf = new byte[NTP_PACKET_SIZE];
        bSendBuf[0] = (byte) 0xE3;
        try
        {
            DatagramPacket SendPacket = new DatagramPacket(
                bSendBuf, 
                bSendBuf.length,
                m_TimeService_IPAddress,
                NTP_Port
            );
            m_TimeService_Socket.send(SendPacket);
        }
        catch (SocketTimeoutException Ex)
        {
            System.err.println("发送超时: " + Ex.getMessage());
            return false;
        }
        catch (Exception Ex)
        {
            System.err.println("发送失败: " + Ex.getMessage());
            return false;
        }
        
        return true;
    }
    
    /**
     * 接收并解析NTP响应
     * @param NTP_Timestamp 输出对象
     * @return 填充好的的NTP_Timestamp，若失败则lUnixTime=0
     */
    private NTP_Timestamp_Data Receive(NTP_Timestamp_Data NTP_Timestamp)
    {
        byte[] bRecvBuf = new byte[NTP_PACKET_SIZE];
        DatagramPacket RecvPacket = new DatagramPacket(bRecvBuf, NTP_PACKET_SIZE);
        
        try
        {
            m_TimeService_Socket.receive(RecvPacket);
        }
        catch (SocketTimeoutException Ex)
        {
            System.err.println("接收超时: 服务器在" + SOCKET_TIMEOUT_MS + "ms内未响应。请检查网络连接或尝试其他服务器。");
            NTP_Timestamp.lUnixTime = 0;
            return NTP_Timestamp;
        }
        catch (Exception Ex)
        {
            System.err.println("接收失败: " + Ex.getMessage());
            NTP_Timestamp.lUnixTime = 0;
            return NTP_Timestamp;
        }
        
        if (RecvPacket.getLength() > 0)
        {
            long l1 = (long) bRecvBuf[40] & 0xFF;
            long l2 = (long) bRecvBuf[41] & 0xFF;
            long l3 = (long) bRecvBuf[42] & 0xFF;
            long l4 = (long) bRecvBuf[43] & 0xFF;
            long secsSince1900 = (l1 << 24) + (l2 << 16) + (l3 << 8) + l4;
            NTP_Timestamp.lUnixTime = secsSince1900 - SeventyYears;
            long lTotalSeconds = NTP_Timestamp.lUnixTime % 86400L;
            NTP_Timestamp.lHour = lTotalSeconds / 3600;
            NTP_Timestamp.lMinute = (lTotalSeconds % 3600) / 60;
            NTP_Timestamp.lSecond = lTotalSeconds % 60;
        }
        else
        {
            NTP_Timestamp.lUnixTime = 0;
        }
        
        return NTP_Timestamp;
    }
    
    /**
     * 获取客户端启动标志
     * @return true表示已启动，false表示未启动
     */
    public Boolean Get_ClientStarted_Flag()
    {
        return m_bNTP_Client_Started;
    }
    
    /**
     * 设置客户端启动标志
     * @param bClient_Started 启动标志
     */
    public void Set_ClientStarted_Flag(Boolean bClient_Started)
    {
        m_bNTP_Client_Started = bClient_Started;
    }
    
    /**
     * 关闭套接字
     */
    public void CloseSocket()
    {
        if (m_TimeService_Socket != null && !m_TimeService_Socket.isClosed())
        {
            try
            {
                m_TimeService_Socket.close();
            }
            catch (Exception Ex)
            {
                System.err.println("ERROR: 关闭套接字时出错: " + Ex.getMessage());
            }
        }
    }
    
    /**
     * 检查套接字是否已创建
     * @return Boolean 是否创建
     */
    public Boolean IsSocketCreated()
    {
        return m_TimeService_Socket != null && !m_TimeService_Socket.isClosed();
    }
}

