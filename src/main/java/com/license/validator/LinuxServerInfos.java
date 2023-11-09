package com.license.validator;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用于获取客户Linux服务器的基本信息
 *
 * @author luchaoxin
 * @version V 1.0
 * @since 2023-11-06
 */
public class LinuxServerInfos extends AbstractServerInfos {

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

        //使用dmidecode命令获取CPU序列号
        String[] shell = {
                "/bin/bash",
                "-c",
                "dmidecode -t processor | grep 'ID' | awk -F ':' '{print $2}' | head -n 1"
        };
        return getString(serialNumber, shell);
    }

    private String getString(String serialNumber, String[] shell) throws IOException {
        Process process = Runtime.getRuntime().exec(shell);
        try (InputStream in = process.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));) {
            String line = reader.readLine();
            if (line != null && !line.isEmpty()) {
                serialNumber = line;
            }
        }
        return serialNumber;
    }

    @Override
    protected String getMainBoardSerial() throws Exception {
        //序列号
        String serialNumber = "";
        //使用dmidecode命令获取主板序列号
        String[] shell = {
                "/bin/bash",
                "-c",
                "dmidecode | grep 'Serial Number' | awk -F ':' '{print $2}' | head -n 1"};
        return getString(serialNumber, shell);
    }
}
