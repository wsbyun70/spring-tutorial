# Step 4 — Service 계층 + 완전한 CRUD REST API

> 이 단계가 끝나면 도서 관리에 필요한 **5개의 REST 엔드포인트**가 모두 동작합니다.
> 더 중요한 것은 **계층을 분리하는 이유**와 **HTTP 메서드별 의미**를 체득하는 것입니다.

---

## 🎯 이번 단계의 목표

- **3계층 아키텍처** 이해 — Controller → Service → Repository
- `@Service` — 비즈니스 로직 계층 분리
- `@RequestBody` — JSON 요청 본문 → Java 객체 자동 변환 (역직렬화)
- `@RequestMapping` 클래스 레벨 — 공통 경로 prefix
- `ResponseEntity<T>` — HTTP 상태 코드 + 헤더 명시적 제어
- `@Transactional` — 메서드 단위 트랜잭션 경계
- HTTP 메서드별 의미 — GET / POST / PUT / DELETE
- REST 모범 사례 — 201 Created + Location 헤더, 204 No Content

---

## 📋 사전 확인

- [Step 3](step-03-jpa-h2.md) 완료 (JPA + H2 + 4권 데이터 자동 삽입)
- 포트 8081 비어있음:
  ```bash
  netstat -ano | grep ":8081"
  ```
- `mvn compile`이 5개 소스를 통과시킴

---

## 📂 추가할 파일

```
src\main\java\com\example\book\
├─ BookTutorialApplication.java                       (변경 없음)
├─ HelloController.java                               (변경 없음 — Step 2 학습용)
├─ Book.java                                          (변경 없음)
├─ BookRepository.java                                (변경 없음)
├─ DataSeeder.java                                    (변경 없음)
├─ BookService.java                                   ⭐ 신규 — 비즈니스 로직
└─ BookController.java                                ⭐ 신규 — REST 엔드포인트
```

**파일 2개만 추가**하면 5개 엔드포인트가 완성됩니다.

---

## 💡 먼저 — 왜 Service 계층을 추가하나?

지금까지의 구조:
```
Controller → Repository → DB
```

문제점:
- 비즈니스 로직이 Controller에 섞이면 **HTTP 처리와 도메인 규칙이 한 클래스에 뒤엉킴**
- 테스트가 어려워짐 (Controller 테스트는 HTTP 컨텍스트 필요)
- 같은 비즈니스 로직을 다른 곳(예: 배치, 다른 컨트롤러)에서 재사용 어려움

3계층 아키텍처:
```
Controller → Service → Repository → DB
```

각 계층의 책임:
| 계층 | 책임 |
|---|---|
| **Controller** | HTTP 변환 (요청 ↔ 응답), 입력 검증, 상태 코드 결정 |
| **Service** | 비즈니스 로직, 트랜잭션 경계, 여러 Repository 조합 |
| **Repository** | DB 접근만 (CRUD, 쿼리) |

이 단계에선 비즈니스 로직이 단순하지만, **계층을 미리 분리해두면** 도메인이 복잡해져도 Controller가 비대해지지 않습니다.

---

## 📝 1단계 — `BookService.java` 작성

위치: `src\main\java\com\example\book\BookService.java`

```java
package com.example.book;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class BookService {

    private final BookRepository repository;

    public BookService(BookRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<Book> findAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Book> findOne(Long id) {
        return repository.findById(id);
    }

    public Book create(Book book) {
        return repository.save(book);
    }

    public Optional<Book> update(Long id, Book changes) {
        return repository.findById(id).map(existing -> {
            existing.setTitle(changes.getTitle());
            existing.setAuthor(changes.getAuthor());
            existing.setPublishedYear(changes.getPublishedYear());
            return existing;
        });
    }

    public boolean delete(Long id) {
        if (!repository.existsById(id)) {
            return false;
        }
        repository.deleteById(id);
        return true;
    }
}
```

### 주요 어노테이션

#### `@Service`
- "이 클래스는 **서비스 계층** Bean입니다"
- 사실 동작상으로는 `@Component`와 동일 — Bean 등록 + Component Scan
- **의도(intent)를 코드로 표현**: 이 클래스가 비즈니스 로직 계층임을 명확히
- AOP 등 추가 처리 시 `@Service`만 골라낼 수 있음

#### `@Transactional` (클래스 레벨)
- "이 클래스의 **모든 public 메서드는 트랜잭션 안에서 실행**됩니다"
- 메서드 진입 시 트랜잭션 시작 → 정상 종료 시 commit → 예외 발생 시 rollback
- DB 무결성을 자동으로 보장

