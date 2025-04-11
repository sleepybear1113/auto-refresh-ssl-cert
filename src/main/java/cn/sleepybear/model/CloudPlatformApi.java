package cn.sleepybear.model;

import java.util.List;

/**
 * 通用的云平台 API 接口，所有云平台的 API 都需要实现这个接口
 *
 * @author sleepybear
 * @date 2025/04/06 23:13
 */
public interface CloudPlatformApi {
    List<PlatformSslCertInfo> getCertInfoList(CloudApiKey apiKey) throws Exception;
}
