package com.license.validator.svr;

import com.license.validator.svr.AbstractServerInfos;

import java.net.InetAddress;
import java.net.SocketException;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用于获取客户Windows服务器的基本信息
 *
 * @author luchaoxin
 * @version V 1.0
 * @since 2023-11-06
 */
public class WindowsServerInfos extends AbstractServerInfos {

    @Override
    protected Set<String> getIpAddress() throws SocketException {
        //获取所有网络接口
        List<InetAddress> inetAddresses = getLocalAllInetAddress();
        if (inetAddresses != null && !inetAddresses.isEmpty()) {
            return inetAddresses.stream()
                    .map(InetAddress::getHostAddress)
                    .map(String::toLowerCase)
                    .collect(Collectors.toSet());
        }
        return Set.of();
    }

    @Override
    protected Set<String> getMacAddress() throws SocketException {
        //1. 获取所有网络接口
        List<InetAddress> inetAddresses = getLocalAllInetAddress();
        if (inetAddresses != null && !inetAddresses.isEmpty()) {
            //2. 获取所有网络接口的Mac地址
            return inetAddresses.stream()
                    .map(this::getMacByInetAddress)
                    .collect(Collectors.toSet());
        }
        return Set.of();
    }

    @Override
    protected String getCPUSerial() throws Exception {
        //序列号
        String serialNumber = "";
        //使用WMIC获取CPU序列号
        Process process = Runtime.getRuntime().exec("wmic cpu get process orid");
        Scanner scanner = new Scanner(process.getInputStream());
        try (scanner) {
            if (scanner.hasNext()) {
                scanner.next();
            }
            if (scanner.hasNext()) {
                serialNumber = scanner.next().trim();
            }
            return serialNumber;
        }
    }

    @Override
    protected String getMainBoardSerial() throws Exception {
        //序列号
        String serialNumber = "";

        //使用WMIC获取主板序列号
        Process process = Runtime.getRuntime().exec("wmic baseboard get serial number");
        process.getOutputStream().close();
        Scanner scanner = new Scanner(process.getInputStream());
        try (scanner) {
            if (scanner.hasNext()) {
                scanner.next();
            }
            if (scanner.hasNext()) {
                serialNumber = scanner.next().trim();
            }

            return serialNumber;
        }
    }
}
