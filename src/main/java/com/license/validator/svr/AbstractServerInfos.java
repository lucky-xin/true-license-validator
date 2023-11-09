package com.license.validator.svr;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;

/**
 * 用于获取客户服务器的基本信息，如：IP、Mac地址、CPU序列号、主板序列号等
 *
 * @author luchaoxin
 * @version V 1.0
 * @since 2023-11-06
 */
public abstract class AbstractServerInfos {

    /**
     * 组装需要额外校验的License参数
     *
     * @return demo.LicenseCheckModel
     */
    public ServerInfo getServerInfos() throws Exception {
        ServerInfo result = new ServerInfo();
        result.setIpAddrs(this.getIpAddress());
        result.setMacAddrs(this.getMacAddress());
        result.setCpuSerial(this.getCPUSerial());
        result.setBoardSerial(this.getMainBoardSerial());
        return result;
    }

    /**
     * 获取IP地址
     *
     * @return java.util.List<java.lang.String>
     */
    protected abstract Set<String> getIpAddress() throws SocketException;

    /**
     * 获取Mac地址
     *
     * @return java.util.List<java.lang.String>
     */
    protected abstract Set<String> getMacAddress() throws SocketException;

    /**
     * 获取CPU序列号
     *
     * @return java.lang.String
     */
    protected abstract String getCPUSerial() throws Exception;

    /**
     * 获取主板序列号
     *
     * @return java.lang.String
     */
    protected abstract String getMainBoardSerial() throws Exception;

    /**
     * 获取当前服务器所有符合条件的InetAddress
     *
     * @return java.util.List<java.net.InetAddress>
     */
    protected List<InetAddress> getLocalAllInetAddress() throws SocketException {
        List<InetAddress> result = new ArrayList<>(4);
        // 遍历所有的网络接口
        for (Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
             networkInterfaces.hasMoreElements(); ) {
            NetworkInterface iface = networkInterfaces.nextElement();
            // 在所有的接口下再遍历IP
            for (Enumeration<InetAddress> inetAddresses = iface.getInetAddresses(); inetAddresses.hasMoreElements(); ) {
                InetAddress inetAddr = inetAddresses.nextElement();
                //排除LoopbackAddress、SiteLocalAddress、LinkLocalAddress、MulticastAddress类型的IP地址
                if (!inetAddr.isLoopbackAddress() /*&& !inetAddr.isSiteLocalAddress()*/
                        && !inetAddr.isLinkLocalAddress() && !inetAddr.isMulticastAddress()) {
                    result.add(inetAddr);
                }
            }
        }

        return result;
    }

    /**
     * 获取某个网络接口的Mac地址
     *
     * @param
     * @return void
     */
    protected String getMacByInetAddress(InetAddress inetAddr) {
        try {
            byte[] mac = NetworkInterface.getByInetAddress(inetAddr).getHardwareAddress();
            StringBuilder stringBuffer = new StringBuilder();
            for (int i = 0; i < mac.length; i++) {
                if (i != 0) {
                    stringBuffer.append("-");
                }
                //将十六进制byte转化为字符串
                String temp = Integer.toHexString(mac[i] & 0xff);
                if (temp.length() == 1) {
                    stringBuffer.append("0").append(temp);
                } else {
                    stringBuffer.append(temp);
                }
            }
            return stringBuffer.toString().toUpperCase();
        } catch (SocketException e) {
            throw new IllegalStateException(e);
        }
    }

}
