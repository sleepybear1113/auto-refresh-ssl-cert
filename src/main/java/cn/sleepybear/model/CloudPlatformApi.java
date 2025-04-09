package cn.sleepybear.model;

import java.util.List;

/**
 * There is description
 *
 * @author sleepybear
 * @date 2025/04/06 23:13
 */
public interface CloudPlatformApi {
    List<PlatformSslCertInfo> getCertInfoList(CloudApiKey apiKey) throws Exception;
}
