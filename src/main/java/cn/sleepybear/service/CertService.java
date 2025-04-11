package cn.sleepybear.service;

import cn.sleepybear.App;
import cn.sleepybear.model.CertInfo;
import cn.sleepybear.model.CloudApiKey;
import cn.sleepybear.model.CloudPlatformActionBase;
import cn.sleepybear.model.PlatformSslCertInfo;
import cn.sleepybear.util.CertUtils;
import cn.sleepybear.util.CommonUtils;
import cn.sleepybear.util.LogUtil;
import lombok.NonNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class CertService {
    public static final List<CloudApiKey> apiKeys = new ArrayList<>();

    public CertService() {
        loadApiKeys();
    }

    public void startLoop() {
        // 虚拟线程启动循环任务
        Thread.ofVirtual().start(() -> {
            while (true) {
                try {
                    // 每隔一段时间执行任务
                    TimeUnit.MINUTES.sleep(5);

                } catch (InterruptedException e) {
                    break;
                }
            }
        });
    }

    private void loadApiKeys() {
        String keyTextFile = App.appConfig.getKeyTextFile();
        if (CommonUtils.notNullOrEmpty(keyTextFile)) {
            apiKeys.clear();
            apiKeys.addAll(loadApiKeys(keyTextFile));
        } else {
            LogUtil.warn("没有配置 API key 文件路径，跳过加载");
        }
    }

    public static List<CloudApiKey> loadApiKeys(String keyTextFile) {
        ArrayList<CloudApiKey> cloudApiKeys = new ArrayList<>();

        File keyFile = new File(keyTextFile);
        if (!keyFile.exists() || !keyFile.isFile()) {
            LogUtil.warn("在路径 %s 下没有找到 API key 的文件".formatted(keyTextFile));
            return cloudApiKeys;
        }

        // 读取 API 密钥文件
        try (BufferedReader reader = new BufferedReader(new FileReader(keyFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();

                // 忽略空行和注释行
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                String[] parts = line.split("@@");
                if (parts.length >= 3) {
                    String cloudPlatform = parts[0].trim();
                    String secretId = parts[1].trim();
                    String secretKey = parts[2].trim();
                    String accountId = parts.length > 3 ? parts[3].trim() : "";

                    if (CloudPlatformActionBase.validPlatforms(cloudPlatform)) {
                        if (CommonUtils.notNullOrEmpty(cloudPlatform) && CommonUtils.notNullOrEmpty(secretId) && CommonUtils.notNullOrEmpty(secretKey)) {
                            cloudApiKeys.add(new CloudApiKey(cloudPlatform, secretId, secretKey, accountId));
                            LogUtil.info("加载 API key: %s - %s - %s".formatted(cloudPlatform, secretId, accountId));
                        }
                    } else {
                        LogUtil.warn("无效的云平台: %s".formatted(cloudPlatform));
                    }
                }
            }
        } catch (IOException e) {
            LogUtil.error("读取 API key 的本地文件失败: %s".formatted(e.getMessage()), e);
        }

        LogUtil.info("加载 API key 数量: %s".formatted(cloudApiKeys.size()));
        return cloudApiKeys;
    }

    public static void main(String[] args) {
        String s = "C:\\Users\\xjx\\Desktop\\Nginx";
        Map<String, CertInfo> certInfoMap = scanLocalCerts(List.of(s), 4);
        for (Map.Entry<String, CertInfo> entry : certInfoMap.entrySet()) {
            String domain = entry.getKey();
            CertInfo certInfo = entry.getValue();
            System.out.println("Domain: " + domain);
            System.out.println("Key File: " + certInfo.getKeyFile().getAbsolutePath());
            System.out.println("Crt File: " + certInfo.getCrtFile().getAbsolutePath());
            System.out.println("Expire Time: " + new Date(certInfo.getExpireTimeAt()));
        }
    }

    public static Map<String, CertInfo> scanLocalCerts(List<String> sslCertPathList, int sslCertPathMaxDepth) {
        Map<String, CertInfo> certMap = new HashMap<>();

        if (sslCertPathList == null || sslCertPathList.isEmpty()) {
            LogUtil.info("没有配置 SSL 证书路径，跳过扫描");
            return certMap;
        }

        for (String path : sslCertPathList) {
            File directory = new File(path);
            if (!directory.exists() || !directory.isDirectory()) {
                LogUtil.warn("路径 %s 不存在或不是一个目录，跳过".formatted(path));
                continue;
            }

            try {
                Files.walkFileTree(Paths.get(path), EnumSet.noneOf(FileVisitOption.class), sslCertPathMaxDepth, new SimpleFileVisitor<>() {
                    @NonNull
                    @Override
                    public FileVisitResult visitFile(Path file, @NonNull BasicFileAttributes attrs) {
                        if (attrs.isRegularFile()) {
                            String fileName = file.getFileName().toString();
                            if (fileName.endsWith(".crt")) {
                                processCertFile(file.toFile(), certMap);
                            }
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @NonNull
                    @Override
                    public FileVisitResult visitFileFailed(Path file, @NonNull IOException exc) {
                        LogUtil.warn("访问文件失败: %s".formatted(file));
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (IOException e) {
                LogUtil.error("扫描目录 %s 失败: %s".formatted(path, e.getMessage()), e);
            }
        }

        return certMap;
    }

    private static void processCertFile(File crtFile, Map<String, CertInfo> certMap) {
        String fileName = crtFile.getName();
        String parentDirName = crtFile.getParentFile().getName();
        String domain = null;
        File keyFile = null;

        // 尝试从文件名获取域名
        if (fileName.endsWith("_bundle.crt")) {
            domain = fileName.substring(0, fileName.length() - "_bundle.crt".length());
            keyFile = new File(crtFile.getParentFile(), domain + ".key");
        } else if (fileName.endsWith(".crt")) {
            domain = fileName.substring(0, fileName.length() - ".crt".length());
            keyFile = new File(crtFile.getParentFile(), domain + ".key");
        }

        // 如果没有从文件名获取到域名或对应的key文件不存在，尝试从目录名获取
        if ((domain == null || !keyFile.exists()) &&
            (parentDirName.equals(domain) || parentDirName.equals(domain + "_nginx"))) {
            domain = parentDirName.replace("_nginx", "");

            // 寻找目录中的key文件
            File[] files = crtFile.getParentFile().listFiles((dir, name) -> name.endsWith(".key"));
            if (files != null && files.length > 0) {
                keyFile = files[0];
            }
        }

        // 如果找到了域名和key文件
        if (domain != null && keyFile != null && keyFile.exists()) {
            Long expireTime = CertUtils.getCertificateExpireTime(crtFile);
            if (expireTime != null) {
                certMap.put(domain, new CertInfo(keyFile, crtFile, expireTime, domain));
                LogUtil.info("找到证书: %s, 过期时间: %s".formatted(domain, new Date(expireTime)));
            }
        }
    }

    public void updateConfigWithLocalCerts(Map<String, CertInfo> certMap) {
        List<PlatformSslCertInfo> existingConfigs = App.appConfig.getPlatformSslCertInfos();
        Map<String, PlatformSslCertInfo> existingConfigMap = existingConfigs.stream()
                .collect(Collectors.toMap(PlatformSslCertInfo::getDomain, config -> config, (a, b) -> a));

        for (Map.Entry<String, CertInfo> entry : certMap.entrySet()) {
            String domain = entry.getKey();
            CertInfo certInfo = entry.getValue();

            PlatformSslCertInfo config = existingConfigMap.getOrDefault(domain, new PlatformSslCertInfo());
            config.setDomain(domain);

            // 设置路径信息
            config.setLocalParentFoldPath(certInfo.getCrtFile().getParentFile().getAbsolutePath());
            config.setKeyFilename(certInfo.getKeyFile().getName());
            config.setCrtFilename(certInfo.getCrtFile().getName());
            config.setExpireTimeAt(certInfo.getExpireTimeAt());

            // 如果是新配置，添加 id 并设置默认值
            if (!existingConfigMap.containsKey(domain)) {
                config.setId(CommonUtils.randomString(8));
                config.setEnable(false);
                existingConfigs.add(config);
            }

            existingConfigMap.put(domain, config);
        }

        App.appConfig.setPlatformSslCertInfos(new ArrayList<>(existingConfigMap.values()));
        App.appConfig.save();
    }

    public void queryCloudCerts() {
        if (apiKeys.isEmpty()) {
            LogUtil.warn("没有找到有效的 API 密钥，无法查询云平台的证书信息");
            return;
        }

        List<PlatformSslCertInfo> existingConfigs = App.appConfig.getPlatformSslCertInfos();
        Map<String, PlatformSslCertInfo> existingConfigMap = existingConfigs.stream().collect(Collectors.toMap(PlatformSslCertInfo::getDomain, config -> config, (a, b) -> a));

        for (CloudApiKey apiKey : apiKeys) {
            try {
                CloudPlatformActionBase cloudPlatformActionBase = new CloudPlatformActionBase();
                List<PlatformSslCertInfo> certInfoList = cloudPlatformActionBase.getCertInfoList(apiKey);
                for (PlatformSslCertInfo certInfo : certInfoList) {
                    String domain = certInfo.getDomain();
                    PlatformSslCertInfo config = existingConfigMap.getOrDefault(domain, new PlatformSslCertInfo());
                    config.setDomain(domain);
                    config.setCertId(certInfo.getCertId());
                    config.setCertStatus(certInfo.getCertStatus());
                    config.setCertRemark(certInfo.getCertRemark());
                    config.setExpireTimeAt(certInfo.getExpireTimeAt());
                    config.setAccountId(apiKey.getAccountId());

                    // 如果是新配置，添加 id 并设置默认值
                    if (!existingConfigMap.containsKey(domain)) {
                        config.setId(CommonUtils.randomString(8));
                        config.setEnable(false);
                        existingConfigs.add(config);
                    }

                    existingConfigMap.put(domain, config);
                }
            } catch (Exception e) {
                LogUtil.error("查询腾讯云证书失败: %s".formatted(e.getMessage()), e);
            }
        }

        App.appConfig.setPlatformSslCertInfos(new ArrayList<>(existingConfigMap.values()));
        App.appConfig.save();
    }

}