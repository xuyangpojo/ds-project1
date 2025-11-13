package ntp_client;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.*;
import javax.swing.*;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 * @name GUI页面
 * @discirption GUI显示系统
 * @author xuyangpojo@icloud.com
 * @date 2025-11-13
 */
public class GUI extends javax.swing.JFrame implements ActionListener
{
    private Client m_NTP_Client;
    private int m_iNumRequestsSent;
    private int m_iNumResponsesReceived;
    private long m_lTotalRoundTripTime;
    private Timer m_Timer_SendNTPRequests;
    private static final int INITIAL_DELAY_MS = 100;
    private static final int REQUEST_INTERVAL_MS = 1000;
    private DefaultListModel<String> m_listModel_NTPServerList;
    private DefaultListModel<String> m_listModel_LocationList;
    private ListSelectionListener m_SelectionListener_NTPServerURLs;
    
    public static void main(String args[]) throws Exception
    {
        try
        {
            javax.swing.UIManager.LookAndFeelInfo[] installedLookAndFeels =
                javax.swing.UIManager.getInstalledLookAndFeels();
            for (int idx = 0; idx < installedLookAndFeels.length; idx++)
            {
                if ("Nimbus".equals(installedLookAndFeels[idx].getName()))
                {
                    javax.swing.UIManager.setLookAndFeel(installedLookAndFeels[idx].getClassName());
                    break;
                }
            }
        }
        catch (Exception Ex)
        {
            System.err.println("设置外观失败: " + Ex.getMessage());
        }
        java.awt.EventQueue.invokeAndWait(new Runnable()
        {
            public void run()
            {
                new GUI().setVisible(true);
            }
        });
    }

