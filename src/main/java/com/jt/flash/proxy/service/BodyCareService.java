package com.jt.flash.proxy.service;

import com.jt.flash.proxy.bean.Body;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * since 2016/6/25.
 */
@Service
public class BodyCareService {

    private static final Logger log = LoggerFactory.getLogger(BodyCareService.class);
    private static Pattern cpu_match = Pattern.compile(".*ni,\\s([\\d\\.]{3,})%id[,\\w\\s%\\.]*");

    private String osName = System.getProperty("os.name");

    public static Body cache = null;

    public Body getBody() {
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapMemoryUsage = memoryMXBean.getHeapMemoryUsage();
        long used = heapMemoryUsage.getUsed();
        long max = heapMemoryUsage.getMax();
        int blue = 0;
        if (!StringUtils.containsIgnoreCase(osName, "windows")) {
            blue = getCpuRateForLinux();
        }
        Body body = new Body(one(used, max), blue);
        return body;
    }

    private int one(long d, long a) {
        return (int) (100 - d * 100 / a);
    }

    public long getUsedMemoryMB() {
        return (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1048576; // 1024 * 1024 =
        // 1048576;
    }

    public long getFreeMemoryMB() {
        return Runtime.getRuntime().freeMemory() / 1048576; // 1024 * 1024 = 1048576;
    }

    public static String parseCpu(String s) {
        Matcher matcher = cpu_match.matcher(s);
        if (matcher.find()) {
            int count = matcher.groupCount();
            if (count == 1) {
                return matcher.group(count);
            }
        }
        return null;
    }

    private static int getCpuRateForLinux() {
        BufferedReader reader = null;
        try {
            //Cpu(s):  0.3%us,  0.2%sy,  0.0%ni, 99.5%id,  0.0%wa,  0.0%hi,  0.0%si,  0.0%st
            Process process = Runtime.getRuntime().exec("top -b -n 1");
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String s = null;
            while ((s = reader.readLine()) != null) {
                String cpuRate = parseCpu(s);
                if (cpuRate != null) {
                    log.info("cpu rate {}", cpuRate);
                    return NumberUtils.toInt(cpuRate);
                }
            }
        } catch (Exception e) {
            log.warn("parse cpu rate fail", e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return 0;
    }
}
