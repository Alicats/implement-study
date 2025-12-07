# Spring Boot SDK Generator Maven Plugin

这是一个 Maven 插件，用于从 Spring Boot Controller 自动生成 Java SDK。

## 功能特性

- 自动扫描 Spring Boot Controller 类
- 生成包含所有 REST API 方法的客户端 SDK
- 生成对应的模型类
- 支持异步 HTTP 调用

## 使用方法

在你的 Maven 项目中，在 `pom.xml` 中添加以下插件配置：

```xml
<plugin>
    <groupId>cn.xej</groupId>
    <artifactId>spring-boot-sdk-generator</artifactId>
    <version>1.0-SNAPSHOT</version>
    <configuration>
        <!-- 可选配置 -->
        <outputDirectory>${project.build.directory}/generated-sources/sdk</outputDirectory>
        <packageName>cn.xej.sdk</packageName>
        <controllerPackage>cn.xej.api.controller</controllerPackage>
    </configuration>
</plugin>
```

然后执行以下命令生成 SDK：

```bash
mvn spring-boot-sdk-generator:generate
```

## 配置参数

| 参数名 | 默认值 | 描述 |
|--------|--------|------|
| outputDirectory | ${project.build.directory}/generated-sources/sdk | SDK 输出目录 |
| packageName | cn.xej.sdk | SDK 包名 |
| controllerPackage | cn.xej.api.controller | 要扫描的 Controller 包名 |

## 生成的 SDK 结构

生成的 SDK 包含以下组件：

1. `ApiClient.java` - 主要的 API 客户端类，包含所有 REST API 方法
2. 模型类 - 与 Controller 中使用的模型对应的类

## 示例

生成的 SDK 可以像这样使用：

```java
ApiClient client = new ApiClient("http://localhost:8080");

// 获取所有用户
CompletableFuture<List<User>> usersFuture = client.getAllUsers();

// 获取特定用户
CompletableFuture<User> userFuture = client.getUserById(1L);

// 创建新用户
User newUser = new User();
newUser.setName("John Doe");
newUser.setEmail("john.doe@example.com");
CompletableFuture<User> createdUserFuture = client.createUser(newUser);
```