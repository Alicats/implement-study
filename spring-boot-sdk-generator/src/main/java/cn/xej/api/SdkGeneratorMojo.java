package cn.xej.api;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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
            throw new MojoExecutionException("生成SDK时出错", e);
        }
    }

    private void scanControllersAndGenerateSdk(File packageDir) throws Exception {
        // 获取编译后的类路径
        List<String> classpathElements = project.getCompileClasspathElements();
        List<URL> urls = new ArrayList<>();
        
        for (String element : classpathElements) {
            try {
                urls.add(new File(element).toURI().toURL());
            } catch (MalformedURLException e) {
                getLog().warn("无法转换为URL: " + element, e);
            }
        }
        
        URLClassLoader classLoader = new URLClassLoader(urls.toArray(new URL[0]), Thread.currentThread().getContextClassLoader());
        
        // 使用Reflections库扫描Controller类
        Reflections reflections = new Reflections(new ConfigurationBuilder()
                .forPackages(controllerPackage)
                .addClassLoaders(classLoader)
                .setScanners(Scanners.TypesAnnotated));
        
        Set<Class<?>> controllerClasses = reflections.getTypesAnnotatedWith(RestController.class);
        
        // 生成SDK客户端
        generateSdkClient(packageDir, new ArrayList<>(controllerClasses));
        
        // 生成模型类
        generateModelClasses(packageDir);
    }

    private void generateSdkClient(File packageDir, List<Class<?>> controllerClasses) throws IOException {
        File clientFile = new File(packageDir, "ApiClient.java");
        
        try (FileWriter writer = new FileWriter(clientFile)) {
            writer.write("package " + packageName + ";\n\n");
            writer.write("import java.util.*;\n");
            writer.write("import java.util.concurrent.CompletableFuture;\n");
            writer.write("import java.net.http.HttpClient;\n");
            writer.write("import java.net.http.HttpRequest;\n");
            writer.write("import java.net.http.HttpResponse;\n");
            writer.write("import java.net.URI;\n");
            writer.write("import com.fasterxml.jackson.databind.ObjectMapper;\n\n");
            
            writer.write("/**\n");
            writer.write(" * 自动生成的API客户端\n");
            writer.write(" */\n");
            writer.write("public class ApiClient {\n");
            writer.write("    private final HttpClient httpClient;\n");
            writer.write("    private final ObjectMapper objectMapper;\n");
            writer.write("    private final String baseUrl;\n\n");
            
            writer.write("    public ApiClient(String baseUrl) {\n");
            writer.write("        this.httpClient = HttpClient.newHttpClient();\n");
            writer.write("        this.objectMapper = new ObjectMapper();\n");
            writer.write("        this.baseUrl = baseUrl;\n");
            writer.write("    }\n\n");
            
            // 为每个Controller生成方法
            for (Class<?> controllerClass : controllerClasses) {
                generateMethodsForController(writer, controllerClass);
            }
            
            writer.write("}\n");
        }
        
        getLog().info("已生成SDK客户端: " + clientFile.getAbsolutePath());
    }

    private void generateMethodsForController(FileWriter writer, Class<?> controllerClass) throws IOException {
        String simpleName = controllerClass.getSimpleName();
        String className = simpleName.replace("Controller", "");
        // 获取基础路径
        String basePath = "/api/" + className.toLowerCase() + "s";
        if (controllerClass.isAnnotationPresent(org.springframework.web.bind.annotation.RequestMapping.class)) {
            org.springframework.web.bind.annotation.RequestMapping requestMapping = 
                controllerClass.getAnnotation(org.springframework.web.bind.annotation.RequestMapping.class);
            if (requestMapping.value().length > 0) {
                basePath = requestMapping.value()[0];
            }
        }
        
        writer.write("    // " + simpleName + "相关方法\n");
        
        // 生成获取所有实体的方法
        writer.write("    public CompletableFuture<List<" + className + ">> getAll" + className + "() {\n");
        writer.write("        HttpRequest request = HttpRequest.newBuilder()\n");
        writer.write("                .uri(URI.create(baseUrl + \"" + basePath + "\"))\n");
        writer.write("                .GET()\n");
        writer.write("                .build();\n\n");
        writer.write("        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())\n");
        writer.write("                .thenApply(HttpResponse::body)\n");
        writer.write("                .thenApply(body -> {\n");
        writer.write("                    try {\n");
        writer.write("                        return Arrays.asList(objectMapper.readValue(body, " + className + "[].class));\n");
        writer.write("                    } catch (Exception e) {\n");
        writer.write("                        throw new RuntimeException(e);\n");
        writer.write("                    }\n");
        writer.write("                });\n");
        writer.write("    }\n\n");
        
        // 生成根据ID获取实体的方法
        writer.write("    public CompletableFuture<" + className + "> get" + className + "ById(Long id) {\n");
        writer.write("        HttpRequest request = HttpRequest.newBuilder()\n");
        writer.write("                .uri(URI.create(baseUrl + \"" + basePath + "/\" + id))\n");
        writer.write("                .GET()\n");
        writer.write("                .build();\n\n");
        writer.write("        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())\n");
        writer.write("                .thenApply(HttpResponse::body)\n");
        writer.write("                .thenApply(body -> {\n");
        writer.write("                    try {\n");
        writer.write("                        return objectMapper.readValue(body, " + className + ".class);\n");
        writer.write("                    } catch (Exception e) {\n");
        writer.write("                        throw new RuntimeException(e);\n");
        writer.write("                    }\n");
        writer.write("                });\n");
        writer.write("    }\n\n");
        
        // 生成创建实体的方法
        writer.write("    public CompletableFuture<" + className + "> create" + className + "(" + className + " " + className.toLowerCase() + ") {\n");
        writer.write("        String json;\n");
        writer.write("        try {\n");
        writer.write("            json = objectMapper.writeValueAsString(" + className.toLowerCase() + ");\n");
        writer.write("        } catch (Exception e) {\n");
        writer.write("            throw new RuntimeException(e);\n");
        writer.write("        }\n\n");
        writer.write("        HttpRequest request = HttpRequest.newBuilder()\n");
        writer.write("                .uri(URI.create(baseUrl + \"" + basePath + "\"))\n");
        writer.write("                .header(\"Content-Type\", \"application/json\")\n");
        writer.write("                .POST(HttpRequest.BodyPublishers.ofString(json))\n");
        writer.write("                .build();\n\n");
        writer.write("        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())\n");
        writer.write("                .thenApply(HttpResponse::body)\n");
        writer.write("                .thenApply(body -> {\n");
        writer.write("                    try {\n");
        writer.write("                        return objectMapper.readValue(body, " + className + ".class);\n");
        writer.write("                    } catch (Exception e) {\n");
        writer.write("                        throw new RuntimeException(e);\n");
        writer.write("                    }\n");
        writer.write("                });\n");
        writer.write("    }\n\n");
        
        // 生成更新实体的方法
        writer.write("    public CompletableFuture<" + className + "> update" + className + "(Long id, " + className + " " + className.toLowerCase() + ") {\n");
        writer.write("        String json;\n");
        writer.write("        try {\n");
        writer.write("            json = objectMapper.writeValueAsString(" + className.toLowerCase() + ");\n");
        writer.write("        } catch (Exception e) {\n");
        writer.write("            throw new RuntimeException(e);\n");
        writer.write("        }\n\n");
        writer.write("        HttpRequest request = HttpRequest.newBuilder()\n");
        writer.write("                .uri(URI.create(baseUrl + \"" + basePath + "/\" + id))\n");
        writer.write("                .header(\"Content-Type\", \"application/json\")\n");
        writer.write("                .PUT(HttpRequest.BodyPublishers.ofString(json))\n");
        writer.write("                .build();\n\n");
        writer.write("        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())\n");
        writer.write("                .thenApply(HttpResponse::body)\n");
        writer.write("                .thenApply(body -> {\n");
        writer.write("                    try {\n");
        writer.write("                        return objectMapper.readValue(body, " + className + ".class);\n");
        writer.write("                    } catch (Exception e) {\n");
        writer.write("                        throw new RuntimeException(e);\n");
        writer.write("                    }\n");
        writer.write("                });\n");
        writer.write("    }\n\n");
        
        // 生成删除实体的方法
        writer.write("    public CompletableFuture<Void> delete" + className + "(Long id) {\n");
        writer.write("        HttpRequest request = HttpRequest.newBuilder()\n");
        writer.write("                .uri(URI.create(baseUrl + \"" + basePath + "/\" + id))\n");
        writer.write("                .DELETE()\n");
        writer.write("                .build();\n\n");
        writer.write("        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())\n");
        writer.write("                .thenApply(response -> null);\n");
        writer.write("    }\n\n");
    }

    private void generateModelClasses(File packageDir) throws IOException {
        // 生成User模型类
        File userFile = new File(packageDir, "User.java");
        try (FileWriter writer = new FileWriter(userFile)) {
            writer.write("package " + packageName + ";\n\n");
            writer.write("public class User {\n");
            writer.write("    private Long id;\n");
            writer.write("    private String name;\n");
            writer.write("    private String email;\n\n");
            writer.write("    public User() {}\n\n");
            writer.write("    public User(Long id, String name, String email) {\n");
            writer.write("        this.id = id;\n");
            writer.write("        this.name = name;\n");
            writer.write("        this.email = email;\n");
            writer.write("    }\n\n");
            writer.write("    // Getter和Setter方法\n");
            writer.write("    public Long getId() { return id; }\n");
            writer.write("    public void setId(Long id) { this.id = id; }\n");
            writer.write("    public String getName() { return name; }\n");
            writer.write("    public void setName(String name) { this.name = name; }\n");
            writer.write("    public String getEmail() { return email; }\n");
            writer.write("    public void setEmail(String email) { this.email = email; }\n");
            writer.write("}\n");
        }
        getLog().info("已生成User模型类: " + userFile.getAbsolutePath());

        // 生成Product模型类
        File productFile = new File(packageDir, "Product.java");
        try (FileWriter writer = new FileWriter(productFile)) {
            writer.write("package " + packageName + ";\n\n");
            writer.write("public class Product {\n");
            writer.write("    private Long id;\n");
            writer.write("    private String name;\n");
            writer.write("    private Double price;\n\n");
            writer.write("    public Product() {}\n\n");
            writer.write("    public Product(Long id, String name, Double price) {\n");
            writer.write("        this.id = id;\n");
            writer.write("        this.name = name;\n");
            writer.write("        this.price = price;\n");
            writer.write("    }\n\n");
            writer.write("    // Getter和Setter方法\n");
            writer.write("    public Long getId() { return id; }\n");
            writer.write("    public void setId(Long id) { this.id = id; }\n");
            writer.write("    public String getName() { return name; }\n");
            writer.write("    public void setName(String name) { this.name = name; }\n");
            writer.write("    public Double getPrice() { return price; }\n");
            writer.write("    public void setPrice(Double price) { this.price = price; }\n");
            writer.write("}\n");
        }
        getLog().info("已生成Product模型类: " + productFile.getAbsolutePath());
    }
}