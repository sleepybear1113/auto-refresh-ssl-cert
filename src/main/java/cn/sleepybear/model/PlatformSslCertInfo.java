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
    private String from;

    /**
     * 13 位时间戳
     */
    private Long startTimeAt;
    private Long expireTimeAt;
    private Long createTimeAt;

    /**
     * 有效天数，单位：天
     */
    private Integer validDays;
    private String certId;
    private String certStatus;
    private String certStatusName;
    private String certRemark;
    private Boolean ignore;

    // ========== 下面是证书的配置信息 ==========

    private Boolean enable;

    /**
     * 过期前多少天自动下载最新的证书
     */
    private Integer beforeExpireDays;
    /**
     * 是否开启，过期前多少天自动下载最新的证书，配合字段 {@link #beforeExpireDays} 才能使用
     */
    private Boolean autoDownload;

    /**
     * 本地证书存放路径
     */
    private String localParentFoldPath;
    /**
     * .key 证书文件名，需要配合 {@link #localParentFoldPath} 使用
     */
    private String keyFilename;
    /**
     * .crt 证书文件名，需要配合 {@link #localParentFoldPath} 使用
     */
    private String crtFilename;

    /**
     * 用户备注信息
     */
    private String describe;

    /**
     * 最后更新时间
     */
    private Long lastUpdateTime;


}
