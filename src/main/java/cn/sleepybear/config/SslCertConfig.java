package cn.sleepybear.config;

import lombok.Data;

@Data
public class SslCertConfig {
    private String id;
    private String cloudPlatform;
    private String accountId;
    private String localParentFoldPath;
    private String keyFilename;
    private String crtFilename;
    private Long expireTimeAt;
    private Integer validDays = 90;
    private Boolean enable = true;
    private String domain;
    private String certId;
    private String certStatus;
    private Long lastUpdateTime;
    private String certRemark;
    private String describe;
    private Integer updatePolicy = 3;
} 