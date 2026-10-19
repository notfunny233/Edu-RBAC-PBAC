package eduadmin;

import com.aliyun.sdk.service.oss2.OSSClient;
import com.aliyun.sdk.service.oss2.OSSClientBuilder;
import com.aliyun.sdk.service.oss2.credentials.CredentialsProvider;
import com.aliyun.sdk.service.oss2.credentials.EnvironmentVariableCredentialsProvider;
import com.aliyun.sdk.service.oss2.models.*;
import com.aliyun.sdk.service.oss2.transport.BinaryData;

import java.nio.file.Files;
import java.nio.file.Paths;

public class Example {
    public static void main(String[] args) {
        // OSS存储桶所在的地域ID，例如杭州cn‑hangzhou，上海cn‑shanghai
        String region = "cn-beijing";
        // OSS存储桶的完整名称，需要填写你自己创建的bucket名字
        String bucket = "not-funny";
        // 对象Key：文件上传到OSS后的路径+文件名，相当于文件在OSS里面的完整名字
        String key = "test.txt";

        // 凭证提供者：从系统环境变量读取阿里云的accessKey，避免硬编码密钥泄露
        CredentialsProvider provider = new EnvironmentVariableCredentialsProvider();
        // 构建OSS客户端建造器
        OSSClientBuilder clientBuilder = OSSClient.newBuilder()
                .credentialsProvider(provider) // 设置凭证
                .region(region); // 设置服务地域

        // try‑with‑resources 自动关闭OSS客户端，不用手动close
        try (OSSClient client = clientBuilder.build()) {

            // 需要上传的字符串数据
            String data = "D:/test.txt";
            //读取本地文件为字节数组
            byte[] fileBytes = Files.readAllBytes(Paths.get(data));

            // 构建上传请求：把字符串内容上传到OSS指定bucket、指定key位置
            PutObjectResult result = client.putObject(PutObjectRequest.newBuilder()
                    .bucket(bucket)     // 指定存储桶
                    .key(key)           // 指定上传后的文件key(路径文件名)
                    .body(BinaryData.fromBytes(fileBytes))// 上传内容
                    .build());

            // 打印上传结果：状态码、请求ID、etag（文件的MD5标识）
            System.out.printf("status code:%d, request id:%s, eTag:%s\n",
                    result.statusCode(), result.requestId(), result.eTag());

        } catch (Exception e) {
            // 如果异常来自OSS服务端，可以打开下面注释拿到详细错误信息
//            ServiceException se = ServiceException.asCause(e);
//            if (se != null) {
//                System.out.printf("ServiceException: requestId:%s, errorCode:%s\n", se.requestId(), se.errorCode());
//            }
            // 打印全部异常信息
            System.out.printf("error:\n%s", e);
        }
    }
}