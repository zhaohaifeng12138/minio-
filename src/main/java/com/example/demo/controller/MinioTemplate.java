package com.example.demo.controller;

import io.minio.*;
import io.minio.errors.MinioException;
import io.minio.http.Method;
import io.minio.messages.Bucket;
import io.minio.messages.Item;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.io.InputStream;
import java.util.*;
import java.util.function.IntFunction;

/**
 * ClassName:MinioBase
 * Description:
 *
 * @date:2021/6/8 11:24
 * @author:赵海峰 2209779030@qq.com
 */
@Component
@Slf4j
public class MinioTemplate implements InitializingBean {

    @Value("${minio.url}")
    private  String ENDPOINT;
    @Value("${minio.bucketName}")
    private  String BUCKET_NAME;
    @Value("${minio.accessKey}")
    private  String ACCESS_KEY;
    @Value("${minio.secretKey}")
    private  String SECRET_KEY;
    private MinioClient client;
    @Override
    public void afterPropertiesSet() throws Exception {
        //断言
    log.info("minio  ---->:{}","AO li get" );
        Assert.hasText(ENDPOINT, "Minio url 为空");
        Assert.hasText(ACCESS_KEY, "Minio accessKey为空");
        Assert.hasText(SECRET_KEY, "Minio secretKey为空");
        this.client  =
                MinioClient.builder()
                        .endpoint(ENDPOINT)
                        .credentials(ACCESS_KEY, SECRET_KEY)
                        .build();
    }
    /**
     * 创建bucket
     *
     * @param bucketName bucket名称
     */
    @SneakyThrows
    public void createBucket(String bucketName) {
        if (! client.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())) {
            client.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        }
    }

    /**
     * 获取全部bucket
     * <p>
     * https://docs.minio.io/cn/java-client-api-reference.html#listBuckets
     */
    @SneakyThrows
    public List<Bucket> getAllBuckets() {
            // 列出所有存储桶
        return client.listBuckets();

    }

    /**
     * 根据bucketName获取信息
     *
     * @param bucketName bucket名称
     */
    @SneakyThrows
    public Optional<Bucket> getBucket(String bucketName) {
        return client.listBuckets().stream().filter(b -> b.name().equals(bucketName)).findFirst();
    }

    /**
     * 根据bucketName删除信息
     *
     * @param bucketName bucket名称
     */
    @SneakyThrows
    public void removeBucket(String bucketName) {
        try {
            // 删除之前先检查`my-bucket`是否存在。
            boolean found = client.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (found) {
                // 删除`my-bucketname`存储桶，注意，只有存储桶为空时才能删除成功。
                client.removeBucket(RemoveBucketArgs.builder().bucket(bucketName).build());
            } else {
                System.out.println("mybucket does not exist");
            }
        } catch(MinioException e) {
            System.out.println("根据bucketName删除信息失败: " + e);
        }
    }

    /**
     * 根据文件前置查询文件
     *
     * @param bucketName bucket名称
     * @param prefix     前缀
     * @param recursive  是否递归查询
     * @return MinioItem 列表
     */
    @SneakyThrows
    public List getAllObjectsByPrefix(String bucketName, String prefix, boolean recursive) {
        List<Item> list = new ArrayList<>();
        Iterable<Result<Item>> objectsIterator = client.listObjects(
                ListObjectsArgs.builder().bucket(bucketName).prefix(prefix)
                .recursive(recursive).build()
               );
        if (objectsIterator != null) {
            Iterator<Result<Item>> iterator = objectsIterator.iterator();
            if (iterator != null) {
                while (iterator.hasNext()) {
                    Result<Item> result = iterator.next();
                    Item item = result.get();
                    list.add(item);
                }
            }
        }

        return list;
    }

    /**
     * 获取文件外链
     *
     * @param bucketName bucket名称
     * @param objectName 文件名称
     * @param expires    过期时间 <=7
     * @return url
     */
    @SneakyThrows
    public String getObjectURL(String bucketName, String objectName, Integer expires) {
        IntFunction<Integer> integerIntFunction = (int i) -> {
            if (i > 7) return 7;
            return i;
        };
        log.info("获取bucketName2----{}",bucketName);
        log.info("获取objectName2----{}",objectName);
        log.info("获取expires2----{}",expires);
        return  client.getPresignedObjectUrl(
                       GetPresignedObjectUrlArgs.builder()
                           .method(Method.GET)
                           .bucket(bucketName)
                           .object(objectName)
                            .expiry(expires*60*60*24)
                            .build());

    }

    /**
     * 获取文件
     *
     * @param bucketName bucket名称
     * @param objectName 文件名称
     * @return 二进制流
     */
    @SneakyThrows
    public InputStream getObject(String bucketName, String objectName) {
        return client.getObject( GetObjectArgs.builder()
                .bucket(bucketName)
                .object(objectName)
                .build());
    }

    /**
     * 上传文件
     *
     * @param bucketName bucket名称
     * @param objectName 文件名称
     * @param stream     文件流
     * @throws Exception https://docs.minio.io/cn/java-client-api-reference.html#putObject
     */
    public void putObject(String bucketName, String objectName, InputStream stream) throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put("X-Amz-Storage-Class", "REDUCED_REDUNDANCY");
        Map<String, String> userMetadata = new HashMap<>();
        userMetadata.put("My-Project", "Project One");
        client.putObject(
                PutObjectArgs.builder().bucket(bucketName).object(objectName).stream(
                        stream, stream.available(), -1)
                        .headers(headers)
                        .userMetadata(userMetadata)
                        .build());
    }

    /**
     * 上传文件
     *
     * @param bucketName  bucket名称
     * @param objectName  文件名称
     * @param stream      文件流
     * @param size        大小
     * @param contextType 类型
     * @throws Exception https://docs.minio.io/cn/java-client-api-reference.html#putObject
     */
    public void putObject(String bucketName, String objectName, InputStream stream, long size, String contextType) throws Exception {
        client.putObject(
                PutObjectArgs.builder().bucket(bucketName)
                .object(objectName).stream(stream,size, -1)
                        .contentType(contextType).build());
    }

    /**
     * 获取文件信息, 如果抛出异常则说明文件不存在
     *
     * @param bucketName bucket名称
     * @param objectName 文件名称
     * @throws Exception https://docs.minio.io/cn/java-client-api-reference.html#statObject
     */
    public StatObjectResponse getObjectInfo(String bucketName, String objectName) throws Exception {
        StatObjectResponse statObjectResponse = client.statObject(StatObjectArgs.builder().bucket(bucketName).object(objectName).build());
        return statObjectResponse;
    }

    /**
     * 删除文件
     *
     * @param bucketName bucket名称
     * @param objectName 文件名称
     * @throws Exception https://docs.minio.io/cn/java-client-api-reference.html#removeObject
     */
    public boolean removeObject(String bucketName, String objectName) throws Exception {
        try {
            client.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build());
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public static void main(String[] args) {
        IntFunction<Integer> integerIntFunction = (int i) -> {
            if (i > 7) return 7;
            return i;
        };

        Integer apply = integerIntFunction.apply(89);
        System.out.println(apply);

    }

}
