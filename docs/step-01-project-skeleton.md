# Step 1 — 프로젝트 골격 만들기

> Spring Boot가 처음인 개발자를 위한 단계별 가이드.
> 이 단계가 끝나면 **빈 Spring Boot 서버**가 8081 포트에서 동작합니다.

---

## 🎯 이번 단계의 목표

- Spring Boot 프로젝트의 **최소 구성** 이해 (파일 3개)
- **Maven** 빌드 시스템 첫 경험
- **`mvn spring-boot:run`** 한 줄로 웹 서버 띄우기
- Spring Boot의 **자동 설정(Auto-Configuration)** 마법 이해

---

## 📋 시작하기 전에 확인할 것

| 도구 | 확인 명령 | 기대 결과 |
|---|---|---|
| JDK 8 | `java -version` | `openjdk version "1.8.0_..."` |
| Maven 3.x | `mvn -version` 또는 `E:\maven\bin\mvn -version` | `Apache Maven 3.9.x` |

> **PATH에 mvn이 없다면?**
> Windows에서 환경변수 PATH에 `E:\maven\bin`을 추가하거나, 매번 절대 경로(`E:\maven\bin\mvn`)로 호출하면 됩니다.

---

## 📂 만들 것 — 파일 3개

```
e:\workspace\spring-tutorial\
├─ pom.xml                                            ← Maven 빌드 설정 (어떤 라이브러리 쓸지)
└─ src\
   ├─ main\
   │  ├─ java\com\example\book\
   │  │  └─ BookTutorialApplication.java              ← 진입점 (main 메서드)
   │  └─ resources\
   │     └─ application.yml                           ← 서버 설정 (포트 등)
   └─ test\java\com\example\book\                     ← (지금은 비어있음, 나중에 테스트 작성용)
```

> **왜 이렇게 깊은 폴더 구조?**
> Maven의 **약속(Convention)** 입니다. 이 위치에 두면 Maven이 자동으로 찾습니다.
> 다른 곳에 두면 빌드 설정에 일일이 경로를 적어야 합니다.

---

## 🛠 1단계 — 디렉토리 생성

```bash
mkdir -p e:/workspace/spring-tutorial/src/main/java/com/example/book
mkdir -p e:/workspace/spring-tutorial/src/main/resources
mkdir -p e:/workspace/spring-tutorial/src/test/java/com/example/book
```

> `-p` 옵션 = 부모 디렉토리도 함께 생성. Windows cmd에서는 `mkdir e:\workspace\spring-tutorial\src\main\java\com\example\book` 처럼 백슬래시 사용.

---

## 📝 2단계 — `pom.xml` 작성

위치: `e:\workspace\spring-tutorial\pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>2.7.18</version>
        <relativePath/>
    </parent>

    <groupId>com.example</groupId>
    <artifactId>book-tutorial</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>book-tutorial</name>
    <description>Spring Boot Tutorial - Book CRUD REST API</description>

    <properties>
        <java.version>1.8</java.version>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

### 줄별 의미

| 부분 | 의미 |
|---|---|
| `<parent>` | "Spring Boot의 표준 설정을 부모로 상속받겠다" — 의존성 버전을 자동으로 맞춰주는 핵심 |
| `<groupId>com.example</groupId>` | 회사/조직 식별자 (Java 패키지명과 보통 같음) |
| `<artifactId>book-tutorial</artifactId>` | 이 프로젝트의 이름 |
| `<version>0.0.1-SNAPSHOT</version>` | 버전. `-SNAPSHOT` = "아직 개발 중" 의미 |
| `<java.version>1.8</java.version>` | 컴파일 대상 Java 버전 |
| `spring-boot-starter-web` | 묶음 의존성 — Spring MVC + Tomcat + Jackson + ... |
| `spring-boot-starter-test` | JUnit 5 + Mockito + AssertJ + ... 한꺼번에 |
| `spring-boot-maven-plugin` | `mvn spring-boot:run`, `mvn package`로 실행 가능 JAR 생성 가능하게 해주는 플러그인 |

### 🪄 `parent`의 마법

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>2.7.18</version>
</parent>
```

이 한 블록이 해주는 일:

1. **버전 자동 관리** — Jackson, Tomcat, JUnit 등 수십 개 라이브러리 버전을 호환되는 조합으로 자동 지정. 우리 `<dependencies>`에 `<version>`을 안 적어도 됨.
2. **빌드 플러그인 기본 설정** — `maven-compiler-plugin`, `maven-resources-plugin` 등 알아서 설정.
3. **UTF-8 인코딩 등 표준 설정** — 자주 빠뜨리는 옵션을 미리 박아둠.

> **참고**: 닫힌 네트워크 환경에서 Eclipse가 `pom.xml` 1라인에 "Unknown" 에러를 표시할 수 있습니다.
> 원인은 외부 XSD 스키마(`maven-4.0.0.xsd`) 다운로드 실패. 빌드와는 무관.
> 거슬리면 `<project>` 태그에서 `xmlns:xsi`, `xsi:schemaLocation` 속성을 제거하세요.

---

