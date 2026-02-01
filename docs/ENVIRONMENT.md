# 环境变量配置文档

本文档描述了 EVE Helper 项目所需的所有环境变量配置。

## 📋 目录

- [快速开始](#快速开始)
- [环境变量列表](#环境变量列表)
- [配置示例](#配置示例)
- [安全最佳实践](#安全最佳实践)

## 🚀 快速开始

### 1. 复制环境变量模板

```bash
cp .env.example .env
```

### 2. 编辑 .env 文件

使用文本编辑器打开 `.env` 文件,填入实际的配置值。

### 3. 验证配置

```bash
# 检查环境变量是否正确加载
mvn spring-boot:run -Dspring.profiles.active=dev
```

## 📝 环境变量列表

### 数据库配置

#### DB_HOST
- **描述**: 数据库服务器主机地址
- **类型**: String
- **默认值**: `localhost`
- **示例**: `192.168.1.100`, `db.example.com`
- **必需**: 否 (开发环境可使用默认值)

#### DB_PORT
- **描述**: 数据库服务器端口
- **类型**: Integer
- **默认值**: `3306`
- **示例**: `3306`, `3307`
- **必需**: 否 (开发环境可使用默认值)

#### DB_SYSTEM_USERNAME
- **描述**: 系统数据库用户名
- **类型**: String
- **默认值**: `test_user` (仅测试环境)
- **示例**: `eve_admin`, `app_user`
- **必需**: 是 (生产环境)

#### DB_SYSTEM_PASSWORD
- **描述**: 系统数据库密码
- **类型**: String
- **默认值**: 无
- **示例**: `your_secure_password_here`
- **必需**: 是
- **安全提示**:
  - 使用强密码 (至少16字符,包含大小写字母、数字和特殊字符)
  - 不要在代码或配置文件中硬编码
  - 定期更换密码

#### DB_EVE_USERNAME
- **描述**: EVE数据库用户名
- **类型**: String
- **默认值**: `test_user` (仅测试环境)
- **示例**: `eve_readonly`, `eve_app`
- **必需**: 是 (生产环境)

#### DB_EVE_PASSWORD
- **描述**: EVE数据库密码
- **类型**: String
- **默认值**: 无
- **示例**: `your_secure_password_here`
- **必需**: 是
- **安全提示**: 同 DB_SYSTEM_PASSWORD

### Redis配置

#### REDIS_HOST
- **描述**: Redis服务器主机地址
- **类型**: String
- **默认值**: `localhost`
- **示例**: `redis.example.com`, `10.0.0.50`
- **必需**: 否 (开发环境可使用默认值)

#### REDIS_PORT
- **描述**: Redis服务器端口
- **类型**: Integer
- **默认值**: `6379`
- **示例**: `6379`, `6380`
- **必需**: 否 (开发环境可使用默认值)

#### REDIS_PASSWORD
- **描述**: Redis服务器密码
- **类型**: String
- **默认值**: 无
- **示例**: `your_redis_password`
- **必需**: 是
- **安全提示**:
  - Redis密码应该足够复杂
  - 生产环境必须启用密码认证
  - 建议使用至少32字符的随机密码

### JWT密钥库配置

#### KEYSTORE_PASSWORD
- **描述**: Java KeyStore密钥库密码
- **类型**: String
- **默认值**: 无
- **示例**: `your_keystore_password`
- **必需**: 是
- **用途**: 用于保护JWT签名密钥
- **安全提示**:
  - 使用强密码保护密钥库
  - 密钥库文件应该妥善保管
  - 不要将密钥库文件提交到版本控制

#### KEY_PASSWORD
- **描述**: KeyStore中密钥的密码
- **类型**: String
- **默认值**: 无
- **示例**: `your_key_password`
- **必需**: 是
- **用途**: 用于保护JWT签名私钥
- **安全提示**: 同 KEYSTORE_PASSWORD

## 📚 配置示例

### 开发环境配置

```bash
# .env (开发环境)

# 数据库配置 - 使用本地数据库
DB_HOST=localhost
DB_PORT=3306
DB_SYSTEM_USERNAME=dev_user
DB_SYSTEM_PASSWORD=dev_password_123
DB_EVE_USERNAME=dev_user
DB_EVE_PASSWORD=dev_password_456

# Redis配置 - 使用本地Redis
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=dev_redis_pass

# JWT密钥库配置
KEYSTORE_PASSWORD=dev_keystore_pass
KEY_PASSWORD=dev_key_pass
```

### 测试环境配置

```bash
# .env (测试环境)

# 数据库配置 - 使用测试数据库
DB_HOST=test-db.internal
DB_PORT=3306
DB_SYSTEM_USERNAME=test_user
DB_SYSTEM_PASSWORD=${TEST_DB_PASSWORD}
DB_EVE_USERNAME=test_user
DB_EVE_PASSWORD=${TEST_DB_PASSWORD}

# Redis配置 - 使用测试Redis
REDIS_HOST=test-redis.internal
REDIS_PORT=6379
REDIS_PASSWORD=${TEST_REDIS_PASSWORD}

# JWT密钥库配置
KEYSTORE_PASSWORD=${TEST_KEYSTORE_PASSWORD}
KEY_PASSWORD=${TEST_KEY_PASSWORD}
```

### 生产环境配置

```bash
# .env (生产环境)
# 注意: 生产环境应该使用密钥管理服务 (如 AWS Secrets Manager, HashiCorp Vault)

# 数据库配置 - 使用生产数据库
DB_HOST=prod-db.example.com
DB_PORT=3306
DB_SYSTEM_USERNAME=prod_app_user
DB_SYSTEM_PASSWORD=${PROD_DB_SYSTEM_PASSWORD}
DB_EVE_USERNAME=prod_eve_user
DB_EVE_PASSWORD=${PROD_DB_EVE_PASSWORD}

# Redis配置 - 使用生产Redis集群
REDIS_HOST=prod-redis.example.com
REDIS_PORT=6379
REDIS_PASSWORD=${PROD_REDIS_PASSWORD}

# JWT密钥库配置
KEYSTORE_PASSWORD=${PROD_KEYSTORE_PASSWORD}
KEY_PASSWORD=${PROD_KEY_PASSWORD}
```

## 🔒 安全最佳实践

### 1. 密码管理

- ✅ **使用强密码**: 至少16字符,包含大小写字母、数字和特殊字符
- ✅ **定期轮换**: 每90天更换一次密码
- ✅ **不要重复使用**: 每个服务使用不同的密码
- ✅ **使用密码管理器**: 如 1Password, LastPass, Bitwarden

### 2. 环境变量保护

- ✅ **不要提交到Git**: 确保 `.env` 在 `.gitignore` 中
- ✅ **限制访问权限**: 只有必要的人员才能访问生产环境变量
- ✅ **使用密钥管理服务**: 生产环境使用 AWS Secrets Manager, Azure Key Vault 等
- ✅ **加密存储**: 如果必须存储在文件中,使用加密工具

### 3. 数据库安全

- ✅ **最小权限原则**: 应用账户只授予必要的权限
- ✅ **使用SSL/TLS**: 数据库连接使用加密传输
- ✅ **网络隔离**: 数据库服务器不应该暴露在公网
- ✅ **定期备份**: 配置自动备份策略

### 4. Redis安全

- ✅ **启用密码认证**: 生产环境必须设置密码
- ✅ **禁用危险命令**: 如 FLUSHALL, FLUSHDB, CONFIG
- ✅ **使用SSL/TLS**: Redis 6.0+ 支持SSL连接
- ✅ **网络隔离**: Redis不应该暴露在公网

### 5. JWT密钥安全

- ✅ **妥善保管密钥库**: 密钥库文件应该加密存储
- ✅ **定期轮换密钥**: 建议每年更换一次签名密钥
- ✅ **使用硬件安全模块**: 生产环境考虑使用HSM
- ✅ **备份密钥**: 安全地备份密钥库文件

## 🔧 故障排查

### 问题: 应用启动失败,提示数据库连接错误

**可能原因**:
1. 数据库服务未启动
2. 数据库地址或端口配置错误
3. 用户名或密码错误
4. 网络连接问题

**解决方法**:
```bash
# 1. 检查数据库服务状态
systemctl status mysql  # Linux
# 或
net start MySQL  # Windows

# 2. 测试数据库连接
mysql -h ${DB_HOST} -P ${DB_PORT} -u ${DB_SYSTEM_USERNAME} -p

# 3. 检查环境变量是否正确加载
echo $DB_HOST
echo $DB_PORT
```

### 问题: Redis连接失败

**可能原因**:
1. Redis服务未启动
2. Redis地址或端口配置错误
3. Redis密码错误
4. 防火墙阻止连接

**解决方法**:
```bash
# 1. 检查Redis服务状态
systemctl status redis  # Linux

# 2. 测试Redis连接
redis-cli -h ${REDIS_HOST} -p ${REDIS_PORT} -a ${REDIS_PASSWORD} ping

# 3. 检查防火墙规则
sudo ufw status  # Linux
```

### 问题: JWT签名验证失败

**可能原因**:
1. 密钥库密码错误
2. 密钥库文件损坏或不存在
3. 密钥别名不匹配

**解决方法**:
```bash
# 1. 验证密钥库文件
keytool -list -keystore eve-jwt.jks -storepass ${KEYSTORE_PASSWORD}

# 2. 检查密钥别名
keytool -list -keystore eve-jwt.jks -storepass ${KEYSTORE_PASSWORD} | grep "eve-jwt"

# 3. 如果密钥库损坏,重新生成
keytool -genkeypair -alias eve-jwt -keyalg RSA -keysize 2048 \
  -keystore eve-jwt.jks -storepass ${KEYSTORE_PASSWORD} \
  -keypass ${KEY_PASSWORD} -validity 3650
```

## 📖 相关文档

- [开发指南](./DEVELOPMENT.md)
- [部署手册](./DEPLOYMENT.md)
- [测试指南](./TESTING.md)
- [安全配置](./SECURITY.md)

## 🆘 获取帮助

如果遇到配置问题,请:

1. 查看应用日志: `logs/spring.log`
2. 检查环境变量: `printenv | grep -E "DB_|REDIS_|KEYSTORE_|KEY_"`
3. 提交Issue: [GitHub Issues](https://github.com/your-org/eve-helper/issues)
4. 联系运维团队

---

**最后更新**: 2026-02-01
**维护者**: EVE Helper Team
