package cn.xej.api;

import freemarker.template.Template;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.bind.annotation.*;

import cn.xej.api.sdk.java.JavaModelCollector;
import cn.xej.api.sdk.java.MethodInfo;
import cn.xej.api.sdk.java.ModelInfo;

import java.io.File;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

/**
 * Maven插件用于从Spring Boot Controller生成Java SDK
 */
@Mojo(name = "generate")
public class SdkGeneratorMojo extends AbstractMojo {

    /**
     * 当前Maven项目对象
     */
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    /**
     * SDK输出目录参数
     */
    @Parameter(property = "outputDirectory", defaultValue = "${project.build.directory}/generate-sdk")
    private File outputDirectory;

    /**
     * SDK包名参数
     */
    @Parameter(property = "packageName", defaultValue = "cn.xej.api")
    private String packageName;

    /**
     * 要扫描的Controller包名
     */
    @Parameter(property = "controllerPackage", defaultValue = "cn.xej.api.controller")
    private String controllerPackage;


    /**
     * 插件的主要执行方法
     *
     * @throws MojoExecutionException 执行异常
     */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("开始生成SDK...");
        getLog().info("输出目录: " + outputDirectory.getAbsolutePath());
        getLog().info("包名: " + packageName);
        getLog().info("Controller包名: " + controllerPackage);

