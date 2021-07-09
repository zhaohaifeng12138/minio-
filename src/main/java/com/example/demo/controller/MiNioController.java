package com.example.demo.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
/**
 * @Author 赵海峰
 * @Description //TODO
 * @Date  2021/7/9
 * @Param
 * @return
 **/
@RestController
@RequestMapping(value = "/apps")
@Slf4j
public class MiNioController {
    @Resource
    private MinioTemplate minioTemplate;
    //文件创建
    @PostMapping(value = "/upload")
    public Map upload(MultipartFile file,String buck_name) {
        Map<String,Object> map =new HashMap<>();
        Map<String,Object> nameMap = new HashMap<String,Object>();
        try {
            getMinio(file, buck_name, nameMap);
        } catch (Exception e) {
            map.put("success",false);
            map.put("errorCode",500);
            map.put("errorMsg",e.getMessage());
            return map;
        }
        map.put("success",true);
        map.put("errorCode",200);
        map.put("errorMsg","成功");
        map.put("data",nameMap);
        return map;
    }

    private void getMinio(MultipartFile file, String buck_name, Map<String, Object> nameMap) throws Exception {
        minioTemplate.createBucket(buck_name);
        String substring = file.getOriginalFilename().substring(0, file.getOriginalFilename().lastIndexOf("."));
        String replace = file.getOriginalFilename().replace(substring, "");
        nameMap.put("fileName", MD5Util.getMD5(substring)+replace);
        InputStream inputStream = file.getInputStream();
        minioTemplate.putObject(buck_name,MD5Util.getMD5(substring)+replace,file.getInputStream());
    }



    //文件删除
    @PostMapping(value = "/deleteApp")
    public Map delete(String name,String buck_name) {
        Map<String,Object> map =new HashMap<>();
        try {
            minioTemplate.removeObject(buck_name,name);
        } catch (Exception e) {
            map.put("success",false);
            map.put("errorCode",500);
            map.put("errorMsg",e.getMessage());
            return map;
        }
        map.put("success",true);
        map.put("errorCode",200);
        map.put("errorMsg","成功");
        return map;
    }

    @GetMapping(value = "/load")
    public void downloadFiles(String buck_name,@RequestParam("filename") String filename, HttpServletResponse httpResponse) {
        try {
            InputStream object = minioTemplate.getObject(
                    buck_name
                    ,filename
            );
//                 Read data from stream
            byte buf[] = new byte[1024];
            int length = 0;
            httpResponse.reset();
            httpResponse.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(filename, "UTF-8"));
            httpResponse.setContentType("application/octet-stream");
            httpResponse.setCharacterEncoding("utf-8");
            OutputStream outputStream = httpResponse.getOutputStream();
            while ((length = object.read(buf)) > 0) {
                outputStream.write(buf, 0, length);
            }
            outputStream.close();
        } catch (Exception ex) {
            log.error("错误--->{}",ex.getMessage());

        }
    }


}