#### `@Transactional(readOnly = true)` (메서드 레벨)
- 클래스 레벨 설정을 **메서드 레벨에서 override**
- 조회 메서드에 붙이면 **JPA 성능 최적화** — 더티 체크(변경 감지) 생략, flush 안 함
- 일부 DB는 별도 read replica에 라우팅 가능

### `update()` 메서드 — JPA의 마법

```java
public Optional<Book> update(Long id, Book changes) {
    return repository.findById(id).map(existing -> {
        existing.setTitle(changes.getTitle());
        existing.setAuthor(changes.getAuthor());
        existing.setPublishedYear(changes.getPublishedYear());
        return existing;   // ⭐ save() 호출 안 함!
    });
}
```

**`repository.save(existing)` 호출 안 했는데도 UPDATE 쿼리가 실행됩니다.**

이게 JPA의 **Dirty Checking (변경 감지)** 입니다:
1. `findById()`로 가져온 엔티티는 영속 상태(Persistent)에 진입
2. 영속 상태 엔티티의 필드를 setter로 변경
3. 트랜잭션 commit 시점에 JPA가 **변경된 필드를 감지**해서 자동 UPDATE 발행

> 같은 효과를 `repository.save(existing)` 명시 호출로도 얻을 수 있지만, **불필요한 호출**.
> 명시적 `save()`가 필요한 경우는 새 엔티티 생성(`create()`) 시뿐.

### `delete()` — `boolean` 반환

```java
public boolean delete(Long id) {
    if (!repository.existsById(id)) {
        return false;
    }
    repository.deleteById(id);
    return true;
}
```

- `existsById()`로 먼저 존재 여부 확인 → 없으면 `false`
- 컨트롤러는 이 boolean을 보고 404를 줄지 204를 줄지 결정

> **왜 그냥 `deleteById()` 호출하지 않는가?**
> 존재하지 않는 ID에 `deleteById()` 호출 시 Spring Data가 `EmptyResultDataAccessException`을 던집니다.
> 명시적 체크가 더 깔끔하고 의도가 분명합니다.

---

## 📝 2단계 — `BookController.java` 작성

위치: `src\main\java\com\example\book\BookController.java`

```java
package com.example.book;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public ResponseEntity<Book> get(@PathVariable Long id) {
        return service.findOne(id)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Book> create(@RequestBody Book book) {
        Book created = service.create(book);
        return ResponseEntity
            .created(URI.create("/api/books/" + created.getId()))
            .body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Book> update(@PathVariable Long id, @RequestBody Book book) {
        return service.update(id, book)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        return service.delete(id)
            ? ResponseEntity.noContent().build()
            : ResponseEntity.notFound().build();
    }
}
```

### 주요 어노테이션

#### `@RequestMapping("/api/books")` (클래스 레벨)
- 이 컨트롤러의 **모든 메서드에 `/api/books` prefix 자동 적용**
- 각 메서드는 그 아래 경로만 명시 (`""`, `"/{id}"` 등)

| 메서드 | 어노테이션 | 실제 URL |
|---|---|---|
| `list` | `@GetMapping` (경로 생략) | `GET /api/books` |
| `get` | `@GetMapping("/{id}")` | `GET /api/books/{id}` |
| `create` | `@PostMapping` | `POST /api/books` |
| `update` | `@PutMapping("/{id}")` | `PUT /api/books/{id}` |
| `delete` | `@DeleteMapping("/{id}")` | `DELETE /api/books/{id}` |

> **`@RestController`는 클래스 레벨에서만**, `@RequestMapping`은 클래스 + 메서드 둘 다 가능.

#### `@RequestBody`
```java
public ResponseEntity<Book> create(@RequestBody Book book) {
```
- HTTP 요청 본문(JSON)을 **Java 객체로 자동 역직렬화**
- Jackson이 JSON 키 ↔ Book의 setter 매핑
- Step 2의 응답 직렬화(Java → JSON)의 **반대 방향**

요청:
```json
{"title":"Refactoring","author":"Martin Fowler","publishedYear":2018}
```
→ Jackson이 자동으로:
1. 기본 생성자로 `new Book()`
2. `setTitle("Refactoring")` 호출
3. `setAuthor("Martin Fowler")` 호출
4. `setPublishedYear(2018)` 호출
→ 메서드에 `Book` 객체로 주입