## 📝 3단계 — 메인 클래스 작성

위치: `e:\workspace\spring-tutorial\src\main\java\com\example\book\BookTutorialApplication.java`

```java
package com.example.book;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BookTutorialApplication {

    public static void main(String[] args) {
        SpringApplication.run(BookTutorialApplication.class, args);
    }
}
```

### `@SpringBootApplication` 분해

이 어노테이션은 사실 **3개의 어노테이션 묶음**입니다.

| 합쳐진 어노테이션 | 역할 |
|---|---|
| `@Configuration` | "이 클래스는 Spring Bean 설정을 담고 있다" |
| `@EnableAutoConfiguration` | "classpath에 있는 라이브러리 보고 필요한 Bean을 자동 생성하라" — **핵심 마법** |
| `@ComponentScan` | "이 클래스가 속한 패키지(`com.example.book`)와 그 하위에서 `@Component`, `@Service`, `@Controller`, `@RestController` 등을 자동 발견하라" |

> **`@ComponentScan` 때문에 패키지 구조가 중요합니다.**
> `BookTutorialApplication.java`를 `com.example.book`에 두면, 그 하위 모든 패키지(`com.example.book.controller`, `com.example.book.service`)가 자동 스캔 대상.
> 메인 클래스보다 **위 패키지**에 있는 클래스는 스캔 안 됩니다.

### `SpringApplication.run()`이 하는 일

1. **Spring Container(IoC 컨테이너) 생성** — Bean을 담는 거대한 Map
2. **Component Scan 실행** — `@Component`, `@RestController` 등이 붙은 클래스를 모두 찾아 Bean으로 등록
3. **Auto-Configuration 실행** — classpath 보고 알아서 설정
   - Tomcat 라이브러리 발견 → 내장 Tomcat 띄움
   - Jackson 발견 → JSON 변환기 Bean 생성
   - JdbcTemplate 발견 → DataSource Bean 생성
4. **Embedded Tomcat 기동** — 8081 포트에서 HTTP 요청 대기
5. **시작 완료 로그 출력** 후 대기 상태

---

## 📝 4단계 — `application.yml` 작성

위치: `e:\workspace\spring-tutorial\src\main\resources\application.yml`

```yaml
server:
  port: 8081
```

> **왜 8081?**
> Spring Boot 기본은 **8080**. 다른 프로젝트가 8080을 쓰고 있을 수 있어 충돌을 피했습니다.
> 어차피 설정으로 바꾸는 방법을 익히는 게 좋습니다.

### `.yml` vs `.properties`

같은 설정을 두 가지 형식으로 쓸 수 있습니다.

`application.properties` 형식 (옛날 방식):
```properties
server.port=8081
```

`application.yml` 형식 (요즘 방식):
```yaml
server:
  port: 8081
```

**YAML이 더 읽기 쉬워서** 최근에는 yml을 더 많이 씁니다. 들여쓰기는 **공백 2칸** (탭 금지).

---

## ▶️ 5단계 — 빌드 & 실행

### 컴파일 먼저 확인

```bash
mvn -f e:/workspace/spring-tutorial/pom.xml clean compile
```

> `-f` 옵션 = pom.xml 위치 지정. 현재 폴더가 프로젝트 안이면 안 적어도 됨.
> `clean` = `target/` 폴더 삭제 (이전 빌드 결과 제거)
> `compile` = `.java` → `.class` 변환

**기대 출력 마지막 부분**:
```
[INFO] Compiling 1 source file to e:\workspace\spring-tutorial\target\classes
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  1.467 s
```

### 서버 기동

```bash
mvn -f e:/workspace/spring-tutorial/pom.xml spring-boot:run
```

**기대 출력**:
```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::               (v2.7.18)

INFO ... Starting BookTutorialApplication using Java 1.8.0_482
INFO ... Tomcat initialized with port(s): 8081 (http)
INFO ... Tomcat started on port(s): 8081 (http) with context path ''
INFO ... Started BookTutorialApplication in 1.24 seconds
```

> **"Started in X seconds" 메시지가 보이면 성공.**
> Ctrl+C로 종료할 수 있습니다.

### HTTP 응답 확인 (다른 터미널에서)

```bash
curl -s -o /dev/null -w "HTTP %{http_code}\n" http://localhost:8081/
curl -s http://localhost:8081/
```

**기대 출력**:
```
HTTP 404
{"timestamp":"2026-05-21T06:16:50.725+00:00","status":404,"error":"Not Found","path":"/"}
```

> **404가 정상입니다.** 아직 우리가 만든 엔드포인트가 없으니까요.
> 중요한 건 응답이 **JSON으로 자동 변환되어 돌아온다**는 점 — Jackson이 작동 중이라는 증거.

---

## ✅ 검증 체크리스트

이 단계가 끝났다고 말하기 전에 모두 확인:

- [ ] `e:\workspace\spring-tutorial\` 디렉토리에 파일 3개가 있다
- [ ] `mvn clean compile` → `BUILD SUCCESS`
- [ ] `mvn spring-boot:run` → `Started BookTutorialApplication in X seconds` 로그
- [ ] `curl http://localhost:8081/` → HTTP 404 + JSON 응답
- [ ] Ctrl+C로 정상 종료됨

