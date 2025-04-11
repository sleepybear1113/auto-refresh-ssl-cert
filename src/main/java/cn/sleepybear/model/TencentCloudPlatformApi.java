package cn.sleepybear.model;

import cn.sleepybear.service.CertService;
import cn.sleepybear.util.TencentCloudApi;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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

        TencentCloudApi.DescribeCertificatesResponse describeCertificatesResponse = TencentCloudApi.describeCertificates(new TencentCloudApi.QueryParams(), apiKey.getSecretId(), apiKey.getSecretKey());
        if (describeCertificatesResponse == null) {
            return list;
        }

        List<TencentCloudApi.DescribeCertificatesResponse.Certificates> certificates = describeCertificatesResponse.getCertificates();
        if (certificates == null || certificates.isEmpty()) {
            return list;
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        for (TencentCloudApi.DescribeCertificatesResponse.Certificates certificate : certificates) {
            PlatformSslCertInfo config = new PlatformSslCertInfo();
            config.setId(certificate.getCertificateId());
            config.setCloudPlatform("tencent");
            config.setDomain(certificate.getDomain());
            config.setStartTimeAt(dateFormat.parse(certificate.getCertBeginTime()).getTime());
            config.setCreateTimeAt(dateFormat.parse(certificate.getInsertTime()).getTime());
            config.setExpireTimeAt(dateFormat.parse(certificate.getCertEndTime()).getTime());
            config.setValidDays(certificate.getValidityPeriod() * 30);
            config.setCertId(certificate.getCertificateId());
            config.setCertStatus(certificate.getStatus());
            config.setCertStatusName(certificate.getStatusName());
            config.setCertRemark(certificate.getAlias());
            config.setAccountId(apiKey.getAccountId());
            config.setIgnore(certificate.getIsIgnore());

            list.add(config);
        }

        return list;
    }

    public static void main(String[] args) throws Exception {
        List<CloudApiKey> cloudApiKeys = CertService.loadApiKeys("key.txt");
        if (cloudApiKeys.isEmpty()) {
            System.out.println("没有可用的 API 密钥");
            return;
        }

        CloudApiKey apiKey = cloudApiKeys.getFirst();
        if (apiKey == null) {
            System.out.println("没有可用的 API 密钥");
            return;
        }

        TencentCloudPlatformApi api = new TencentCloudPlatformApi();
        api.getCertInfoList(apiKey).forEach(System.out::println);
    }
}
