package com.project.marketplace.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.storage")
public class StorageProperties {

    // CloudFront 주소가 비어 있으면 테스트와 로컬에서 object key만 반환하게 둠
    private String cloudFrontBaseUrl = "";

    // S3 관련 설정을 app.storage.s3 하위로 묶어 환경변수 매핑을 단순화함
    private final S3 s3 = new S3();

    public String getCloudFrontBaseUrl() {
        return cloudFrontBaseUrl;
    }

    public void setCloudFrontBaseUrl(String cloudFrontBaseUrl) {
        this.cloudFrontBaseUrl = trimTrailingSlash(cloudFrontBaseUrl);
    }

    public S3 getS3() {
        return s3;
    }

    public String getPrefix(ImageUploadType uploadType) {
        if (uploadType == ImageUploadType.BANNER) {
            return trimSlashes(s3.bannerPrefix);
        }

        return trimSlashes(s3.productPrefix);
    }

    public String buildPublicUrl(String objectKey) {
        String normalizedKey = trimLeadingSlash(objectKey);
        if (cloudFrontBaseUrl == null || cloudFrontBaseUrl.isBlank()) {
            return normalizedKey;
        }

        return cloudFrontBaseUrl + "/" + normalizedKey;
    }

    private String trimTrailingSlash(String value) {
        if (value == null) {
            return "";
        }

        return value.replaceAll("/+$", "");
    }

    private String trimLeadingSlash(String value) {
        if (value == null) {
            return "";
        }

        return value.replaceAll("^/+", "");
    }

    private String trimSlashes(String value) {
        return trimTrailingSlash(trimLeadingSlash(value));
    }

    public static class S3 {

        // AWS SDK 클라이언트가 리전을 항상 갖도록 서울 리전을 기본값으로 둠
        private String region = "ap-northeast-2";

        // 운영 배포에서만 실제 버킷명을 주입하고 테스트에서는 빈 값으로 둘 수 있게 함
        private String bucket = "";

        // 상품 이미지와 홈 배너 이미지를 서로 다른 prefix로 분리함
        private String productPrefix = "products";

        private String bannerPrefix = "home";

        // 과도한 이미지 업로드를 막기 위한 기본 제한값을 설정함
        private int maxUploadSizeMb = 5;

        public String getRegion() {
            return region;
        }

        public void setRegion(String region) {
            this.region = region;
        }

        public String getBucket() {
            return bucket;
        }

        public void setBucket(String bucket) {
            this.bucket = bucket;
        }

        public String getProductPrefix() {
            return productPrefix;
        }

        public void setProductPrefix(String productPrefix) {
            this.productPrefix = productPrefix;
        }

        public String getBannerPrefix() {
            return bannerPrefix;
        }

        public void setBannerPrefix(String bannerPrefix) {
            this.bannerPrefix = bannerPrefix;
        }

        public int getMaxUploadSizeMb() {
            return maxUploadSizeMb;
        }

        public void setMaxUploadSizeMb(int maxUploadSizeMb) {
            this.maxUploadSizeMb = maxUploadSizeMb;
        }
    }
}
