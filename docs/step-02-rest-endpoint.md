# Step 2 — 첫 REST 엔드포인트

> 이 단계가 끝나면 **4개의 동작하는 HTTP 엔드포인트**가 만들어집니다.
> Spring MVC의 핵심 패턴(`@RestController`, `@PathVariable`, `@RequestParam`, 자동 JSON 변환)을 모두 익힙니다.

---

## 🎯 이번 단계의 목표

- `@RestController`로 HTTP 요청을 처리하는 클래스 만들기
- URL 매핑 어노테이션 4종 이해 (`@GetMapping` 등)
- 동적 URL 처리 — `@PathVariable`
- 쿼리스트링 처리 — `@RequestParam` (기본값 포함)
- 객체 반환 → JSON 자동 변환 (Jackson)

---

## 📋 사전 확인

- [Step 1](step-01-project-skeleton.md) 완료 (프로젝트 골격 + 서버 기동 검증)
- 이전 단계의 Java 프로세스가 **종료**되어 있는지 확인 (포트 8081)

```bash
netstat -ano | grep ":8081"
# 아무것도 안 나오면 OK
```

> ⚠️ **자주 만나는 함정 — 좀비 Java 프로세스**
> Maven으로 띄운 `spring-boot:run`을 Ctrl+C / 터미널 닫기로 종료하면
> Maven은 종료되지만 자식 Java 프로세스가 남아 포트를 잡고 있을 수 있습니다.
> 해결: `taskkill /F /PID <PID>` (Windows) 또는 `kill -9 <PID>` (Linux/Mac)

---

## 📂 추가할 파일

```
src\main\java\com\example\book\
├─ BookTutorialApplication.java                       (그대로)
└─ HelloController.java                               ⭐ 신규
```

**Step 2에서 변경되는 파일은 1개뿐**입니다. 새로 만드는 것만.

---

## 📝 `HelloController.java` 작성

위치: `e:\workspace\spring-tutorial\src\main\java\com\example\book\HelloController.java`

```java
package com.example.book;

import java.util.HashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    @GetMapping("/hello")
    public String hello() {
        return "Hello, Spring Boot!";
    }

    @GetMapping("/hello/{name}")
    public String helloName(@PathVariable String name) {
        return "Hello, " + name + "!";
    }

    @GetMapping("/greet")
    public String greet(
            @RequestParam(defaultValue = "Guest") String name,
            @RequestParam(defaultValue = "1") int times) {
        return "Hi " + name + "! (" + times + " times)";
    }

    @GetMapping("/info")
    public Map<String, Object> info() {
        Map<String, Object> result = new HashMap<>();
        result.put("app", "book-tutorial");
        result.put("version", "0.0.1-SNAPSHOT");
        result.put("javaVersion", System.getProperty("java.version"));
        return result;
    }
}
```

---

## 🔍 코드 줄별 해부

### 1. `@RestController` — 가장 중요한 어노테이션

```java
@RestController
public class HelloController {
```

이 한 줄이 하는 일:
- **`@Controller` + `@ResponseBody` 결합** — 즉, "이 클래스는 웹 컨트롤러이고, 메서드 반환값을 그대로 HTTP 응답 본문으로 쓴다"
- Spring Boot 기동 시 **Component Scan**으로 발견되어 자동 Bean 등록
- 메서드의 매핑 어노테이션(`@GetMapping` 등)을 보고 **URL 라우팅 테이블** 자동 구축

> **`@Controller` vs `@RestController` 차이**
>
> | | `@Controller` | `@RestController` |
> |---|---|---|
> | 용도 | HTML 페이지 반환 (Thymeleaf, JSP) | JSON/XML API 반환 |
> | 반환값 | View 이름 (템플릿 찾아 렌더링) | 응답 본문에 직접 쓰기 |
> | 추가 어노테이션 | 메서드마다 `@ResponseBody` 필요 | 불필요 (이미 포함됨) |
>
> 우리는 REST API를 만들므로 **`@RestController`**.

---

