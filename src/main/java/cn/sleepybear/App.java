package cn.sleepybear;

import cn.sleepybear.config.AppConfig;
import cn.sleepybear.config.CommandLineArgs;
import cn.sleepybear.server.SimpleHttpServer;
import cn.sleepybear.service.CertService;
import cn.sleepybear.util.LogUtil;

import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class App {
    public static AppConfig appConfig = new AppConfig();

    public static void main(String[] args) {
        LogUtil.init();

        try {
            // 解析命令行参数
            CommandLineArgs cmdArgs = CommandLineArgs.parse(args);

            // 加载配置
            appConfig = AppConfig.load(cmdArgs.getConfigPath());

            int port = appConfig.getPort();
            // 端口优先使用命令行参数
            if (cmdArgs.getPort() != null) {
                port = cmdArgs.getPort();
            }

            // 创建证书服务
            CertService certService = new CertService();
            // 启动循环任务
            certService.startLoop();

            // 如果有配置 HTTP 服务，俺么启动 HTTP 服务器
            SimpleHttpServer server = new SimpleHttpServer(certService);
            if (!Boolean.TRUE.equals(appConfig.getRunServer())) {
                LogUtil.info("配置文件中设置了不启动 HTTP 服务器，程序将不会启动 HTTP 服务器!");
            } else if (port <= 0 || port > 65535) {
                LogUtil.warn("端口不在正常范围内: %s, 将不会启动 HTTP 服务器!".formatted(port));
            } else {
                server.start(port);
            }

            // 防止主线程退出
            Thread.ofPlatform().start(() -> {
                while (true) {
                    try {
                        TimeUnit.SECONDS.sleep(1);
                    } catch (InterruptedException e) {
                        LogUtil.error("主线程被中断", e);
                        break;
                    }
                }
            });

            // 循环接收用户输入
            Scanner scanner = new Scanner(System.in);
            label:
            while (true) {
                String input = scanner.nextLine().toLowerCase();
                switch (input) {
                    case "exit":
                        LogUtil.info("程序退出");
                        break label;
                    case "start server":
                        LogUtil.info("启动 HTTP 服务器......");
                        server.start(appConfig.getPort());
                        break;
                    case "stop server":
                        LogUtil.info("停止 HTTP 服务器......");
                        server.stop();
                        break;
                    case "reload config":
                        appConfig = AppConfig.load(cmdArgs.getConfigPath());
                        LogUtil.info("配置文件重新加载成功");
                        break;
                    case "help":
                        LogUtil.info("可用命令: exit, start server, stop server, reload config, help");
                        break;
                    default:
                        LogUtil.warn("未知命令: %s".formatted(input));
                        break;
                }
            }
        } catch (Exception e) {
            LogUtil.error("程序运行失败:", e);
            System.exit(1);
        }
    }

}
