# Step 5 — 입력 검증 + 전역 예외 처리

> **튜토리얼의 마지막 단계.** 이 단계가 끝나면 우리 API는 **운영 환경에서도 부끄럽지 않은** 모습이 됩니다.
> 잘못된 입력은 자동으로 거절되고, 모든 에러가 **일관된 JSON 포맷**으로 응답됩니다.

---

## 🎯 이번 단계의 목표

- **Bean Validation 표준** — `@NotBlank`, `@NotNull`, `@Size`, `@Min`, `@Max`
- **`@Valid`** — 요청 본문(`@RequestBody`) 자동 검증
- **사용자 정의 예외** — `BookNotFoundException`
- **`@RestControllerAdvice` + `@ExceptionHandler`** — 전역 예외 처리
- **일관된 에러 응답 JSON 포맷** — timestamp, status, message, path, fieldErrors
- **Service/Controller 단순화** — Optional 분기 제거, 예외로 흐름 통일
- **`@ResponseStatus`** — 메서드 레벨 HTTP 상태 코드 선언

---

## 🤔 왜 이 단계가 필요한가?

Step 4까지의 API의 약점:

```bash
# 빈 title로 책 생성 시도 — 그대로 저장됨 ⚠️
curl -X POST http://localhost:8081/api/books \
  -H "Content-Type: application/json" \
  -d '{"title":"","author":"","publishedYear":null}'
# → 201 Created (잘못된 데이터가 DB에 들어감)

# 없는 ID 조회 — 빈 404
curl http://localhost:8081/api/books/999
# → HTTP 404 (본문 없음, 어떤 ID가 없는지 알 수 없음)
```

Step 5 이후:

```bash
# 잘못된 입력 → 400 + 어떤 필드가 왜 잘못됐는지
{"timestamp":"...","status":400,"error":"Bad Request",
 "message":"Validation failed","path":"/api/books",
 "fieldErrors":[
   {"field":"title","message":"공백일 수 없습니다"},
   {"field":"publishedYear","message":"널이어서는 안됩니다"}
 ]}

# 없는 ID → 404 + 의미 있는 메시지
{"timestamp":"...","status":404,"error":"Not Found",
 "message":"Book not found: id=999","path":"/api/books/999"}
```

---

## 📂 변경되는 파일

```
e:\workspace\spring-tutorial\
├─ pom.xml                                            ✏️ starter-validation 추가
└─ src\main\java\com\example\book\
   ├─ Book.java                                       ✏️ 검증 어노테이션 6개 추가
   ├─ BookService.java                                ✏️ Optional → 예외 던지기
   ├─ BookController.java                             ✏️ @Valid 추가, Optional 분기 제거
   ├─ BookNotFoundException.java                      ⭐ 신규 — 사용자 정의 예외
   ├─ ErrorResponse.java                              ⭐ 신규 — 에러 응답 DTO
   └─ GlobalExceptionHandler.java                     ⭐ 신규 — @RestControllerAdvice
```

---

## 📝 1단계 — `pom.xml`에 validation 의존성 추가

`<dependencies>` 블록에 추가:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

### 왜 별도 의존성이 필요한가?

- **Spring Boot 2.2 이전**: `spring-boot-starter-web`에 validation이 포함됨 (자동)
- **Spring Boot 2.3+**: validation이 분리됨 — **명시적으로 추가해야 함**

이건 자주 빠뜨리는 함정. 안 추가하면 `@Valid`가 **조용히 동작 안 함** (예외도 안 던짐).

### 들어오는 것
- **Hibernate Validator** — Bean Validation 명세(JSR-380)의 표준 구현
- **Jakarta EL** — 검증 메시지 안의 표현식 평가용
- **다국어 메시지 번들** — 한국어, 영어 등 자동 적용 (이번에 우리가 본 한국어 에러 메시지의 출처)

---

## 📝 2단계 — `Book.java`에 검증 어노테이션 추가

```java
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Entity
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 200)
    private String title;

    @NotBlank
    @Size(max = 100)
    private String author;

    @NotNull
    @Min(1000)
    @Max(9999)
    private Integer publishedYear;
    
    // 생성자, getter/setter 그대로
}
```

### 주요 어노테이션 정리

