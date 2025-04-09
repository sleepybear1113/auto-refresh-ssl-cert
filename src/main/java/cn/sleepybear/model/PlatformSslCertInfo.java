package cn.sleepybear.model;

import lombok.Data;

/**
 * There is description
 *
 * @author sleepybear
 * @date 2025/04/06 23:22
 */
@Data
public class PlatformSslCertInfo {
    private String id;
    private String cloudPlatform;
    private String accountId;
    private String domain;
    private Long startTimeAt;
    private Long expireTimeAt;
    private Integer validDays = 90;
    private String certId;
    private String certStatus;
    private String certRemark;
}
