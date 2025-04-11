package cn.sleepybear.server;

import cn.sleepybear.App;
import cn.sleepybear.model.CertInfo;
import cn.sleepybear.service.CertService;
import cn.sleepybear.util.CommonUtils;
import cn.sleepybear.util.LogUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

public class SimpleHttpServer {
    private final CertService certService;

    private HttpServer server;

    private Boolean isRunning = false;

    public SimpleHttpServer(CertService certService) {
        this.certService = certService;
    }

    public void start(int port) {
        if (isRunning) {
            LogUtil.warn("HTTP 服务器已经在运行中，无法重复启动");
            return;
        }

        if (CommonUtils.isPortInUse(port)) {
            LogUtil.warn("端口 %s 已被占用, HTTP 服务器将不启动!".formatted(port));
            return;
        }

        // 创建 HTTP 服务器
        try {
            this.server = HttpServer.create(new InetSocketAddress(port), 0);
        } catch (IOException e) {
            LogUtil.error("创建HTTP服务器失败，端口 %s 可能被占用".formatted(port), e);
            return;
        }

        // 设置线程池
        this.server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());

        // 注册 API 路径
        this.server.createContext("/api/getConfigJson", new GetConfigJsonHandler());
        this.server.createContext("/api/refreshLocalSslCert", new RefreshLocalSslCertHandler());
        this.server.createContext("/api/getTencentCerts", new GetTencentCertsHandler());

        this.server.start();
        LogUtil.info("HTTP 服务器已启动，运行于端口 %s".formatted(port));
        isRunning = true;
    }

    public void stop() {
        if (!isRunning) {
            LogUtil.warn("HTTP 服务器未运行，无法停止");
            return;
        }
        this.server.stop(0);
        LogUtil.info("HTTP 服务器已经停止");
        isRunning = false;
    }

    private class GetConfigJsonHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                String response = CommonUtils.GSON.toJson(App.appConfig);
                sendResponse(exchange, 200, response);
            } catch (Exception e) {
                LogUtil.error("解析配置文件失败，请检查配置文件的格式。程序将使用默认配置。错误：%s".formatted(e.getMessage()), e);
                sendErrorResponse(exchange, e);
            }
        }
    }

    private class RefreshLocalSslCertHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                Map<String, Object> result = new HashMap<>();
                Map<String, Object> certs = new HashMap<>();

                Map<String, CertInfo> certInfoMap = CertService.scanLocalCerts(App.appConfig.getSslCertPathList(), App.appConfig.getSslCertPathMaxDepth());
                certService.updateConfigWithLocalCerts(certInfoMap);
                certInfoMap.forEach((domain, certInfo) -> {
                    Map<String, Object> cert = new HashMap<>();
                    cert.put("keyFile", certInfo.getKeyFile().getAbsolutePath());
                    cert.put("crtFile", certInfo.getCrtFile().getAbsolutePath());
                    cert.put("expireTimeAt", certInfo.getExpireTimeAt());
                    certs.put(domain, cert);
                });

                result.put("status", "success");
                result.put("message", "Local SSL certificates refreshed");
                result.put("certs", certs);

                sendResponse(exchange, 200, CommonUtils.GSON.toJson(result));
            } catch (Exception e) {
                LogUtil.error("刷新本地证书失败: %s".formatted(e.getMessage()), e);
                sendErrorResponse(exchange, e);
            }
        }
    }

    private class GetTencentCertsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                certService.queryCloudCerts();

                Map<String, Object> result = new HashMap<>();
                result.put("status", "success");
                result.put("message", "Tencent Cloud certificates queried");
                result.put("config", App.appConfig);

                sendResponse(exchange, 200, CommonUtils.GSON.toJson(result));
            } catch (Exception e) {
                LogUtil.error("查询腾讯云证书失败: %s".formatted(e.getMessage()), e);
                sendErrorResponse(exchange, e);
            }
        }
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, response.getBytes(StandardCharsets.UTF_8).length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes(StandardCharsets.UTF_8));
        }
    }

    private void sendErrorResponse(HttpExchange exchange, Exception e) throws IOException {
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("status", "error");
        errorResponse.put("message", e.getMessage());

        String response = CommonUtils.GSON.toJson(errorResponse);
        sendResponse(exchange, 500, response);
    }
} 