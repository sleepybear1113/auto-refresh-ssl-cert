package cn.sleepybear;

import cn.sleepybear.config.AppConfig;
import cn.sleepybear.config.CommandLineArgs;
import cn.sleepybear.server.SimpleHttpServer;
import cn.sleepybear.service.CertService;
import cn.sleepybear.util.CommonUtils;
import cn.sleepybear.util.LogUtil;

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
            if (port <= 0 || port > 65535) {
                LogUtil.warn("端口不在正常范围内: %s, 将不会启动 HTTP 服务器!".formatted(port));
            } else {
                // 判断端口是否被占用
                if (CommonUtils.isPortInUse(port)) {
                    LogUtil.warn("端口 %s 已被占用, HTTP 服务器将不启动!".formatted(port));
                } else {
                    SimpleHttpServer server = new SimpleHttpServer(port, certService);
                    server.start();
                }
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
        } catch (Exception e) {
            LogUtil.error("程序运行失败:", e);
            System.exit(1);
        }
    }

}