        try {
            // 确保输出目录存在
            if (!outputDirectory.exists()) {
                outputDirectory.mkdirs();
            }

            // 创建包目录结构
            String packagePath = packageName.replace(".", "/");
            File packageDir = new File(outputDirectory, packagePath);
            if (!packageDir.exists()) {
                packageDir.mkdirs();
            }

            // 扫描Controller类并生成SDK
            scanControllersAndGenerateSdk(packageDir);

            getLog().info("SDK生成完成!");

        } catch (Exception e) {
            getLog().error("生成SDK时出错", e);
            throw new MojoExecutionException("生成SDK时出错", e);
        }
    }

    
    private void scanControllersAndGenerateSdk(File packageDir) throws Exception {
        Set<Class<?>> controllerClasses = getControllerClasses();
        if (controllerClasses == null)
            return; // 如果没有编译输出目录，直接返回

        // 生成ApiClient和models 创建models目录
        File modelsDir = new File(packageDir, "models");
        if (!modelsDir.exists()) {
            modelsDir.mkdirs();
        }
        
        JavaModelCollector collector = new JavaModelCollector(getLog());

        // 扫描收集请求和返参(递归)
        scanApiModelAndMethodInfo(collector, controllerClasses);
        // 生成ApiModel
        generateApiModel(collector.getCollectedModels(), modelsDir);
        // 生成ApiClient
        generateApiClient(collector.getCollectedMethods(), packageDir);
        // 清空收集器
        collector.clear();
    }

    // 查找target/classes/controllerPackage目录下的controller类
    @Nullable
    private Set<Class<?>> getControllerClasses() throws MalformedURLException, MojoFailureException {
        // 构建完整的类路径 - 关键改进：使用当前项目的类路径
        List<URL> urls = new ArrayList<>();

        // 添加当前项目的编译输出目录（包含业务代码）
        String projectOutputDirectory = project.getBuild().getOutputDirectory();
        File outputDir = new File(projectOutputDirectory);
        if (outputDir.exists()) {
            urls.add(outputDir.toURI().toURL());
            getLog().info("添加项目输出目录到类路径: " + outputDir.getAbsolutePath());
        } else {
            getLog().warn("项目输出目录不存在: " + outputDir.getAbsolutePath());
            return null;
        }

        // 添加项目的依赖到类路径
        addProjectDependenciesToClasspath(urls);

        // 创建URLClassLoader - 使用当前线程的类加载器作为父加载器
        URLClassLoader classLoader = new URLClassLoader(
                urls.toArray(new URL[0]),
                Thread.currentThread().getContextClassLoader()
        );

        // 直接从target/classes目录扫描Controller类
        Set<Class<?>> controllerClasses = new HashSet<>();

        // 将包名转换为文件路径
        String packagePath = controllerPackage.replace(".", File.separator);
        File controllerDir = new File(outputDir, packagePath);

        // 如果Controller目录存在，则扫描其中的所有.class文件
        if (controllerDir.exists() && controllerDir.isDirectory()) {
            getLog().info("扫描Controller目录: " + controllerDir.getAbsolutePath());

            // 递归扫描目录下的所有.class文件
            List<File> classFiles = new ArrayList<>();
            scanClassFiles(controllerDir, classFiles);

            // 加载每个.class文件
            for (File classFile : classFiles) {
                try {
                    // 计算类的全限定名
                    String relativePath = outputDir.toURI().relativize(classFile.toURI()).getPath();
                    // 直接替换正斜杠为点号，因为URI的getPath()总是返回正斜杠分隔的路径
                    String className = relativePath.replace("/", ".").replace(".class", "");

                    // 加载类
                    Class<?> clazz = classLoader.loadClass(className);

                    // 检查是否是Controller类（这里仍然检查@RestController注解，以确保只处理Controller类）
                    if (clazz.isAnnotationPresent(RestController.class)) {
                        controllerClasses.add(clazz);
                    }
                } catch (ClassNotFoundException e) {
                    getLog().warn("无法加载类: " + classFile.getAbsolutePath(), e);
                } catch (Exception e) {
                    getLog().warn("处理类文件时发生错误: " + classFile.getAbsolutePath(), e);
                }
            }
        } else {
            getLog().warn("Controller目录不存在: " + controllerDir.getAbsolutePath());
        }
        return controllerClasses;
    }


    /**
     * 递归扫描目录下的所有.class文件
     */
    private void scanClassFiles(File dir, List<File> classFiles) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    // 递归扫描子目录
                    scanClassFiles(file, classFiles);
                } else if (file.getName().endsWith(".class")) {
                    // 添加.class文件到列表
                    classFiles.add(file);
                }
            }
        }
    }
    
    private void addProjectDependenciesToClasspath(List<URL> urls) throws MojoFailureException {
        try {
            List<String> classpathElements = project.getCompileClasspathElements();

            for (String element : classpathElements) {
                try {
                    File file = new File(element);
                    if (file.exists()) {
                        urls.add(file.toURI().toURL());
                        getLog().debug("添加依赖到类路径: " + element);
                    } else {
                        getLog().warn("依赖文件不存在: " + element);
                    }
                } catch (MalformedURLException e) {
                    getLog().warn("无法转换为URL: " + element, e);
                }
            }
        } catch (Exception e) {
            throw new MojoFailureException("添加项目依赖到类路径时出错", e);
        }
    }


    // 扫描生成ApiMethod/ApiModel 核心
    private void scanApiModelAndMethodInfo(JavaModelCollector collector, Set<Class<?>> controllerClasses) {
        for (Class<?> controllerClass : controllerClasses) {
            for (java.lang.reflect.Method method : controllerClass.getDeclaredMethods()) {
                if (method.isAnnotationPresent(PostMapping.class)) {
                    collector.registerMethod(method);
                    collector.registerModels(method);
                }
            }
        }
    }

     /**
     * 生成统一的ApiModel类
     */
    private void generateApiModel(Set<ModelInfo> collectedModels, File modelsDir) throws Exception {
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
            getLog().info("生成模型类: " + outputFile.getAbsolutePath());
        }
    }

    /**
     * 生成统一的ApiClient类
     */
    private void generateApiClient(Set<MethodInfo> methodInfos, File outputDir) throws Exception {
        // 然后生成ApiClient类
        String fileName = "ApiClient.java";
        File outputFile = new File(outputDir, fileName);

        // 配置FreeMarker模板引擎
        freemarker.template.Configuration cfg = new freemarker.template.Configuration(freemarker.template.Configuration.VERSION_2_3_31);
        cfg.setDefaultEncoding("UTF-8");
        cfg.setClassForTemplateLoading(this.getClass(), "/");
        
        // 读取模板文件
        getLog().info("读取ApiClient模板文件...");
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
        getLog().info("生成ApiClient文件: " + outputFile.getAbsolutePath());
    }
    

}