| 어노테이션 | 대상 타입 | 동작 |
|---|---|---|
| `@NotNull` | 모든 타입 | `null` 거절 |
| `@NotEmpty` | String, Collection, Array, Map | `null` + 길이 0 거절 |
| `@NotBlank` | String 전용 | `null` + 빈 문자열 + **공백만 있는 문자열** 거절 |
| `@Size(min=?, max=?)` | String, Collection 등 | 길이/원소 수 범위 |
| `@Min(?)` / `@Max(?)` | 숫자 타입 | 값 범위 (정수) |
| `@DecimalMin` / `@DecimalMax` | 숫자 타입 | 값 범위 (소수 포함) |
| `@Positive` / `@Negative` | 숫자 타입 | 부호 |
| `@Email` | String | 이메일 형식 |
| `@Pattern(regexp=...)` | String | 정규식 매칭 |
| `@Past` / `@Future` | 날짜 타입 | 과거/미래 |

### `@NotNull` vs `@NotEmpty` vs `@NotBlank` 정확한 차이

```java
@NotNull   String s = null;       // 거절 ❌
@NotNull   String s = "";         // 통과 ✅
@NotNull   String s = "   ";      // 통과 ✅

@NotEmpty  String s = null;       // 거절 ❌
@NotEmpty  String s = "";         // 거절 ❌
@NotEmpty  String s = "   ";      // 통과 ✅

@NotBlank  String s = null;       // 거절 ❌
@NotBlank  String s = "";         // 거절 ❌
@NotBlank  String s = "   ";      // 거절 ❌
@NotBlank  String s = "abc";      // 통과 ✅
```

**문자열에는 거의 항상 `@NotBlank`가 안전.**

---

## 📝 3단계 — `BookNotFoundException.java` 신규

```java
package com.example.book;

public class BookNotFoundException extends RuntimeException {

    private final Long bookId;

    public BookNotFoundException(Long bookId) {
        super("Book not found: id=" + bookId);
        this.bookId = bookId;
    }

    public Long getBookId() {
        return bookId;
    }
}
```

### 왜 사용자 정의 예외인가?

- **의미가 분명** — `RuntimeException`보다 `BookNotFoundException`이 의도를 명확히 전달
- **타입으로 분기** — `@ExceptionHandler(BookNotFoundException.class)`로 정확히 잡을 수 있음
- **추가 정보 보관** — `bookId` 같은 컨텍스트를 캡슐화

### 왜 `RuntimeException`을 상속?

- **체크 예외(checked)** = `throws` 선언 필수, 호출자가 처리 강제
- **런타임 예외(unchecked)** = 처리 강제 없음, 자유롭게 던질 수 있음

Spring 트랜잭션은 **`RuntimeException`만 자동 rollback**합니다. 체크 예외는 기본 rollback 안 됨. 비즈니스 예외는 **거의 항상 `RuntimeException` 계열**로 만드는 것이 관용.

---

## 📝 4단계 — `ErrorResponse.java` 신규

```java
package com.example.book;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private final LocalDateTime timestamp;
    private final int status;
    private final String error;
    private final String message;
    private final String path;
    private final List<FieldError> fieldErrors;

    public ErrorResponse(int status, String error, String message,
                         String path, List<FieldError> fieldErrors) {
        this.timestamp = LocalDateTime.now();
        // ... 나머지 필드 할당
    }

    // getters ...

    public static class FieldError {
        private final String field;
        private final String message;
        // 생성자, getters
    }
}
```

### 설계 결정 — 일관된 에러 응답 포맷

모든 에러 응답을 같은 구조로 통일:
- `timestamp` — 발생 시각
- `status` — HTTP 상태 코드 (숫자)
- `error` — HTTP 상태명 ("Not Found", "Bad Request")
- `message` — 사람이 읽을 수 있는 설명
- `path` — 요청 경로
- `fieldErrors` — 검증 실패 시 필드별 상세 (옵션)

이 포맷은 Spring Boot의 기본 에러 응답과 **호환**됩니다 (`org.springframework.boot.web.error.DefaultErrorAttributes`).

### `@JsonInclude(JsonInclude.Include.NON_NULL)`

- **`null` 필드는 JSON에서 제외**
- 검증 실패가 아닌 응답에서 `"fieldErrors":null` 같은 노이즈 제거

