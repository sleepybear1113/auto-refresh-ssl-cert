package cn.sleepybear.config;

import cn.sleepybear.util.CommonUtils;
import cn.sleepybear.util.LogUtil;
import lombok.Data;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Data
public class AppConfig {

    // 默认配置
    private String configPath = "config.json";
    private Integer port = 30900;
    private String keyTextFile = "key.txt";
    private Integer sslCertPathMaxDepth = 3;
    private List<String> sslCertPathList = new ArrayList<>();
    private List<SslCertConfig> sslCertConfig = new ArrayList<>();

    public static AppConfig load(String configPath) {
        File configFile = new File(configPath);
        AppConfig config = new AppConfig();
        config.setConfigPath(configPath);
        LogUtil.info("读取配置文件，位置： %s".formatted(configPath));

        if (configFile.exists() && configFile.isFile()) {
            try {
                String json = Files.readString(configFile.toPath());
                config = CommonUtils.GSON.fromJson(json, AppConfig.class);
            } catch (IOException e) {
                LogUtil.warn("解析配置文件失败，请检查配置文件的格式。程序将使用默认配置。错误：%s".formatted(e.getMessage()));
            }
        } else {
            LogUtil.info("在给定的位置找不到对应的配置文件 %s, 程序将使用默认配置。".formatted(configPath));
        }

        return config;
    }

    public void save() {
        try {
            Files.createDirectories(Paths.get(configPath).getParent());
            String json = CommonUtils.GSON.toJson(this);
            Files.writeString(Paths.get(configPath), json);
            LogUtil.info("配置文件保存至：%s".formatted(configPath));
        } catch (IOException e) {
            LogUtil.error("保存配置文件到本地失败: %s".formatted(e.getMessage()), e);
        }
    }
} 