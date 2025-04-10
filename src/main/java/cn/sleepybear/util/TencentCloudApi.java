package cn.sleepybear.util;

import cn.sleepybear.model.CloudApiKey;
import cn.sleepybear.service.CertService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;

public class TencentCloudApi {
    private static final String CONTENT_TYPE = "application/json; charset=utf-8";
    private static final String VERSION = "2019-12-05";
    private static final String ALGORITHM = "TC3-HMAC-SHA256";
    private static final String SIGNED_HEADERS = "content-type;host";
    private static final String TC3REQUEST = "tc3_request";

    private static final Gson GSON = new GsonBuilder().create();

    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public static void main(String[] args) throws Exception {
        List<CloudApiKey> cloudApiKeys = CertService.loadApiKeys("key.txt");
        if (cloudApiKeys.isEmpty()) {
            System.out.println("没有可用的 API 密钥");
            return;
        }

        CloudApiKey apiKey = cloudApiKeys.getFirst();

//        testList(apiKey);
        testDownload(apiKey);
    }

    public static void testList(CloudApiKey apiKey ) throws Exception {
        DescribeCertificatesQuery queryParams = new DescribeCertificatesQuery();
        queryParams.setOffset(0);
        queryParams.setLimit(10);

        List<DescribeCertificatesResponse.Certificates> certificates = describeCertificates(queryParams, apiKey.getSecretId(), apiKey.getSecretKey());
        certificates.forEach(o -> System.out.println(GSON.toJson(o)));
    }

    public static void testDownload(CloudApiKey apiKey)  throws Exception {
        DescribeDownloadCertificateUrlQuery queryParams = new DescribeDownloadCertificateUrlQuery();
        String id = "NAEngsSV";
        queryParams.setCertificateId(id);
        queryParams.setServiceType("nginx");
        DescribeDownloadCertificateUrlResponse response = describeDownloadCertificateUrl(queryParams, apiKey.getSecretId(), apiKey.getSecretKey());
        if (response.getError() != null) {
            System.out.println("错误: " + response.getError().getMessage());
        } else {
            System.out.println("证书文件: " + response.getDownloadFilename());
            System.out.println("证书链接: " + response.getDownloadCertificateUrl());
        }
    }

    public static List<DescribeCertificatesResponse.Certificates> describeCertificates(QueryParams params, String secretId, String secretKey) throws Exception {
        String service = "ssl";
        String endpoint = service + ".tencentcloudapi.com";
        String action = "DescribeCertificates";

        String payload = params.toJson();

        Map<String, String> headers = buildHeaders(action, secretId, secretKey, service, endpoint, payload);

        JsonElement jsonElement = sendRequest(payload, headers, endpoint);
        DescribeCertificatesResponse describeCertificatesResponse = GSON.fromJson(jsonElement, DescribeCertificatesResponse.class);
        if (describeCertificatesResponse == null) {
            System.out.println("响应体为空");
            return new ArrayList<>();
        }

        if (describeCertificatesResponse.getCertificates() == null || describeCertificatesResponse.getCertificates().length == 0) {
            System.out.println("没有解析到证书");
            return new ArrayList<>();
        }

        return new ArrayList<>(List.of(describeCertificatesResponse.getCertificates()));
    }

    public static DescribeDownloadCertificateUrlResponse describeDownloadCertificateUrl(DescribeDownloadCertificateUrlQuery params, String secretId, String secretKey) throws Exception {
        String service = "ssl";
        String endpoint = service + ".tencentcloudapi.com";
        String action = "DescribeDownloadCertificateUrl";

        String payload = params.toJson();

        Map<String, String> headers = buildHeaders(action, secretId, secretKey, service, endpoint, payload);

        JsonElement jsonElement = sendRequest(payload, headers, endpoint);
        return GSON.fromJson(jsonElement, DescribeDownloadCertificateUrlResponse.class);
    }

    private static String buildCanonicalRequest(String endPoint, String payload) {
        String method = "POST";
        String canonicalUri = "/";
        String canonicalQueryString = "";
        String canonicalHeaders = "content-type:" + CONTENT_TYPE + "\n" +
                                  "host:" + endPoint + "\n";
        String hashedRequestPayload = sha256Hex(payload);
        return method + "\n" +
               canonicalUri + "\n" +
               canonicalQueryString + "\n" +
               canonicalHeaders + "\n" +
               SIGNED_HEADERS + "\n" +
               hashedRequestPayload;
    }

