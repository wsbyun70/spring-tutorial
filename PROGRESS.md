# PROGRESS — 이어서 진행하기 위한 가이드

> **새 세션에서 이 프로젝트를 다시 열었을 때 가장 먼저 읽어야 할 문서.**
> 어디까지 했고, 다음에 무엇을 할지, 어떤 함정에 주의해야 할지 모두 여기에.

---

## 🎯 프로젝트 목표

Spring Boot를 처음 배우는 개발자를 위해 **도서(Book) CRUD REST API**를 단계별로 직접 만들어보는 튜토리얼.
**전체 5단계 중 3단계 완료**.

---

## ✅ 진행 완료 — Step 1 ~ 3

| Step | 주제 | 결과물 | 문서 |
|---|---|---|---|
| 1 | 프로젝트 골격 | `pom.xml` + `BookTutorialApplication` + `application.yml`. 빈 Spring Boot 서버 8081 기동 | [docs/step-01](docs/step-01-project-skeleton.md) |
| 2 | 첫 REST 엔드포인트 | `HelloController` — 4개 GET 엔드포인트 (`/hello`, `/hello/{name}`, `/greet`, `/info`) | [docs/step-02](docs/step-02-rest-endpoint.md) |
| 3 | JPA + H2 | `Book` 엔티티 + `BookRepository` + `DataSeeder` + H2 콘솔. 4권 샘플 데이터 자동 삽입 | [docs/step-03](docs/step-03-jpa-h2.md) |

### 현재 시점 검증 명령
```bash
# 컴파일 통과 확인
mvn -f e:/workspace/spring-tutorial/pom.xml compile
# → Compiling 5 source files, BUILD SUCCESS

# 서버 기동
mvn -f e:/workspace/spring-tutorial/pom.xml spring-boot:run
# → "Started BookTutorialApplication in X seconds"
# → "=== Loaded all books ===" 로그에 4권 표시
# → "=== findByAuthor('Robert C. Martin') ===" 로그에 2권 표시
```

---

## 🔜 다음 단계 — Step 4 (Service + CRUD)

### 학습 목표
- **3계층 아키텍처** — Controller → Service → Repository
- `@Service` — 비즈니스 로직 계층 분리
- `@RequestBody` — JSON 요청 본문 → POJO 자동 변환
- `@RequestMapping` 클래스 레벨 공통 경로
- `ResponseEntity` — HTTP 상태 코드 명시적 제어
- `@Transactional` — 명시적 트랜잭션 경계
- HTTP 메서드별 의미 (GET=조회, POST=생성, PUT=수정, DELETE=삭제)

### 만들 것
1. **`BookService.java`** — `@Service` 비즈니스 로직 계층
   - `create(Book)` / `findOne(Long)` / `findAll()` / `update(Long, Book)` / `delete(Long)`
   - `findOne` 시 없으면 예외 throw
2. **`BookController.java`** — `@RestController` REST 엔드포인트
   - `@RequestMapping("/api/books")` 클래스 레벨 공통 경로
   - 5개 메서드 (GET list/get, POST create, PUT update, DELETE delete)
   - `ResponseEntity`로 상태 코드 제어 (201 Created 등)
3. **`HelloController.java`** — 기존 그대로 유지 (학습 비교용)

### 예상 엔드포인트
| 메서드 | 경로 | 본문 | 응답 |
|---|---|---|---|
| GET | `/api/books` | — | 도서 전체 목록 (JSON 배열) |
| GET | `/api/books/{id}` | — | 단일 도서 (404 또는 200) |
| POST | `/api/books` | `{"title":"...","author":"...","publishedYear":...}` | 생성된 도서 + 201 Created |
| PUT | `/api/books/{id}` | 도서 JSON | 수정된 도서 |
| DELETE | `/api/books/{id}` | — | 204 No Content |

### 검증 시나리오
```bash
# 1) 목록 (4권)
curl http://localhost:8081/api/books

# 2) 단일 조회
curl http://localhost:8081/api/books/1

# 3) 신규 생성
curl -X POST http://localhost:8081/api/books \
  -H "Content-Type: application/json" \
  -d '{"title":"Refactoring","author":"Martin Fowler","publishedYear":2018}'

# 4) 수정
curl -X PUT http://localhost:8081/api/books/1 \
  -H "Content-Type: application/json" \
  -d '{"title":"Clean Code (2nd)","author":"Robert C. Martin","publishedYear":2024}'

# 5) 삭제
curl -X DELETE http://localhost:8081/api/books/2

# 6) 다시 목록 확인 (id=2가 빠지고 id=5가 추가됨)
curl http://localhost:8081/api/books
```

---

## 🔜 다음 다음 단계 — Step 5 (검증 + 예외 처리)

### 학습 목표
- **Bean Validation (`@Valid`)** — 입력 검증 자동화
- `@NotBlank`, `@NotNull`, `@Size`, `@Min`, `@Max` 등 검증 어노테이션
- `@ExceptionHandler` — 전역 예외 처리기
- `@RestControllerAdvice` — 모든 컨트롤러에 공통 적용
- 일관된 에러 응답 JSON 포맷
- HTTP 상태 코드 매핑 (400, 404, 500 등)

### 만들 것
1. `Book.java`에 검증 어노테이션 추가
2. `BookController`의 `@RequestBody`에 `@Valid` 추가
3. 신규 `GlobalExceptionHandler.java` — `@RestControllerAdvice`로 일관된 에러 응답
4. 사용자 정의 예외 (`BookNotFoundException`)

