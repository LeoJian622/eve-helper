# 部署运维手册

本文档提供 EVE Helper 项目的部署流程、运维监控和故障处理指南。

## 📋 目录

- [部署流程](#部署流程)
- [环境配置](#环境配置)
- [监控告警](#监控告警)
- [常见问题处理](#常见问题处理)
- [回滚流程](#回滚流程)
- [性能优化](#性能优化)

## 🚀 部署流程

### 前置要求

#### 服务器要求
- **操作系统**: Linux (Ubuntu 20.04+ / CentOS 7+)
- **CPU**: 2核心以上
- **内存**: 4GB以上
- **磁盘**: 20GB以上可用空间

#### 软件要求
- **JDK**: 11 或更高版本
- **MySQL**: 8.0+
- **Redis**: 5.0+
- **Nginx**: 1.18+ (可选,用于反向代理)

### 1. 准备部署包

#### 方式1: 本地构建

```bash
# 克隆代码
git clone https://github.com/your-org/eve-helper.git
cd eve-helper

# 切换到发布分支
git checkout v0.0.2

# 构建应用
mvn clean package -DskipTests

# 部署包位置
ls -lh target/eve-helper-0.0.2-SNAPSHOT.jar
```

#### 方式2: CI/CD自动构建

```bash
# 从CI/CD系统下载构建产物
wget https://ci.example.com/artifacts/eve-helper-0.0.2-SNAPSHOT.jar
```

### 2. 服务器环境准备

#### 安装JDK

```bash
# Ubuntu/Debian
sudo apt update
sudo apt install openjdk-11-jdk

# CentOS/RHEL
sudo yum install java-11-openjdk-devel

# 验证安装
java -version
```

#### 安装MySQL

```bash
# Ubuntu/Debian
sudo apt install mysql-server

# CentOS/RHEL
sudo yum install mysql-server

# 启动MySQL
sudo systemctl start mysql
sudo systemctl enable mysql

# 安全配置
sudo mysql_secure_installation
```

#### 安装Redis

```bash
# Ubuntu/Debian
sudo apt install redis-server

# CentOS/RHEL
sudo yum install redis

# 启动Redis
sudo systemctl start redis
sudo systemctl enable redis

# 配置Redis密码
sudo vim /etc/redis/redis.conf
# 添加: requirepass your_redis_password

# 重启Redis
sudo systemctl restart redis
```

### 3. 数据库初始化

```bash
# 创建数据库
mysql -u root -p << EOF
CREATE DATABASE IF NOT EXISTS eve_helper CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS eve CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 创建应用用户
CREATE USER 'eve_app'@'localhost' IDENTIFIED BY 'secure_password';
GRANT ALL PRIVILEGES ON eve_helper.* TO 'eve_app'@'localhost';
GRANT SELECT ON eve.* TO 'eve_app'@'localhost';
FLUSH PRIVILEGES;
EOF

# 导入数据库结构
mysql -u root -p eve_helper < db/schema.sql
mysql -u root -p eve < db/eve_schema.sql
```

### 4. 配置环境变量

```bash
# 创建应用目录
sudo mkdir -p /opt/eve-helper
sudo mkdir -p /opt/eve-helper/logs
sudo mkdir -p /opt/eve-helper/config

# 创建环境变量文件
sudo vim /opt/eve-helper/config/.env
```

填入以下内容:

```bash
# 数据库配置
DB_HOST=localhost
DB_PORT=3306
DB_SYSTEM_USERNAME=eve_app
DB_SYSTEM_PASSWORD=secure_password
DB_EVE_USERNAME=eve_app
DB_EVE_PASSWORD=secure_password

# Redis配置
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=your_redis_password

# JWT密钥库配置
KEYSTORE_PASSWORD=your_keystore_password
KEY_PASSWORD=your_key_password
```

### 5. 部署应用

#### 复制部署包

```bash
# 复制JAR文件
sudo cp eve-helper-0.0.2-SNAPSHOT.jar /opt/eve-helper/

# 复制密钥库文件
sudo cp eve-jwt.jks /opt/eve-helper/config/

# 设置权限
sudo chown -R eve-app:eve-app /opt/eve-helper
sudo chmod 600 /opt/eve-helper/config/.env
```

#### 创建systemd服务

```bash
sudo vim /etc/systemd/system/eve-helper.service
```

填入以下内容:

```ini
[Unit]
Description=EVE Helper Application
After=syslog.target network.target mysql.service redis.service

[Service]
Type=simple
User=eve-app
Group=eve-app
WorkingDirectory=/opt/eve-helper
EnvironmentFile=/opt/eve-helper/config/.env
ExecStart=/usr/bin/java \
    -Xms512m \
    -Xmx2g \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=200 \
    -Dspring.profiles.active=prod \
    -Dserver.port=9999 \
    -Dlogging.file.path=/opt/eve-helper/logs \
    -jar /opt/eve-helper/eve-helper-0.0.2-SNAPSHOT.jar

SuccessExitStatus=143
StandardOutput=journal
StandardError=journal
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
```

#### 启动服务

```bash
# 重新加载systemd配置
sudo systemctl daemon-reload

# 启动服务
sudo systemctl start eve-helper

# 设置开机自启
sudo systemctl enable eve-helper

# 查看服务状态
sudo systemctl status eve-helper

# 查看日志
sudo journalctl -u eve-helper -f
```

### 6. 配置Nginx反向代理 (可选)

```bash
sudo vim /etc/nginx/sites-available/eve-helper
```

填入以下内容:

```nginx
upstream eve-helper {
    server 127.0.0.1:9999;
}

server {
    listen 80;
    server_name eve-helper.example.com;

    # 重定向到HTTPS
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name eve-helper.example.com;

    # SSL证书配置
    ssl_certificate /etc/ssl/certs/eve-helper.crt;
    ssl_certificate_key /etc/ssl/private/eve-helper.key;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;

    # 日志配置
    access_log /var/log/nginx/eve-helper-access.log;
    error_log /var/log/nginx/eve-helper-error.log;

    # 代理配置
    location / {
        proxy_pass http://eve-helper;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # WebSocket支持
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";

        # 超时配置
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }

    # 静态资源缓存
    location ~* \.(jpg|jpeg|png|gif|ico|css|js)$ {
        proxy_pass http://eve-helper;
        expires 30d;
        add_header Cache-Control "public, immutable";
    }

    # 健康检查端点
    location /actuator/health {
        proxy_pass http://eve-helper;
        access_log off;
    }
}
```

启用配置:

```bash
# 创建软链接
sudo ln -s /etc/nginx/sites-available/eve-helper /etc/nginx/sites-enabled/

# 测试配置
sudo nginx -t

# 重启Nginx
sudo systemctl restart nginx
```

### 7. 验证部署

```bash
# 检查应用健康状态
curl http://localhost:9999/actuator/health

# 检查API响应
curl http://localhost:9999/api/v1/health

# 查看应用日志
tail -f /opt/eve-helper/logs/spring.log
```

## 🔧 环境配置

### 生产环境配置

创建 `application-prod.yml`:

```yaml
server:
  port: 9999
  compression:
    enabled: true
    mime-types: text/html,text/xml,text/plain,text/css,application/javascript,application/json

spring:
  profiles:
    active: prod
  datasource:
    druid:
      system:
        username: ${DB_SYSTEM_USERNAME}
        password: ${DB_SYSTEM_PASSWORD}
        url: jdbc:mysql://${DB_HOST}:${DB_PORT}/eve_helper?serverTimezone=UTC&useUnicode=true&characterEncoding=utf8&useSSL=true
        initial-size: 10
        min-idle: 10
        max-active: 50
        max-wait: 60000
      eve:
        username: ${DB_EVE_USERNAME}
        password: ${DB_EVE_PASSWORD}
        url: jdbc:mysql://${DB_HOST}:${DB_PORT}/eve?useUnicode=true&characterEncoding=utf8&useSSL=true
        initial-size: 10
        min-idle: 10
        max-active: 50
  redis:
    host: ${REDIS_HOST}
    port: ${REDIS_PORT}
    password: ${REDIS_PASSWORD}
    timeout: 5000ms
    lettuce:
      pool:
        max-active: 20
        max-idle: 10
        min-idle: 5

logging:
  level:
    root: INFO
    xyz.foolcat.eve.evehelper: INFO
  file:
    path: /opt/eve-helper/logs
    max-size: 100MB
    max-history: 30

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

## 📊 监控告警

### 1. Prometheus监控

#### 安装Prometheus

```bash
# 下载Prometheus
wget https://github.com/prometheus/prometheus/releases/download/v2.40.0/prometheus-2.40.0.linux-amd64.tar.gz
tar xvfz prometheus-2.40.0.linux-amd64.tar.gz
cd prometheus-2.40.0.linux-amd64

# 配置Prometheus
vim prometheus.yml
```

添加EVE Helper监控目标:

```yaml
scrape_configs:
  - job_name: 'eve-helper'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['localhost:9999']
```

启动Prometheus:

```bash
./prometheus --config.file=prometheus.yml
```

### 2. Grafana可视化

#### 安装Grafana

```bash
# Ubuntu/Debian
sudo apt-get install -y software-properties-common
sudo add-apt-repository "deb https://packages.grafana.com/oss/deb stable main"
wget -q -O - https://packages.grafana.com/gpg.key | sudo apt-key add -
sudo apt-get update
sudo apt-get install grafana

# 启动Grafana
sudo systemctl start grafana-server
sudo systemctl enable grafana-server
```

访问 http://localhost:3000 (默认用户名/密码: admin/admin)

#### 配置数据源

1. 添加Prometheus数据源
2. 导入Spring Boot Dashboard (ID: 4701)
3. 配置告警规则

### 3. 关键指标监控

#### JVM指标
- **堆内存使用率**: `jvm_memory_used_bytes / jvm_memory_max_bytes`
- **GC频率**: `rate(jvm_gc_pause_seconds_count[5m])`
- **GC耗时**: `jvm_gc_pause_seconds_sum`

#### 应用指标
- **请求QPS**: `rate(http_server_requests_seconds_count[1m])`
- **响应时间**: `http_server_requests_seconds_sum / http_server_requests_seconds_count`
- **错误率**: `rate(http_server_requests_seconds_count{status=~"5.."}[5m])`

#### 数据库指标
- **连接池使用率**: `hikaricp_connections_active / hikaricp_connections_max`
- **慢查询**: 通过MySQL慢查询日志监控

#### Redis指标
- **连接数**: `redis_connected_clients`
- **内存使用**: `redis_memory_used_bytes`
- **命令执行**: `rate(redis_commands_processed_total[1m])`

### 4. 告警规则

创建告警规则文件 `alerts.yml`:

```yaml
groups:
  - name: eve-helper-alerts
    interval: 30s
    rules:
      # 应用健康检查
      - alert: ApplicationDown
        expr: up{job="eve-helper"} == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "EVE Helper应用宕机"
          description: "应用已经宕机超过1分钟"

      # 内存使用率告警
      - alert: HighMemoryUsage
        expr: (jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"}) > 0.9
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "内存使用率过高"
          description: "堆内存使用率超过90%"

      # 响应时间告警
      - alert: HighResponseTime
        expr: histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m])) > 1
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "响应时间过长"
          description: "95分位响应时间超过1秒"

      # 错误率告警
      - alert: HighErrorRate
        expr: rate(http_server_requests_seconds_count{status=~"5.."}[5m]) > 0.05
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "错误率过高"
          description: "5xx错误率超过5%"

      # 数据库连接池告警
      - alert: DatabaseConnectionPoolExhausted
        expr: (hikaricp_connections_active / hikaricp_connections_max) > 0.9
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "数据库连接池即将耗尽"
          description: "连接池使用率超过90%"
```

## 🔥 常见问题处理

### 问题1: 应用无法启动

#### 症状
- systemd服务启动失败
- 日志显示端口被占用或数据库连接失败

#### 排查步骤

```bash
# 1. 查看服务状态
sudo systemctl status eve-helper

# 2. 查看详细日志
sudo journalctl -u eve-helper -n 100 --no-pager

# 3. 检查端口占用
sudo netstat -tlnp | grep 9999

# 4. 测试数据库连接
mysql -h ${DB_HOST} -P ${DB_PORT} -u ${DB_SYSTEM_USERNAME} -p

# 5. 测试Redis连接
redis-cli -h ${REDIS_HOST} -p ${REDIS_PORT} -a ${REDIS_PASSWORD} ping
```

#### 解决方法

```bash
# 如果端口被占用,终止占用进程或修改端口
sudo kill -9 <PID>

# 如果数据库连接失败,检查配置和权限
mysql -u root -p
GRANT ALL PRIVILEGES ON eve_helper.* TO 'eve_app'@'localhost';
FLUSH PRIVILEGES;

# 重启服务
sudo systemctl restart eve-helper
```

### 问题2: 内存溢出 (OOM)

#### 症状
- 应用突然崩溃
- 日志显示 `java.lang.OutOfMemoryError`

#### 排查步骤

```bash
# 1. 查看堆转储文件
ls -lh /opt/eve-helper/*.hprof

# 2. 使用jmap分析内存
jmap -heap <PID>
jmap -histo:live <PID> | head -20

# 3. 查看GC日志
grep "Full GC" /opt/eve-helper/logs/gc.log
```

#### 解决方法

```bash
# 1. 增加堆内存
sudo vim /etc/systemd/system/eve-helper.service
# 修改: -Xms1g -Xmx4g

# 2. 启用堆转储
# 添加JVM参数: -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/opt/eve-helper/dumps

# 3. 重启服务
sudo systemctl daemon-reload
sudo systemctl restart eve-helper
```

### 问题3: 数据库连接池耗尽

#### 症状
- 应用响应缓慢
- 日志显示 `Connection pool exhausted`

#### 排查步骤

```bash
# 1. 查看数据库连接数
mysql -u root -p -e "SHOW PROCESSLIST;"

# 2. 查看应用连接池状态
curl http://localhost:9999/actuator/metrics/hikaricp.connections.active
curl http://localhost:9999/actuator/metrics/hikaricp.connections.max

# 3. 查看慢查询
mysql -u root -p -e "SELECT * FROM information_schema.processlist WHERE time > 10;"
```

#### 解决方法

```yaml
# 调整连接池配置
spring:
  datasource:
    druid:
      system:
        max-active: 100  # 增加最大连接数
        max-wait: 30000  # 减少等待时间
```

### 问题4: Redis连接超时

#### 症状
- 缓存操作失败
- 日志显示 `Redis connection timeout`

#### 排查步骤

```bash
# 1. 测试Redis连接
redis-cli -h ${REDIS_HOST} -p ${REDIS_PORT} -a ${REDIS_PASSWORD} ping

# 2. 查看Redis状态
redis-cli -h ${REDIS_HOST} -p ${REDIS_PORT} -a ${REDIS_PASSWORD} info

# 3. 检查网络延迟
ping ${REDIS_HOST}
```

#### 解决方法

```yaml
# 调整Redis超时配置
spring:
  redis:
    timeout: 10000ms  # 增加超时时间
    lettuce:
      pool:
        max-active: 50  # 增加连接池大小
```

### 问题5: 高CPU使用率

#### 症状
- CPU使用率持续超过80%
- 应用响应缓慢

#### 排查步骤

```bash
# 1. 查看进程CPU使用
top -p <PID>

# 2. 查看线程CPU使用
top -H -p <PID>

# 3. 生成线程转储
jstack <PID> > thread_dump.txt

# 4. 分析热点方法
# 使用async-profiler或JProfiler
```

#### 解决方法

1. 优化慢查询
2. 添加缓存
3. 优化算法
4. 增加服务器资源

## 🔄 回滚流程

### 1. 准备回滚

```bash
# 1. 停止当前服务
sudo systemctl stop eve-helper

# 2. 备份当前版本
sudo cp /opt/eve-helper/eve-helper-0.0.2-SNAPSHOT.jar \
       /opt/eve-helper/backup/eve-helper-0.0.2-SNAPSHOT.jar.$(date +%Y%m%d%H%M%S)

# 3. 备份数据库
mysqldump -u root -p eve_helper > eve_helper_backup_$(date +%Y%m%d%H%M%S).sql
```

### 2. 执行回滚

```bash
# 1. 恢复旧版本JAR
sudo cp /opt/eve-helper/backup/eve-helper-0.0.1-SNAPSHOT.jar \
       /opt/eve-helper/eve-helper-0.0.2-SNAPSHOT.jar

# 2. 如果有数据库变更,恢复数据库
mysql -u root -p eve_helper < eve_helper_backup_20260201.sql

# 3. 启动服务
sudo systemctl start eve-helper

# 4. 验证服务
curl http://localhost:9999/actuator/health
```

### 3. 验证回滚

```bash
# 1. 检查服务状态
sudo systemctl status eve-helper

# 2. 查看日志
sudo journalctl -u eve-helper -f

# 3. 测试关键功能
curl http://localhost:9999/api/v1/test

# 4. 监控指标
# 查看Grafana仪表板,确认指标正常
```

## ⚡ 性能优化

### 1. JVM调优

```bash
# 推荐的JVM参数
-Xms2g                              # 初始堆大小
-Xmx4g                              # 最大堆大小
-XX:+UseG1GC                        # 使用G1垃圾收集器
-XX:MaxGCPauseMillis=200            # 最大GC暂停时间
-XX:+ParallelRefProcEnabled         # 并行处理引用
-XX:+UnlockExperimentalVMOptions    # 解锁实验性选项
-XX:+DisableExplicitGC              # 禁用显式GC
-XX:+AlwaysPreTouch                 # 预分配内存
-XX:G1NewSizePercent=30             # 新生代最小比例
-XX:G1MaxNewSizePercent=40          # 新生代最大比例
-XX:G1HeapRegionSize=16M            # G1区域大小
-XX:G1ReservePercent=15             # 保留内存比例
-XX:InitiatingHeapOccupancyPercent=45  # 触发并发GC的堆占用阈值
```

### 2. 数据库优化

```sql
-- 添加索引
CREATE INDEX idx_user_id ON market_orders(user_id);
CREATE INDEX idx_created_at ON market_orders(created_at);

-- 优化查询
EXPLAIN SELECT * FROM market_orders WHERE user_id = 123;

-- 定期优化表
OPTIMIZE TABLE market_orders;
```

### 3. Redis优化

```bash
# redis.conf优化配置
maxmemory 2gb
maxmemory-policy allkeys-lru
save ""  # 禁用RDB持久化(如果不需要)
appendonly yes  # 启用AOF持久化
appendfsync everysec
```

### 4. 应用层优化

- 启用HTTP压缩
- 使用连接池
- 实现缓存策略
- 异步处理耗时操作
- 使用CDN加速静态资源

## 📚 相关文档

- [环境变量配置](./ENVIRONMENT.md)
- [开发指南](./DEVELOPMENT.md)
- [测试指南](./TESTING.md)
- [安全配置](./SECURITY.md)

## 🆘 紧急联系

- **运维团队**: ops@example.com
- **开发团队**: dev@example.com
- **值班电话**: +86-xxx-xxxx-xxxx
- **Slack频道**: #eve-helper-ops

---

**最后更新**: 2026-02-01
**维护者**: EVE Helper Ops Team
