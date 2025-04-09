package cn.sleepybear.util;

import lombok.Data;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.*;

public class LogUtil {
    private static final Logger logger = Logger.getLogger("Log");
    private static boolean initialized = false;

    @Data
    public static class Config {
        private boolean logToConsole = true;
        private boolean logToFile = false;
        private String logFilePath = "app.log";
        private int maxFileSizeInBytes = 5 * 1024 * 1024; // 5 MB
        private int maxBackupFiles = 3;
        private Level logLevel = Level.INFO;
        private Formatter formatter = new SimpleFormatter();
    }

    public static void init() {
        init(new Config());
    }

    public static void init(Config config) {
        if (initialized) {
            return;
        }
        initialized = true;

        logger.setUseParentHandlers(false); // 禁用默认控制台输出
        logger.setLevel(config.getLogLevel());

        try {
            if (config.isLogToConsole()) {
                ConsoleHandler consoleHandler = new ConsoleHandler();
                consoleHandler.setLevel(config.getLogLevel());
                consoleHandler.setFormatter(config.getFormatter());
                logger.addHandler(consoleHandler);
            }

            if (config.isLogToFile()) {
                // 确保目录存在
                Path logPath = Path.of(config.getLogFilePath()).toAbsolutePath();
                Files.createDirectories(logPath.getParent());

                // 配置文件滚动策略：按文件大小
                FileHandler fileHandler = new FileHandler(
                        logPath.toString(),
                        config.getMaxFileSizeInBytes(),
                        config.getMaxBackupFiles(),
                        true // append
                );
                fileHandler.setLevel(config.getLogLevel());
                fileHandler.setFormatter(config.getFormatter());
                logger.addHandler(fileHandler);
            }
        } catch (IOException e) {
            System.err.println("初始化Log失败: " + e.getMessage());
        }
    }

    public static void info(String message) {
        logger.info(message);
    }

    public static void warn(String message) {
        logger.warning(message);
    }

    public static void error(String message, Throwable throwable) {
        logger.log(Level.SEVERE, message, throwable);
    }

    public static void debug(String message) {
        logger.fine(message);
    }

    public static void setLevel(Level level) {
        logger.setLevel(level);
        for (Handler handler : logger.getHandlers()) {
            handler.setLevel(level);
        }
    }
}