### 검증 시나리오
```bash
# 빈 title로 생성 시도 → 400 Bad Request + 에러 메시지 JSON
curl -X POST http://localhost:8081/api/books \
  -H "Content-Type: application/json" \
  -d '{"title":"","author":"X","publishedYear":2024}'

# 존재하지 않는 ID 조회 → 404 Not Found + 에러 JSON
curl http://localhost:8081/api/books/999
```

---

## 🚨 재개할 때 가장 먼저 확인할 것

### 1) 좀비 Java 프로세스 확인 (자주 발생!)
```bash
netstat -ano | grep ":8081"
# 결과가 있으면:
taskkill //F //PID <PID>
```
> Maven `spring-boot:run`을 Ctrl+C로 종료하면 자식 Java 프로세스가 남아 포트를 잡고 있을 때가 있습니다.

### 2) 빌드 상태 검증
```bash
mvn -f e:/workspace/spring-tutorial/pom.xml clean compile
```
"Compiling 5 source files / BUILD SUCCESS"가 나와야 정상.

### 3) 서버 기동 + 데이터 로딩 검증
```bash
mvn -f e:/workspace/spring-tutorial/pom.xml spring-boot:run
```
로그에서:
- `Started BookTutorialApplication`
- `=== Loaded all books ===` 4행
- `=== findByAuthor('Robert C. Martin') ===` 2행

모두 확인되면 OK.

---

## 🧩 핵심 설계 결정 (왜 이렇게 했는가)

| 결정 | 이유 |
|---|---|
| **포트 8081** | 기본 8080은 다른 프로젝트(flowstudio)가 점유 중 — 충돌 회피 |
| **Java 8** | 사용자 PC에 설치된 JDK 버전. Spring Boot 2.7.x가 마지막 Java 8 지원 라인 |
| **Spring Boot 2.7.18** | 최신 2.7 패치. Java 17 강제하는 3.x는 환경 호환성 이슈 회피 |
| **H2 인메모리** | 별도 DB 설치 불필요. 학습용으로 충분. 운영은 PostgreSQL/MySQL/Oracle 권장 |
| **`ddl-auto: create-drop`** | 매 기동마다 깨끗한 상태에서 시작. 운영 환경에서는 절대 사용 금지 |
| **`show-sql: true`** | 학습용 — Hibernate가 실행하는 SQL을 로그로 직접 볼 수 있음 |
| **`@Entity`에 `protected` 기본 생성자** | JPA 리플렉션 요구사항. `public`보다 안전 |
| **메서드 이름 기반 쿼리** (`findByAuthor`) | `@Query` 없이도 동작하는 마법을 체험시키기 위함 |
| **DataSeeder의 `CommandLineRunner`** | 기동 후 자동 실행되는 진입점. `@PostConstruct`보다 트랜잭션 초기화 후 호출되어 안전 |

---

## ⚠️ 알려진 함정 (학습자가 자주 겪는 것)

1. **Maven 자식 Java 프로세스 좀비** — 위 "재개할 때 확인" 1번 참조
2. **H2 콘솔의 JDBC URL 기본값** — `jdbc:h2:~/test`가 기본인데 우리 DB는 `jdbc:h2:mem:bookdb`. 반드시 수정해야 접속됨
3. **Windows curl에서 `&` 명령 잘림** — 쿼리스트링은 따옴표 필수: `curl "http://...?a=1&b=2"`
4. **`pom.xml` 1라인 "Unknown" 에러 (Eclipse)** — 닫힌 네트워크에서 XSD 다운로드 실패. 빌드와 무관. 거슬리면 `xmlns:xsi`, `xsi:schemaLocation` 제거
5. **`mvn` 명령 PATH 누락** — 절대 경로 `/e/maven/bin/mvn` 또는 `E:\maven\bin\mvn.cmd` 사용

---

## 🎓 학습자가 지금까지 익힌 핵심 개념

### Spring Boot 기초
- `@SpringBootApplication` = `@Configuration` + `@EnableAutoConfiguration` + `@ComponentScan`
- Starter 의존성으로 묶음 라이브러리 + 자동 버전 호환
- Embedded Tomcat (JAR 안에 내장)
- Convention over Configuration

### Spring MVC
- `@RestController` vs `@Controller`
- HTTP 메서드 매핑 (`@GetMapping` 등 5종)
- `@PathVariable`, `@RequestParam` (+ `defaultValue`)
- HttpMessageConverter (Jackson 자동 JSON 변환)
- DispatcherServlet 요청 처리 흐름

### Spring Data JPA
- `@Entity`, `@Id`, `@GeneratedValue`
- `JpaRepository<Entity, ID>` 인터페이스 상속만으로 CRUD 무료
- 메서드 이름 기반 쿼리 자동 생성
- `ddl-auto`, `show-sql` 등 JPA 설정
- 영속화 계층 (Repository) 분리

### 그 외
- `@Component` Bean 등록 + 생성자 주입
- `CommandLineRunner` 기동 후 코드 실행
- `application.yml` 외부 설정
- H2 인메모리 + 브라우저 콘솔

---

## 📋 Step 4 시작 시 첫 메시지 권장

이 프로젝트 새 세션에서 처음 열었다면 다음을 차례로 수행:

1. `README.md`와 이 `PROGRESS.md` 읽기
2. 위 "재개할 때 가장 먼저 확인할 것" 3단계 수행
3. Step 3 마지막 동작 (DataSeeder 로그)이 정상이면 → "Step 4 진행하자"로 시작

Claude(또는 다른 AI 어시스턴트) 사용 시:
> "spring-tutorial 프로젝트 Step 4 진행해줘. 끝나면 문서 작성하는 거 잊지 말고."

이렇게 말하면 위 "다음 단계 — Step 4" 섹션의 계획대로 진행될 것입니다.
