package cn.sleepybear.model;

import lombok.Data;

import java.io.File;

@Data
public class CertInfo {
    private File keyFile;
    private File crtFile;
    private Long expireTimeAt;
    private String domain;

    public CertInfo(File keyFile, File crtFile, Long expireTimeAt, String domain) {
        this.keyFile = keyFile;
        this.crtFile = crtFile;
        this.expireTimeAt = expireTimeAt;
        this.domain = domain;
    }
} 