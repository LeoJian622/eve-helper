# 部署运维手册

本文档提供 EVE Helper 项目的部署流程、运维监控和故障处理指南。

## 📋 目录

- [部署流程](#部署流程)
- [环境配置](#环境配置)
- [监控告警](#监控告警)
- [常见问题处理](#常见问题处理)
- [回滚流程](#回滚流程)
- [JWT 签名密钥轮换(007)](#jwt-签名密钥轮换007)
- [性能优化](#性能优化)

## 🚀 部署流程

### 前置要求

#### 服务器要求
- **操作系统**: Linux (Ubuntu 20.04+ / CentOS 7+)
- **CPU**: 2核心以上
- **内存**: 4GB以上
- **磁盘**: 20GB以上可用空间

#### 软件要求
- **JDK**: 17
- **MySQL**: 8.0+
- **Redis**: 5.0+
- **Nginx**: 1.18+ (可选,用于反向代理)

### 1. 准备部署包

#### 方式1: 本地构建

```bash
# 克隆代码
git clone https://github.com/LeoJian622/eve-helper.git
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
sudo apt install openjdk-17-jdk

# CentOS/RHEL
sudo yum install java-17-openjdk-devel

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

# 表结构不随仓库分发,请从现有环境导出或依据 PO/Mapper XML 自行建表
# mysql -u root -p eve_helper < schema.sql
# mysql -u root -p eve < eve_schema.sql
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

# JWT密钥库配置(007:生产密钥必须文件系统外置,详见「JWT 签名密钥轮换」章节)
# location 必须是绝对路径;classpath/裸文件名会被 SecurityBaselineValidator 拒绝启动
KEYSTORE_LOCATION=/etc/eve-helper/eve-jwt.jks
KEYSTORE_ALIAS=eve-jwt
KEYSTORE_PASSWORD=your_keystore_password
KEY_PASSWORD=your_key_password
```

#### ⚠️ 启动安全基线:4 项硬门禁(007 T038 起为正向白名单)

`SecurityBaselineValidator` 在启动期校验 4 项配置。**除 `test` profile 外**(含未设 profile),任一不满足即**拒绝启动**。

T038 起语义由「黑名单」改为**正向白名单** —— **键缺失不再视为合规**:

| 键 | 要求 | 缺失时 |
|----|------|--------|
| `mybatis-plus.configuration.log-impl` | 必须显式为 `org.apache.ibatis.logging.slf4j.Slf4jImpl` | **拒启** |
| `logging.level.root` | 必须显式声明,且非 debug/trace | **拒启** |
| **全部** `logging.level.*` | 任一为 debug/trace 即拒(不限于 `web`) | — |
| `eve.helper.debug.access-token-endpoint.enabled` | 必须显式为字面 `false`(`yes`/`1`/`on` 均拒) | **拒启** |
| `security.keystore.location` | 文件系统绝对路径,禁 `classpath:` 与裸文件名 | **拒启** |

> **升级到 T038 后首次部署前,请确认各 profile 中没有任何 debug/trace 级别的 logger**。
> 例如 `logging.level.reactor.netty: debug` 或 mapper 包 debug 都会导致拒启 ——
> 这是有意的:前者使 ESI 请求头(含 `Authorization: Bearer`)落盘,
> 后者配合 Slf4jImpl 会打 SQL 绑定参数,使 `refresh_token` 明文入日志。
>
> 自检命令(在各 profile 配置文件上执行):
> ```bash
> grep -nE "^\s+(root|[a-z.]+):\s*(debug|trace)\s*$" src/main/resources/application-{ali,aliw,prod}.yml
> # 期望:无输出。有输出则该行必须改为 info 或更高,否则应用拒启
> grep -nE "log-impl|access-token-endpoint" -A1 src/main/resources/application-{ali,aliw,prod}.yml
> # 期望:log-impl 为 Slf4jImpl(或未覆盖,继承 application.yml);enabled 为 false
> ```

拒启时日志会明确指出违规的键与当前值,按提示修正即可。

### 5. 部署应用

#### 复制部署包

```bash
# 复制JAR文件
sudo cp eve-helper-0.0.2-SNAPSHOT.jar /opt/eve-helper/

# 部署密钥库文件(007:统一放 /etc/eve-helper/,不放应用目录;详见「JWT 签名密钥轮换」章节)
sudo mkdir -p /etc/eve-helper
sudo cp eve-jwt.jks /etc/eve-helper/eve-jwt.jks

# 设置权限(SC-009:目录 700 / 文件 600 / 属主为服务账号)
sudo chown -R eve-app:eve-app /opt/eve-helper
sudo chown -R eve-app:eve-app /etc/eve-helper
sudo chmod 700 /etc/eve-helper
sudo chmod 600 /etc/eve-helper/eve-jwt.jks
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
    -Dspring.profiles.active=pro \
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

创建 `application-prod.yml`(对应 `pro` profile):

```yaml
server:
  port: 9999
  compression:
    enabled: true
    mime-types: text/html,text/xml,text/plain,text/css,application/javascript,application/json

spring:
  profiles:
    active: pro
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
        # 仅暴露健康检查与构建信息。**不含 metrics/prometheus** ——
        # 007 T037 已移除三个零引用的 io.prometheus 死依赖,项目无 metrics 导出通路。
        # 如需指标监控须新增 micrometer-registry-prometheus,涉技术栈冻结(宪法第四条)
        include: health,info
```

## 📊 监控告警

> **现状(2026-08-12,007 T037)**:本项目**没有 metrics 导出通路**。原 `pom.xml` 中三个
> `io.prometheus` 依赖(`prometheus-metrics-core` / `-instrumentation-jvm` /
> `-exporter-httpserver`,均 1.0.0)在 `src/main/java` **零引用** —— 从未创建
> `PrometheusRegistry`、从未启动 exporter、从未注册 `JvmMetrics`,且全部 profile
> 均无 `management:` 配置。**它们是死依赖,已于 T037 删除。**
>
> 原本此处有 Prometheus 安装、Grafana 面板、`jvm_memory_used_bytes` 等指标名、
> `alerts.yml` 规则等约 150 行运维指南 —— 那些内容**在本项目从未可用**
> (`/actuator/prometheus` 端点不存在,指标名一个都不会出现,告警规则永不触发),
> 属于误导性文档,已一并删除。
>
> **如需指标监控**:须新增 `micrometer-registry-prometheus` 依赖 —— 技术栈已冻结
> (宪法第四条),**须走宪法修订程序并另开 spec**,不得直接添加。

### 1. 健康检查

`spring-boot-starter-actuator` 保留,提供健康检查与构建信息:

```bash
# 应用健康状态(含数据源、Redis 连通性)
curl http://localhost:9999/actuator/health

# 构建信息
curl http://localhost:9999/actuator/info
```

生产环境的 `management.endpoints.web.exposure.include` 应仅含 `health,info`
(见上方「应用配置」)。**不要**暴露 `env` / `beans` / `configprops` —— 它们会
泄露配置细节(数据源 URL、Bean 结构)。

> ⚠️ `/actuator/health` 默认不需认证时会暴露组件状态。若该端点在白名单中,
> 建议设 `management.endpoint.health.show-details: never`,仅返回 UP/DOWN。

### 2. 日志监控(当前唯一可用的告警通路)

日志是本项目**现存唯一**可运维的告警来源。日志位置见「应用配置」的
`logging.file.path`(示例为 `/opt/eve-helper/logs`)。

#### 安全告警标记

应用会在检测到安全异常时输出**稳定的可 grep 标记**,用于日志告警规则匹配:

| 标记 | 含义 | 来源 |
|------|------|------|
| `[SECURITY_ALERT:REFRESH_FLOOD]` | refresh 端点无效请求洪泛(60s 窗口内超过 1000 次) | `RefreshRateLimiterService`(007 T016/T037) |
| `[SECURITY_ALERT:REFRESH_TARGETED]` | **单个账号** refresh 失败超 10 次/分钟(定向刷该账号,或客户端无退避重试) | `RefreshRateLimiterService`(007 T046) |

**两者响应动作不同,故标记有意分开**:

| 标记 | 排查方向 | 处置 |
|------|----------|------|
| `REFRESH_FLOOD` | 全局无效请求洪泛,与具体账号无关 | 入口层限流(反代/网关);**不要**在业务线程上加延迟(007 T036 已论证其为 DoS 放大器) |
| `REFRESH_TARGETED` | 日志中的 `userId=` 即目标账号 | 查该账号是否凭证外泄 → 需要时清其 refresh token 并通知改密;若为客户端重试循环,修客户端 |

> ⚠️ **标记字符串不得随意修改** —— 改动等于让既有告警规则静默失效。
> 两个常量均有测试断言保护(`RefreshRateLimiterTest`,T037/T046),
> 且有断言强制二者**不得相同**。

#### 告警配置示例

**方式一:grep + cron(最简,无额外组件)**

```bash
# /opt/eve-helper/scripts/check-security-alerts.sh
#!/bin/bash
LOG_DIR=/opt/eve-helper/logs
WINDOW_MIN=5

# 近 5 分钟内出现安全告警标记则通知
if find "$LOG_DIR" -name '*.log' -mmin -${WINDOW_MIN} -exec \
     grep -l 'SECURITY_ALERT' {} + >/dev/null 2>&1; then
    MSG=$(find "$LOG_DIR" -name '*.log' -mmin -${WINDOW_MIN} -exec \
            grep -h 'SECURITY_ALERT' {} + | tail -20)
    # 替换为实际通知渠道(邮件/钉钉/企业微信 webhook)
    echo "$MSG" | mail -s "[EVE Helper] 安全告警" ops@example.com
fi
```

```cron
*/5 * * * * /opt/eve-helper/scripts/check-security-alerts.sh
```

**方式二:Loki + Promtail(如已有日志基础设施)**

```yaml
# Loki 告警规则
groups:
  - name: eve-helper-security
    rules:
      - alert: RefreshTokenFlood
        expr: |
          count_over_time({job="eve-helper"} |= "[SECURITY_ALERT:REFRESH_FLOOD]" [5m]) > 0
        for: 0m
        labels:
          severity: warning
        annotations:
          summary: "refresh 端点检测到无效请求洪泛"
          description: "60s 窗口内无效 refresh 超过阈值,可能是 token 猜测攻击。检查来源 IP 分布并考虑入口层限流。"
```

#### 需要人工关注的日志模式

| grep 模式 | 含义 | 处置 |
|-----------|------|------|
| `SECURITY_ALERT` | 见上表 | 按标记类型处置 |
| `启动基线校验失败` | 安全基线不满足,应用已拒绝启动 | 检查 keystore 路径、日志级别、调试端点配置 |
| `OutOfMemoryError` | 内存溢出 | 见「常见问题处理」 |
| `Connection pool exhausted` | Druid 连接池耗尽 | 见「常见问题处理」 |
| `Redis connection timeout` | Redis 不可达 | 见「常见问题处理」 |
| `ERROR.*EsiException` | ESI 接口异常 | 检查 ESI 服务状态与 token 有效性 |

### 3. 容量基线

以下为 `application.yml` 显式声明的容量参数(007 T036 起显式化,值即 Boot 默认):

| 参数 | 值 | 含义 |
|------|-----|------|
| `server.tomcat.threads.max` | 200 | 工作线程上限 —— **洪泛容忍度的分母** |
| `server.tomcat.threads.min-spare` | 10 | 常驻空闲线程 |
| `server.tomcat.accept-count` | 100 | 线程满后的排队长度,队满即拒新连接 |
| `server.tomcat.max-connections` | 8192 | 最大并发连接 |

> ⚠️ **不要在业务线程内做阻塞式限流**(如 `Thread.sleep` 延迟整形)。
> 那会把工作线程数变成全站可用性上限 —— 007 T036 已因此推翻一版设计,
> 详见 `RefreshRateLimiterService` 类注释。速率限制应在**入口层**
> (反向代理 / 网关 / `max-connections`)实施。

#### 数据库与缓存

- **连接池**: 项目使用 **Druid**(非 HikariCP)。活跃/最大连接经 **Druid StatView 页面**监控
  (`/druid/index.html`,生产须加认证或关闭)
- **慢查询**: Druid `slow-sql-millis` 阈值(5000ms,见 `application.yml`)+ MySQL 慢查询日志
- **Redis**: 用 `redis-cli info` 查看连接数与内存;认证请用 `REDISCLI_AUTH` 环境变量而非 `-a`
  参数(避免口令进入进程列表与 shell 历史)

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
REDISCLI_AUTH=${REDIS_PASSWORD} redis-cli -h ${REDIS_HOST} -p ${REDIS_PORT} ping
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

# 2. 查看应用连接池状态 (Druid;指标名以实际暴露为准)
curl http://localhost:9999/actuator/metrics/druid.connections.active
curl http://localhost:9999/actuator/metrics/druid.connections.max

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
REDISCLI_AUTH=${REDIS_PASSWORD} redis-cli -h ${REDIS_HOST} -p ${REDIS_PORT} ping

# 2. 查看Redis状态
REDISCLI_AUTH=${REDIS_PASSWORD} redis-cli -h ${REDIS_HOST} -p ${REDIS_PORT} info

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

> ⚠️ **auth 键驱逐风险(007 T049,评审 MEDIUM-1)**:上述 `maxmemory-policy allkeys-lru`(**以及 `volatile-lru`**)会驱逐带 TTL 的键。eve-helper 的 auth 键 —— `refresh_token:*`、`refresh_session:*`、`refresh_owner:*`、`session_revoked:*`、`session_access_jti:*`、token 黑名单 —— **全部带 TTL**。驱逐 `refresh_token:*` 是 fail-safe(凭证失效),但驱逐 `refresh_session:*` / `session_revoked:*` / `refresh_owner:*` 索引是 **fail-open**(登出撤销静默失效、tombstone 被绕过),方向不对称。**`volatile-lru` 无效**:所有 auth 键都带 TTL,与 allkeys-lru 对它们行为一致。建议:**auth 键独立 Redis 实例**(或不设 maxmemory / 容量充足),不与业务缓存混用。

### 4. 应用层优化

- 启用HTTP压缩
- 使用连接池
- 实现缓存策略
- 异步处理耗时操作
- 使用CDN加速静态资源

## 🔑 JWT 签名密钥轮换(007)

> **适用事件**:`src/main/resources/eve-jwt.jks`(旧生产 RSA 签名密钥)自提交 `5d139d8「增加token认证」` 起长期处于版本控制中,须视为已泄露。
> 本章节既是该事件的执行清单,也是今后例行轮换的模板。**全文严禁出现真实口令** —— 所有命令中的口令均为占位符。

### 事件声明(SC-007 / FR-013)

- **旧签名密钥已泄露**:旧私钥(`eve-jwt.jks`,别名 `eve-jwt`)长期存在于 git 历史,且当时生产 profile 配置伴随明文弱 keystore 口令,**必须视为已被攻击者获取**。
- **旧密钥不得再用于任何环境**(包括测试环境);其签发的任何 token 均不可信。
- 不改写 git 历史(FR-014:禁止强推共享分支)——轮换后旧私钥不再验证任何 token,历史中的副本失去价值。
- 轮换日期:`____`(执行人填写,必填)

### 轮换前公告(Q5,必须提前发布)

本次轮换会清空全部 refresh token,**所有用户将被登出**,必须提前公告。模板:

```text
【维护公告】EVE Helper 计划于 <日期> <时间段> 进行安全升级(签名密钥轮换)。
期间服务将重启,可能出现短暂不可用(约 <时长>);
升级完成后所有登录状态将被重置,请在升级后重新登录。
给您带来不便,敬请谅解。
```

公告发布记录:时间 `____` / 渠道 `____`

### 轮换步骤

> 建议低峰期执行(FR-011)。执行人为人类用户(口令由用户单独生成与保管,FR-002:不经 AI、不入文档、不入库)。

#### 步骤 1:新密钥对(生成 / 核验)

```bash
# 生成新 RSA 密钥对(若新密钥已生成,跳过本步,直接做步骤 3 指纹核对)
keytool -genkeypair \
  -alias eve-jwt \
  -keyalg RSA -keysize 2048 \
  -validity 3650 \
  -keystore eve-helper.jks \
  -storepass '<STORE_PASS>' \
  -keypass '<KEY_PASS>' \
  -dname "CN=eve-helper, OU=Ops, O=EveHelper, C=CN"
```

- 密钥长度必须 ≥ 2048 位:`KeyStoreKeyFactory` 启动时硬校验,弱密钥直接拒绝启动。
- `<STORE_PASS>` / `<KEY_PASS>` 由执行人自行生成,仅记录在秘密管理渠道(不得出现在本文档、仓库或 AI 对话中)。
- keystore 文件**严禁进入 git**:直接在目标服务器生成,或离线传输。

#### 步骤 2:部署到服务器固定路径(SC-009)

```bash
sudo mkdir -p /etc/eve-helper
sudo cp <新keystore路径> /etc/eve-helper/eve-jwt.jks
sudo chown -R eve-app:eve-app /etc/eve-helper
sudo chmod 700 /etc/eve-helper
sudo chmod 600 /etc/eve-helper/eve-jwt.jks

# 留证(SC-009):以下命令输出粘贴到「轮换记录」
stat /etc/eve-helper /etc/eve-helper/eve-jwt.jks
```

#### 步骤 3:指纹核对(L-5)

```bash
keytool -list -v -keystore /etc/eve-helper/eve-jwt.jks -storepass '<STORE_PASS>'
```

- [ ] 新生产密钥指纹 **≠** `test-only.jks` 指纹 `FD:9F:19:27:61:...:CA:0F:B4` —— 防止把入库的测试 keystore 误部署为生产密钥
- [ ] 新密钥指纹(SHA-256)已记录:`____`

#### 步骤 4:配置切换

1. 部署环境变量文件(参考 `.env.example`)添加:
   ```bash
   KEYSTORE_LOCATION=/etc/eve-helper/eve-jwt.jks
   KEYSTORE_ALIAS=eve-jwt
   KEYSTORE_PASSWORD=<经秘密管理渠道配置>
   KEY_PASSWORD=<经秘密管理渠道配置>
   ```
2. 生产 profile(如 `application-aliw.yml`)的 `security.keystore.location` 改为 `/etc/eve-helper/eve-jwt.jks`:
   - ⚠️ **必须是文件系统绝对路径**:`classpath:` 前缀或裸文件名在生产 profile 下会被 `SecurityBaselineValidator` 拒绝启动(fail-closed,SC-005),防止静默回退到旧 keystore。
   - ⚠️ profile 若自定义 `eve.helper.whiteUrlList`,必须包含 `POST:/auth/tokens`(List 属性整体覆盖 application.yml,漏掉则 refresh 端点加白静默失效,SC-016)。

#### 步骤 5:重启(FR-022,禁止滚动重启)

- **单实例**:`sudo systemctl restart eve-helper`
- **多实例**:必须**停机窗口**或**全部实例同时重启**,**禁止滚动重启** —— 滚动期间新旧密钥并存会造成随机认证失败;且 `TokenService` 在生成新 token 前先删除旧 refresh token,客户端重试循环会消耗掉 refresh token。
- 重启后验证:
  - `curl http://localhost:9999/actuator/health` 正常
  - 启动日志无 keystore 加载失败(口令/别名/路径错误会直接拒绝启动,不会带病运行)
  - 构建产物检查(SC-003):`mvn clean package` 后 `unzip -l target/*.jar | grep '\.jks'` 仅含 `test-only.jks`(入库测试密钥,设计内),不含生产 keystore

#### 步骤 6:轮换验证(SC-014 / SC-006)

**SC-014 是唯一能证明「密钥对确实换掉」的验证**(仅验证「已存在的旧 token 失效」在别名/口令变更但密钥未变时会误判通过):

1. 用旧私钥**现场签发一个全新的(未过期)token**,调用任一受保护 API → 断言返回 **401**。
2. 断言新公钥 modulus 与旧公钥 modulus **不同**(`keytool -list -v` 输出比对)。
3. **SC-006**:用旧 refresh token `POST /auth/tokens` → 返回 HTTP 400 与明确业务错误(「Refresh Token无效或已过期」),而非异常逃逸或 403。

#### 步骤 7:清空 Redis `refresh_token:*`(SC-008)

```bash
# ① 必填:记录清空前数量
redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" \
  --scan --pattern 'refresh_token:*' | wc -l
# 清空前数量: ____

# ② 清空(分批 DEL)
redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" \
  --scan --pattern 'refresh_token:*' \
  | xargs -r -n 200 redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" DEL

# ③ 必填:验证清空后数量为 0
redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" \
  --scan --pattern 'refresh_token:*' | wc -l
# 清空后数量: ____ (必须为 0)
```

- 键模式来自 `TokenService`(`refresh_token:{jti}` → userId);清空后全体用户 refresh 失败、被迫重新登录(Q1 决策选项 B)。
- ESI accessToken 缓存(约 19 分钟 TTL)自然过期即可,无需手动清理。

#### 步骤 8:六表审计(FR-021 / SC-013)

**为什么是六张表**:RBAC 的 url→role 映射来自 `sys_permission` / `sys_role_permission` / `sys_role` 三表 JOIN;伪造 ROOT token 提权**不必碰 `sys_permission`** —— 只在 `sys_role_permission` 插一行即可。

**能力边界(执行前必读)**:表审计只能发现**新增/篡改**痕迹;对**读取型外泄**(`sys_user` 的 BCrypt 口令哈希、`eve_account.refresh_token` 明文存储的 ESI 长期凭证)**完全失明,亦无补偿控制**。若怀疑 ESI 凭证已被读走,唯一处置是让受影响角色重新走 ESI 授权。此外,本节时间判断依赖 `gmt_create`/`gmt_modified`,而**有数据库写权限的攻击者可事后伪造这两列**——对精心操作的写入型入侵,「未发现新增/篡改痕迹」亦是可能假阴性;须以第 7 项的 binlog 保留期核查作为旁证(007 T041)。

时间锚点:提交 `5d139d8`(「增加token认证」)是最早可能入侵时间,重点看其后创建/变更的行。

**1. `sys_user`** —— 全表比对预期账号清单
```sql
SELECT id, username, nickname, `status`, deleted, gmt_create, gmt_modified
FROM eve_helper.sys_user ORDER BY id;
```
判断方法:是否存在预期清单之外的账号(重点 `5d139d8` 之后创建);`status` / `deleted` 有无异常值。
结论(执行人填):____

**2. `sys_user_role`** —— 是否有非预期 ROOT/管理员绑定
```sql
SELECT ur.id, ur.user_id, u.username, ur.role_id, r.`name` AS role_name
FROM eve_helper.sys_user_role ur
  LEFT JOIN eve_helper.sys_user u ON ur.user_id = u.id
  LEFT JOIN eve_helper.sys_role r ON ur.role_id = r.id
ORDER BY ur.id;
```
判断方法:预期清单外的用户是否绑定了特权角色。
结论(执行人填):____

**3. `sys_permission`** —— 权限定义有无新增/篡改
```sql
SELECT id, `name`, url_perm, btn_perm, gmt_create, gmt_modified
FROM eve_helper.sys_permission ORDER BY id;
```
判断方法:有无新增/变更的 `url_perm`(格式 `METHOD:PATH`)。
结论(执行人填):____

**4. `sys_role_permission`** —— 提权的**最易被漏掉路径**
```sql
SELECT rp.id, rp.role_id, r.`name` AS role_name, rp.permission_id, p.url_perm
FROM eve_helper.sys_role_permission rp
  LEFT JOIN eve_helper.sys_role r ON rp.role_id = r.id
  LEFT JOIN eve_helper.sys_permission p ON rp.permission_id = p.id
ORDER BY rp.id;
```
判断方法:普通角色是否新增了不该有的权限关联。
结论(执行人填):____

**5. `sys_role`** —— 有无新增角色或篡改
```sql
SELECT id, `name`, code, `status`, gmt_create, gmt_modified
FROM eve_helper.sys_role ORDER BY id;
```
判断方法:有无未知角色,`code` / `status` 有无异常变更。
结论(执行人填):____

**6. `eve_account`** —— 有无非预期角色绑定
```sql
SELECT character_id, character_name, user_id, `type`, gmt_create, gmt_modified
FROM eve_helper.eve_account ORDER BY character_id;
```
判断方法:有无预期清单外的游戏角色 / 用户绑定。⚠️ `refresh_token` 列刻意不在查询列中 —— 其是否被读走审计不可见(见能力边界)。
结论(执行人填):____

**7. binlog / 慢日志保留期核查(时间列可伪造的补偿证据,007 T041)** —— 上述六表的时间列可被写权限攻击者伪造,binlog 是独立于表数据的可信写入记录;慢日志可佐证入侵窗口内的异常重查询。
```sql
SHOW VARIABLES LIKE 'log_bin';
SHOW VARIABLES LIKE 'binlog_expire_logs_seconds';
SHOW BINARY LOGS;
SHOW VARIABLES LIKE 'slow_query_log';
```
判断方法:binlog 是否开启、保留期(`binlog_expire_logs_seconds` / 现存最早 binlog 时间)是否**覆盖时间锚点 `5d139d8`**。若覆盖:用 `mysqlbinlog` 过滤该窗口内对六表的写语句,与上面 1~6 的审计结论交叉核对,不一致处以 binlog 为准;若未覆盖(binlog 已轮转清理):本步结论只能表述为「仅凭表内时间列,不能排除伪造」,并如实记入归档。
结论(执行人填):____

**升级路径**:任何一张表发现非预期账号/角色/权限映射 → **立即暂停轮换收尾,升级为入侵响应**:强制全体改密 + 吊销全部会话 + ESI 重新授权。

#### 步骤 9:记录归档

审计总结论**只能表述为**(SC-013 强制措辞):

> 已审计 6 表并完成 binlog 保留期核查(结论见步骤 8 第 7 项),未发现**新增/篡改**痕迹;时间列可被伪造、读取型外泄(口令哈希、ESI 凭证)无法检测。

**严禁**表述为「确认未被入侵」—— 审计对读取型外泄失明、时间列可被伪造(binlog 未覆盖入侵窗口时尤甚),该表述会构成虚假的安全结论。

归档位置:下方「轮换记录」+ `docs/reviews/`。

### 轮换记录(每次执行填写)

| 项 | 记录 |
|----|------|
| 轮换日期 / 执行人 | ____ |
| 公告发布时间 / 渠道 | ____ |
| 新密钥 SHA-256 指纹 | ____ |
| 指纹 ≠ test-only.jks(`FD:9F:19:27:61:...:CA:0F:B4`)核对 | ☐ |
| SC-009 stat 留证(目录 700 / 文件 600 / 属主服务账号) | ____ |
| SC-014 旧密钥新签 token 被拒验证 | ☐ |
| SC-006 旧 refresh token 明确业务错误验证 | ☐ |
| `refresh_token:*` 清空前数量 | ____ |
| `refresh_token:*` 清空后数量(= 0) | ____ |
| 审计总结论(强制措辞,含步骤 8 第 7 项 binlog 核查结论) | ____ |

## 📚 相关文档

- [环境变量配置](./ENVIRONMENT.md)
- [开发指南](./DEVELOPMENT.md)

## 🆘 紧急联系

- **运维团队**: ops@example.com
- **开发团队**: dev@example.com
- **值班电话**: +86-xxx-xxxx-xxxx
- **Slack频道**: #eve-helper-ops

---

**最后更新**: 2026-08-12(新增 007 JWT 签名密钥轮换章节)
**维护者**: EVE Helper Ops Team
