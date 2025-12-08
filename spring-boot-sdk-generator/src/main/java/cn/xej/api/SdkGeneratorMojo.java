package cn.xej.api;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.reflections.Configuration;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
        // 构建完整的类路径 - 关键改进：使用当前项目的类路径
        List<URL> urls = new ArrayList<>();

        // 添加当前项目的编译输出目录（包含业务代码）
        String projectOutputDirectory = project.getBuild().getOutputDirectory();
        if (projectOutputDirectory != null) {
            File outputDir = new File(projectOutputDirectory);
            if (outputDir.exists()) {
                urls.add(outputDir.toURI().toURL());
                getLog().info("添加项目输出目录到类路径: " + outputDir.getAbsolutePath());
            } else {
                getLog().warn("项目输出目录不存在: " + outputDir.getAbsolutePath());
            }
        }

        // 添加项目的依赖到类路径
        addProjectDependenciesToClasspath(urls);

        getLog().info("类路径URL数量: " + urls.size());
        for (URL url : urls) {
            getLog().info("类路径URL: " + url.toString());
        }

        // 创建URLClassLoader - 使用当前线程的类加载器作为父加载器
        URLClassLoader classLoader = new URLClassLoader(
                urls.toArray(new URL[0]),
                Thread.currentThread().getContextClassLoader()
        );

        // 临时设置上下文类加载器，以便Reflections能正确扫描
        Thread.currentThread().setContextClassLoader(classLoader);

        try {
            // 使用Reflections库扫描Controller类
            Configuration configuration = new ConfigurationBuilder()
                    .forPackages(controllerPackage)
                    .addClassLoaders(classLoader)
                    .setScanners(Scanners.TypesAnnotated);

            Reflections reflections = new Reflections(configuration);

            // 先列出所有扫描到的类型，以便调试
            Set<String> allTypes = reflections.getAllTypes();
            getLog().info("扫描到的所有类型数量: " + allTypes.size());

            // 输出所有扫描到的类型，便于调试
            for (String typeName : allTypes) {
                if (typeName.startsWith(controllerPackage)) {
                    getLog().info("扫描到的类型: " + typeName);
                }
            }

            // 查找@RestController注解的类
            Set<Class<?>> controllerClasses = reflections.getTypesAnnotatedWith(RestController.class);
            getLog().info("找到的@RestController注解的类数量: " + controllerClasses.size());

            // 输出找到的Controller类
            for (Class<?> clazz : controllerClasses) {
                getLog().info("找到的Controller类: " + clazz.getName());

                // 生成SDK相关文件
                generateControllerSdk(clazz, packageDir);
            }

            // 如果没有找到Controller，尝试手动扫描
            if (controllerClasses.isEmpty()) {
                getLog().warn("未通过注解扫描找到Controller，尝试手动扫描类路径...");

                // 手动扫描所有类
                for (String typeName : allTypes) {
                    if (typeName.startsWith(controllerPackage)) {
                        try {
                            Class<?> clazz = classLoader.loadClass(typeName);
                            if (clazz.isAnnotationPresent(RestController.class)) {
                                controllerClasses.add(clazz);
                                getLog().info("手动找到Controller类: " + typeName);

                                // 生成SDK相关文件
                                generateControllerSdk(clazz, packageDir);
                            }
                        } catch (ClassNotFoundException e) {
                            getLog().warn("无法加载类: " + typeName, e);
                        } catch (NoClassDefFoundError e) {
                            getLog().warn("类定义缺失: " + typeName, e);
                        }
                    }
                }
            }

            // 如果仍然没有找到Controller，提供更详细的错误信息
            if (controllerClasses.isEmpty()) {
                getLog().warn("在包 " + controllerPackage + " 中未找到任何 @RestController 注解的类");
                getLog().info("请确认:");
                getLog().info("1. controllerPackage 配置是否正确: " + controllerPackage);
                getLog().info("2. 相关Controller类是否已编译到 " + projectOutputDirectory);
                getLog().info("3. Controller类是否正确使用了 @RestController 注解");
            }

        } finally {
            // 恢复原始的上下文类加载器
            Thread.currentThread().setContextClassLoader(ClassLoader.getSystemClassLoader());
        }
    }



    /**
     * 添加项目依赖到类路径
     */
    private void addProjectDependenciesToClasspath(List<URL> urls) throws MojoFailureException {
        try {
//            List<String> classpathElements = project.getRuntimeClasspathElements();
//            if (classpathElements == null) {
//                classpathElements = project.getCompileClasspathElements();
//            }

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


    /**
     * 生成Controller的SDK文件
     */
    private void generateControllerSdk(Class<?> controllerClass, File outputDir) throws Exception {
        String className = controllerClass.getSimpleName();
        String fileName = className + "Sdk.java";
        File outputFile = new File(outputDir, fileName);

        StringBuilder sdkCode = new StringBuilder();
        sdkCode.append("package ").append(packageName).append(";\n\n");
        sdkCode.append("import org.springframework.web.client.RestTemplate;\n");
        sdkCode.append("import org.springframework.http.*;\n");
        sdkCode.append("import ").append(controllerClass.getName()).append(";\n\n");

        sdkCode.append("/**\n * ").append(className).append(" SDK客户端\n */\n");
        sdkCode.append("public class ").append(className).append("Sdk {\n");
        sdkCode.append("    private final RestTemplate restTemplate;\n");
        sdkCode.append("    private final String baseUrl;\n\n");

        sdkCode.append("    public ").append(className).append("Sdk(String baseUrl) {\n");
        sdkCode.append("        this.restTemplate = new RestTemplate();\n");
        sdkCode.append("        this.baseUrl = baseUrl.endsWith(\"/\") ? baseUrl : baseUrl + \"/\";\n");
        sdkCode.append("    }\n\n");

        // 生成方法
        for (java.lang.reflect.Method method : controllerClass.getDeclaredMethods()) {
            if (isRequestMappingMethod(method)) {
                generateMethodSdk(method, sdkCode);
            }
        }

        sdkCode.append("}\n");

        // 写入文件
        java.nio.file.Files.write(outputFile.toPath(), sdkCode.toString().getBytes("UTF-8"));
        getLog().info("生成SDK文件: " + outputFile.getAbsolutePath());
    }


    /**
     * 检查方法是否是请求映射方法
     */
    private boolean isRequestMappingMethod(java.lang.reflect.Method method) {
        return method.isAnnotationPresent(RequestMapping.class) ||
                method.isAnnotationPresent(GetMapping.class) ||
                method.isAnnotationPresent(PostMapping.class) ||
                method.isAnnotationPresent(PutMapping.class) ||
                method.isAnnotationPresent(DeleteMapping.class) ||
                method.isAnnotationPresent(PatchMapping.class);
    }

    /**
     * 生成方法SDK
     */
    private void generateMethodSdk(java.lang.reflect.Method method, StringBuilder sdkCode) {
        // 获取映射信息
        String mappingValue = "";
        String httpMethod = "POST"; // 默认POST

        if (method.isAnnotationPresent(RequestMapping.class)) {
            RequestMapping mapping = method.getAnnotation(RequestMapping.class);
            mappingValue = mapping.value().length > 0 ? mapping.value()[0] : "";
            httpMethod = mapping.method().length > 0 ? mapping.method()[0].name() : "GET";
        } else if (method.isAnnotationPresent(GetMapping.class)) {
            GetMapping mapping = method.getAnnotation(GetMapping.class);
            mappingValue = mapping.value().length > 0 ? mapping.value()[0] : "";
            httpMethod = "GET";
        } else if (method.isAnnotationPresent(PostMapping.class)) {
            PostMapping mapping = method.getAnnotation(PostMapping.class);
            mappingValue = mapping.value().length > 0 ? mapping.value()[0] : "";
            httpMethod = "POST";
        } else if (method.isAnnotationPresent(PutMapping.class)) {
            PutMapping mapping = method.getAnnotation(PutMapping.class);
            mappingValue = mapping.value().length > 0 ? mapping.value()[0] : "";
            httpMethod = "PUT";
        } else if (method.isAnnotationPresent(DeleteMapping.class)) {
            DeleteMapping mapping = method.getAnnotation(DeleteMapping.class);
            mappingValue = mapping.value().length > 0 ? mapping.value()[0] : "";
            httpMethod = "DELETE";
        } else if (method.isAnnotationPresent(PatchMapping.class)) {
            PatchMapping mapping = method.getAnnotation(PatchMapping.class);
            mappingValue = mapping.value().length > 0 ? mapping.value()[0] : "";
            httpMethod = "PATCH";
        }

        // 生成方法签名
        String methodName = method.getName();
        sdkCode.append("    public ").append(method.getReturnType().getSimpleName()).append(" ")
                .append(methodName).append("(");

        // 添加参数
        Class<?>[] paramTypes = method.getParameterTypes();
        for (int i = 0; i < paramTypes.length; i++) {
            if (i > 0) sdkCode.append(", ");
            sdkCode.append(paramTypes[i].getSimpleName()).append(" arg").append(i);
        }

        sdkCode.append(") {\n");
        sdkCode.append("        String url = baseUrl + \"").append(mappingValue).append("\";\n");
        sdkCode.append("        HttpHeaders headers = new HttpHeaders();\n");
        sdkCode.append("        headers.setContentType(MediaType.APPLICATION_JSON);\n\n");

        // 创建请求体
        if (paramTypes.length > 0) {
            sdkCode.append("        HttpEntity<").append(paramTypes[0].getSimpleName()).append("> request = \n");
            sdkCode.append("            new HttpEntity<>(arg0, headers);\n\n");
        } else {
            sdkCode.append("        HttpEntity<String> request = new HttpEntity<>(headers);\n\n");
        }

        sdkCode.append("        ResponseEntity<").append(method.getReturnType().getSimpleName()).append("> response = \n");
        sdkCode.append("            restTemplate.exchange(url, HttpMethod.").append(httpMethod).append(", request, ")
                .append(method.getReturnType().getSimpleName()).append(".class);\n\n");

        sdkCode.append("        return response.getBody();\n");
        sdkCode.append("    }\n\n");
    }
}