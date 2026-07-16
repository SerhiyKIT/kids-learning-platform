package ua.kidlearn.aipipeline;

import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import ua.kidlearn.config.MinioProperties;

/** Real audio storage backend, talking to MinIO via its S3-compatible API. */
@Component
@ConditionalOnProperty(prefix = "app.tts", name = "storage", havingValue = "minio", matchIfMissing = true)
class MinioAudioStorage implements AudioStorage {

	private final S3Client s3Client;
	private final MinioProperties properties;
	// No MinIO call happens until the first store(); bean construction/startup does no network I/O,
	// so a full Spring context can boot without MinIO being reachable (e.g. in CI).
	private final AtomicBoolean bucketEnsured = new AtomicBoolean(false);

	MinioAudioStorage(MinioProperties properties) {
		this.properties = properties;
		this.s3Client = S3Client.builder()
				.endpointOverride(URI.create(properties.endpoint()))
				.region(Region.US_EAST_1) // MinIO ignores this; the SDK requires some region
				.credentialsProvider(StaticCredentialsProvider
						.create(AwsBasicCredentials.create(properties.accessKey(), properties.secretKey())))
				.forcePathStyle(true) // MinIO requires path-style bucket addressing
				.build();
	}

	private synchronized void ensureBucketExists() {
		if (bucketEnsured.get()) {
			return;
		}
		try {
			s3Client.headBucket(HeadBucketRequest.builder().bucket(properties.bucket()).build());
		} catch (S3Exception e) {
			if (e.statusCode() == 404) {
				s3Client.createBucket(CreateBucketRequest.builder().bucket(properties.bucket()).build());
			} else {
				throw e;
			}
		}
		bucketEnsured.set(true);
	}

	@Override
	public String store(String objectKey, byte[] data, String contentType) {
		ensureBucketExists();
		s3Client.putObject(
				PutObjectRequest.builder().bucket(properties.bucket()).key(objectKey).contentType(contentType).build(),
				RequestBody.fromBytes(data));
		return properties.endpoint() + "/" + properties.bucket() + "/" + objectKey;
	}

}
