package cn.sleepybear.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Scanner;

public class CertUtils {
    public static Long getCertificateExpireTime(File certFile) {
        try {
            // 读取证书文件内容
            String certContent = readFileContent(certFile);
            if (certContent == null || certContent.isEmpty()) {
                return null;
            }

            // 判断PEM格式证书内容
            if (!isPemContentValid(certContent)) {
                return null;
            }

            // 解析证书
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            X509Certificate cert = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(certContent.getBytes()));

            // 获取过期时间
            Date expireDate = cert.getNotAfter();
            return expireDate.getTime();
        } catch (CertificateException e) {
            LogUtil.error("证书解析失败: %s".formatted(certFile.getAbsolutePath()), e);
            return null;
        } catch (Exception e) {
            LogUtil.error("处理证书时发生错误: %s".formatted(certFile.getAbsolutePath()), e);
            return null;
        }
    }

    private static String readFileContent(File file) {
        try (FileInputStream fis = new FileInputStream(file);
             Scanner scanner = new Scanner(fis)) {
            scanner.useDelimiter("\\A");
            return scanner.hasNext() ? scanner.next() : "";
        } catch (Exception e) {
            LogUtil.error("读取文件失败: %s".formatted(file.getAbsolutePath()), e);
            return null;
        }
    }

    private static boolean isPemContentValid(String certContent) {
        String beginCertText = "-----BEGIN CERTIFICATE-----";
        String endCertText = "-----END CERTIFICATE-----";
        return certContent.contains(beginCertText) && certContent.contains(endCertText);
    }
} 