> **id 필드는 어떻게?**
> 요청 JSON에 `id`가 없으면 setter가 호출되지 않아 `null` 그대로. JPA가 INSERT 시 DB가 자동 할당.

---

### 🌟 `ResponseEntity<T>` — HTTP 응답을 완전 제어

지금까지(Step 2)는 메서드가 `String`, `Map`을 직접 반환했고, Spring이 알아서 200 OK + 본문으로 만들어줬습니다. 단순한 케이스엔 그게 좋습니다.

하지만 REST API에서는 **상태 코드를 정확히 구분**하는 것이 중요합니다:

| 상황 | HTTP 코드 | 의미 |
|---|---|---|
| 정상 조회 | 200 OK | 결과를 본문에 포함 |
| 정상 생성 | **201 Created** | 새 리소스 생성됨, **Location 헤더에 경로** |
| 정상 삭제 | **204 No Content** | 성공했지만 응답 본문 없음 |
| 리소스 없음 | **404 Not Found** | 요청한 리소스 존재 안 함 |

`ResponseEntity`로 이 모두를 표현:

```java
// 200 OK + 본문
return ResponseEntity.ok(book);

// 201 Created + Location 헤더 + 본문
return ResponseEntity.created(URI.create("/api/books/5")).body(book);

// 204 No Content (본문 없음)
return ResponseEntity.noContent().build();

// 404 Not Found
return ResponseEntity.notFound().build();

// 직접 구성
return ResponseEntity.status(HttpStatus.ACCEPTED)
    .header("X-Custom", "value")
    .body(book);
```

### `Optional` + `ResponseEntity` 패턴

```java
@GetMapping("/{id}")
public ResponseEntity<Book> get(@PathVariable Long id) {
    return service.findOne(id)
        .map(ResponseEntity::ok)                          // Optional이 있으면 200
        .orElseGet(() -> ResponseEntity.notFound().build()); // 없으면 404
}
```

- Service가 `Optional<Book>` 반환 → 컨트롤러가 200/404로 변환
- 함수형 스타일: `.map().orElseGet()`
- `orElse` 대신 `orElseGet`을 쓰는 이유: `notFound().build()`는 **항상 호출되지 않아야** 효율적

> **`orElse` vs `orElseGet` (Spring과 무관, Java 기초)**
> ```java
> .orElse(ResponseEntity.notFound().build())       // 항상 평가됨 (낭비)
> .orElseGet(() -> ResponseEntity.notFound().build())  // 빈 Optional일 때만 평가됨
> ```
> Optional이 비어있지 않으면(=대부분의 경우) `orElseGet`이 조금 더 효율적.

---

## ▶️ 빌드 & 실행

```bash
mvn -f e:/workspace/spring-tutorial/pom.xml compile
# → Compiling 7 source files, BUILD SUCCESS

mvn -f e:/workspace/spring-tutorial/pom.xml spring-boot:run
# → Started BookTutorialApplication
# → DataSeeder가 4권 자동 삽입 (Step 3 그대로)
```

---

## 🧪 검증 — 5개 엔드포인트 × 정상/예외 케이스

### 1) 전체 목록 (4권)

```bash
curl http://localhost:8081/api/books
```
**기대 응답** (HTTP 200):
```json
[
  {"id":1,"title":"Clean Code","author":"Robert C. Martin","publishedYear":2008},
  {"id":2,"title":"Effective Java","author":"Joshua Bloch","publishedYear":2017},
  {"id":3,"title":"Clean Architecture","author":"Robert C. Martin","publishedYear":2017},
  {"id":4,"title":"Spring in Action","author":"Craig Walls","publishedYear":2022}
]
```

### 2) 단일 조회 (성공)

```bash
curl -i http://localhost:8081/api/books/1
```
**기대 응답** (HTTP 200):
```
HTTP/1.1 200 
Content-Type: application/json

{"id":1,"title":"Clean Code","author":"Robert C. Martin","publishedYear":2008}
```

### 3) 단일 조회 (없는 ID)

```bash
curl -i http://localhost:8081/api/books/999
```
**기대 응답** (HTTP 404):
```
HTTP/1.1 404 
Content-Length: 0
```
→ **본문 없음**, 상태 코드만.

### 4) 신규 생성

```bash
curl -i -X POST http://localhost:8081/api/books \
  -H "Content-Type: application/json" \
  -d '{"title":"Refactoring","author":"Martin Fowler","publishedYear":2018}'
```
**기대 응답** (HTTP 201):
```
HTTP/1.1 201 
Location: /api/books/5         ← ⭐ 자동 생성된 리소스의 URL
Content-Type: application/json

{"id":5,"title":"Refactoring","author":"Martin Fowler","publishedYear":2018}
```