    private static String buildStringToSign(String timestamp, String credentialScope, String canonicalRequest) {
        return ALGORITHM + "\n" + timestamp + "\n" + credentialScope + "\n" + sha256Hex(canonicalRequest);
    }

    private static byte[] getSignatureKey(String secretKey, String date, String service) throws Exception {
        byte[] secretDate = hmacSHA256(date, ("TC3" + secretKey).getBytes(StandardCharsets.UTF_8));
        byte[] secretService = hmacSHA256(service, secretDate);
        return hmacSHA256(TC3REQUEST, secretService);
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

    private static Map<String, String> buildHeaders(String action, String secretId, String secretKey, String service, String endPoint, String payload) throws Exception {
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        String date = sdf.format(new Date(Long.parseLong(timestamp) * 1000));

        String canonicalRequest = buildCanonicalRequest(endPoint, payload);
        String credentialScope = date + "/" + service + "/" + TC3REQUEST;
        String stringToSign = buildStringToSign(timestamp, credentialScope, canonicalRequest);
        byte[] signingKey = getSignatureKey(secretKey, date, service);
        String signature = bytesToHex(hmacSHA256(stringToSign, signingKey)).toLowerCase();

        String authorization = "%s Credential=%s/%s, SignedHeaders=%s, Signature=%s".formatted(ALGORITHM, secretId, credentialScope, SIGNED_HEADERS, signature);

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", CONTENT_TYPE);
        headers.put("Authorization", authorization);
        headers.put("X-TC-Action", action);
        headers.put("X-TC-Version", VERSION);
        headers.put("X-TC-Timestamp", timestamp);
        return headers;
    }

    private static JsonElement sendRequest(String payload, Map<String, String> headers, String endPoint) throws IOException, InterruptedException {
        // 构建请求体
        HttpRequest.BodyPublisher bodyPublisher = HttpRequest.BodyPublishers.ofString(payload);

        // 构建请求
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create("https://" + endPoint))
                .timeout(Duration.ofSeconds(5))
                .POST(bodyPublisher);

        // 添加请求头
        headers.forEach(requestBuilder::header);

        HttpRequest request = requestBuilder.build();

        // 发送请求并获取响应
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.body() != null && !response.body().isEmpty()) {
            String responseBody = response.body();
            JsonElement jsonElement = GSON.fromJson(responseBody, JsonElement.class);
            return jsonElement.getAsJsonObject().get("Response");
        } else {
            System.out.println("Empty response");
            return null;
        }
    }

    public static void downloadFile(String url, String toFile) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() == 200) {
                Files.write(Paths.get(toFile), response.body());
                System.out.println("文件下载成功: " + toFile);
            } else {
                System.out.println("下载失败，状态码: " + response.statusCode());
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    @EqualsAndHashCode(callSuper = true)
    @Data
    public static class DescribeCertificatesQuery extends QueryParams {
        @SerializedName("Offset")
        private Integer offset;
        @SerializedName("Limit")
        private Integer limit;
        @SerializedName("SearchKey")
        private String searchKey;
    }

    @EqualsAndHashCode(callSuper = true)
    @Data
    public static class DescribeDownloadCertificateUrlQuery extends QueryParams {
        @SerializedName("CertificateId")
        private String certificateId;
        @SerializedName("ServiceType")
        private String serviceType;
    }

    @Data
    public static class QueryParams {
        public String toJson() {
            return GSON.toJson(this);
        }
    }

    @Data
    public static class BaseResponse {
        private String RequestId;
        private ErrorRes Error;

        @Data
        public static class ErrorRes {
            private String Code;
            private String Message;
        }
    }

    // 响应结果对象，后续可扩展
    @EqualsAndHashCode(callSuper = true)
    @Data
    public static class DescribeCertificatesResponse extends BaseResponse {
        private Certificates[] Certificates;
        private Integer TotalCount;

        @Data
        public static class Certificates {
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

    @EqualsAndHashCode(callSuper = true)
    @Data
    public static class DescribeDownloadCertificateUrlResponse extends BaseResponse {
        private String DownloadCertificateUrl;
        private String DownloadFilename;
    }
}
