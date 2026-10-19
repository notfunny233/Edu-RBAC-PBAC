package eduadmin.Controller;

import eduadmin.anno.RequiresPermission;
import eduadmin.pojo.Result;
import eduadmin.utils.AliyunOSSOperator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;

@Slf4j
@RestController

public class UploadController {

    @Autowired
    private AliyunOSSOperator aliyunOSSOperator;

   /* @PostMapping("/upload")                                         //可能抛出IO异常，不要处理，谁调用谁处理
    public Result upload(String name, Integer age, MultipartFile file) throws IOException {
        log.info("接收参数: {},{},{}", name, age, file);
        // 获取原始文件名
        String originalFilename = file.getOriginalFilename();

        //新的文件名（防止重复）
        //截取原始文件名的后缀（包含点，例如 .jpg .png）
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        //生成UUID随机名称 + 写死.png（这里代码有bug，没有使用上面的extension）
        String newFileName = UUID.randomUUID().toString() + extension;

        // 保存文件
        file.transferTo(new File("D:/images/" +originalFilename));
        return Result.success();
    }*/
//===============================================上传本地存储 =====================================================
//===============================================上传阿里云存储 ===================================================
    @RequiresPermission("upload:file")
    @PostMapping("/upload")
    //MultipartFile 是 Spring 框架提供的接口（interface），专门用来接收前端上传的文件。
    public Result upload(MultipartFile file) throws Exception {
        log.info("接收参数: {}",file.getOriginalFilename());
        String originalFilename = file.getOriginalFilename();
        byte[] content=file.getBytes();
        String url = aliyunOSSOperator.upload(content, originalFilename);
        log.info("文件上传的url为：{}",url);
        return Result.success(url);
    }






}