```java
// 검증 실패 시
{"timestamp":"...","status":400,"error":"Bad Request","message":"...","path":"...","fieldErrors":[...]}

// 404 시 (fieldErrors 자동 생략)
{"timestamp":"...","status":404,"error":"Not Found","message":"...","path":"..."}
```

### 왜 `final` 필드 + 생성자 주입?

- **불변(immutable) 객체** — 한 번 만들어지면 안 바뀜
- 스레드 안전 — 동시 요청에 안전
- DTO는 거의 항상 불변으로 만드는 게 좋음

### 정적 중첩 클래스 (`static class FieldError`)

- `ErrorResponse.FieldError`로 접근
- `FieldError`라는 이름이 Spring의 `org.springframework.validation.FieldError`와 겹치지만 **다른 패키지**라 충돌 없음
- 관련 클래스를 한 파일에 묶어 응집도 향상

---

## 📝 5단계 — `GlobalExceptionHandler.java` 신규 (핵심!)

```java
package com.example.book;

import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BookNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleBookNotFound(
            BookNotFoundException ex, HttpServletRequest request) {
        ErrorResponse body = new ErrorResponse(
            HttpStatus.NOT_FOUND.value(),
            "Not Found",
            ex.getMessage(),
            request.getRequestURI(),
            null
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult()
            .getFieldErrors().stream()
            .map(fe -> new ErrorResponse.FieldError(fe.getField(), fe.getDefaultMessage()))
            .collect(Collectors.toList());

        ErrorResponse body = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "Bad Request",
            "Validation failed",
            request.getRequestURI(),
            fieldErrors
        );
        return ResponseEntity.badRequest().body(body);
    }
}
```

### `@RestControllerAdvice`

- `@ControllerAdvice` + `@ResponseBody` 결합
- "이 클래스는 **모든 컨트롤러를 가로채는 글로벌 처리기**입니다"
- 컨트롤러에서 예외가 throw되면 → 이 클래스의 `@ExceptionHandler`가 잡음

```
요청 → DispatcherServlet → BookController
                              ↓ throw BookNotFoundException
                          GlobalExceptionHandler.handleBookNotFound()
                              ↓
                          ErrorResponse 응답
```

### `@ExceptionHandler(클래스)`

- "이 메서드가 **해당 예외 타입을 처리**합니다"
- **상속 관계도 인식** — `@ExceptionHandler(RuntimeException.class)`는 그 하위도 모두 잡음
- 더 구체적인 핸들러가 우선 (BookNotFoundException 핸들러가 RuntimeException 핸들러보다 우선)

### `MethodArgumentNotValidException`

- `@Valid @RequestBody`의 검증 실패 시 Spring이 던지는 표준 예외
- `getBindingResult().getFieldErrors()` — 각 필드의 실패 정보를 모두 모음
- 우리는 이를 `ErrorResponse.FieldError` 리스트로 변환

### `HttpServletRequest` 자동 주입

```java
public ResponseEntity<ErrorResponse> handleBookNotFound(
        BookNotFoundException ex, HttpServletRequest request) {
```

- `@ExceptionHandler` 메서드의 파라미터에 `HttpServletRequest`를 적으면 **Spring이 자동 주입**
- 현재 요청의 URI, 헤더 등을 가져와 에러 응답에 포함 가능

---

## 📝 6단계 — `BookService.java` 리팩터

**변경 전 (Step 4)** — Optional 반환:
```java
public Optional<Book> findOne(Long id) {
    return repository.findById(id);
}

public boolean delete(Long id) {
    if (!repository.existsById(id)) return false;
    repository.deleteById(id);
    return true;
}
```

**변경 후 (Step 5)** — 예외 던지기:
```java
public Book findOne(Long id) {
    return repository.findById(id)
        .orElseThrow(() -> new BookNotFoundException(id));
}

public void delete(Long id) {
    if (!repository.existsById(id)) {
        throw new BookNotFoundException(id);
    }
    repository.deleteById(id);
}
```

### 왜 이렇게 바꿨나?

**Step 4 방식의 문제**:
- 모든 Controller가 똑같이 `Optional` → 404 변환을 반복
- 메서드마다 `.map().orElseGet()` 패턴이 산재
- 깜빡하면 500 에러로 새어나감

