package cn.sleepybear.config;

import cn.sleepybear.util.LogUtil;
import lombok.Data;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@Data
public class CommandLineArgs {
    private String configPath = "config.json";
    private String keyPath = "key.txt";
    private Integer port = 30900;

    public static CommandLineArgs parse(String[] args) {
        CommandLineArgs cmdArgs = new CommandLineArgs();

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];

            if ("-c".equals(arg) && i + 1 < args.length) {
                cmdArgs.setConfigPath(args[++i]);
                LogUtil.info("读取配置文件，位置： %s".formatted(cmdArgs.getConfigPath()));
            } else if ("-p".equals(arg) && i + 1 < args.length) {
                try {
                    int p = Integer.parseInt(args[++i]);
                    cmdArgs.setPort(p);
                    LogUtil.info("端口设置为: %s".formatted(cmdArgs.getPort()));
                } catch (NumberFormatException e) {
                    LogUtil.warn("无效的端口号: %s, 使用默认端口".formatted(args[i]));
                }
            } else if ("-k".equals(arg) && i + 1 < args.length) {
                cmdArgs.setKeyPath(args[++i]);
                LogUtil.info("密钥文件路径设置为: %s".formatted(cmdArgs.getKeyPath()));
            }
        }

        // 确保配置文件路径的目录存在
        File configFile = new File(cmdArgs.getConfigPath());
        try {
            Files.createDirectories(configFile.getParentFile().toPath());
        } catch (IOException e) {
            LogUtil.error("创建配置文件目录失败: %s".formatted(e.getMessage()), e);
        }

        return cmdArgs;
    }
} 