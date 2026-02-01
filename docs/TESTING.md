# 测试指南

本文档提供 EVE Helper 项目的测试策略、测试编写规范和测试执行指南。

## 📋 目录

- [测试策略](#测试策略)
- [测试环境配置](#测试环境配置)
- [单元测试](#单元测试)
- [集成测试](#集成测试)
- [测试覆盖率](#测试覆盖率)
- [测试最佳实践](#测试最佳实践)

## 🎯 测试策略

EVE Helper 采用多层次测试策略:

```
测试金字塔
    /\
   /E2E\          端到端测试 (10%)
  /------\
 /集成测试\        集成测试 (30%)
/----------\
/  单元测试  \      单元测试 (60%)
/------------\
```

### 测试类型

#### 1. 单元测试 (Unit Tests)
- **目标**: 测试单个类或方法的功能
- **工具**: JUnit 5, Mockito
- **覆盖率目标**: 80%+
- **执行频率**: 每次提交

#### 2. 集成测试 (Integration Tests)
- **目标**: 测试多个组件的协作
- **工具**: Spring Boot Test, TestContainers
- **覆盖率目标**: 60%+
- **执行频率**: 每次合并到主分支

#### 3. 端到端测试 (E2E Tests)
- **目标**: 测试完整的业务流程
- **工具**: REST Assured, Selenium
- **覆盖率目标**: 关键业务流程
- **执行频率**: 发布前

## 🔧 测试环境配置

### 1. 配置测试环境变量

创建 `.env.test` 文件:

```bash
# 测试数据库配置
DB_HOST=localhost
DB_PORT=3306
DB_SYSTEM_USERNAME=test_user
DB_SYSTEM_PASSWORD=test_password
DB_EVE_USERNAME=test_user
DB_EVE_PASSWORD=test_password

# 测试Redis配置
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=test_redis_password

# 测试JWT配置
KEYSTORE_PASSWORD=test_keystore_password
KEY_PASSWORD=test_key_password
```

### 2. 配置测试数据库

```bash
# 创建测试数据库
mysql -u root -p << EOF
CREATE DATABASE IF NOT EXISTS eve_helper_test CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS eve_test CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE USER 'test_user'@'localhost' IDENTIFIED BY 'test_password';
GRANT ALL PRIVILEGES ON eve_helper_test.* TO 'test_user'@'localhost';
GRANT ALL PRIVILEGES ON eve_test.* TO 'test_user'@'localhost';
FLUSH PRIVILEGES;
EOF
```

### 3. 测试配置文件

`src/test/resources/application-test.yml`:

```yaml
spring:
  profiles:
    active: test
  datasource:
    druid:
      system:
        username: ${DB_SYSTEM_USERNAME:test_user}
        password: ${DB_SYSTEM_PASSWORD}
        url: jdbc:mysql://${DB_HOST:localhost}:${DB_PORT:3306}/eve_helper_test
        initial-size: 1
        min-idle: 1
        max-active: 5
      eve:
        username: ${DB_EVE_USERNAME:test_user}
        password: ${DB_EVE_PASSWORD}
        url: jdbc:mysql://${DB_HOST:localhost}:${DB_PORT:3306}/eve_test
        initial-size: 1
        min-idle: 1
        max-active: 5
  redis:
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6379}
    password: ${REDIS_PASSWORD}

logging:
  level:
    root: INFO
    xyz.foolcat.eve.evehelper: DEBUG
```

## 🧪 单元测试

### 测试结构

```
src/test/java/xyz/foolcat/eve/evehelper/
├── application/
│   ├── assembler/
│   │   └── UserConverterTest.java
│   └── service/
│       └── UserApplicationServiceTest.java
├── domain/
│   └── service/
│       ├── system/
│       │   └── SysUserServiceTest.java
│       └── eve/
│           └── InvTypesServiceTest.java
├── infrastructure/
│   ├── external/
│   │   └── esi/
│   │       └── api/
│   │           └── CharacterApiTest.java
│   └── persistence/
│       └── UserRepositoryTest.java
└── interfaces/
    └── web/
        └── controller/
            └── CharacterControllerTest.java
```

### 单元测试示例

#### 1. Service层测试

```java
package xyz.foolcat.eve.evehelper.domain.service.system;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("用户服务测试")
class SysUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SysUserService sysUserService;

    @Test
    @DisplayName("根据用户名加载用户 - 成功")
    void loadUserByUsername_Success() {
        // Given
        String username = "testuser";
        User mockUser = User.builder()
                .username(username)
                .password("encoded_password")
                .enabled(true)
                .build();

        when(userRepository.findByUsername(username))
                .thenReturn(Optional.of(mockUser));

        // When
        UserDetails result = sysUserService.loadUserByUsername(username);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo(username);
        assertThat(result.isEnabled()).isTrue();

        verify(userRepository, times(1)).findByUsername(username);
    }

    @Test
    @DisplayName("根据用户名加载用户 - 用户不存在")
    void loadUserByUsername_UserNotFound() {
        // Given
        String username = "nonexistent";
        when(userRepository.findByUsername(username))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> sysUserService.loadUserByUsername(username))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining(username);

        verify(userRepository, times(1)).findByUsername(username);
    }

    @Test
    @DisplayName("创建用户 - 成功")
    void createUser_Success() {
        // Given
        CreateUserRequest request = CreateUserRequest.builder()
                .username("newuser")
                .password("password123")
                .email("newuser@example.com")
                .build();

        User savedUser = User.builder()
                .id(1L)
                .username(request.getUsername())
                .email(request.getEmail())
                .build();

        when(userRepository.existsByUsername(request.getUsername()))
                .thenReturn(false);
        when(userRepository.save(any(User.class)))
                .thenReturn(savedUser);

        // When
        User result = sysUserService.createUser(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getUsername()).isEqualTo(request.getUsername());

        verify(userRepository).existsByUsername(request.getUsername());
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("创建用户 - 用户名已存在")
    void createUser_UsernameExists() {
        // Given
        CreateUserRequest request = CreateUserRequest.builder()
                .username("existinguser")
                .password("password123")
                .build();

        when(userRepository.existsByUsername(request.getUsername()))
                .thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> sysUserService.createUser(request))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("existinguser");

        verify(userRepository).existsByUsername(request.getUsername());
        verify(userRepository, never()).save(any(User.class));
    }
}
```

#### 2. Controller层测试

```java
package xyz.foolcat.eve.evehelper.interfaces.web.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CharacterController.class)
@ActiveProfiles("test")
@DisplayName("角色控制器测试")
class CharacterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CharacterApplicationService characterService;

    @Test
    @DisplayName("获取角色信息 - 成功")
    @WithMockUser(username = "testuser")
    void getCharacter_Success() throws Exception {
        // Given
        Long characterId = 123456L;
        CharacterDTO mockCharacter = CharacterDTO.builder()
                .characterId(characterId)
                .name("Test Character")
                .corporationId(98765L)
                .build();

        when(characterService.getCharacter(characterId))
                .thenReturn(mockCharacter);

        // When & Then
        mockMvc.perform(get("/api/v1/characters/{id}", characterId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.characterId").value(characterId))
                .andExpect(jsonPath("$.name").value("Test Character"))
                .andExpect(jsonPath("$.corporationId").value(98765L));

        verify(characterService, times(1)).getCharacter(characterId);
    }

    @Test
    @DisplayName("获取角色信息 - 角色不存在")
    @WithMockUser(username = "testuser")
    void getCharacter_NotFound() throws Exception {
        // Given
        Long characterId = 999999L;
        when(characterService.getCharacter(characterId))
                .thenThrow(new CharacterNotFoundException(characterId));

        // When & Then
        mockMvc.perform(get("/api/v1/characters/{id}", characterId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Character not found"));

        verify(characterService, times(1)).getCharacter(characterId);
    }

    @Test
    @DisplayName("获取角色信息 - 未授权")
    void getCharacter_Unauthorized() throws Exception {
        // Given
        Long characterId = 123456L;

        // When & Then
        mockMvc.perform(get("/api/v1/characters/{id}", characterId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());

        verify(characterService, never()).getCharacter(anyLong());
    }
}
```

#### 3. Repository层测试

```java
package xyz.foolcat.eve.evehelper.infrastructure.persistence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("用户仓储测试")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("根据用户名查找用户 - 成功")
    void findByUsername_Success() {
        // Given
        User user = User.builder()
                .username("testuser")
                .password("password")
                .email("test@example.com")
                .build();
        userRepository.save(user);

        // When
        Optional<User> result = userRepository.findByUsername("testuser");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("testuser");
        assertThat(result.get().getEmail()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("根据用户名查找用户 - 用户不存在")
    void findByUsername_NotFound() {
        // When
        Optional<User> result = userRepository.findByUsername("nonexistent");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("检查用户名是否存在 - 存在")
    void existsByUsername_True() {
        // Given
        User user = User.builder()
                .username("existinguser")
                .password("password")
                .build();
        userRepository.save(user);

        // When
        boolean exists = userRepository.existsByUsername("existinguser");

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("检查用户名是否存在 - 不存在")
    void existsByUsername_False() {
        // When
        boolean exists = userRepository.existsByUsername("nonexistent");

        // Then
        assertThat(exists).isFalse();
    }
}
```

### 运行单元测试

```bash
# 运行所有单元测试
mvn test

# 运行特定测试类
mvn test -Dtest=SysUserServiceTest

# 运行特定测试方法
mvn test -Dtest=SysUserServiceTest#loadUserByUsername_Success

# 跳过测试
mvn package -DskipTests
```

## 🔗 集成测试

### 集成测试示例

```java
package xyz.foolcat.eve.evehelper.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DisplayName("角色API集成测试")
class CharacterApiIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("完整的角色查询流程")
    void completeCharacterQueryFlow() {
        // 1. 用户登录
        LoginRequest loginRequest = new LoginRequest("testuser", "password");
        ResponseEntity<LoginResponse> loginResponse = restTemplate
                .postForEntity("/api/v1/auth/login", loginRequest, LoginResponse.class);

        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        String token = loginResponse.getBody().getToken();

        // 2. 查询角色列表
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<CharacterDTO[]> charactersResponse = restTemplate
                .exchange("/api/v1/characters", HttpMethod.GET, request, CharacterDTO[].class);

        assertThat(charactersResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(charactersResponse.getBody()).isNotEmpty();

        // 3. 查询角色详情
        Long characterId = charactersResponse.getBody()[0].getCharacterId();
        ResponseEntity<CharacterDTO> characterResponse = restTemplate
                .exchange("/api/v1/characters/" + characterId, HttpMethod.GET, request, CharacterDTO.class);

        assertThat(characterResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(characterResponse.getBody().getCharacterId()).isEqualTo(characterId);
    }
}
```

### 使用TestContainers

```java
package xyz.foolcat.eve.evehelper.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@DisplayName("使用TestContainers的集成测试")
class TestContainersIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("eve_helper_test")
            .withUsername("test")
            .withPassword("test");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:6.0")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.redis.host", redis::getHost);
        registry.add("spring.redis.port", redis::getFirstMappedPort);
    }

    @Test
    @DisplayName("测试数据库和Redis集成")
    void testDatabaseAndRedisIntegration() {
        // 测试逻辑
    }
}
```

## 📊 测试覆盖率

### 生成覆盖率报告

```bash
# 使用JaCoCo生成覆盖率报告
mvn clean test jacoco:report

# 查看报告
open target/site/jacoco/index.html
```

### 配置JaCoCo

在 `pom.xml` 中添加:

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.8</version>
    <executions>
        <execution>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
        <execution>
            <id>jacoco-check</id>
            <goals>
                <goal>check</goal>
            </goals>
            <configuration>
                <rules>
                    <rule>
                        <element>PACKAGE</element>
                        <limits>
                            <limit>
                                <counter>LINE</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>0.80</minimum>
                            </limit>
                        </limits>
                    </rule>
                </rules>
            </configuration>
        </execution>
    </executions>
</plugin>
```

### 覆盖率目标

| 层级 | 目标覆盖率 |
|------|-----------|
| Domain层 | 90%+ |
| Application层 | 85%+ |
| Infrastructure层 | 75%+ |
| Interfaces层 | 80%+ |
| **整体** | **80%+** |

## ✅ 测试最佳实践

### 1. 测试命名规范

```java
// 方法名格式: methodName_scenario_expectedBehavior
@Test
void loadUserByUsername_UserExists_ReturnsUserDetails() { }

@Test
void loadUserByUsername_UserNotFound_ThrowsException() { }

@Test
void createUser_ValidInput_ReturnsCreatedUser() { }
```

### 2. 使用Given-When-Then模式

```java
@Test
void testExample() {
    // Given - 准备测试数据和环境
    User user = new User("testuser", "password");
    when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

    // When - 执行被测试的方法
    UserDetails result = userService.loadUserByUsername("testuser");

    // Then - 验证结果
    assertThat(result).isNotNull();
    assertThat(result.getUsername()).isEqualTo("testuser");
}
```

### 3. 使用AssertJ进行断言

```java
// 推荐使用AssertJ
assertThat(result).isNotNull();
assertThat(result.getUsername()).isEqualTo("testuser");
assertThat(result.getAuthorities()).hasSize(2);

// 而不是JUnit的断言
// assertEquals("testuser", result.getUsername());
```

### 4. 测试异常情况

```java
@Test
void testException() {
    // 使用assertThatThrownBy
    assertThatThrownBy(() -> service.methodThatThrows())
            .isInstanceOf(CustomException.class)
            .hasMessageContaining("expected message");
}
```

### 5. 使用@DisplayName提高可读性

```java
@DisplayName("用户服务测试")
class UserServiceTest {

    @Test
    @DisplayName("根据用户名加载用户 - 成功场景")
    void loadUserByUsername_Success() { }
}
```

### 6. 避免测试之间的依赖

```java
// ❌ 错误 - 测试之间有依赖
private User sharedUser;

@Test
void test1() {
    sharedUser = new User("test");
}

@Test
void test2() {
    // 依赖test1的执行结果
    assertThat(sharedUser).isNotNull();
}

// ✅ 正确 - 每个测试独立
@Test
void test1() {
    User user = new User("test");
    // 测试逻辑
}

@Test
void test2() {
    User user = new User("test");
    // 测试逻辑
}
```

### 7. 使用@BeforeEach和@AfterEach

```java
@BeforeEach
void setUp() {
    // 每个测试前执行
    testData = createTestData();
}

@AfterEach
void tearDown() {
    // 每个测试后执行
    cleanupTestData();
}
```

### 8. Mock外部依赖

```java
@Mock
private ExternalApiClient externalApiClient;

@Test
void testWithExternalDependency() {
    // Mock外部API调用
    when(externalApiClient.fetchData())
            .thenReturn(mockData);

    // 测试逻辑
}
```

## 📚 相关文档

- [开发指南](./DEVELOPMENT.md)
- [环境变量配置](./ENVIRONMENT.md)
- [部署手册](./DEPLOYMENT.md)

## 🆘 获取帮助

- **测试问题**: 在Slack #eve-helper-testing频道提问
- **CI/CD问题**: 联系DevOps团队
- **文档**: 查看 [JUnit 5文档](https://junit.org/junit5/docs/current/user-guide/)

---

**最后更新**: 2026-02-01
**维护者**: EVE Helper QA Team
