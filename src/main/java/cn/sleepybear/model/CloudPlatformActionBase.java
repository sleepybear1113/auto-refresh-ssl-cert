package cn.sleepybear.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * There is description
 *
 * @author sleepybear
 * @date 2025/04/06 23:07
 */
public class CloudPlatformActionBase {
    private static final Set<String> VALID_PLATFORMS = Set.of("tencent");

    public static boolean validPlatforms(String platform) {
        return VALID_PLATFORMS.contains(platform);
    }

    public List<PlatformSslCertInfo> getCertInfoList(CloudApiKey apiKey) {
        if (apiKey == null) {
            return new ArrayList<>();
        }

        String platform = apiKey.getCloudPlatform();
        if (platform == null || platform.isEmpty()) {
            return new ArrayList<>();
        }

        if (!validPlatforms(platform)) {
            return new ArrayList<>();
        }

        List<PlatformSslCertInfo> certInfoList = new ArrayList<>();
        // 根据平台类型调用相应的 API 获取证书信息
        try {
            switch (platform) {
                case "tencent":
                    // 调用腾讯云API获取证书信息
                    return new TencentCloudPlatformApi().getCertInfoList(apiKey);
                // 其他平台的处理逻辑
                case "aliyun":
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            return new ArrayList<>();
        }

        return certInfoList;
    }
}