**Step 5 방식의 장점**:
- Service는 단순한 비즈니스 흐름만 표현
- 404 변환 책임은 **`GlobalExceptionHandler` 한 곳**에 집중
- "없으면 안 됨"이라는 도메인 규칙이 코드로 명확히 표현

### `orElseThrow(() -> ...)` 패턴

- Java 8+ `Optional`의 메서드
- "값이 있으면 꺼내고, 없으면 람다가 반환하는 예외를 던진다"
- 의도가 명확하고 짧음

---

## 📝 7단계 — `BookController.java` 단순화

**변경 전 (Step 4)** — Optional 분기:
```java
@GetMapping("/{id}")
public ResponseEntity<Book> get(@PathVariable Long id) {
    return service.findOne(id)
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.notFound().build());
}
```

**변경 후 (Step 5)** — 단순 반환:
```java
@GetMapping("/{id}")
public Book get(@PathVariable Long id) {
    return service.findOne(id);    // 없으면 자동으로 404 응답
}
```

### 전체 BookController 변화

```java
@RestController
@RequestMapping("/api/books")
public class BookController {

    private final BookService service;

    public BookController(BookService service) {
        this.service = service;
    }

    @GetMapping
    public List<Book> list() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public Book get(@PathVariable Long id) {
        return service.findOne(id);
    }

    @PostMapping
    public ResponseEntity<Book> create(@Valid @RequestBody Book book) {
        Book created = service.create(book);
        return ResponseEntity
            .created(URI.create("/api/books/" + created.getId()))
            .body(created);
    }

    @PutMapping("/{id}")
    public Book update(@PathVariable Long id, @Valid @RequestBody Book book) {
        return service.update(id, book);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
```

### 변경 포인트
- `@Valid` 추가 — POST/PUT 본문 자동 검증
- 반환 타입 단순화 — 대부분 `ResponseEntity` 제거
- `@ResponseStatus(HttpStatus.NO_CONTENT)` — 메서드 레벨로 204 선언

### `@ResponseStatus` vs `ResponseEntity.noContent().build()`

```java
// 어느 쪽도 동등하게 204를 반환합니다
@ResponseStatus(HttpStatus.NO_CONTENT)
public void delete(@PathVariable Long id) { service.delete(id); }

// vs

public ResponseEntity<Void> delete(@PathVariable Long id) {
    service.delete(id);
    return ResponseEntity.noContent().build();
}
```

선택 기준:
- **항상 같은 상태 코드**면 `@ResponseStatus`가 깔끔
- **조건부**(예: 정상이면 200, 부분 성공이면 207)면 `ResponseEntity`

---

## ▶️ 빌드 & 실행

```bash
mvn -f e:/workspace/spring-tutorial/pom.xml clean compile
# → Compiling 10 source files (3개 신규 추가됨), BUILD SUCCESS

mvn -f e:/workspace/spring-tutorial/pom.xml spring-boot:run
```

---

## 🧪 검증 — 정상/검증실패/Not Found 모든 케이스

### A) 정상 POST (검증 통과)

```bash
curl -i -X POST http://localhost:8081/api/books \
  -H "Content-Type: application/json" \
  -d '{"title":"Refactoring","author":"Martin Fowler","publishedYear":2018}'
```

**기대 응답** — HTTP 201 + 생성된 책:
```json
{"id":5,"title":"Refactoring","author":"Martin Fowler","publishedYear":2018}
```

---

### B) 검증 실패 — 빈 title + null publishedYear

```bash
curl -i -X POST http://localhost:8081/api/books \
  -H "Content-Type: application/json" \
  -d '{"title":"","author":"X","publishedYear":null}'
```

**기대 응답** — HTTP 400 + 필드별 에러:
```json
{
  "timestamp":"2026-05-21T16:15:24.871",
  "status":400,
  "error":"Bad Request",
  "message":"Validation failed",
  "path":"/api/books",
  "fieldErrors":[
    {"field":"title","message":"공백일 수 없습니다"},
    {"field":"publishedYear","message":"널이어서는 안됩니다"}
  ]
}
```

> **에러 메시지가 한국어**로 자동 적용! Hibernate Validator는 시스템 로케일이나 `Accept-Language` 헤더를 보고 i18n 메시지 번들에서 자동 선택합니다.

---

### C) 검증 실패 — publishedYear 범위 초과

