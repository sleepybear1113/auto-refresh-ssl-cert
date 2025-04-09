package cn.sleepybear.model;

import cn.sleepybear.util.TencentCloudApi;

import java.util.ArrayList;
import java.util.List;

/**
 * There is description
 *
 * @author sleepybear
 * @date 2025/04/06 23:17
 */
public class TencentCloudPlatformApi implements CloudPlatformApi {
    @Override
    public List<PlatformSslCertInfo> getCertInfoList(CloudApiKey apiKey) throws Exception {
        List<PlatformSslCertInfo> list = new ArrayList<>();

        List<TencentCloudApi.DescribeCertificatesResponse.Certificates> certificates = TencentCloudApi.describeCertificates(new TencentCloudApi.QueryParams(), apiKey.getSecretId(), apiKey.getSecretKey());

        for (TencentCloudApi.DescribeCertificatesResponse.Certificates certificate : certificates) {
            PlatformSslCertInfo config = new PlatformSslCertInfo();
            config.setId(certificate.getCertificateId());
            config.setCloudPlatform("tencent");
//            config.setDomain(certificate.getDomain());
//            config.setStartTimeAt(certificate.getStartTime());
//            config.setExpireTimeAt(certificate.getExpireTime());
            config.setValidDays(90);
            config.setCertId(certificate.getCertificateId());
//            config.setCertStatus(certificate.getCertificateStatus());
//            config.setCertRemark(certificate.getCertificateRemark());
            config.setAccountId(apiKey.getAccountId());

            list.add(config);
        }

        return list;
    }
}