### 2. `@GetMapping` — HTTP GET 요청 매핑

```java
@GetMapping("/hello")
public String hello() {
    return "Hello, Spring Boot!";
}
```

- `GET http://localhost:8081/hello` 요청이 오면 이 메서드 호출
- 반환된 `String`은 그대로 응답 본문이 됨 (Content-Type: `text/plain`)

> **HTTP 메서드별 매핑 어노테이션 (총 5종)**
>
> | 어노테이션 | HTTP 메서드 | 용도 |
> |---|---|---|
> | `@GetMapping` | GET | 조회 |
> | `@PostMapping` | POST | 생성 |
> | `@PutMapping` | PUT | 전체 수정 |
> | `@PatchMapping` | PATCH | 부분 수정 |
> | `@DeleteMapping` | DELETE | 삭제 |
>
> 모두 `@RequestMapping(method = RequestMethod.XXX)`의 단축 어노테이션.

---

### 3. `@PathVariable` — URL 경로의 동적 부분

```java
@GetMapping("/hello/{name}")
public String helloName(@PathVariable String name) {
    return "Hello, " + name + "!";
}
```

- URL 패턴 `{name}` 위치의 값이 `name` 파라미터로 자동 주입
- `GET /hello/Alice` → `name = "Alice"`
- `GET /hello/Bob` → `name = "Bob"`

**여러 PathVariable**:
```java
@GetMapping("/books/{bookId}/chapters/{chapterId}")
public String chapter(@PathVariable Long bookId, @PathVariable int chapterId) { ... }
```

> **변수명이 다를 때는 명시**:
> ```java
> @GetMapping("/users/{userId}")
> public String user(@PathVariable("userId") String id) { ... }
> ```

---

### 4. `@RequestParam` — 쿼리스트링 처리

```java
@GetMapping("/greet")
public String greet(
        @RequestParam(defaultValue = "Guest") String name,
        @RequestParam(defaultValue = "1") int times) {
    return "Hi " + name + "! (" + times + " times)";
}
```

- URL: `/greet?name=Bob&times=3` → `name="Bob", times=3`
- `defaultValue` 덕분에 파라미터 안 보내도 OK → `/greet` → `name="Guest", times=1`
- **타입 자동 변환** — `times=3` (문자열) → `int 3`

> **유용한 옵션**
> ```java
> @RequestParam(name = "q", required = false) String query
> ```
> - `name` — URL 파라미터명 변경 (변수명과 달라도 됨)
> - `required = false` — 없어도 에러 안 남 (값은 `null`)
> - `defaultValue` — 없을 때 기본값 (required=false 자동 적용됨)

---

### 5. 객체 반환 → JSON 자동 변환

```java
@GetMapping("/info")
public Map<String, Object> info() {
    Map<String, Object> result = new HashMap<>();
    result.put("app", "book-tutorial");
    result.put("version", "0.0.1-SNAPSHOT");
    result.put("javaVersion", System.getProperty("java.version"));
    return result;
}
```

**`Map`을 반환했는데 JSON 응답이 됩니다.** 어떻게?

1. `@RestController`의 `@ResponseBody` 기능이 작동
2. Spring MVC가 반환 객체를 적절한 변환기에 넘김 (HttpMessageConverter)
3. **Jackson** (`starter-web`에 포함)이 `Map` → JSON 문자열로 변환
4. `Content-Type: application/json` 헤더 자동 설정

`List`, POJO, `Set` 등 거의 모든 객체가 자동 변환됩니다.

```java
// POJO도 OK
public class Book { private String title; private String author; /* getters */ }

@GetMapping("/sample-book")
public Book sample() {
    return new Book("Clean Code", "Robert Martin");
}
// 응답: {"title":"Clean Code","author":"Robert Martin"}
```

> Jackson은 **getter 메서드**를 보고 JSON 키 생성. private 필드 직접 접근 안 함.
> Lombok의 `@Getter`나 직접 작성한 getter가 필요.

---

## ▶️ 빌드 & 실행