```bash
curl -i -X POST http://localhost:8081/api/books \
  -H "Content-Type: application/json" \
  -d '{"title":"X","author":"Y","publishedYear":99}'
```

**기대 응답** — HTTP 400:
```json
{"timestamp":"...","status":400,...,"fieldErrors":[
  {"field":"publishedYear","message":"1000 이상이어야 합니다"}
]}
```

---

### D) 검증 실패 — title 200자 초과

```bash
LONG_TITLE=$(printf 'A%.0s' {1..201})
curl -X POST http://localhost:8081/api/books \
  -H "Content-Type: application/json" \
  -d "{\"title\":\"$LONG_TITLE\",\"author\":\"X\",\"publishedYear\":2024}"
```

**기대 응답** — HTTP 400:
```json
{"...","fieldErrors":[
  {"field":"title","message":"크기가 0에서 200 사이여야 합니다"}
]}
```

---

### E) GET 없는 ID — 이제 JSON 본문 동봉 (Step 4와 비교!)

```bash
curl -i http://localhost:8081/api/books/999
```

**기대 응답** — HTTP 404 + 의미 있는 메시지:
```json
{
  "timestamp":"2026-05-21T16:15:44.530",
  "status":404,
  "error":"Not Found",
  "message":"Book not found: id=999",
  "path":"/api/books/999"
}
```

Step 4의 빈 본문 404와 비교하면 운영성 향상이 분명합니다.

---

### F) DELETE 기존 ID — 여전히 204

```bash
curl -i -X DELETE http://localhost:8081/api/books/2
# → HTTP 204, 본문 없음
```

`@ResponseStatus(HttpStatus.NO_CONTENT)`의 동작.

---

## ✅ 검증 체크리스트

- [ ] `pom.xml`에 `starter-validation` 추가됨
- [ ] `Book.java`에 검증 어노테이션 6개 (`@NotBlank` ×2, `@Size` ×2, `@NotNull`, `@Min`, `@Max`)
- [ ] `BookNotFoundException`, `ErrorResponse`, `GlobalExceptionHandler` 3개 신규 파일
- [ ] `BookService` Optional → 예외로 리팩터
- [ ] `BookController` @Valid 추가, Optional 분기 제거
- [ ] `mvn compile` → `Compiling 10 source files`, `BUILD SUCCESS`
- [ ] 정상 POST → 201
- [ ] 빈 title POST → 400 + `fieldErrors` 배열 (한국어 메시지)
- [ ] publishedYear=99 POST → 400 + Min 위반 메시지
- [ ] GET 없는 ID → 404 + JSON body (Step 4 빈 응답과 차이)
- [ ] DELETE 기존 ID → 204

---

## 📚 핵심 개념 정리

### 1. Bean Validation 표준 (JSR-380)

- **표준 명세** — `javax.validation.constraints.*`
- **구현체** — Hibernate Validator (Spring Boot 기본 포함)
- **자동 i18n** — 메시지 번들로 다국어 자동 적용
- **확장 가능** — 사용자 정의 어노테이션도 만들 수 있음

### 2. `@Valid` 트리거 위치

```java
public ResponseEntity<Book> create(@Valid @RequestBody Book book) {
```

`@Valid`가 붙은 파라미터를 만나면:
1. JSON → 객체 역직렬화 (Jackson)
2. **검증 실행** — 어노테이션 기반
3. 실패 시 → `MethodArgumentNotValidException` 자동 throw
4. (우리의 `GlobalExceptionHandler`가 잡아 400 응답으로 변환)

### 3. `@RestControllerAdvice` 동작 원리

- Spring이 기동 시 `@RestControllerAdvice` Bean을 모두 발견
- 컨트롤러에서 예외 발생 → DispatcherServlet이 `@ExceptionHandler` 검색
- 일치하는 핸들러 호출, 반환값이 응답
- **컨트롤러보다 더 구체적인 핸들러가 우선**

### 4. 예외 처리 우선순위

여러 `@ExceptionHandler`가 있을 때:
1. 같은 컨트롤러 안의 `@ExceptionHandler` (우선)
2. `@ControllerAdvice`의 `@ExceptionHandler`
3. 더 구체적인 예외 타입이 우선 (BookNotFoundException > RuntimeException > Exception)

### 5. 트랜잭션 + 예외