> `id`가 요청 본문에 없었지만 **DB가 자동 할당한 값(5)으로 응답**됨. JPA의 `@GeneratedValue` 동작.

### 5) 수정 (성공)

```bash
curl -i -X PUT http://localhost:8081/api/books/1 \
  -H "Content-Type: application/json" \
  -d '{"title":"Clean Code (2nd ed.)","author":"Robert C. Martin","publishedYear":2024}'
```
**기대 응답** (HTTP 200):
```json
{"id":1,"title":"Clean Code (2nd ed.)","author":"Robert C. Martin","publishedYear":2024}
```

> **`save()`를 호출하지 않았는데도 UPDATE가 실행**됩니다. JPA Dirty Checking의 결과.

### 6) 수정 (없는 ID)

```bash
curl -i -X PUT http://localhost:8081/api/books/999 \
  -H "Content-Type: application/json" \
  -d '{"title":"X","author":"Y","publishedYear":2024}'
```
**기대 응답** (HTTP 404):
```
HTTP/1.1 404 
```

### 7) 삭제 (성공)

```bash
curl -i -X DELETE http://localhost:8081/api/books/2
```
**기대 응답** (HTTP 204):
```
HTTP/1.1 204 
```
→ **본문 없음** (No Content). 성공했지만 클라이언트에게 돌려줄 데이터가 없음.

### 8) 삭제 (없는 ID)

```bash
curl -i -X DELETE http://localhost:8081/api/books/999
```
**기대 응답** (HTTP 404).

### 9) 최종 목록 — 변경 누적 확인

```bash
curl http://localhost:8081/api/books
```
**기대 응답** — id=1은 수정됨, id=2는 삭제됨, id=5는 추가됨:
```json
[
  {"id":1,"title":"Clean Code (2nd ed.)","author":"Robert C. Martin","publishedYear":2024},
  {"id":3,"title":"Clean Architecture","author":"Robert C. Martin","publishedYear":2017},
  {"id":4,"title":"Spring in Action","author":"Craig Walls","publishedYear":2022},
  {"id":5,"title":"Refactoring","author":"Martin Fowler","publishedYear":2018}
]
```

---

## ✅ 검증 체크리스트

- [ ] `BookService.java`, `BookController.java` 파일 작성 완료
- [ ] `mvn compile` → `Compiling 7 source files`, `BUILD SUCCESS`
- [ ] 서버 기동 성공
- [ ] `GET /api/books` → 4권 JSON 배열 (200)
- [ ] `GET /api/books/1` → 단일 객체 (200)
- [ ] `GET /api/books/999` → 본문 없음 (404)
- [ ] `POST /api/books` → 새 객체 + `Location: /api/books/5` 헤더 (201)
- [ ] `PUT /api/books/1` → 수정된 객체 (200)
- [ ] `PUT /api/books/999` → 404
- [ ] `DELETE /api/books/2` → 본문 없음 (204)
- [ ] `DELETE /api/books/999` → 404
- [ ] 최종 목록에서 변경이 누적 반영됨

---

## 📚 핵심 개념 정리

### 1. 3계층 아키텍처와 의존성 방향

```
        Controller (HTTP)
            ↓ 호출
         Service (비즈니스 로직)
            ↓ 호출
        Repository (DB)
            ↓ 사용
         Database
```

**의존성은 한 방향**으로만:
- Controller가 Service를 안다 (✅)
- Service가 Repository를 안다 (✅)
- Repository는 Service를 모른다 (✅)
- Service는 Controller를 모른다 (✅)

이걸 어기면 **순환 의존**이 생기고 구조가 무너집니다.

### 2. HTTP 메서드별 의미 (REST 원칙)

| 메서드 | 의미 | 멱등성 | 안전성 |
|---|---|---|---|
| **GET** | 조회 | 예 | 예 (데이터 변경 없음) |
| **POST** | 생성 | 아니오 | 아니오 |
| **PUT** | 전체 수정 (덮어쓰기) | 예 | 아니오 |
| **PATCH** | 부분 수정 | 케바케 | 아니오 |
| **DELETE** | 삭제 | 예 | 아니오 |

- **멱등성** — 같은 요청을 여러 번 보내도 결과가 같음. PUT을 100번 호출해도 동일.
- **안전성** — 서버 상태를 변경하지 않음. GET은 캐싱 가능.

