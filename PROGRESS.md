# PROGRESS — 튜토리얼 완주 + 다음 학습 가이드

> **이 프로젝트는 5단계 모두 완료된 상태입니다.** ✅
> 더 진행할 학습 주제는 마지막 섹션 "여기서 더 나아가려면" 참조.

---

## 🎯 프로젝트 목표

Spring Boot를 처음 배우는 개발자를 위해 **도서(Book) CRUD REST API**를 단계별로 직접 만들어보는 한글 튜토리얼.
**전체 5단계 모두 완료**. 약 300줄의 코드로 운영 환경에서도 부끄럽지 않은 REST API가 완성됐습니다.

---

## ✅ 완료된 단계 — Step 1 ~ 5

| Step | 주제 | 결과물 | 문서 |
|---|---|---|---|
| 1 | 프로젝트 골격 | `pom.xml` + `BookTutorialApplication` + `application.yml`. 빈 Spring Boot 서버 8081 기동 | [docs/step-01](docs/step-01-project-skeleton.md) |
| 2 | 첫 REST 엔드포인트 | `HelloController` — 4개 GET 엔드포인트 (`/hello`, `/hello/{name}`, `/greet`, `/info`) | [docs/step-02](docs/step-02-rest-endpoint.md) |
| 3 | JPA + H2 | `Book` 엔티티 + `BookRepository` + `DataSeeder` + H2 콘솔. 4권 샘플 데이터 자동 삽입 | [docs/step-03](docs/step-03-jpa-h2.md) |
| 4 | Service 계층 + CRUD | `BookService` + `BookController` — 5개 REST 엔드포인트 (list/get/create/update/delete) | [docs/step-04](docs/step-04-service-crud.md) |
| 5 | 검증 + 예외 처리 | `@Valid` Bean Validation + `BookNotFoundException` + `GlobalExceptionHandler` — 일관된 에러 응답 | [docs/step-05](docs/step-05-validation-exception.md) |

### 최종 산출물

```
src/main/java/com/example/book/
├─ BookTutorialApplication.java     ← 진입점 (Step 1)
├─ HelloController.java              ← 학습용 인사 엔드포인트 (Step 2)
├─ Book.java                         ← Entity + 검증 (Step 3, 5)
├─ BookRepository.java               ← JpaRepository (Step 3)
├─ DataSeeder.java                   ← 초기 데이터 (Step 3)
├─ BookService.java                  ← 비즈니스 로직 (Step 4, 5)
├─ BookController.java               ← REST CRUD (Step 4, 5)
├─ BookNotFoundException.java        ← 사용자 정의 예외 (Step 5)
├─ ErrorResponse.java                ← 에러 응답 DTO (Step 5)
└─ GlobalExceptionHandler.java       ← 전역 예외 처리 (Step 5)
```

---

## 🧪 전체 동작 검증 명령