- `RuntimeException` → 트랜잭션 자동 rollback
- 체크 예외 → 기본 rollback **안 됨** (의외!)
- `@Transactional(rollbackFor = Exception.class)`로 명시 가능

→ **비즈니스 예외는 항상 `RuntimeException` 계열로** 만드는 것이 안전.

### 6. 에러 응답 표준화의 가치

- 클라이언트가 **일관된 포맷**으로 에러를 파싱 가능
- 로깅 시스템이 **동일 필드**(timestamp, path, status)로 분류 가능
- 모니터링/알람 규칙을 **재사용** 가능

---

## ⚠️ 자주 만나는 에러

### `@Valid`를 붙였는데 검증이 동작 안 함
**원인 1**: `spring-boot-starter-validation` 의존성 누락 (Spring Boot 2.3+).
**해결**: pom.xml에 명시적 추가.

**원인 2**: `@Valid`를 빠뜨림.
**해결**: `@Valid @RequestBody Book book`로 정확히 적기.

**원인 3**: 검증 어노테이션 import 잘못
- `javax.validation.constraints.NotBlank` ✅ (Bean Validation 표준)
- `org.hibernate.validator.constraints.NotBlank` ❌ (구버전, deprecated)

### 검증 실패 시 500 응답이 나옴
**원인**: `MethodArgumentNotValidException`을 잡는 `@ExceptionHandler` 없음.
**해결**: `GlobalExceptionHandler`에 추가 (또는 Spring Boot 2.3+의 기본 `ProblemDetail` 응답에 의존).

### `@RestControllerAdvice`가 동작 안 함
**원인**: Component Scan 범위 밖에 있음.
**해결**: 메인 클래스(`BookTutorialApplication`) 이하 패키지에 두기.

### 에러 메시지가 영어로 나옴
**원인**: 시스템 로케일이 영어 또는 `Accept-Language: en` 헤더.
**해결**:
- 시스템 로케일 변경, 또는
- Spring의 `LocaleResolver` 설정, 또는
- 메시지를 직접 지정: `@NotBlank(message = "제목은 필수입니다")`

### `BookNotFoundException`을 잡지 못함
**원인 1**: 사용자 정의 예외가 `Exception` 상속 (체크 예외) — 컴파일러가 throws 강제.
**해결**: `RuntimeException` 상속으로 변경.

**원인 2**: Controller 메서드 내에서 try-catch로 삼킴.
**해결**: 예외를 그대로 흘려보내기.

---

## 🔤 용어 사전 (이번 단계 신규)

| 용어 | 의미 |
|---|---|
| **Bean Validation** | Java 표준 검증 명세 (JSR-380) |
| **Hibernate Validator** | Bean Validation의 가장 유명한 구현체 |
| **`@Valid`** | 객체의 검증 어노테이션을 실제로 실행하라는 트리거 |
| **`@RestControllerAdvice`** | 모든 컨트롤러를 가로채는 전역 어드바이스 (REST 응답용) |
| **`@ExceptionHandler`** | 특정 예외를 잡는 메서드 마커 |
| **Checked Exception** | `Exception` 직접 상속 — `throws` 강제 |
| **Unchecked Exception** | `RuntimeException` 상속 — `throws` 불필요 |
| **DTO (Data Transfer Object)** | 계층 간 데이터 전달용 객체 (Entity와 분리됨) |
| **Immutable Object** | 한 번 생성되면 상태가 변하지 않는 객체 (`final` 필드) |
| **AOP (Aspect-Oriented Programming)** | 횡단 관심사를 분리하는 패러다임 — `@RestControllerAdvice`의 기반 |

---

## 🧠 한 번 더 강조 — Step 4 → Step 5의 코드 단순화

### Controller `get` 메서드 변화

```java
// Step 4: Optional 분기 직접 처리
@GetMapping("/{id}")
public ResponseEntity<Book> get(@PathVariable Long id) {
    return service.findOne(id)
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.notFound().build());
}

// Step 5: 도메인 흐름만 표현
@GetMapping("/{id}")
public Book get(@PathVariable Long id) {
    return service.findOne(id);
}
```

**코드 줄 수가 줄었을 뿐 아니라** — 이 컨트롤러는 더 이상 "없으면 404"를 알 필요가 없습니다.
그건 도메인 규칙이고, `BookService` + `GlobalExceptionHandler`가 책임집니다.

