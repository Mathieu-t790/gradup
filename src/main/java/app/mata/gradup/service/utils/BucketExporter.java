package app.mata.gradup.service.utils;

import app.mata.gradup.file.bucket.BucketComponent;
import java.io.File;
import java.nio.file.Files;
import java.time.Duration;
import lombok.SneakyThrows;

public final class BucketExporter {

  private static final Duration EXPIRATION = Duration.ofHours(1);

  private BucketExporter() {}

  /** Uploads {@code content} as a temporary file and returns a presigned download URL. */
  @SneakyThrows
  public static String uploadAndPresign(BucketComponent bucket, byte[] content, String bucketKey) {
    File file = Files.createTempFile("export-", ".tmp").toFile();
    Files.write(file.toPath(), content);
    bucket.upload(file, bucketKey);
    return presign(bucket, bucketKey);
  }

  public static String presign(BucketComponent bucket, String bucketKey) {
    return bucket.presign(bucketKey, EXPIRATION).toString();
  }
}
