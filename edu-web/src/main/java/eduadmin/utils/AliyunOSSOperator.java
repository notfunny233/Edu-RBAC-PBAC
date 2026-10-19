package eduadmin.utils;

import com.aliyun.sdk.service.oss2.OSSClient;
import com.aliyun.sdk.service.oss2.credentials.EnvironmentVariableCredentialsProvider;
import com.aliyun.sdk.service.oss2.transport.BinaryData;
import com.aliyun.sdk.service.oss2.models.PutObjectRequest;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 阿里云OSS V2 SDK 文件上传工具类
 * SDK版本：alibabacloud‑oss‑v2:0.5.1
 * 密钥获取方式：读取操作系统系统环境变量
 * 环境变量名：OSS_ACCESS_KEY_ID、OSS_ACCESS_KEY_SECRET
 * 注意：V2为新版预览SDK，课程教学主流使用V1版本
 */
@Component
public class AliyunOSSOperator {

    @Autowired
    private AliyunOSSProperties aliyunOSSProperties;

    /**
     * OSS V2客户端对象，重量级资源
     * 只在容器启动时创建1个实例，不要每次上传新建
     */
    private OSSClient ossClient;

    /**
     * 无参构造，里面不要操作任何@Autowired注入的成员变量
     */
    public AliyunOSSOperator() {
    }

    /**
     * Spring完成全部属性注入之后执行，在这里初始化oss客户端
     */
    @PostConstruct
    public void initOssClient() {
        // 从系统环境变量加载AccessKey密钥凭证
        EnvironmentVariableCredentialsProvider credentialsProvider = new EnvironmentVariableCredentialsProvider();

        String endpoint = aliyunOSSProperties.getEndpoint();
        String region = aliyunOSSProperties.getRegion();

        // 使用建造者模式构建OSS客户端实例
        ossClient = OSSClient.newBuilder()
                .region(region)                          // 指定服务地域
                .credentialsProvider(credentialsProvider) // 设置密钥凭证
                .endpoint(endpoint)                      // 设置访问域名
                .build();                                // 生成最终客户端对象
    }

    /**
     * 字节数组方式上传文件到阿里云OSS
     * @param content 文件的字节数组，文件原始二进制数据
     * @param originalFilename 用户上传的原始文件名，用来解析文件后缀名
     * @return 返回文件公网可直接访问的完整HTTP URL地址
     */
    public String upload(byte[] content, String originalFilename) {
        String endpoint = aliyunOSSProperties.getEndpoint();
        String bucketName = aliyunOSSProperties.getBucketName();

        // 获取当前系统日期，格式化为 yyyy/MM，按年、月分层存放文件，方便管理
        String dir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));

        // 查找最后一个小数点下标，用来截取文件后缀
        int dotIndex = originalFilename.lastIndexOf(".");
        // 三元表达式：存在小数点就截取后缀，没有后缀则赋值空字符串，防止字符串下标越界异常
        String suffix = dotIndex >= 0 ? originalFilename.substring(dotIndex) : "";

        // UUID生成全局唯一字符串，拼接后缀，避免服务器文件名重名覆盖
        String newFileName = UUID.randomUUID() + suffix;

        // OSS对象完整路径：目录路径 / 新文件名，相当于OSS里面的完整文件路径
        String objectName = dir + "/" + newFileName;

        // 构建上传请求对象，V2SDK固定使用 newBuilder() 建造者模式
        PutObjectRequest request = PutObjectRequest.newBuilder()
                .bucket(bucketName)                      // 指定要上传到哪个bucket
                .key(objectName)                        // 指定OSS内部完整文件路径名称
                .body(BinaryData.fromBytes(content))    // 将字节数组包装为V2要求的BinaryData对象
                .build();                               // 组装生成请求对象

        // 执行文件上传，将二进制数据推送至阿里云OSS服务器
        ossClient.putObject(request);

        // 去掉endpoint前面https://协议头，拼接成文件访问域名
        String domain = bucketName + "." + endpoint.replace("https://", "");
        // 拼接完整公网访问url返回给调用方
        return "https://" + domain + "/" + objectName;
    }
}
