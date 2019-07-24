package de.adorsys.datasafe.rest.impl.config;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import de.adorsys.datasafe.business.impl.service.DaggerDefaultDatasafeServices;
import de.adorsys.datasafe.business.impl.service.DaggerVersionedDatasafeServices;
import de.adorsys.datasafe.business.impl.service.DefaultDatasafeServices;
import de.adorsys.datasafe.business.impl.service.VersionedDatasafeServices;
import de.adorsys.datasafe.directory.api.config.DFSConfig;
import de.adorsys.datasafe.directory.impl.profile.config.DefaultDFSConfig;
import de.adorsys.datasafe.directory.impl.profile.config.MultiDFSConfig;
import de.adorsys.datasafe.storage.api.SchemeDelegatingStorage;
import de.adorsys.datasafe.storage.api.StorageService;
import de.adorsys.datasafe.storage.impl.db.DatabaseConnectionRegistry;
import de.adorsys.datasafe.storage.impl.db.DatabaseCredentials;
import de.adorsys.datasafe.storage.impl.db.DatabaseStorageService;
import de.adorsys.datasafe.storage.impl.fs.FileSystemStorageService;
import de.adorsys.datasafe.storage.impl.s3.S3StorageService;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.inject.Inject;
import java.net.URI;
import java.nio.file.Paths;
import java.security.Security;
import java.util.Set;
import java.util.concurrent.Executors;

/**
 * Configures default (non-versioned) Datasafe service that uses S3 client as storage provider.
 * Encryption provider: BouncyCastle.
 */
@Slf4j
@Configuration
public class DatasafeConfig {
    public static final String FILESYSTEM_ENV = "USE_FILESYSTEM";

    private static final Set<String> ALLOWED_TABLES = ImmutableSet.of("private_profiles", "public_profiles");

    private DatasafeProperties datasafeProperties;

    @Inject
    DatasafeConfig(DatasafeProperties datasafeProperties) {
        this.datasafeProperties = datasafeProperties;
    }

    @Bean
    @ConditionalOnProperty(name = "DATASAFE_SINGLE_STORAGE", havingValue="true")
    DFSConfig singleDfsConfig(DatasafeProperties properties) {
        return new DefaultDFSConfig(properties.getSystemRoot(), properties.getKeystorePassword());
    }

    @Bean
    @ConditionalOnMissingBean(DFSConfig.class)
    DFSConfig multiDfsConfig(DatasafeProperties properties) {
        return new MultiDFSConfig(URI.create(properties.getS3Path()), URI.create(properties.getDbProfilePath()),
                properties.getKeystorePassword());
    }

    /**
     * @return Default implementation of Datasafe services.
     */
    @Bean
    DefaultDatasafeServices datasafeService(StorageService storageService, DFSConfig dfsConfig) {

        Security.addProvider(new BouncyCastleProvider());

        return DaggerDefaultDatasafeServices
                .builder()
                .config(dfsConfig)
                .storage(storageService)
                .build();
    }

    @Bean
    VersionedDatasafeServices versionedDatasafeServices(StorageService storageService, DFSConfig dfsConfig) {

        Security.addProvider(new BouncyCastleProvider());

        return DaggerVersionedDatasafeServices
                .builder()
                .config(dfsConfig)
                .storage(storageService)
                .build();
    }

    @Bean
    @ConditionalOnProperty(FILESYSTEM_ENV)
    StorageService fsStorageService(DatasafeProperties properties) {
        String root = System.getenv(FILESYSTEM_ENV);
        log.info("==================== FILESYSTEM");
        log.info("build DFS to FILESYSTEM with root " + root);
        properties.setSystemRoot(root);
        return new FileSystemStorageService(Paths.get(root));
    }
    /**
     * @return S3 based storage service
     */
    @Bean
    @ConditionalOnProperty(name = "DATASAFE_SINGLE_STORAGE", havingValue="true")
    StorageService singleStorageService(AmazonS3 s3, DatasafeProperties properties) {
        return new S3StorageService(
                s3,
                properties.getBucketName(),
                Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())
        );
    }

    /**
     * @return storage service based on the two data storage. Profiles saving to the relation DB(like MySQL) and
     * data to the storage like Amazon S3, MinIO, CEPH
     */
    @Bean
    @ConditionalOnMissingBean(StorageService.class)
    StorageService multiStorageService(DatasafeProperties properties) {
        StorageService db = new DatabaseStorageService(ALLOWED_TABLES, new DatabaseConnectionRegistry(
                ImmutableMap.of(properties.getDbUrl(),
                        new DatabaseCredentials(properties.getDbUsername(), properties.getDbPassword()))
            )
        );

        S3StorageService s3StorageService = new S3StorageService(s3(properties), properties.getBucketName(),
                Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())
        );

        StorageService multiDfs = new SchemeDelegatingStorage(
                ImmutableMap.of(
                        "s3", s3StorageService,
                        "jdbc-mysql", db
                )
        );

        return multiDfs;
    }

    @Bean
    AmazonS3 s3(DatasafeProperties properties) {
        AmazonS3 amazonS3;

        boolean useEndpoint = properties.getAmazonUrl() != null;

        AWSStaticCredentialsProvider credentialsProvider = new AWSStaticCredentialsProvider(
                new BasicAWSCredentials(properties.getAmazonAccessKeyID(), properties.getAmazonSecretAccessKey())
        );

        AmazonS3ClientBuilder builder = AmazonS3ClientBuilder.standard()
                .withCredentials(credentialsProvider);

        if(useEndpoint) {
            builder = builder.withEndpointConfiguration(
                    new AwsClientBuilder.EndpointConfiguration(
                            properties.getAmazonUrl(),
                            properties.getAmazonRegion())
            ).enablePathStyleAccess();
        } else {
            builder.withRegion(properties.getAmazonRegion());
        }

        amazonS3 = builder.build();

        // used by local deployment in conjunction with minio
        if (useEndpoint && !amazonS3.doesBucketExistV2(properties.getBucketName())) {
            amazonS3.createBucket(properties.getBucketName());
        }

        return amazonS3;
    }


}