---

## 📚 핵심 개념 정리

### 1. `@SpringBootApplication` = 3-in-1
- `@Configuration` — Bean 설정 클래스
- `@EnableAutoConfiguration` — classpath 기반 자동 설정
- `@ComponentScan` — `@Component` 자동 발견

### 2. Starter 의존성
- `starter-web` → MVC + Tomcat + Jackson + ...
- `starter-test` → JUnit + Mockito + AssertJ + ...
- 묶음으로 들어와서 **버전 호환** 걱정 없음.

### 3. Embedded Tomcat
- 옛날: WAR 파일 → 별도 Tomcat 서버에 배포
- Spring Boot: **JAR 안에 Tomcat 포함** → `java -jar`로 바로 실행
- 운영 환경 단순화 (서버 설치 불필요)

### 4. Convention over Configuration
- 약속된 위치(`src/main/java`, `src/main/resources`)에 두면 알아서 인식
- 설정은 **다를 때만** 적기 — 기본값으로 충분하면 안 적어도 됨

### 5. IoC (Inversion of Control)
- 내가 `new` 안 하고 **Spring이 객체 생성**
- 내가 객체 가져오는 게 아니라 **Spring이 주입**(`@Autowired`)
- 다음 단계에서 실제로 보게 됨

---

## ⚠️ 자주 만나는 에러

### "JAVA_HOME not set"
```
Error: JAVA_HOME not found in your environment
```
**해결**: 환경변수 `JAVA_HOME`을 JDK 설치 경로로 설정 (예: `E:\jdk8`).

### "port 8081 already in use"
```
Web server failed to start. Port 8081 was already in use.
```
**해결**: `application.yml`의 포트를 다른 번호로 변경 (예: 8082), 또는 8081을 쓰는 프로세스를 찾아 종료.
```bash
# Windows에서 포트 점유 프로세스 찾기
netstat -ano | findstr :8081
taskkill /PID <PID> /F
```

### "Failed to download artifact ..."
```
[ERROR] Failed to execute goal ... Could not resolve dependencies
```
**원인**: 인터넷 미연결 또는 사내망에서 Maven Central 차단.
**해결**:
1. 인터넷 연결 확인
2. 회사 사내 Maven repository가 있으면 `~/.m2/settings.xml`에 mirror 추가
3. 한 번 다운로드된 의존성은 `~/.m2/repository/`에 캐시됨 — 오프라인에서도 재사용 가능

### "package org.springframework... does not exist"
**원인**: Maven 의존성 다운로드 실패 또는 IDE가 의존성 인식 못 함.
**해결**: `mvn clean compile`을 명령줄에서 먼저 성공시키고, IDE에서 Maven 프로젝트 재import.

---

## 🔤 용어 사전

| 용어 | 의미 |
|---|---|
| **POM** | Project Object Model — Maven의 프로젝트 설명 파일(`pom.xml`) |
| **Artifact** | Maven이 만들거나 가져오는 산출물 (JAR, WAR 등). `artifactId`로 식별 |
| **Dependency** | 외부 라이브러리 의존성 — `<dependency>`로 선언 |
| **Starter** | 관련 의존성을 묶어둔 Spring Boot의 메타 패키지 (`spring-boot-starter-*`) |
| **Bean** | Spring Container가 관리하는 객체 |
| **IoC Container** | Bean을 담아두고 의존성 주입을 처리하는 Spring의 핵심 |
| **Auto-Configuration** | classpath 보고 필요한 Bean을 자동 생성하는 Spring Boot 기능 |
| **Embedded Server** | JAR 안에 내장된 웹 서버 (Tomcat) — 별도 설치 불필요 |
| **Convention over Configuration** | 약속된 구조를 따르면 설정 안 해도 동작 |
| **Component Scan** | `@Component` 계열 어노테이션이 붙은 클래스를 자동으로 Bean 등록 |

---

## ➡️ 다음 단계 미리보기 — Step 2

**`@RestController`로 첫 엔드포인트 만들기**

```java
@RestController
public class HelloController {
    @GetMapping("/hello")
    public String hello() {
        return "Hello, Spring Boot!";
    }
}
```

→ `curl http://localhost:8081/hello` → `Hello, Spring Boot!` 응답

이때 등장할 새 개념:
- `@RestController` vs `@Controller`
- `@GetMapping`, `@PostMapping` 등 매핑 어노테이션
- Spring MVC의 요청 처리 흐름
- DevTools로 자동 재시작 (옵션)

---

## 📦 이 단계의 산출물

```
e:\workspace\spring-tutorial\
├─ pom.xml                                            (40 줄)
├─ src\main\java\com\example\book\
│  └─ BookTutorialApplication.java                    (12 줄)
└─ src\main\resources\
   └─ application.yml                                 (2 줄)
```

**총 코드량: 54 줄.** 이것만으로 동작하는 웹 서버가 됩니다.

이게 Spring Boot가 사랑받는 이유입니다 — **보일러플레이트 최소화**.
