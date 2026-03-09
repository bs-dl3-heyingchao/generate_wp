# wp-tool-api

基于 Spring Boot 的 RESTful API 工程（JDK 17）。

## 基本信息

- GroupId: `com.neusoft.bsdl`
- ArtifactId: `wp-tool-api`
- Package: `com.neusoft.bsdl.wptool.api`
- Spring Boot: `4.0.3`
- 依赖模块: `com.neusoft.bsdl:wp-tool-core:1.0.0-SNAPSHOT`
- 默认端口: `8080`

## API 文档

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- 分组 OpenAPI(JSON): `http://localhost:8080/v3/api-docs/wp-tool`

## 统一响应结构

所有接口返回以下结构：

```json
{
  "code": 200,
  "message": "OK",
  "data": {},
  "timestamp": "2026-03-09T01:23:45.678Z"
}
```

## 接口列表

### 1) 健康检查

- 方法/路径: `GET /api/v1/health-check`

成功响应示例：

```json
{
  "code": 200,
  "message": "OK",
  "data": {
    "status": "UP"
  },
  "timestamp": "2026-03-09T01:23:45.678Z"
}
```

### 2) 画面设计书 Excel 解析

- 方法/路径: `POST /api/v1/excel/parse-screen`
- Content-Type: `multipart/form-data`
- 参数:
  - `file` (必填): Excel 文件

`curl` 示例：

```bash
curl -X POST "http://localhost:8080/api/v1/excel/parse-screen" \
  -F "file=@./screen-design.xlsx"
```

成功响应说明：

- `data` 为 `ScreenExcelContent`
- `data.sheetList[*].content` 已被序列化为 JSON 字符串

失败响应示例（缺少 `file` 参数）：

```json
{
  "code": 400,
  "message": "Required part 'file' is not present.",
  "data": null,
  "timestamp": "2026-03-09T01:23:45.678Z"
}
```

## 常见错误码

- `400`: 请求参数错误、上传文件为空、解析失败
- `404`: 访问了不存在的资源
- `500`: 服务内部异常

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

应用启动后可访问：

- `http://localhost:8080/api/v1/health-check`
- `http://localhost:8080/swagger-ui/index.html`

## Docker 镜像打包

已提供 `Dockerfile`（本地先打包 Jar，再拷贝进镜像）。

在 `wp-tool-api` 目录执行：

```bash
mvn clean package -DskipTests
docker build -t wp-tool-api:latest .
```

运行容器：

```bash
docker run --rm -p 8080:8080 wp-tool-api:latest
```