    public GUI()
    {
        m_SelectionListener_NTPServerURLs = new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent listSelectionEvent) {
                if (!listSelectionEvent.getValueIsAdjusting()) {
                    OnServerSelectionChanged();
                }
            }
        };
        m_listModel_NTPServerList = new DefaultListModel<String>();
        m_listModel_LocationList = new DefaultListModel<String>();
        initComponents();
        Populate_NTP_Server_List();
        m_NTP_Client = new Client();
        Boolean bSocketOpenSuccess = m_NTP_Client.CreateSocket();
        if (!bSocketOpenSuccess)
        {
            JOptionPane.showMessageDialog(
                null,
                "创建套接字失败, 无法启动NTP客户端。\n请检查网络连接和防火墙设置。",
                "NTP客户端错误",
                JOptionPane.ERROR_MESSAGE
            );
            CloseSocketAndExit();
            return;
        }
        m_iNumRequestsSent = 0;
        m_iNumResponsesReceived = 0;
        m_lTotalRoundTripTime = 0;
        UpdateStatisticsDisplay();
        InitialiseControls();
    }
    
    private void InitialiseControls()
    {
        jPanel_NTPServerAddressDetails.setEnabled(false);
        jTextField_URL.setEnabled(false);
        jTextField_Port.setEnabled(false);
        jTextField_ServerIPAddress.setEnabled(false);
        jTextField_UNIX_Time.setEnabled(false);
        jTextField_UTC_Time.setEnabled(false);
        jTextField_RoundTripTime.setEnabled(false);
        jTextField_TimeOffset.setEnabled(false);
        jTextField_NumRequestsSent.setEnabled(false);
        jTextField_NumResponsesReceived.setEnabled(false);
        jTextField_SuccessRate.setEnabled(false);
        jTextField_AvgRoundTripTime.setEnabled(false);
        jList_NTPServerURLs.setEnabled(true);
        JScrollPane_NTPServerURLs.setEnabled(true);
        jList_NTPServerLocations.setEnabled(false);
        JScrollPane_NTPServerLocations.setEnabled(false);
        jButton_StartNTPClient.setEnabled(true);
        jButton_Done.setEnabled(true);
        Initialise_ServerURL_listBox();
    }
    
    private void OnServerSelectionChanged()
    {
        int iSelectionIndex = jList_NTPServerURLs.getSelectedIndex();
        if (iSelectionIndex >= 0 && iSelectionIndex < jList_NTPServerLocations.getModel().getSize())
        {
            jList_NTPServerLocations.setSelectedIndex(iSelectionIndex);
            Get_ServerURL_listBox_Selection();
            jTextField_UNIX_Time.setText("");
            jTextField_UTC_Time.setText("");
            jTextField_RoundTripTime.setText("");
            jTextField_TimeOffset.setText("");
            m_iNumRequestsSent = 0;
            m_iNumResponsesReceived = 0;
            m_lTotalRoundTripTime = 0;
            UpdateStatisticsDisplay();
        }
    }
    
    void Start_Timer_SendNTPRequests()
    {
        m_Timer_SendNTPRequests = new Timer("NTPRequestTimer", true);
        m_Timer_SendNTPRequests.scheduleAtFixedRate(
            new Get_NTP_Timestamp(),
            INITIAL_DELAY_MS,
            REQUEST_INTERVAL_MS
        );
    }
    
    class Get_NTP_Timestamp extends TimerTask
    {
        public void run()
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    Client.NTP_Timestamp_Data NTP_Timestamp = m_NTP_Client.Get_NTP_Timestamp();
                    
                    switch (NTP_Timestamp.eResultCode)
                    {
                        case NTP_Success:
                            m_iNumRequestsSent++;
                            m_iNumResponsesReceived++;
                            m_lTotalRoundTripTime += NTP_Timestamp.lRoundTripTime;
                            jTextField_UNIX_Time.setText(Long.toString(NTP_Timestamp.lUnixTime));
                            String sUTC_Time = String.format("%02d:%02d:%02d",
                                NTP_Timestamp.lHour,
                                NTP_Timestamp.lMinute,
                                NTP_Timestamp.lSecond
                            );
                            jTextField_UTC_Time.setText(sUTC_Time);
                            jTextField_RoundTripTime.setText(
                                String.format("%d ms", NTP_Timestamp.lRoundTripTime)
                            );
                            String sTimeOffset = String.format("%+d ms", NTP_Timestamp.lTimeOffset);
                            jTextField_TimeOffset.setText(sTimeOffset);
                            UpdateStatisticsDisplay();
                            break;
                            
                        case NTP_ServerAddressNotSet:
                            // 服务器地址未设置，不计数
                            break;
                        case NTP_SendFailed:
                            // 发送失败，增加请求计数并更新统计
                            m_iNumRequestsSent++;
                            UpdateStatisticsDisplay();
                            break;
                        case NTP_ReceiveFailed:
                            // 接收失败，增加请求计数并更新统计
                            m_iNumRequestsSent++;
                            UpdateStatisticsDisplay();
                            break;
                    }
                }
            });
        }
    }
    
    void Populate_NTP_Server_List()
    {
        m_listModel_NTPServerList.clear();
        m_listModel_LocationList.clear();
        
        // 国内常用服务器
        m_listModel_NTPServerList.addElement("ntp.aliyun.com");
        m_listModel_LocationList.addElement("阿里云NTP服务器（推荐）");
        
        m_listModel_NTPServerList.addElement("ntp1.aliyun.com");
        m_listModel_LocationList.addElement("阿里云NTP服务器（备用）");
        
        m_listModel_NTPServerList.addElement("cn.pool.ntp.org");
        m_listModel_LocationList.addElement("NTP Pool中国区域");
        
        m_listModel_NTPServerList.addElement("pool.ntp.org");
        m_listModel_LocationList.addElement("NTP Pool（自动选择最近服务器）");
        
        // 国际常用服务器（国内通常可访问）
        m_listModel_NTPServerList.addElement("time.windows.com");
        m_listModel_LocationList.addElement("微软Windows时间服务器");
        
        m_listModel_NTPServerList.addElement("time.apple.com");
        m_listModel_LocationList.addElement("苹果时间服务器");
        
        m_listModel_NTPServerList.addElement("time.cloudflare.com");
        m_listModel_LocationList.addElement("Cloudflare时间服务器");
        
        // NTP Pool不同层级
        m_listModel_NTPServerList.addElement("0.pool.ntp.org");
        m_listModel_LocationList.addElement("NTP Pool（Stratum 0）");
        
        m_listModel_NTPServerList.addElement("1.pool.ntp.org");
        m_listModel_LocationList.addElement("NTP Pool（Stratum 1）");
        
        m_listModel_NTPServerList.addElement("2.pool.ntp.org");
        m_listModel_LocationList.addElement("NTP Pool（Stratum 2）");
    }
    
    private void Initialise_ServerURL_listBox()
    {
        if (m_listModel_NTPServerList.size() > 0)
        {
            jList_NTPServerURLs.setSelectedIndex(0);
            jList_NTPServerLocations.setSelectedIndex(0);
            Get_ServerURL_listBox_Selection();
        }
    }
    
    private void Get_ServerURL_listBox_Selection()
    {
        Object selectedValue = jList_NTPServerURLs.getSelectedValue();
        if (selectedValue != null)
        {
            String sSelectedURL = selectedValue.toString();
            jTextField_URL.setText(sSelectedURL);
            SetUp_TimeService_AddressStruct(sSelectedURL);
        }
    }
    
    void SetUp_TimeService_AddressStruct(String sURL)
    {
        InetAddress TimeService_IPAddress = m_NTP_Client.SetUp_TimeService_AddressStruct(sURL);
        if (TimeService_IPAddress != null)
        {
            jTextField_ServerIPAddress.setText(TimeService_IPAddress.getHostAddress());
            jTextField_Port.setText(Integer.toString(m_NTP_Client.GetPort()));
        }
        else
        {
            jTextField_ServerIPAddress.setText("未找到");
            jTextField_Port.setText("");
            JOptionPane.showMessageDialog(
                this,
                "无法解析服务器地址: " + sURL + "\n请检查网络连接或选择其他服务器。",
                "DNS解析失败",
                JOptionPane.WARNING_MESSAGE
            );
        }
    }
    
    void UpdateStatisticsDisplay()
    {
        jTextField_NumRequestsSent.setText(Integer.toString(m_iNumRequestsSent));
        jTextField_NumResponsesReceived.setText(Integer.toString(m_iNumResponsesReceived));
        
        String sSuccessRate;
        if (m_iNumRequestsSent > 0)
        {
            double dSuccessRate = (double) m_iNumResponsesReceived / m_iNumRequestsSent * 100.0;
            sSuccessRate = String.format("%.1f%%", dSuccessRate);
        }
        else
        {
            sSuccessRate = "0.0%";
        }
        jTextField_SuccessRate.setText(sSuccessRate);
        
        String sAvgRTT;
        if (m_iNumResponsesReceived > 0)
        {
            long lAvgRTT = m_lTotalRoundTripTime / m_iNumResponsesReceived;
            sAvgRTT = String.format("%d ms", lAvgRTT);
        }
        else
        {
            sAvgRTT = "0 ms";
        }
        jTextField_AvgRoundTripTime.setText(sAvgRTT);
        
        jTextField_NumRequestsSent.repaint();
        jTextField_NumResponsesReceived.repaint();
        jTextField_SuccessRate.repaint();
        jTextField_AvgRoundTripTime.repaint();
        
        if (jPanel_Time_Status != null)
        {
            jPanel_Time_Status.revalidate();
            jPanel_Time_Status.repaint();
        }
        
        java.awt.Toolkit.getDefaultToolkit().sync();
    }
    
    void StopTimer()
    {
        if (m_Timer_SendNTPRequests != null)
        {
            m_Timer_SendNTPRequests.cancel();
            m_Timer_SendNTPRequests.purge();
            m_Timer_SendNTPRequests = null;
        }
    }
    
    void CloseSocketAndExit()
    {
        StopTimer();
        if (m_NTP_Client != null)
        {
            m_NTP_Client.CloseSocket();
        }
        System.exit(0);
    }
    
    public void actionPerformed(ActionEvent e)
    {
        if (jButton_Done == e.getSource())
        {
            CloseSocketAndExit();
        }
        else if (jButton_StartNTPClient == e.getSource())
        {
            if (!m_NTP_Client.Get_ClientStarted_Flag())
            {
                jButton_StartNTPClient.setText("停止NTP请求");
                jList_NTPServerURLs.setEnabled(false);
                m_iNumRequestsSent = 0;
                m_iNumResponsesReceived = 0;
                m_lTotalRoundTripTime = 0;
                UpdateStatisticsDisplay();
                Start_Timer_SendNTPRequests();
                m_NTP_Client.Set_ClientStarted_Flag(true);
            } else {
                jButton_StartNTPClient.setText("启动NTP请求");
                jList_NTPServerURLs.setEnabled(true);
                m_NTP_Client.Set_ClientStarted_Flag(false);
                StopTimer();
            }
        }
    }
    
    private JPanel jPanel_NTPServerAddressDetails;
    private JLabel jLabel_URL;
    private JTextField jTextField_URL;
    private JLabel jLabel_Port;
    private JTextField jTextField_Port;
    private JLabel jLabel_ServerIPAddress;
    private JTextField jTextField_ServerIPAddress;
    private JPanel jPanel_Time_Status;
    private JLabel jLabel_UNIX_Time;
    private JTextField jTextField_UNIX_Time;
    private JLabel jLabel_UTC_Time;
    private JTextField jTextField_UTC_Time;
    private JLabel jLabel_RoundTripTime;
    private JTextField jTextField_RoundTripTime;
    private JLabel jLabel_TimeOffset;
    private JTextField jTextField_TimeOffset;
    private JLabel jLabel_NumRequestsSent;
    private JTextField jTextField_NumRequestsSent;
    private JLabel jLabel_NumResponsesReceived;
    private JTextField jTextField_NumResponsesReceived;
    private JLabel jLabel_SuccessRate;
    private JTextField jTextField_SuccessRate;
    private JLabel jLabel_AvgRoundTripTime;
    private JTextField jTextField_AvgRoundTripTime;
    private JPanel jPanel_NTPServerSelection;
    private JLabel jLabel_NIST_Servers;
    private JLabel jLabel_NTPServerURLs;
    private JLabel jLabel_NTPServerLocations;
    private JList<String> jList_NTPServerURLs;
    private JScrollPane JScrollPane_NTPServerURLs;
    private JList<String> jList_NTPServerLocations;
    private JScrollPane JScrollPane_NTPServerLocations;
    private JPanel jPanel_Controls;
    private JButton jButton_StartNTPClient;
    private JButton jButton_Done;
    
    private void initComponents()
    {
        jLabel_URL = new JLabel();
        jLabel_URL.setText("URL");
        jTextField_URL = new JTextField();
        jTextField_URL.setMaximumSize(new Dimension(250, 30));
        jTextField_URL.setHorizontalAlignment(JTextField.CENTER);
        
        jLabel_Port = new JLabel();
        jLabel_Port.setText("端口");
        jTextField_Port = new JTextField();
        jTextField_Port.setMaximumSize(new Dimension(120, 30));
        jTextField_Port.setHorizontalAlignment(JTextField.CENTER);
        
        jLabel_ServerIPAddress = new JLabel();
        jLabel_ServerIPAddress.setText("服务器IP地址");
        jTextField_ServerIPAddress = new JTextField();
        jTextField_ServerIPAddress.setMaximumSize(new Dimension(150, 30));
        jTextField_ServerIPAddress.setHorizontalAlignment(JTextField.CENTER);
        
        jPanel_NTPServerAddressDetails = new JPanel();
        jPanel_NTPServerAddressDetails.setPreferredSize(new Dimension(400, 120));
        jPanel_NTPServerAddressDetails.setBorder(BorderFactory.createTitledBorder("选中的NTP服务器地址"));
        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel_NTPServerAddressDetails);
        jPanel_NTPServerAddressDetails.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                .addGroup(jPanel1Layout.createSequentialGroup()
                    .addComponent(jLabel_URL)
                    .addGap(10, 10, 10)
                    .addComponent(jTextField_URL, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGroup(jPanel1Layout.createSequentialGroup()
                    .addComponent(jLabel_Port)
                    .addGap(10, 10, 10)
                    .addComponent(jTextField_Port, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGroup(jPanel1Layout.createSequentialGroup()
                    .addComponent(jLabel_ServerIPAddress)
                    .addGap(10, 10, 10)
                    .addComponent(jTextField_ServerIPAddress, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                .addGroup(jPanel1Layout.createSequentialGroup()
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                        .addComponent(jLabel_URL)
                        .addComponent(jTextField_URL, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGap(10, 10, 10)
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                        .addComponent(jLabel_Port)
                        .addComponent(jTextField_Port, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGap(10, 10, 10)
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                        .addComponent(jLabel_ServerIPAddress)
                        .addComponent(jTextField_ServerIPAddress, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
        );
        
        jLabel_UNIX_Time = new JLabel();
        jLabel_UNIX_Time.setText("UNIX时间");
        jTextField_UNIX_Time = new JTextField();
        jTextField_UNIX_Time.setMaximumSize(new Dimension(150, 30));
        jTextField_UNIX_Time.setHorizontalAlignment(JTextField.CENTER);
        
        jLabel_UTC_Time = new JLabel();
        jLabel_UTC_Time.setText("UTC时间");
        jTextField_UTC_Time = new JTextField();
        jTextField_UTC_Time.setMaximumSize(new Dimension(120, 30));
        jTextField_UTC_Time.setHorizontalAlignment(JTextField.CENTER);
        
        jLabel_RoundTripTime = new JLabel();
        jLabel_RoundTripTime.setText("往返时间(RTT)");
        jTextField_RoundTripTime = new JTextField();
        jTextField_RoundTripTime.setMaximumSize(new Dimension(100, 30));
        jTextField_RoundTripTime.setHorizontalAlignment(JTextField.CENTER);
        
        jLabel_TimeOffset = new JLabel();
        jLabel_TimeOffset.setText("时间偏移");
        jTextField_TimeOffset = new JTextField();
        jTextField_TimeOffset.setMaximumSize(new Dimension(120, 30));
        jTextField_TimeOffset.setHorizontalAlignment(JTextField.CENTER);
        
        jLabel_NumRequestsSent = new JLabel();
        jLabel_NumRequestsSent.setText("发送的NTP请求数");
        jTextField_NumRequestsSent = new JTextField();
        jTextField_NumRequestsSent.setMaximumSize(new Dimension(80, 30));
        jTextField_NumRequestsSent.setHorizontalAlignment(JTextField.CENTER);
        
        jLabel_NumResponsesReceived = new JLabel();
        jLabel_NumResponsesReceived.setText("接收的NTP响应数");
        jTextField_NumResponsesReceived = new JTextField();
        jTextField_NumResponsesReceived.setMaximumSize(new Dimension(80, 30));
        jTextField_NumResponsesReceived.setHorizontalAlignment(JTextField.CENTER);
        
        jLabel_SuccessRate = new JLabel();
        jLabel_SuccessRate.setText("成功率");
        jTextField_SuccessRate = new JTextField();
        jTextField_SuccessRate.setMaximumSize(new Dimension(80, 30));
        jTextField_SuccessRate.setHorizontalAlignment(JTextField.CENTER);
        
        jLabel_AvgRoundTripTime = new JLabel();
        jLabel_AvgRoundTripTime.setText("平均往返时间");
        jTextField_AvgRoundTripTime = new JTextField();
        jTextField_AvgRoundTripTime.setMaximumSize(new Dimension(100, 30));
        jTextField_AvgRoundTripTime.setHorizontalAlignment(JTextField.CENTER);
        
        jPanel_Time_Status = new JPanel();
        jPanel_Time_Status.setPreferredSize(new Dimension(400, 350));
        jPanel_Time_Status.setBorder(BorderFactory.createTitledBorder("时间和状态"));
        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel_Time_Status);
        jPanel_Time_Status.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                .addGroup(jPanel2Layout.createSequentialGroup()
                    .addComponent(jLabel_UNIX_Time)
                    .addGap(10, 10, 10)
                    .addComponent(jTextField_UNIX_Time, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGroup(jPanel2Layout.createSequentialGroup()
                    .addComponent(jLabel_UTC_Time)
                    .addGap(10, 10, 10)
                    .addComponent(jTextField_UTC_Time, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGroup(jPanel2Layout.createSequentialGroup()
                    .addComponent(jLabel_RoundTripTime)
                    .addGap(10, 10, 10)
                    .addComponent(jTextField_RoundTripTime, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGroup(jPanel2Layout.createSequentialGroup()
                    .addComponent(jLabel_TimeOffset)
                    .addGap(10, 10, 10)
                    .addComponent(jTextField_TimeOffset, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addComponent(jLabel_NumRequestsSent)
                .addComponent(jTextField_NumRequestsSent, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(jLabel_NumResponsesReceived)
                .addComponent(jTextField_NumResponsesReceived, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(jLabel_SuccessRate)
                .addComponent(jTextField_SuccessRate, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(jLabel_AvgRoundTripTime)
                .addComponent(jTextField_AvgRoundTripTime, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                .addGroup(jPanel2Layout.createSequentialGroup()
                    .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                        .addComponent(jLabel_UNIX_Time)
                        .addComponent(jTextField_UNIX_Time, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGap(10, 10, 10)
                    .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                        .addComponent(jLabel_UTC_Time)
                        .addComponent(jTextField_UTC_Time, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGap(10, 10, 10)
                    .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                        .addComponent(jLabel_RoundTripTime)
                        .addComponent(jTextField_RoundTripTime, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGap(10, 10, 10)
                    .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                        .addComponent(jLabel_TimeOffset)
                        .addComponent(jTextField_TimeOffset, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGap(15, 15, 15)
                    .addComponent(jLabel_NumRequestsSent)
                    .addComponent(jTextField_NumRequestsSent, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGap(10, 10, 10)
                    .addComponent(jLabel_NumResponsesReceived)
                    .addComponent(jTextField_NumResponsesReceived, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGap(10, 10, 10)
                    .addComponent(jLabel_SuccessRate)
                    .addComponent(jTextField_SuccessRate, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGap(10, 10, 10)
                    .addComponent(jLabel_AvgRoundTripTime)
                    .addComponent(jTextField_AvgRoundTripTime, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        
        jLabel_NIST_Servers = new JLabel();
        jLabel_NIST_Servers.setText("提供了多个NTP服务器（优先推荐国内服务器）");
        jLabel_NTPServerURLs = new JLabel();
        jLabel_NTPServerURLs.setText("NTP服务器URL");
        jLabel_NTPServerLocations = new JLabel();
        jLabel_NTPServerLocations.setText("位置/描述");
        
        jList_NTPServerURLs = new JList<String>(m_listModel_NTPServerList);
        jList_NTPServerURLs.setMaximumSize(new Dimension(300, 250));
        jList_NTPServerURLs.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        jList_NTPServerURLs.addListSelectionListener(m_SelectionListener_NTPServerURLs);
        JScrollPane_NTPServerURLs = new javax.swing.JScrollPane(
            jList_NTPServerURLs,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
        );
        
        jList_NTPServerLocations = new JList<String>(m_listModel_LocationList);
        jList_NTPServerLocations.setMaximumSize(new Dimension(300, 250));
        jList_NTPServerLocations.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane_NTPServerLocations = new javax.swing.JScrollPane(
            jList_NTPServerLocations,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
        );
        
        jPanel_NTPServerSelection = new JPanel();
        jPanel_NTPServerSelection.setPreferredSize(new Dimension(500, 300));
        jPanel_NTPServerSelection.setBorder(BorderFactory.createTitledBorder("NTP服务器选择"));
        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel_NTPServerSelection);
        jPanel_NTPServerSelection.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                .addGroup(jPanel3Layout.createSequentialGroup()
                    .addComponent(jLabel_NIST_Servers))
                .addGroup(jPanel3Layout.createSequentialGroup()
                    .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                        .addComponent(jLabel_NTPServerURLs)
                        .addComponent(JScrollPane_NTPServerURLs, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                        .addComponent(jLabel_NTPServerLocations)
                        .addComponent(JScrollPane_NTPServerLocations, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                .addGroup(jPanel3Layout.createSequentialGroup()
                    .addComponent(jLabel_NIST_Servers)
                    .addGap(20, 20, 20)
                    .addGroup(jPanel3Layout.createParallelGroup()
                        .addGroup(jPanel3Layout.createSequentialGroup()
                            .addComponent(jLabel_NTPServerURLs)
                            .addComponent(JScrollPane_NTPServerURLs, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGroup(jPanel3Layout.createSequentialGroup()
                            .addComponent(jLabel_NTPServerLocations)
                            .addComponent(JScrollPane_NTPServerLocations, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
        );
        
        jButton_StartNTPClient = new JButton();
        jButton_StartNTPClient.setText("启动NTP请求");
        jButton_StartNTPClient.setMaximumSize(new Dimension(150, 35));
        jButton_StartNTPClient.addActionListener(this);
        
        jButton_Done = new JButton();
        jButton_Done.setText("完成");
        jButton_Done.setMaximumSize(new Dimension(100, 35));
        jButton_Done.addActionListener(this);
        
        jPanel_Controls = new JPanel();
        jPanel_Controls.setPreferredSize(new Dimension(500, 100));
        jPanel_Controls.setBorder(BorderFactory.createTitledBorder("控制"));
        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel_Controls);
        jPanel_Controls.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                .addGroup(jPanel4Layout.createSequentialGroup()
                    .addGap(150, 150, 150)
                    .addComponent(jButton_StartNTPClient, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGap(50, 50, 50)
                    .addComponent(jButton_Done, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGap(150, 150, 150))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                .addGroup(jPanel4Layout.createParallelGroup()
                    .addComponent(jButton_StartNTPClient, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton_Done, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        
        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        getContentPane().setPreferredSize(new Dimension(950, 500));
        
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                .addGroup(layout.createSequentialGroup()
                    .addGroup(layout.createParallelGroup()
                        .addComponent(jPanel_NTPServerAddressDetails, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jPanel_Time_Status, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createParallelGroup()
                        .addComponent(jPanel_NTPServerSelection, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jPanel_Controls, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                .addGroup(layout.createSequentialGroup()
                    .addComponent(jPanel_NTPServerAddressDetails, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGap(20, 20, 20)
                    .addComponent(jPanel_Time_Status, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGroup(layout.createSequentialGroup()
                    .addComponent(jPanel_NTPServerSelection, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGap(20, 20, 20)
                    .addComponent(jPanel_Controls, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setTitle("网络时间协议(NTP)客户端");
        pack();
        setLocationRelativeTo(null);
    }
}