### 예외 기반 흐름의 이점

- **Controller**: HTTP 변환에만 집중
- **Service**: 비즈니스 규칙 표현 ("없으면 예외")
- **Handler**: HTTP 매핑 책임을 한곳에 집중

→ **각 계층이 자기 일에만 집중**.

---

## 🎉 튜토리얼 완주!

Step 1부터 5까지 진행하며 익힌 것:

| 영역 | 익힌 것 |
|---|---|
| **Spring Boot 기초** | `@SpringBootApplication`, Starter, Embedded Tomcat, Auto-Configuration |
| **Spring MVC** | `@RestController`, `@GetMapping` 등 5종, `@PathVariable`, `@RequestParam`, `@RequestBody` |
| **응답 제어** | `ResponseEntity`, `@ResponseStatus`, Jackson 자동 변환 |
| **Spring Data JPA** | `@Entity`, `@Id`, `@GeneratedValue`, `JpaRepository`, 메서드 이름 쿼리, Dirty Checking |
| **DB & 설정** | H2 인메모리, `application.yml`, `ddl-auto`, H2 콘솔 |
| **계층 분리** | Controller → Service → Repository, `@Service`, `@Component`, 생성자 주입 |
| **트랜잭션** | `@Transactional`, `readOnly`, RuntimeException 자동 rollback |
| **검증** | Bean Validation (`@NotBlank`, `@Size`, `@Min` 등), `@Valid` |
| **예외 처리** | 사용자 정의 예외, `@RestControllerAdvice`, `@ExceptionHandler` |
| **REST 모범 사례** | HTTP 메서드 의미, 상태 코드(200/201/204/400/404), Location 헤더 |

총 코드 약 **300줄**로 운영 환경에서도 부끄럽지 않은 REST API가 완성됐습니다.

---

## 🚀 여기서 더 나아가려면

이 튜토리얼은 핵심만 다뤘습니다. 다음 주제를 직접 시도해보세요:

1. **DTO 분리** — `Book` Entity를 직접 노출하지 말고 `BookRequest`/`BookResponse` DTO 사용
2. **DB 마이그레이션** — Flyway 또는 Liquibase로 스키마 버전 관리
3. **운영 DB 연동** — PostgreSQL / MySQL / Oracle (application.yml 설정 + JDBC 드라이버 의존성)
4. **테스트** — `@SpringBootTest`, `@DataJpaTest`, `MockMvc`로 단위/통합 테스트
5. **로깅** — Logback 설정, 구조화 로깅(JSON)
6. **보안** — Spring Security로 인증/권한
7. **API 문서화** — Springdoc OpenAPI로 Swagger UI 자동 생성
8. **메트릭/모니터링** — Spring Boot Actuator + Prometheus + Grafana
9. **컨테이너화** — Dockerfile + docker-compose
10. **CI/CD** — GitHub Actions로 자동 빌드/테스트

각 주제마다 별도 학습이 필요합니다. 천천히 하나씩.

---

## 📦 이 단계의 산출물

```
e:\workspace\spring-tutorial\
├─ pom.xml                                            ✏️ +5 줄
└─ src\main\java\com\example\book\
   ├─ Book.java                                       ✏️ +13 줄 (어노테이션)
   ├─ BookService.java                                ✏️ 리팩터 (-5 줄)
   ├─ BookController.java                             ✏️ 단순화 (-5 줄)
   ├─ BookNotFoundException.java                      ⭐ +15 줄
   ├─ ErrorResponse.java                              ⭐ +50 줄
   └─ GlobalExceptionHandler.java                     ⭐ +45 줄
```

**최종 전체 코드량: 약 300 줄.**

---

## 🏁 마무리

처음 빈 디렉토리에서 시작해 — 5단계만에 완전한 REST API가 됐습니다.

- 어노테이션 한 줄, 인터페이스 한 줄, 메서드 이름 하나로 동작하는 **Spring Boot의 본질**을 체험
- **계층 분리**의 가치와 책임 경계의 효용
- **표준에 맞춘 REST API** — HTTP 메서드/상태 코드/검증/에러 응답

이제 다른 도메인으로 같은 패턴을 적용해보세요. Book 대신 Order, User, Product 등 — 구조는 그대로입니다.

**축하합니다!** 🎉
