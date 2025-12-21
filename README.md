# MedCard Server

Server backend cho ứng dụng MedCard, sử dụng Netty để xử lý HTTP REST API requests từ desktop app.

## Tính năng

- Sử dụng Netty framework để xử lý HTTP requests hiệu quả
- RESTful API endpoints cho các operations:
  - Medicines (CRUD)
  - Prescriptions (CRUD)
  - Inventory (import/export, logs)
  - History/Audit
  - Dashboard statistics
- Database connection pooling với HikariCP
- Standard HTTP/JSON protocol - dễ debug với Postman, curl, etc.
- CORS support

## Cấu trúc

```
server/
├── src/main/java/dnd/server/
│   ├── MedCardServer.java          # Main server class
│   ├── handler/
│   │   └── ConnectionHandler.java   # Xử lý client connections
│   ├── api/
│   │   ├── Request.java            # Request object
│   │   ├── Response.java           # Response object
│   │   └── ApiRouter.java          # Router cho các endpoints
│   └── endpoints/                  # Các endpoint handlers
│       ├── MedicinesEndpoint.java
│       ├── PrescriptionsEndpoint.java
│       ├── InventoryLogsEndpoint.java
│       └── ...
└── build.gradle
```

## Yêu cầu

- Java 11+
- MySQL database
- File `db.properties` ở thư mục gốc của project (cùng cấp với `server/`)

## Cấu hình

File `db.properties` cần có các thông tin sau:

```properties
db.url=jdbc:mysql://localhost:3306/medcard?useSSL=false&serverTimezone=UTC
db.driver=com.mysql.cj.jdbc.Driver
db.username=root
db.password=your_password

db.minConnections=2
db.maxConnections=10
db.connectionTimeout=30000
db.idleTimeout=600000
db.leakDetectionThreshold=60000
```

## Build và chạy

### Sử dụng Gradle

```bash
# Build
cd server
../gradlew build

# Chạy server (port mặc định 8888)
../gradlew runServer

# Hoặc chạy với port tùy chỉnh
java -cp build/classes/java/main:build/libs/* dnd.server.MedCardServer 8888
```

### Sử dụng Java trực tiếp

```bash
# Compile
cd server
javac -cp "path/to/hikaricp.jar:path/to/mysql-connector.jar:path/to/gson.jar" src/main/java/dnd/server/**/*.java

# Chạy
java -cp ".:path/to/libs/*" dnd.server.MedCardServer 8888
```

## API Protocol

Server sử dụng standard HTTP REST API với JSON format.

### Request Format

Standard HTTP request:
- **Method**: GET, POST, PUT, DELETE
- **URL**: `http://localhost:8888/api/{endpoint}`
- **Headers**: `Content-Type: application/json`
- **Body**: JSON (cho POST/PUT)

### Response Format

Standard HTTP response với JSON body:

**Success:**
```json
{
  "statusCode": 200,
  "message": "Success",
  "data": {}
}
```

**Error:**
```json
{
  "statusCode": 404,
  "error": {
    "message": "Not found"
  }
}
```

## API Endpoints

### Medicines
- `GET /api/medicines` - Lấy danh sách thuốc
- `GET /api/medicines/:id` - Lấy chi tiết thuốc
- `POST /api/medicines` - Tạo thuốc mới
- `PUT /api/medicines/:id` - Cập nhật thuốc
- `DELETE /api/medicines/:id` - Xóa thuốc

### Prescriptions
- `GET /api/prescriptions` - Lấy danh sách đơn thuốc
- `GET /api/prescriptions/:id` - Lấy chi tiết đơn thuốc
- `POST /api/prescriptions` - Tạo đơn thuốc mới
- `PUT /api/prescriptions/:id` - Cập nhật đơn thuốc

### Inventory
- `GET /api/inventory/logs` - Lấy lịch sử xuất nhập kho
- `POST /api/inventory/import` - Nhập kho
- `POST /api/inventory/export` - Xuất kho

### History
- `GET /api/history?type=inventory|audit` - Lấy lịch sử

### Dashboard
- `GET /api/dashboard/stats` - Lấy thống kê tổng quan

### Health Check
- `GET /api/health` - Kiểm tra trạng thái server và database

## Ví dụ sử dụng

### Sử dụng curl

**GET /api/medicines**
```bash
curl http://localhost:8888/api/medicines
```

**POST /api/medicines**
```bash
curl -X POST http://localhost:8888/api/medicines \
  -H "Content-Type: application/json" \
  -d '{
    "code": "MED001",
    "name": "Paracetamol 500mg",
    "unit": "viên",
    "quantity": 100,
    "min_quantity": 10
  }'
```

**GET /api/medicines/:id**
```bash
curl http://localhost:8888/api/medicines/MED001
```

**PUT /api/medicines/:id**
```bash
curl -X PUT http://localhost:8888/api/medicines/MED001 \
  -H "Content-Type: application/json" \
  -d '{
    "quantity": 150
  }'
```

**DELETE /api/medicines/:id**
```bash
curl -X DELETE http://localhost:8888/api/medicines/MED001
```

### Sử dụng Postman

1. Import collection hoặc tạo request mới
2. Method: GET/POST/PUT/DELETE
3. URL: `http://localhost:8888/api/{endpoint}`
4. Headers: `Content-Type: application/json`
5. Body (cho POST/PUT): Raw JSON

## Lưu ý

- Server mặc định chạy trên port 8888
- Mỗi request sẽ tạo một connection mới (HTTP-like)
- Server tự động quản lý connection pool cho database
- Đảm bảo MySQL đang chạy và database `medcard` đã được tạo trước khi start server