### 컴파일

```bash
mvn -f e:/workspace/spring-tutorial/pom.xml compile
```

**기대 출력 마지막 부분**:
```
[INFO] Changes detected - recompiling the module!
[INFO] Compiling 2 source files to e:\workspace\spring-tutorial\target\classes
[INFO] BUILD SUCCESS
```

**`2 source files`** — `BookTutorialApplication.java` + `HelloController.java`.

### 서버 기동

```bash
mvn -f e:/workspace/spring-tutorial/pom.xml spring-boot:run
```

이전과 동일한 기동 로그가 나와야 합니다.

---

## 🧪 검증 — curl로 4가지 케이스 호출

```bash
# 1) 기본 엔드포인트
curl http://localhost:8081/hello
# → Hello, Spring Boot!

# 2) PathVariable
curl http://localhost:8081/hello/Alice
# → Hello, Alice!

# 3) RequestParam (값 제공)
curl "http://localhost:8081/greet?name=Bob&times=3"
# → Hi Bob! (3 times)

# 4) RequestParam (기본값 사용)
curl http://localhost:8081/greet
# → Hi Guest! (1 times)

# 5) JSON 응답
curl http://localhost:8081/info
# → {"app":"book-tutorial","javaVersion":"1.8.0_482","version":"0.0.1-SNAPSHOT"}
```

> **Windows cmd에서는 `&`가 명령어 구분자**입니다.
> 쿼리스트링에 `&`가 있으면 따옴표로 감싸야 합니다: `curl "http://...&times=3"`

---

## ✅ 검증 체크리스트

- [ ] `HelloController.java` 파일 작성 완료
- [ ] `mvn compile` → `Compiling 2 source files`, `BUILD SUCCESS`
- [ ] 서버 기동 후 `/hello` 호출 → `Hello, Spring Boot!`
- [ ] `/hello/Alice` 호출 → `Hello, Alice!`
- [ ] `/greet?name=Bob&times=3` → `Hi Bob! (3 times)`
- [ ] `/greet` (파라미터 없이) → `Hi Guest! (1 times)`
- [ ] `/info` → JSON 응답 (Content-Type: application/json)

---

## 📚 핵심 개념 정리

### 1. Spring MVC 요청 처리 흐름

```
HTTP Request → DispatcherServlet → HandlerMapping (URL→메서드 매칭)
                                 → HandlerAdapter (메서드 호출)
                                 → @RestController 메서드 실행
                                 → 반환값 → HttpMessageConverter (객체→JSON)
                                 → HTTP Response
```

**DispatcherServlet** — Spring MVC의 진입점 (Front Controller 패턴).
우리가 직접 보지 않지만 모든 요청이 여기를 거칩니다.

### 2. Component Scan + Bean 자동 등록
- `@RestController`는 `@Component`의 특수 형태
- `BookTutorialApplication`의 `@SpringBootApplication`이 트리거하는 ComponentScan이 `HelloController`를 자동 발견 → Bean으로 등록
- **우리가 직접 `new HelloController()` 하지 않습니다.** Spring이 합니다.

### 3. HttpMessageConverter
- 객체 ↔ HTTP 본문 변환 담당
- 반환 객체 → JSON: `MappingJackson2HttpMessageConverter` (Jackson)
- 반환 객체 → XML: `MappingJackson2XmlHttpMessageConverter` (XML starter 추가 시)
- 반환 객체 → 문자열: `StringHttpMessageConverter`
- **Spring이 반환 타입과 클라이언트의 `Accept` 헤더 보고 자동 선택**.

### 4. 자동 타입 변환
```java
@RequestParam int times      // "3" → 3
@PathVariable Long id        // "42" → 42L
@PathVariable LocalDate date // "2026-05-21" → LocalDate 객체
```
변환 실패 시 Spring이 400 Bad Request 자동 응답.

---

## ⚠️ 자주 만나는 에러

