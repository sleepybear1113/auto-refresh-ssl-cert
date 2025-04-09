package cn.sleepybear.util;

import cn.sleepybear.model.CloudApiKey;
import cn.sleepybear.service.CertService;
import com.google.gson.Gson;
import lombok.Data;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;

public class TencentCloudApi {

    private static final String CONTENT_TYPE = "application/json; charset=utf-8";
    private static final String ENDPOINT = "ssl.tencentcloudapi.com";
    private static final String SERVICE = "ssl";
    private static final String VERSION = "2019-12-05";

    private static final Gson GSON = new Gson();

    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();


    public static void main(String[] args) throws Exception {
        List<CloudApiKey> cloudApiKeys = CertService.loadApiKeys("key.txt");
        if (cloudApiKeys.isEmpty()) {
            System.out.println("没有可用的 API 密钥");
            return;
        }

        CloudApiKey apiKey = cloudApiKeys.getFirst();

        QueryParams queryParams = new QueryParams();
        queryParams.setOffset(0);
        queryParams.setLimit(10);

        describeCertificates(queryParams, apiKey.getSecretId(), apiKey.getSecretKey());
    }

    public static List<DescribeCertificatesResponse.CertificateSet> describeCertificates(QueryParams params, String secretId, String secretKey) throws Exception {
        String action = "DescribeCertificates";
        String region = "ap-guangzhou";

        Map<String, Object> bodyParams = params.buildBodyParams();
        String payload = GSON.toJson(bodyParams);

        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date(Long.parseLong(timestamp) * 1000));

        String canonicalRequest = buildCanonicalRequest(payload);
        String credentialScope = date + "/" + SERVICE + "/tc3_request";
        String stringToSign = buildStringToSign(timestamp, credentialScope, canonicalRequest);
        byte[] signingKey = getSignatureKey(secretKey, date, SERVICE, "tc3_request");
        String signature = bytesToHex(hmacSHA256(stringToSign, signingKey)).toLowerCase();

        String authorization = String.format(
                "TC3-HMAC-SHA256 Credential=%s/%s, SignedHeaders=content-type;host, Signature=%s",
                secretId, credentialScope, signature
        );

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", CONTENT_TYPE);
        headers.put("Host", ENDPOINT);
        headers.put("Authorization", authorization);
        headers.put("X-TC-Action", action);
        headers.put("X-TC-Version", VERSION);
        headers.put("X-TC-Region", region);
        headers.put("X-TC-Timestamp", timestamp);

        String res = sendRequest(payload, headers);
        if (res == null) {
            System.out.println("请求失败，响应为空");
            return new ArrayList<>();
        }
        DescribeCertificatesResponse describeCertificatesResponse = GSON.fromJson(res, DescribeCertificatesResponse.class);
        if (describeCertificatesResponse == null) {
            System.out.println("解析响应失败");
            return new ArrayList<>();
        }

        if (describeCertificatesResponse.getCertificateSet() == null || describeCertificatesResponse.getCertificateSet().length == 0) {
            System.out.println("没有解析到证书");
            return new ArrayList<>();
        }

        return new ArrayList<>(List.of(describeCertificatesResponse.getCertificateSet()));
    }

    private static String buildCanonicalRequest(String payload) {
        String method = "POST";
        String canonicalUri = "/";
        String canonicalQueryString = "";
        String canonicalHeaders = "content-type:" + CONTENT_TYPE + "\n" +
                                  "host:" + ENDPOINT + "\n";
        String signedHeaders = "content-type;host";
        String hashedRequestPayload = sha256Hex(payload);
        return method + "\n" +
               canonicalUri + "\n" +
               canonicalQueryString + "\n" +
               canonicalHeaders + "\n" +
               signedHeaders + "\n" +
               hashedRequestPayload;
    }

    private static String buildStringToSign(String timestamp, String credentialScope, String canonicalRequest) {
        return "TC3-HMAC-SHA256\n" + timestamp + "\n" + credentialScope + "\n" + sha256Hex(canonicalRequest);
    }

    private static byte[] getSignatureKey(String secretKey, String date, String service, String requestType) throws Exception {
        byte[] secretDate = hmacSHA256(date, ("TC3" + secretKey).getBytes(StandardCharsets.UTF_8));
        byte[] secretService = hmacSHA256(service, secretDate);
        return hmacSHA256(requestType, secretService);
    }

    private static byte[] hmacSHA256(String data, byte[] key) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, mac.getAlgorithm()));
        return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
    }

    private static String sha256Hex(String s) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(s.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash).toLowerCase();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    private static String sendRequest(String payload, Map<String, String> headers) throws IOException, InterruptedException {
        // 构建请求体
        HttpRequest.BodyPublisher bodyPublisher = HttpRequest.BodyPublishers.ofString(payload);

        // 构建请求
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create("https://" + ENDPOINT))
                .timeout(Duration.ofSeconds(30))
                .POST(bodyPublisher);

        // 添加请求头
        headers.forEach(requestBuilder::header);

        HttpRequest request = requestBuilder.build();

        // 发送请求并获取响应
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.body() != null && !response.body().isEmpty()) {
            String responseBody = response.body();
            System.out.println("Response: " + responseBody);
            return responseBody;
        } else {
            System.out.println("Empty response");
            return null;
        }
    }

    // 查询参数封装，使用 Lombok
    @Data
    public static class QueryParams {
        private Integer offset;
        private Integer limit;
        private String searchKey;

        /**
         * 使用反射构建请求体参数
         *
         * @return Map<String, Object>
         */
        public Map<String, Object> buildBodyParams() {
            Map<String, Object> params = new HashMap<>();
            // 使用反射获取所有字段
            for (var field : this.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                try {
                    Object value = field.get(this);
                    if (value != null) {
                        String name = field.getName();
                        // 首字母大写
                        name = name.substring(0, 1).toUpperCase() + name.substring(1);
                        params.put(name, value);
                    }
                } catch (IllegalAccessException ignored) {
                }
            }

            return params;
        }
    }

    // 响应结果对象，后续可扩展
    @Data
    public static class DescribeCertificatesResponse {
        private String RequestId;
        private CertificateSet[] CertificateSet;

        @Data
        public static class CertificateSet {
            private String CertificateId;
            private String Alias;
            private String Type;
            private String Status;
            private String Source;
            private String CreateTime;
            private String StartTime;
            private String ExpireTime;
            // 根据实际返回字段继续补充
        }
    }
}
