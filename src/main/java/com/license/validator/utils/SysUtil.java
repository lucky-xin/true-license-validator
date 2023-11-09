package com.license.validator.utils;

import com.license.validator.svr.AbstractServerInfos;
import com.license.validator.svr.LinuxServerInfos;
import com.license.validator.svr.ServerInfo;
import com.license.validator.svr.WindowsServerInfos;

/**
 * Sys
 *
 * @author luchaoxin
 * @version V 1.0
 * @since 2023-11-07
 */
public class SysUtil {

    /**
     * 获取当前服务器需要额外校验的License参数
     *
     * @return demo.LicenseCheckModel
     */
    public static ServerInfo getServerInfo() throws Exception {
        //操作系统类型
        String osName = System.getProperty("os.name").toLowerCase();
        AbstractServerInfos abstractServerInfos = null;
        //根据不同操作系统类型选择不同的数据获取方法
        if (osName.startsWith("windows")) {
            abstractServerInfos = new WindowsServerInfos();
        } else if (osName.startsWith("linux")) {
            abstractServerInfos = new LinuxServerInfos();
        } else {
            abstractServerInfos = new LinuxServerInfos();
        }
        return abstractServerInfos.getServerInfos();
    }
}
