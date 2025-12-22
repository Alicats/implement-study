package cn.xej.api.sdk.java;
import java.io.File;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.web.bind.annotation.PostMapping;

import freemarker.template.Template;

public class JavaSdkCodeGenerator {

    public void generate(Set<Class<?>> controllerClasses, File outputDir, String packageName) throws Exception {
        // 创建包目录结构
        String packagePath = packageName.replace(".", "/");
        File packageDir = new File(outputDir, packagePath);
        packageDir.mkdirs();
                
        // 生成ApiClient和models 创建models目录
        File modelsDir = new File(packageDir, "models");
        modelsDir.mkdirs();
        
        JavaModelCollector collector = new JavaModelCollector();

        // 扫描收集请求和返参(递归)
        for (Class<?> controllerClass : controllerClasses) {
            for (java.lang.reflect.Method method : controllerClass.getDeclaredMethods()) {
                if (method.isAnnotationPresent(PostMapping.class)) {
                    collector.registerMethod(method);
                    collector.registerModels(method);
                }
            }
        }
        // 生成ApiModel
        generateApiModel(collector.getCollectedModels(), modelsDir, packageName);
        // 生成ApiClient
        generateApiClient(collector.getCollectedMethods(), packageDir, packageName);
        // 清空收集器
        collector.clear();
    }


    /**
     * 生成统一的ApiModel类
     */
    private void generateApiModel(Set<ModelInfo> collectedModels, File modelsDir, String packageName) throws Exception {
        // 配置FreeMarker模板引擎
        freemarker.template.Configuration cfg = new freemarker.template.Configuration(freemarker.template.Configuration.VERSION_2_3_31);
        cfg.setDefaultEncoding("UTF-8");
        cfg.setClassForTemplateLoading(this.getClass(), "/");
        
        // 读取模板文件
        Template template = cfg.getTemplate("api-model.ftl");
        
        // 遍历收集到的模型类
        for (ModelInfo modelInfo : collectedModels) {
            File outputFile = new File(modelsDir, modelInfo.getClassName() + ".java");
            // 如果文件已存在，跳过
            if (outputFile.exists()) {
                continue;
            }
            
            // 准备模板数据
            Map<String, Object> dataModel = new HashMap<>();
            dataModel.put("model", modelInfo);
            dataModel.put("packageName", packageName);
            
            // 处理模板
            StringWriter writer = new StringWriter();
            template.process(dataModel, writer);
            
            // 写入文件
            java.nio.file.Files.write(outputFile.toPath(), writer.toString().getBytes("UTF-8"));
        }
    }

    /**
     * 生成统一的ApiClient类
     */
    private void generateApiClient(Set<MethodInfo> methodInfos, File outputDir, String packageName) throws Exception {
        // 然后生成ApiClient类
        String fileName = "ApiClient.java";
        File outputFile = new File(outputDir, fileName);

        // 配置FreeMarker模板引擎
        freemarker.template.Configuration cfg = new freemarker.template.Configuration(freemarker.template.Configuration.VERSION_2_3_31);
        cfg.setDefaultEncoding("UTF-8");
        cfg.setClassForTemplateLoading(this.getClass(), "/");
        
        Template template = cfg.getTemplate("api-client.ftl");
        
        // 准备模板数据
        Map<String, Object> dataModel = new HashMap<>();
        dataModel.put("packageName", packageName);
        dataModel.put("methods", methodInfos);
        
        // 处理模板
        StringWriter writer = new StringWriter();
        template.process(dataModel, writer);
        
        // 写入文件
        java.nio.file.Files.write(outputFile.toPath(), writer.toString().getBytes("UTF-8"));
    }
}
