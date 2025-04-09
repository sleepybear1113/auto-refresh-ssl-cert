package cn.sleepybear.model;

import lombok.Data;

@Data
public class CloudApiKey {

    private String cloudPlatform;
    private String secretId;
    private String secretKey;
    private String accountId;

    public CloudApiKey(String cloudPlatform, String secretId, String secretKey, String accountId) {
        this.cloudPlatform = cloudPlatform;
        this.secretId = secretId;
        this.secretKey = secretKey;
        this.accountId = accountId;
    }


} 