### "Required request parameter 'name' is not present"
```
GET /greet  (name 파라미터 없음)
→ 400 Bad Request
```
**원인**: `@RequestParam`에 `required=false` 또는 `defaultValue`가 없는데 파라미터를 안 보냄.
**해결**: 기본값 지정 또는 `required = false` 추가.

### "No primary or default constructor found"
JSON 응답으로 POJO를 반환하려는데 에러.
**원인**: Jackson이 객체 역직렬화 시 기본 생성자(no-arg constructor)를 찾는데 없음. (요청 처리 시 발생)
**해결**: 기본 생성자 추가 또는 Lombok `@NoArgsConstructor`.

### "Ambiguous mapping"
```
java.lang.IllegalStateException: Ambiguous mapping. Cannot map 'helloName' method
```
**원인**: 같은 URL 패턴이 두 메서드에 매핑됨.
**해결**: URL 패턴을 다르게 하거나, HTTP 메서드를 분리.

### Windows curl에서 `&`로 명령 잘림
```bash
curl http://localhost:8081/greet?name=Bob&times=3
# bash: times=3: command not found
```
**해결**: URL 전체를 따옴표로 감싸기 → `curl "http://...?name=Bob&times=3"`

---

## 🔤 용어 사전 (이번 단계 신규)

| 용어 | 의미 |
|---|---|
| **DispatcherServlet** | Spring MVC의 Front Controller — 모든 HTTP 요청의 진입점 |
| **Handler** | 요청을 처리하는 컨트롤러 메서드 |
| **HandlerMapping** | URL → Handler 매칭 담당 |
| **HttpMessageConverter** | 객체 ↔ HTTP 본문 (JSON, XML 등) 변환기 |
| **Path Variable** | URL 경로의 동적 부분 (`/books/{id}`의 `{id}`) |
| **Query Parameter** | URL의 `?` 이후 부분 (`?name=Bob&age=20`) |
| **Content Negotiation** | 클라이언트 `Accept` 헤더 기반 응답 형식 결정 |
| **Serialization** | 객체 → 텍스트(JSON 등) 변환 (Jackson의 역할) |
| **Deserialization** | 텍스트(JSON 등) → 객체 변환 (Step 4에서 활용) |

---

## 🧠 한 번 더 강조 — 자동 설정의 위력

이번 단계에서 우리가 작성한 것:
- 컨트롤러 클래스 1개 (35 줄)

이번 단계에서 **Spring Boot가 알아서 해준 것**:
- URL 매칭 라우팅 테이블 생성
- HTTP 요청 파싱
- 파라미터 자동 변환 (String → int, Long 등)
- 메서드 호출
- 반환 객체 → JSON 직렬화 (Jackson)
- Content-Type 헤더 자동 설정
- 에러 시 적절한 HTTP 상태 코드 응답

전통적인 Servlet 코드로 같은 일을 하려면 수백 줄이 필요합니다.

---

## ➡️ 다음 단계 미리보기 — Step 3

**JPA + H2로 데이터베이스 도입**

```java
@Entity
public class Book {
    @Id @GeneratedValue
    private Long id;
    private String title;
    private String author;
    // ...
}

public interface BookRepository extends JpaRepository<Book, Long> { }
```

이때 등장할 새 개념:
- `@Entity` — 테이블 매핑
- `@Id`, `@GeneratedValue` — 기본키
- `JpaRepository` — CRUD 메서드 자동 생성 (구현체 없음!)
- H2 인메모리 DB + 콘솔 (브라우저로 SQL 직접 실행 가능)
- `application.yml` 확장 — DB 연결 설정

---

## 📦 이 단계의 산출물

```
e:\workspace\spring-tutorial\
├─ pom.xml                                            (변경 없음)
├─ src\main\java\com\example\book\
│  ├─ BookTutorialApplication.java                    (변경 없음)
│  └─ HelloController.java                            ⭐ +35 줄
└─ src\main\resources\
   └─ application.yml                                 (변경 없음)
```

**추가된 코드량: 35 줄.** 동작하는 REST API 4개가 완성됐습니다.
