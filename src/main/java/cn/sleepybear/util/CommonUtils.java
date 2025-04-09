package cn.sleepybear.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.net.ServerSocket;

/**
 * There is description
 *
 * @author sleepybear
 * @date 2025/04/05 23:15
 */
public class CommonUtils {
    public static final Gson GSON = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").setPrettyPrinting().create();

    public static boolean isPortInUse(int port) {
        try (var socket = new ServerSocket(port)) {
            return false; // Port is not in use
        } catch (java.io.IOException e) {
            return true; // Port is in use
        }
    }

    public static String randomString(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int randomChar = (int) (Math.random() * 62);
            if (randomChar < 10) {
                sb.append((char) ('0' + randomChar));
            } else if (randomChar < 36) {
                sb.append((char) ('A' + randomChar - 10));
            } else {
                sb.append((char) ('a' + randomChar - 36));
            }
        }
        return sb.toString();
    }

    public static boolean notNullOrEmpty(String str) {
        return str != null && !str.isEmpty();
    }
}