### 3. HTTP 상태 코드 분류

| 범위 | 의미 | 예시 |
|---|---|---|
| **2xx** | 성공 | 200 OK, 201 Created, 204 No Content |
| **3xx** | 리다이렉트 | 301 Moved, 302 Found |
| **4xx** | 클라이언트 오류 | 400 Bad Request, 401 Unauthorized, 404 Not Found |
| **5xx** | 서버 오류 | 500 Internal Server Error, 503 Service Unavailable |

→ **REST API는 적절한 상태 코드로 의도를 정확히 전달**해야 합니다.

### 4. JPA Dirty Checking (변경 감지)

```java
@Transactional
public Optional<Book> update(Long id, Book changes) {
    return repository.findById(id).map(existing -> {
        existing.setTitle(changes.getTitle());   // 영속 상태 엔티티의 필드 변경
        return existing;                          // save() 안 호출!
    });
}
// → 트랜잭션 commit 시점에 JPA가 UPDATE 자동 실행
```

엔티티 상태 다이어그램:
- **Transient (비영속)** — `new Book(...)` 직후, JPA가 모름
- **Persistent (영속)** — `save()` 또는 `findById()` 후, JPA가 추적 중
- **Detached (준영속)** — 트랜잭션 종료 후
- **Removed (삭제)** — `delete()` 호출 후

**영속 상태에서만** Dirty Checking이 작동합니다.

### 5. 트랜잭션 전파 (`@Transactional`의 동작)

- 메서드 진입 시 → 트랜잭션 시작 (또는 기존 트랜잭션에 합류)
- 정상 종료 → commit
- **RuntimeException 발생 시** → rollback (체크 예외는 기본 rollback 안 함!)

> Spring의 트랜잭션은 **AOP 프록시**로 동작합니다.
> 같은 클래스 안의 메서드 호출(`this.someMethod()`)은 프록시를 거치지 않아 트랜잭션이 적용 안 될 수 있음.
> 핵심 함정 — 다른 Bean을 거치거나 `@Transactional` 어노테이션을 호출되는 메서드에 직접 붙여야 함.

---

## ⚠️ 자주 만나는 에러

### "HttpMessageNotReadableException: Required request body is missing"
```
POST /api/books  (본문 없음)
→ 400 Bad Request
```
**원인**: `Content-Type: application/json` 헤더 누락 또는 본문 자체가 비어있음.
**해결**: curl에 `-H "Content-Type: application/json" -d '{"...":"..."}'` 정확히 지정.

### "JSON parse error: Cannot construct instance of `Book`"
**원인**: Book 클래스에 Jackson이 호출할 **기본 생성자가 없음** 또는 접근 불가.
**해결**: Book에 `public Book() {}` 또는 `protected Book() {}` 추가 (Step 3에서 추가 완료).

### "Failed to convert value of type 'String' to 'Long'"
```
GET /api/books/abc
→ 400 Bad Request
```
**원인**: `@PathVariable Long id`인데 URL에 숫자가 아닌 값.
**해결**: 자동 발생하는 400 응답이 정상 동작. 별도 처리 불필요.

### POST 응답에 `id`가 `null`로 옴
**원인**: `@GeneratedValue` 누락 또는 `repository.save()` 호출 안 함.
**해결**: Book.java에 `@GeneratedValue(strategy = GenerationType.IDENTITY)` 확인.

### `update()`에서 setter 호출했는데 DB 반영 안 됨
**원인**:
1. 메서드에 `@Transactional`이 없음 → 영속 상태가 즉시 끝남
2. 또는 같은 클래스 안에서 `this.update()` 호출 → 프록시 우회로 트랜잭션 미적용
**해결**: `@Transactional` 확인, 또는 외부 Bean에서 호출.

### "Cannot delete or update a parent row: foreign key constraint fails"
**원인**: 다른 테이블에서 FK로 참조 중인 row를 삭제하려 함. (이번 단계엔 해당 없음)
**해결**: 자식 row 먼저 삭제, 또는 cascade 설정.

---

## 🔤 용어 사전 (이번 단계 신규)

