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
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashSet;
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
                    .setScanners(Scanners.TypesAnnotated, Scanners.SubTypes);

            Reflections reflections = new Reflections(configuration);

            // 查找@RestController注解的类
            Set<Class<?>> controllerClasses = reflections.getTypesAnnotatedWith(RestController.class);
            // 过滤掉不在指定包内的Controller类
            Set<Class<?>> filteredControllerClasses = new HashSet<>();
            for (Class<?> controllerClass : controllerClasses) {
                if (controllerClass.getPackage().getName().equals(controllerPackage)) {
                    filteredControllerClasses.add(controllerClass);
                }
            }
            controllerClasses = filteredControllerClasses;
            
            // 输出扫描到的@RestController注解的类，便于调试
            for (Class<?> controllerClass : controllerClasses) {
                getLog().info("扫描到的@RestController注解的类: " + controllerClass.getName());
            }

            // 如果找到Controller，生成ApiClient和models
            if (!controllerClasses.isEmpty()) {
                // 创建models目录
                File modelsDir = new File(packageDir, "models");
                if (!modelsDir.exists()) {
                    modelsDir.mkdirs();
                }
                getLog().info("创建models目录: " + modelsDir.getAbsolutePath());

                // 生成统一的ApiClient
                generateApiClient(controllerClasses, packageDir, modelsDir);
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
     * 生成统一的ApiClient类
     */
    private void generateApiClient(Set<Class<?>> controllerClasses, File outputDir, File modelsDir) throws Exception {
        String fileName = "ApiClient.java";
        File outputFile = new File(outputDir, fileName);

        StringBuilder apiClientCode = new StringBuilder();
        apiClientCode.append("package ").append(packageName).append("; ");
        apiClientCode.append("import org.springframework.web.client.RestTemplate;\n");
        apiClientCode.append("import org.springframework.http.*;\n");
        apiClientCode.append("import java.util.Map;");
        apiClientCode.append("import java.util.HashMap;");
        apiClientCode.append("import ").append(packageName).append(".models.*;\n\n");

        apiClientCode.append("/**\n");
        apiClientCode.append(" * 统一API客户端\n");
        apiClientCode.append(" */\n");
        apiClientCode.append("public class ApiClient {\n");
        apiClientCode.append("    private final RestTemplate restTemplate;\n");
        apiClientCode.append("    private final String baseUrl;\n\n");

        apiClientCode.append("    public ApiClient(String baseUrl) {\n");
        apiClientCode.append("        this.restTemplate = new RestTemplate();\n");
        apiClientCode.append("        this.baseUrl = baseUrl.endsWith(\"/\") ? baseUrl : baseUrl + \"/\";\n");
        apiClientCode.append("    }\n\n");

        // 生成所有Controller的方法
        for (Class<?> controllerClass : controllerClasses) {
            getLog().info("处理Controller类: " + controllerClass.getName());
            generateControllerMethods(controllerClass, apiClientCode, modelsDir);
        }

        apiClientCode.append("}\n");

        // 写入文件
        java.nio.file.Files.write(outputFile.toPath(), apiClientCode.toString().getBytes("UTF-8"));
        getLog().info("生成ApiClient文件: " + outputFile.getAbsolutePath());
    }

    /**
     * 生成Controller的所有方法到ApiClient中
     */
    private void generateControllerMethods(Class<?> controllerClass, StringBuilder apiClientCode, File modelsDir) throws Exception {
        // 获取类级别的@RequestMapping注解
        String classMapping = "";
        if (controllerClass.isAnnotationPresent(RequestMapping.class)) {
            RequestMapping classAnnotation = controllerClass.getAnnotation(RequestMapping.class);
            if (classAnnotation.value().length > 0) {
                classMapping = classAnnotation.value()[0];
            }
        }

        // 生成方法
        for (java.lang.reflect.Method method : controllerClass.getDeclaredMethods()) {
            if (isRequestMappingMethod(method)) {
                generateMethodApi(method, classMapping, apiClientCode, modelsDir);
            }
        }
    }

    /**
     * 生成API方法到ApiClient中
     */
    private void generateMethodApi(java.lang.reflect.Method method, String classMapping, StringBuilder apiClientCode, File modelsDir) throws Exception {
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

        // 组合完整的URL路径
        String fullMapping = classMapping + mappingValue;
        
        // 生成方法签名
        String methodName = method.getName();
        apiClientCode.append("    public ").append(method.getReturnType().getSimpleName()).append(" ");
        apiClientCode.append(methodName).append("(");

        // 添加参数
        Class<?>[] paramTypes = method.getParameterTypes();
        java.lang.reflect.Parameter[] parameters = method.getParameters();
        
        // 生成模型类
        generateModelClasses(method, modelsDir);
        
        for (int i = 0; i < paramTypes.length; i++) {
            if (i > 0) apiClientCode.append(", ");
            apiClientCode.append(paramTypes[i].getSimpleName()).append(" ").append(parameters[i].getName());
        }

        apiClientCode.append(") {\n");
        apiClientCode.append("        String url = baseUrl + \"");
        apiClientCode.append(fullMapping);
        apiClientCode.append("\";\n");
        apiClientCode.append("        HttpHeaders headers = new HttpHeaders();\n");
        apiClientCode.append("        headers.setContentType(MediaType.APPLICATION_JSON);\n\n");

        // 创建请求体
        if (paramTypes.length > 0) {
            apiClientCode.append("        HttpEntity<");
            apiClientCode.append(paramTypes[0].getSimpleName());
            apiClientCode.append("> request = \n");
            apiClientCode.append("            new HttpEntity<");
            apiClientCode.append(paramTypes[0].getSimpleName());
            apiClientCode.append(">(").append(parameters[0].getName()).append(", headers);\n\n");
        } else {
            apiClientCode.append("        HttpEntity<String> request = new HttpEntity<>(headers);\n\n");
        }

        apiClientCode.append("        ResponseEntity<");
        apiClientCode.append(method.getReturnType().getSimpleName());
        apiClientCode.append("> response = \n");
        apiClientCode.append("            restTemplate.exchange(url, HttpMethod.");
        apiClientCode.append(httpMethod);
        apiClientCode.append(", request, ");
        apiClientCode.append(method.getReturnType().getSimpleName());
        apiClientCode.append(".class);\n\n");

        apiClientCode.append("        return response.getBody();\n");
        apiClientCode.append("    }\n\n");
    }

    /**
     * 生成模型类（请求和响应）
     */
    private void generateModelClasses(java.lang.reflect.Method method, File modelsDir) throws Exception {
        String methodName = method.getName();
        
        // 生成请求模型（示例：CreateUserRequest）
        String requestClassName = capitalizeFirstLetter(methodName) + "Request";
        generateRequestModel(method, requestClassName, modelsDir);
        
        // 生成响应模型（示例：CreateUserResponse）
        String responseClassName = capitalizeFirstLetter(methodName) + "Response";
        generateResponseModel(method, responseClassName, modelsDir);
        
        // 生成业务对象模型（示例：User）
        generateBusinessModels(method, modelsDir);
    }

    /**
 * 生成请求模型类
     */
    private void generateRequestModel(java.lang.reflect.Method method, String className, File modelsDir) throws Exception {
        File outputFile = new File(modelsDir, className + ".java");
        
        // 如果文件已存在，跳过
        if (outputFile.exists()) {
            getLog().info("模型类已存在，跳过生成: " + outputFile.getAbsolutePath());
            return;
        }
        
        StringBuilder modelCode = new StringBuilder();
        modelCode.append("package ").append(packageName).append(".models;\n\n");
        modelCode.append("import java.io.Serializable;\n");
        modelCode.append("import com.fasterxml.jackson.annotation.JsonProperty;\n");
        modelCode.append("import java.util.*;\n\n");
        
        modelCode.append("/**\n");
        modelCode.append(" * ").append(className).append(" 请求模型\n");
        modelCode.append(" */\n");
        modelCode.append("public class ").append(className).append(" implements Serializable {\n");
        modelCode.append("    private static final long serialVersionUID = 1L;\n\n");
        
        // 查找@RequestBody注解的参数
        java.lang.reflect.Parameter[] parameters = method.getParameters();
        Class<?> requestBodyType = null;
        for (java.lang.reflect.Parameter parameter : parameters) {
            if (parameter.isAnnotationPresent(RequestBody.class)) {
                requestBodyType = parameter.getType();
                break;
            }
        }
        
        // 如果找到@RequestBody参数，提取其字段
        if (requestBodyType != null && !requestBodyType.isPrimitive() && !requestBodyType.getName().startsWith("java.lang.")) {
            // 获取请求体类型的所有字段
            java.lang.reflect.Field[] fields = requestBodyType.getDeclaredFields();
            for (java.lang.reflect.Field field : fields) {
                // 跳过静态字段
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                
                String fieldName = field.getName();
                Type fieldType = field.getGenericType();
                String fieldTypeName = getFieldTypeName(fieldType);
                
                // 添加字段
                modelCode.append("    @JsonProperty(\"").append(fieldName).append("\")\n");
                modelCode.append("    private ").append(fieldTypeName).append(" ").append(fieldName).append(";\n\n");
            }
            
            // 添加getter和setter
            for (java.lang.reflect.Field field : fields) {
                // 跳过静态字段
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                
                String fieldName = field.getName();
                Type fieldType = field.getGenericType();
                String fieldTypeName = getFieldTypeName(fieldType);
                String capitalizedFieldName = capitalizeFirstLetter(fieldName);
                
                // 添加getter
                if (fieldTypeName.equals("boolean")) {
                    modelCode.append("    public boolean is").append(capitalizedFieldName).append("() {\n");
                } else {
                    modelCode.append("    public ").append(fieldTypeName).append(" get").append(capitalizedFieldName).append("() {\n");
                }
                modelCode.append("        return ").append(fieldName).append(";\n");
                modelCode.append("    }\n\n");
                
                // 添加setter
                modelCode.append("    public void set").append(capitalizedFieldName).append("(").append(fieldTypeName).append(" ").append(fieldName).append(") {\n");
                modelCode.append("        this.").append(fieldName).append(" = ").append(fieldName).append(";\n");
                modelCode.append("    }\n\n");
            }
        } else {
            // 如果没有@RequestBody参数或参数是基本类型，生成空的请求模型
            modelCode.append("    // 无请求体参数\n");
        }
        
        modelCode.append("}\n");
        
        // 写入文件
        java.nio.file.Files.write(outputFile.toPath(), modelCode.toString().getBytes("UTF-8"));
        getLog().info("生成请求模型类: " + outputFile.getAbsolutePath());
    }
    
    /**
     * 生成响应模型类
     */
    private void generateResponseModel(java.lang.reflect.Method method, String className, File modelsDir) throws Exception {
        File outputFile = new File(modelsDir, className + ".java");
        
        // 如果文件已存在，跳过
        if (outputFile.exists()) {
            getLog().info("模型类已存在，跳过生成: " + outputFile.getAbsolutePath());
            return;
        }
        
        StringBuilder modelCode = new StringBuilder();
        modelCode.append("package ").append(packageName).append(".models;\n\n");
        modelCode.append("import java.io.Serializable;\n");
        modelCode.append("import com.fasterxml.jackson.annotation.JsonProperty;\n");
        modelCode.append("import java.util.*;\n\n");
        
        modelCode.append("/**\n");
        modelCode.append(" * ").append(className).append(" 响应模型\n");
        modelCode.append(" */\n");
        modelCode.append("public class ").append(className).append(" implements Serializable {\n");
        modelCode.append("    private static final long serialVersionUID = 1L;\n\n");
        
        // 获取方法的返回类型
        Class<?> returnType = method.getReturnType();
        
        // 如果返回类型是自定义类型（不是基本类型或java.lang包下的类型），提取其字段
        if (returnType != null && !returnType.isPrimitive() && !returnType.getName().startsWith("java.lang.")) {
            // 获取返回类型的所有字段
            java.lang.reflect.Field[] fields = returnType.getDeclaredFields();
            for (java.lang.reflect.Field field : fields) {
                // 跳过静态字段
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                
                String fieldName = field.getName();
                Type fieldType = field.getGenericType();
                String fieldTypeName = getFieldTypeName(fieldType);
                
                // 添加字段
                modelCode.append("    @JsonProperty(\"").append(fieldName).append("\")\n");
                modelCode.append("    private ").append(fieldTypeName).append(" ").append(fieldName).append(";\n\n");
            }
            
            // 添加getter和setter
            for (java.lang.reflect.Field field : fields) {
                // 跳过静态字段
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                
                String fieldName = field.getName();
                Type fieldType = field.getGenericType();
                String fieldTypeName = getFieldTypeName(fieldType);
                String capitalizedFieldName = capitalizeFirstLetter(fieldName);
                
                // 添加getter
                if (fieldTypeName.equals("boolean")) {
                    modelCode.append("    public boolean is").append(capitalizedFieldName).append("() {\n");
                } else {
                    modelCode.append("    public ").append(fieldTypeName).append(" get").append(capitalizedFieldName).append("() {\n");
                }
                modelCode.append("        return ").append(fieldName).append(";\n");
                modelCode.append("    }\n\n");
                
                // 添加setter
                modelCode.append("    public void set").append(capitalizedFieldName).append("(").append(fieldTypeName).append(" ").append(fieldName).append(") {\n");
                modelCode.append("        this.").append(fieldName).append(" = ").append(fieldName).append(";\n");
                modelCode.append("    }\n\n");
            }
        } else {
            // 如果返回类型是基本类型或简单类型，生成包含该类型的响应模型
            String fieldType = returnType.getSimpleName();
            modelCode.append("    @JsonProperty(\"result\")\n");
            modelCode.append("    private ").append(fieldType).append(" result;\n\n");
            
            // 添加getter和setter
            if (fieldType.equals("boolean")) {
                modelCode.append("    public boolean isResult() {\n");
            } else {
                modelCode.append("    public ").append(fieldType).append(" getResult() {\n");
            }
            modelCode.append("        return result;\n");
            modelCode.append("    }\n\n");
            
            modelCode.append("    public void setResult(").append(fieldType).append(" result) {\n");
            modelCode.append("        this.result = result;\n");
            modelCode.append("    }\n\n");
        }
        
        modelCode.append("}\n");
        
        // 写入文件
        java.nio.file.Files.write(outputFile.toPath(), modelCode.toString().getBytes("UTF-8"));
        getLog().info("生成响应模型类: " + outputFile.getAbsolutePath());
    }
    
    /**
     * 生成业务对象模型类
     */
    private void generateBusinessModels(java.lang.reflect.Method method, File modelsDir) throws Exception {
        Set<Class<?>> businessClasses = new HashSet<>();
        
        // 分析方法参数中的自定义类型
        java.lang.reflect.Parameter[] parameters = method.getParameters();
        for (java.lang.reflect.Parameter parameter : parameters) {
            Class<?> paramType = parameter.getType();
            collectBusinessClasses(paramType, businessClasses);
        }
        
        // 分析返回类型中的自定义类型
        Class<?> returnType = method.getReturnType();
        collectBusinessClasses(returnType, businessClasses);
        
        // 为所有收集到的自定义类型生成模型类
        for (Class<?> businessClass : businessClasses) {
            String className = businessClass.getSimpleName();
            generateBusinessModelClass(businessClass, className, modelsDir);
        }
    }
    
    /**
     * 收集业务对象类
     */
    private void collectBusinessClasses(Class<?> type, Set<Class<?>> businessClasses) {
        // 如果是基本类型或java.lang包下的类型，跳过
        if (type == null || type.isPrimitive() || type.getName().startsWith("java.lang.") || 
            type.getName().startsWith("java.util.") || type.getName().startsWith("java.time.")) {
            return;
        }
        
        // 获取类名
        String simpleName = type.getSimpleName();
        
        // 如果不是请求类或响应类，添加到业务类集合
        if (!simpleName.endsWith("Request") && !simpleName.endsWith("Response")) {
            businessClasses.add(type);
        }
            
        // 分析当前类型的字段（无论是否是请求/响应类，都需要分析字段以提取业务对象）
        java.lang.reflect.Field[] fields = type.getDeclaredFields();
        for (java.lang.reflect.Field field : fields) {
            Class<?> fieldType = field.getType();
            
            // 如果是数组类型，分析其元素类型
            if (fieldType.isArray()) {
                collectBusinessClasses(fieldType.getComponentType(), businessClasses);
            }
            // 如果是集合类型，分析其泛型参数
            else if (java.util.Collection.class.isAssignableFrom(fieldType)) {
                java.lang.reflect.Type genericType = field.getGenericType();
                if (genericType instanceof java.lang.reflect.ParameterizedType) {
                    java.lang.reflect.ParameterizedType paramType = (java.lang.reflect.ParameterizedType) genericType;
                    java.lang.reflect.Type[] typeArguments = paramType.getActualTypeArguments();
                    for (java.lang.reflect.Type typeArg : typeArguments) {
                        if (typeArg instanceof Class) {
                            collectBusinessClasses((Class<?>) typeArg, businessClasses);
                        }
                    }
                }
            }
            // 否则，直接收集字段类型
            else {
                collectBusinessClasses(fieldType, businessClasses);
            }
        }
    }
    
    /**
     * 生成单个业务对象模型类
     */
    private void generateBusinessModelClass(Class<?> businessClass, String className, File modelsDir) throws Exception {
        File outputFile = new File(modelsDir, className + ".java");
        
        // 如果文件已存在，跳过
        if (outputFile.exists()) {
            getLog().info("业务对象类已存在，跳过生成: " + outputFile.getAbsolutePath());
            return;
        }
        
        StringBuilder modelCode = new StringBuilder();
        modelCode.append("package ").append(packageName).append(".models;\n\n");
        modelCode.append("import java.io.Serializable;\n");
        modelCode.append("import com.fasterxml.jackson.annotation.JsonProperty;\n");
        modelCode.append("import java.util.*;\n\n");
        
        modelCode.append("/**\n");
        modelCode.append(" * ").append(className).append(" 业务对象模型\n");
        modelCode.append(" */\n");
        modelCode.append("public class ").append(className).append(" implements Serializable {\n");
        modelCode.append("    private static final long serialVersionUID = 1L;\n\n");
        
        // 获取业务类的所有字段
        java.lang.reflect.Field[] fields = businessClass.getDeclaredFields();
        for (java.lang.reflect.Field field : fields) {
            // 跳过静态字段
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            
            String fieldName = field.getName();
            Type fieldType = field.getGenericType();
            String fieldTypeName = getFieldTypeName(fieldType);
            
            // 添加字段
            modelCode.append("    @JsonProperty(\"").append(fieldName).append("\")\n");
            modelCode.append("    private ").append(fieldTypeName).append(" ").append(fieldName).append(";\n\n");
        }
        
        // 添加getter和setter
        for (java.lang.reflect.Field field : fields) {
            // 跳过静态字段
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            
            String fieldName = field.getName();
            Type fieldType = field.getGenericType();
            String fieldTypeName = getFieldTypeName(fieldType);
            String capitalizedFieldName = capitalizeFirstLetter(fieldName);
            
            // 添加getter
            if (fieldTypeName.equals("boolean")) {
                modelCode.append("    public boolean is").append(capitalizedFieldName).append("() {\n");
            } else {
                modelCode.append("    public ").append(fieldTypeName).append(" get").append(capitalizedFieldName).append("() {\n");
            }
            modelCode.append("        return ").append(fieldName).append(";\n");
            modelCode.append("    }\n\n");
            
            // 添加setter
            modelCode.append("    public void set").append(capitalizedFieldName).append("(").append(fieldTypeName).append(" ").append(fieldName).append(") {\n");
            modelCode.append("        this.").append(fieldName).append(" = ").append(fieldName).append(";\n");
            modelCode.append("    }\n\n");
        }
        
        modelCode.append("}\n");
        
        // 写入文件
        java.nio.file.Files.write(outputFile.toPath(), modelCode.toString().getBytes("UTF-8"));
        getLog().info("生成业务对象模型类: " + outputFile.getAbsolutePath());
    }
    
    /**
     * 获取完整的字段类型名称，包括泛型参数
     */
    private String getFieldTypeName(Type type) {
        if (type instanceof Class) {
            Class<?> clazz = (Class<?>) type;
            return clazz.getSimpleName();
        } else if (type instanceof ParameterizedType) {
            ParameterizedType paramType = (ParameterizedType) type;
            Type rawType = paramType.getRawType();
            Type[] typeArgs = paramType.getActualTypeArguments();
            
            StringBuilder typeName = new StringBuilder();
            typeName.append(getFieldTypeName(rawType));
            typeName.append("<");
            
            for (int i = 0; i < typeArgs.length; i++) {
                if (i > 0) typeName.append(", ");
                typeName.append(getFieldTypeName(typeArgs[i]));
            }
            
            typeName.append(">");
            return typeName.toString();
        }
        return type.toString();
    }
    
    /**
     * 将字符串首字母大写
     */
    private String capitalizeFirstLetter(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
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
}