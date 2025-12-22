package cn.xej.api;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.bind.annotation.*;

import cn.xej.api.sdk.java.JavaSdkCodeGenerator;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

/**
 * Maven插件用于从Spring Boot Controller生成SDK
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
    public void execute() throws MojoExecutionException {
        getLog().info("开始生成SDK...");
        try {
            URLClassLoader classLoader = creatClassLoader();
            Set<Class<?>> controllerClasses = getControllerClasses(classLoader);
            if (controllerClasses == null || controllerClasses.isEmpty()) {
                getLog().warn("未找到任何Controller类");
                return;
            }
           
            JavaSdkCodeGenerator javaGenerator = new JavaSdkCodeGenerator();
            javaGenerator.generate(controllerClasses, new File(outputDirectory, "java"), packageName);

            getLog().info("SDK生成完成!");

        } catch (Exception e) {
            getLog().error("生成SDK时出错", e);
            throw new MojoExecutionException("生成SDK时出错", e);
        }
    }

    private URLClassLoader creatClassLoader() throws Exception{
        // 构建完整的类路径 - 关键改进：使用当前项目的类路径
        List<URL> urls = new ArrayList<>();

        // 添加当前项目的编译输出目录（包含业务代码）
        String projectOutputDirectory = project.getBuild().getOutputDirectory();
        File outputDir = new File(projectOutputDirectory);
        urls.add(outputDir.toURI().toURL());

        List<String> classpathElements = project.getCompileClasspathElements();
        for (String element : classpathElements) {
            File file = new File(element);
            urls.add(file.toURI().toURL());
        }

        // 创建URLClassLoader - 使用当前线程的类加载器作为父加载器
        return new URLClassLoader(
                urls.toArray(new URL[0]),
                Thread.currentThread().getContextClassLoader()
        );
    }

    // 查找target/classes/controllerPackage目录下的controller类
    @Nullable
    private Set<Class<?>> getControllerClasses(URLClassLoader classLoader) throws Exception {
        String projectOutputDirectory = project.getBuild().getOutputDirectory();
        File outputDir = new File(projectOutputDirectory);

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
}