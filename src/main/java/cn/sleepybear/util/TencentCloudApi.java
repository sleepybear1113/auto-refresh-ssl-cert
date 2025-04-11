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

        testList(apiKey);
//        testDownload(apiKey);
    }

    public static void testList(CloudApiKey apiKey) throws Exception {
        DescribeCertificatesQuery queryParams = new DescribeCertificatesQuery();
        queryParams.setOffset(0);
        queryParams.setLimit(10);

        DescribeCertificatesResponse describeCertificatesResponse = describeCertificates(queryParams, apiKey.getSecretId(), apiKey.getSecretKey());
        if (describeCertificatesResponse == null || describeCertificatesResponse.getError() != null) {
            System.out.println("错误: " + (describeCertificatesResponse != null ? describeCertificatesResponse.getError().getMessage() : ""));
            return;
        }
        describeCertificatesResponse.getCertificates().forEach(o -> System.out.println(GSON.toJson(o)));
    }

    public static void testDownload(CloudApiKey apiKey) throws Exception {
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

    public static DescribeCertificatesResponse describeCertificates(QueryParams params, String secretId, String secretKey) throws Exception {
        String service = "ssl";
        String endpoint = service + ".tencentcloudapi.com";
        String action = "DescribeCertificates";

        String payload = params.toJson();

        Map<String, String> headers = buildHeaders(action, secretId, secretKey, service, endpoint, payload);

        JsonElement jsonElement = sendRequest(payload, headers, endpoint);
        DescribeCertificatesResponse describeCertificatesResponse = GSON.fromJson(jsonElement, DescribeCertificatesResponse.class);
        if (describeCertificatesResponse == null) {
            System.out.println("响应体为空");
            return null;
        }

        return describeCertificatesResponse;
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

        /**
         * 数组，证书 ID 数组，传入后只返回该证书 ID 的证书信息，如["FxG06njc"]
         */
        @SerializedName("CertIds")
        private List<String> CertIds;

        /**
         * 搜索关键词，模糊匹配证书 ID、备注名称、证书域名
         */
        @SerializedName("SearchKey")
        private String searchKey;

        /**
         * 默认按照证书申请时间降序； 若传排序则按到期时间排序：DESC = 证书到期时间降序， ASC = 证书到期时间升序。示例值：DESC
         */
        @SerializedName("ExpirationSort")
        private String expirationSort;

        /**
         * 数组，证书状态：0 = 审核中，1 = 已通过，2 = 审核失败，3 = 已过期，4 = 已添加DNS记录，5 = 企业证书，待提交，6 = 订单取消中，7 = 已取消，8 = 已提交资料， 待上传确认函，9 = 证书吊销中，10 = 已吊销，11 = 重颁发中，12 = 待上传吊销确认函，13 = 免费证书待提交资料。14 = 已退款。 15 = 证书迁移中
         * <br>示例值：[1]
         */
        @SerializedName("CertificateStatus")
        private List<String> certificateStatus;

        /**
         * 筛选来源， upload：上传证书， buy：腾讯云证书， 不传默认全部
         */
        @SerializedName("FilterSource")
        private String filterSource;
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
        private List<Certificates> Certificates;
        private Integer TotalCount;

        @Data
        public static class Certificates {
            private String OwnerUin;
            private String Domain;
            private String CertificateId;

            /**
             * 证书来源：
             * trustasia：亚洲诚信，
             * upload：用户上传。
             * wosign：沃通
             * sheca：上海CA
             */
            private String From;

            /**
             * 备注
             */
            private String Alias;

            /**
             * 证书状态：0 = 审核中，1 = 已通过，2 = 审核失败，3 = 已过期，4 = 自动添加DNS记录，5 = 企业证书，待提交资料，6 = 订单取消中，7 = 已取消，8 = 已提交资料， 待上传确认函，9 = 证书吊销中，10 = 已吊销，11 = 重颁发中，12 = 待上传吊销确认函，13 = 免费证书待提交资料。14 = 证书已退款。 15 = 证书迁移中
             */
            private String Status;
            private String StatusName;
            private String StatusMsg;

            /**
             * 验证类型：DNS_AUTO = 自动DNS验证，DNS = 手动DNS验证，FILE = 文件验证，DNS_PROXY = DNS代理验证。FILE_PROXY = 文件代理验证
             */
            private String VerifyType;

            /**
             * 证书生效时间。示例值：2018-09-18 20:00:00
             */
            private String CertBeginTime;
            private String CertEndTime;
            /**
             * 创建时间
             */
            private String InsertTime;

            /**
             * 证书有效期，单位（月）
             */
            private Integer ValidityPeriod;

            /**
             * 是否已忽略到期通知
             */
            private Boolean IsIgnore;
        }
    }

    @EqualsAndHashCode(callSuper = true)
    @Data
    public static class DescribeDownloadCertificateUrlResponse extends BaseResponse {
        private String DownloadCertificateUrl;
        private String DownloadFilename;
    }
}
