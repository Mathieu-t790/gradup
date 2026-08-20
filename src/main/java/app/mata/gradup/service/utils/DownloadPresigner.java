package app.mata.gradup.service.utils;

import app.mata.gradup.file.bucket.BucketComponent;
import app.mata.gradup.file.bucket.BucketConf;
import java.net.URL;
import java.time.Duration;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

@Component
@AllArgsConstructor
public class DownloadPresigner {

  private final BucketComponent bucketComponent;
  private final BucketConf bucketConf;

  public URL presign(String bucketKey, Duration expiration, String fileName) {
    if (fileName == null || fileName.isBlank()) {
      return bucketComponent.presign(bucketKey, expiration);
    }
    GetObjectRequest getObjectRequest =
        GetObjectRequest.builder()
            .bucket(bucketConf.getBucketName())
            .key(bucketKey)
            .responseContentDisposition("attachment; filename=\"" + fileName + "\"")
            .build();
    PresignedGetObjectRequest presignedRequest =
        bucketConf
            .getS3Presigner()
            .presignGetObject(
                GetObjectPresignRequest.builder()
                    .signatureDuration(expiration)
                    .getObjectRequest(getObjectRequest)
                    .build());
    return presignedRequest.url();
  }
}