```bash
# 컴파일
mvn -f e:/workspace/spring-tutorial/pom.xml clean compile
# → Compiling 10 source files, BUILD SUCCESS

# 기동
mvn -f e:/workspace/spring-tutorial/pom.xml spring-boot:run
# → Started BookTutorialApplication
# → DataSeeder가 4권 자동 삽입

# 정상 시나리오
curl http://localhost:8081/api/books                                        # 200 + 4권
curl http://localhost:8081/api/books/1                                       # 200 + 단일
curl -X POST http://localhost:8081/api/books -H "Content-Type: application/json" \
  -d '{"title":"Refactoring","author":"Martin Fowler","publishedYear":2018}' # 201 + Location
curl -X DELETE http://localhost:8081/api/books/2                             # 204

# 에러 시나리오 (Step 5에서 추가)
curl http://localhost:8081/api/books/999                                     # 404 + JSON
curl -X POST http://localhost:8081/api/books -H "Content-Type: application/json" \
  -d '{"title":"","author":"X","publishedYear":null}'                        # 400 + fieldErrors
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
"Compiling 10 source files / BUILD SUCCESS"가 나와야 정상.

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
| **`@Entity`에 `protected` 기본 생성자** | JPA 리플렉션 요구사항. Jackson도 protected 허용 |
| **메서드 이름 기반 쿼리** (`findByAuthor`) | `@Query` 없이도 동작하는 마법을 체험시키기 위함 |
| **DataSeeder의 `CommandLineRunner`** | 기동 후 자동 실행되는 진입점. `@PostConstruct`보다 트랜잭션 초기화 후 호출되어 안전 |
| **`BookNotFoundException` extends `RuntimeException`** | Spring 트랜잭션 자동 rollback + throws 강제 없음 |
| **`@JsonInclude(NON_NULL)`** in ErrorResponse | `fieldErrors:null` 노이즈 제거 |
| **사용자 정의 예외 → GlobalExceptionHandler 일원화** | Controller에서 Optional 분기 제거, 책임 분리 |

---

## ⚠️ 알려진 함정 (학습자가 자주 겪는 것)

1. **Maven 자식 Java 프로세스 좀비** — 위 "재개할 때 확인" 1번 참조
2. **H2 콘솔의 JDBC URL 기본값** — `jdbc:h2:~/test`가 기본인데 우리 DB는 `jdbc:h2:mem:bookdb`. 반드시 수정해야 접속됨
3. **Windows curl에서 `&` 명령 잘림** — 쿼리스트링은 따옴표 필수: `curl "http://...?a=1&b=2"`
4. **`pom.xml` 1라인 "Unknown" 에러 (Eclipse)** — 닫힌 네트워크에서 XSD 다운로드 실패. 빌드와 무관. 거슬리면 `xmlns:xsi`, `xsi:schemaLocation` 제거
5. **`mvn` 명령 PATH 누락** — 절대 경로 `/e/maven/bin/mvn` 또는 `E:\maven\bin\mvn.cmd` 사용
6. **`@Valid` 침묵 — Spring Boot 2.3+** — `starter-validation`을 명시 추가 안 하면 검증이 조용히 동작 안 함
7. **검증 어노테이션 import 혼동** — `javax.validation.constraints.*` ✅, `org.hibernate.validator.constraints.*` ❌ (deprecated)

---

## 🎓 학습자가 익힌 핵심 개념 (전 단계 누적)

### Spring Boot 기초 (Step 1)
- `@SpringBootApplication` = `@Configuration` + `@EnableAutoConfiguration` + `@ComponentScan`
- Starter 의존성으로 묶음 라이브러리 + 자동 버전 호환
- Embedded Tomcat (JAR 안에 내장)
- Convention over Configuration

### Spring MVC (Step 2)
- `@RestController` vs `@Controller`
- HTTP 메서드 매핑 (`@GetMapping` 등 5종)
- `@PathVariable`, `@RequestParam` (+ `defaultValue`)
- HttpMessageConverter (Jackson 자동 JSON 변환)
- DispatcherServlet 요청 처리 흐름

### Spring Data JPA (Step 3)
- `@Entity`, `@Id`, `@GeneratedValue`
- `JpaRepository<Entity, ID>` 인터페이스 상속만으로 CRUD 무료
- 메서드 이름 기반 쿼리 자동 생성
- `ddl-auto`, `show-sql` 등 JPA 설정
- 영속화 계층 (Repository) 분리

### Service 계층 + REST 모범 사례 (Step 4)
- 3계층 아키텍처 (Controller → Service → Repository)
- `@Service` + `@Transactional` (+ `readOnly` 최적화)
- `@RequestBody` JSON → Java 역직렬화
- `ResponseEntity` HTTP 상태 코드 + 헤더 제어
- HTTP 메서드별 의미 (멱등성, 안전성)
- JPA Dirty Checking (save 없이 UPDATE 자동 발행)

### 검증 + 예외 처리 (Step 5)
- Bean Validation (`@NotBlank`, `@NotNull`, `@Size`, `@Min`, `@Max`)
- `@Valid` 트리거 위치
- 사용자 정의 예외 (`RuntimeException` 상속)
- `@RestControllerAdvice` + `@ExceptionHandler`
- 일관된 에러 응답 DTO
- `@JsonInclude(NON_NULL)`, `@ResponseStatus`

---

## 🚀 여기서 더 나아가려면 (다음 학습 주제)

이 튜토리얼은 핵심만 다뤘습니다. 다음 주제는 별도 학습 단위:

| 주제 | 학습 효과 | 예상 분량 |
|---|---|---|
| **DTO 분리** | Entity를 직접 노출하지 말고 `BookRequest`/`BookResponse` DTO 사용 — API 계약과 DB 모델 분리 | 1~2일 |
| **DB 마이그레이션** | Flyway 또는 Liquibase로 스키마 버전 관리 (`ddl-auto` 의존성 탈피) | 1일 |
| **운영 DB 연동** | PostgreSQL / MySQL / Oracle (application.yml 설정 + JDBC 드라이버 의존성) | 1~2일 |
| **테스트 작성** | `@SpringBootTest`, `@DataJpaTest`, `MockMvc`로 단위/통합 테스트 | 1주 |
| **로깅** | Logback 설정, 구조화 로깅(JSON), MDC | 1일 |
| **Spring Security** | 인증/권한, JWT, OAuth2 | 1~2주 |
| **API 문서화** | Springdoc OpenAPI로 Swagger UI 자동 생성 | 1일 |
| **메트릭/모니터링** | Spring Boot Actuator + Prometheus + Grafana | 1주 |
| **컨테이너화** | Dockerfile + docker-compose | 1일 |
| **CI/CD** | GitHub Actions로 자동 빌드/테스트/배포 | 1주 |

각 주제는 독립적이라 관심 있는 것부터 시작하면 됩니다.

---

## 📋 새 세션에서 이 프로젝트 다시 열기

1. `README.md`와 이 `PROGRESS.md` 읽기
2. 위 "재개할 때 가장 먼저 확인할 것" 3단계 수행
3. 모두 정상이면:
   - **튜토리얼 복습** → `docs/step-XX-*.md` 차례로 읽기
   - **더 나아가기** → 위 "여기서 더 나아가려면" 표에서 주제 선택
   - **다른 도메인 적용** → Book 대신 Order/User/Product 등으로 같은 5단계 패턴 재현 연습

축하합니다! 🎉
