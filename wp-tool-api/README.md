# wp-tool-api

基于 Spring Boot 的 RESTful API 工程（JDK 17）。

## 基本信息

- GroupId: `com.neusoft.bsdl`
- ArtifactId: `wp-tool-api`
- Package: `com.neusoft.bsdl.wptool.api`
- 依赖模块: `com.neusoft.bsdl:wp-tool-core:1.0.0-SNAPSHOT`

## 接口

- `GET /api/v1/greeting`

示例响应：

```json
{
  "app": "wp-tool-api",
  "message": "Hello from wp-tool-core"
}
```

## 启动与测试

先在 `wp-tool-core` 目录执行：

```bash
mvn clean install
```

再在 `wp-tool-api` 目录执行：

```bash
mvn clean test
mvn spring-boot:run
```

## Docker 镜像打包

已提供 `wp-tool-api/Dockerfile`（多阶段构建，会先构建 `wp-tool-core`，再打包 `wp-tool-api`）。

在 `wp-tools` 根目录执行：

```bash
docker build -f wp-tool-api/Dockerfile -t wp-tool-api:latest .
```

运行容器：

```bash
docker run --rm -p 8080:8080 wp-tool-api:latest
```