| 용어 | 의미 |
|---|---|
| **3-Tier Architecture** | Controller / Service / Repository 3계층 분리 |
| **Service Layer** | 비즈니스 로직 담당 계층 (`@Service`) |
| **REST** | Representational State Transfer — HTTP 자원 중심의 API 설계 원칙 |
| **CRUD** | Create / Read / Update / Delete — 4가지 기본 데이터 조작 |
| **Idempotent (멱등)** | 같은 요청을 여러 번 보내도 결과가 동일 (GET, PUT, DELETE) |
| **Serialization (직렬화)** | Java 객체 → JSON 등 텍스트 (응답) |
| **Deserialization (역직렬화)** | JSON 등 텍스트 → Java 객체 (요청 본문) |
| **Dirty Checking** | JPA가 영속 상태 엔티티의 변경을 자동 감지 → UPDATE 발행 |
| **Persistence Context** | JPA가 관리하는 엔티티 캐시 (트랜잭션 범위) |
| **Optional<T>** | "값이 있을 수도 없을 수도 있다"는 의도를 타입으로 표현 (Java 8+) |
| **AOP Proxy** | 어노테이션(@Transactional 등) 동작을 위해 Spring이 런타임에 만드는 프록시 객체 |

---

## 🧠 한 번 더 강조 — 우리가 만든 것 vs Spring이 자동으로 한 것

이 단계에서 **우리가 작성한 코드**:
- BookService.java (45 줄)
- BookController.java (55 줄)
- **총 100 줄**

이 단계에서 **Spring Boot가 자동으로 한 것**:
- BookService Bean 등록 + Repository 의존성 자동 주입
- BookController Bean 등록 + Service 의존성 자동 주입
- 5개 URL 라우팅 테이블 구축
- HTTP 메서드 파싱, 경로 파싱, PathVariable 추출
- 요청 본문 JSON → Book 객체 자동 역직렬화 (Jackson)
- 응답 객체 → JSON 자동 직렬화 (Jackson)
- ResponseEntity → HTTP 응답 헤더/상태 코드 구성
- 트랜잭션 매니저로 메서드별 트랜잭션 경계 관리
- JPA Dirty Checking → 자동 UPDATE SQL 생성
- 404 응답 시 Content-Length 0 자동 설정

---

## ➡️ 다음 단계 미리보기 — Step 5

**검증(Validation) + 예외 처리**

현재 API의 약점:
- `POST {"title":"","author":"","publishedYear":null}` 같이 **잘못된 데이터도 그대로 저장됨**
- 404 응답에 **어떤 ID가 없었는지** 정보 없음
- DB 에러 등이 발생하면 500 응답에 **스택 트레이스가 노출**됨

Step 5에서 추가할 것:
```java
// Book 클래스에 검증 어노테이션
public class Book {
    @NotBlank
    @Size(min = 1, max = 200)
    private String title;
    
    @NotBlank
    private String author;
    
    @NotNull @Min(1000) @Max(9999)
    private Integer publishedYear;
}

// 컨트롤러에 @Valid
public ResponseEntity<Book> create(@Valid @RequestBody Book book) { ... }

// 전역 예외 처리기
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handle(...) { ... }
}
```

→ 잘못된 요청은 **400 + 구조화된 에러 응답**, 없는 리소스는 **404 + 식별 가능한 메시지**.

---

## 📦 이 단계의 산출물

```
e:\workspace\spring-tutorial\
├─ pom.xml                                            (변경 없음)
├─ src\main\java\com\example\book\
│  ├─ BookTutorialApplication.java                    (변경 없음)
│  ├─ HelloController.java                            (변경 없음)
│  ├─ Book.java                                       (변경 없음)
│  ├─ BookRepository.java                             (변경 없음)
│  ├─ DataSeeder.java                                 (변경 없음)
│  ├─ BookService.java                                ⭐ 신규 +45 줄
│  └─ BookController.java                             ⭐ 신규 +55 줄
└─ src\main\resources\
   └─ application.yml                                 (변경 없음)
```

**추가 코드량: 약 100 줄**. 완전한 도서 CRUD REST API 5개 엔드포인트가 동작합니다.

---

## 🎉 이 시점에서 가능한 일

이제 다음과 같은 일을 할 수 있습니다:

1. **Postman / curl로 REST API 호출** — 모든 표준 CRUD 동작
2. **간단한 프론트엔드와 연동** — React/Vue/Angular에서 fetch API로 호출 가능
3. **H2 콘솔에서 SQL로 직접 검증** — 컨트롤러로 추가한 데이터가 즉시 DB에 반영됨

**여전히 부족한 것**:
- 잘못된 입력 거절 → Step 5에서 추가
- 의미 있는 에러 메시지 → Step 5에서 추가
- 운영 환경 DB 연결 (Oracle/PostgreSQL) → 튜토리얼 범위 밖
- 인증/권한 → 튜토리얼 범위 밖
