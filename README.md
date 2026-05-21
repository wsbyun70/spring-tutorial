# Spring Boot Tutorial — 도서 관리 REST API

> **Spring Boot를 전혀 모르는 개발자**가 한 단계씩 따라가며 동작하는 REST API를 직접 만드는 한글 튜토리얼.
> 각 단계마다 **왜 그렇게 하는지** 설명, **기대 출력**, **자주 만나는 에러**, **용어 사전**까지 포함.

---

## 📌 한눈에 보기

- **목표**: Spring Boot로 도서(Book) CRUD REST API 만들기 (총 5단계)
- **스택**: Spring Boot 2.7.18 · Java 8 · Maven 3.9 · H2 (인메모리)
- **현재 진행 상황**: Step 1 ~ 3 완료 (Step 4, 5 남음 — [PROGRESS.md](PROGRESS.md) 참조)
- **대상 독자**: Spring Boot를 처음 접하는 개발자

---

## 🚀 빠른 실행

### 사전 요구 사항
- JDK 8 (또는 그 이상)
- Maven 3.6+

### 컴파일 & 실행

```bash
# 1) 컴파일 확인
mvn -f e:/workspace/spring-tutorial/pom.xml compile

# 2) 서버 기동
mvn -f e:/workspace/spring-tutorial/pom.xml spring-boot:run
```

> 프로젝트 폴더로 이동했다면 `-f` 옵션 생략 가능: `mvn spring-boot:run`

서버가 `http://localhost:8081`에서 동작합니다.

### 동작 확인

```bash
# 기본 인사
curl http://localhost:8081/hello

# Path Variable
curl http://localhost:8081/hello/Alice

# Query Parameter
curl "http://localhost:8081/greet?name=Bob&times=3"

# JSON 응답
curl http://localhost:8081/info
```

### H2 데이터베이스 콘솔 (브라우저)

```
http://localhost:8081/h2-console
```

- **JDBC URL**: `jdbc:h2:mem:bookdb` ⚠️ 기본값(`jdbc:h2:~/test`)에서 반드시 변경
- **User**: `sa`
- **Password**: (비움)

→ Connect → `SELECT * FROM book` 실행 시 4권 표시.

---

## 📚 학습 가이드 (단계별 문서)

각 단계는 독립적으로 읽을 수 있고, 순서대로 따라가면 처음부터 동일한 결과물이 만들어집니다.

| 단계 | 주제 | 핵심 개념 | 문서 |
|---|---|---|---|
| **Step 1** | 프로젝트 골격 | `@SpringBootApplication`, Starter, Embedded Tomcat | [docs/step-01-project-skeleton.md](docs/step-01-project-skeleton.md) |
| **Step 2** | 첫 REST 엔드포인트 | `@RestController`, `@PathVariable`, `@RequestParam`, JSON 자동 변환 | [docs/step-02-rest-endpoint.md](docs/step-02-rest-endpoint.md) |
| **Step 3** | JPA + H2 데이터베이스 | `@Entity`, `JpaRepository`, 메서드 이름 기반 쿼리 | [docs/step-03-jpa-h2.md](docs/step-03-jpa-h2.md) |
| **Step 4** | Service 계층 + CRUD | `@Service`, `@RequestBody`, `ResponseEntity` | _(예정)_ |
| **Step 5** | 검증 + 예외 처리 | `@Valid`, `@ExceptionHandler` | _(예정)_ |

각 문서에 포함되는 내용:
- 🎯 이 단계의 목표
- 📂 만들/변경할 파일
- 📝 코드 + **줄별 설명** (왜 이렇게 쓰는지)
- ▶️ 빌드 & 실행 + **기대 출력**
- ✅ 검증 체크리스트
- 📚 핵심 개념 정리
- ⚠️ 자주 만나는 에러 + 해결
- 🔤 용어 사전

---

## 📂 프로젝트 구조

```
spring-tutorial/
├─ README.md                                  ← 이 파일
├─ PROGRESS.md                                ← 진행 상황 + 다음 단계 가이드
├─ pom.xml                                    ← Maven 빌드 설정
├─ .gitignore
│
├─ docs/                                       ← 단계별 학습 문서
│  ├─ step-01-project-skeleton.md
│  ├─ step-02-rest-endpoint.md
│  └─ step-03-jpa-h2.md
│
└─ src/
   ├─ main/
   │  ├─ java/com/example/book/
   │  │  ├─ BookTutorialApplication.java       ← 진입점
   │  │  ├─ HelloController.java               ← Step 2: REST 엔드포인트
   │  │  ├─ Book.java                          ← Step 3: Entity
   │  │  ├─ BookRepository.java                ← Step 3: JpaRepository
   │  │  └─ DataSeeder.java                    ← Step 3: 초기 데이터 삽입
   │  └─ resources/
   │     └─ application.yml                    ← 서버 / DB / JPA 설정
   └─ test/
      └─ java/com/example/book/                ← (아직 비어있음)
```

---

## 🎯 현재 동작하는 기능 (Step 3 시점)

### HTTP 엔드포인트
| 메서드 | 경로 | 응답 |
|---|---|---|
| GET | `/hello` | `Hello, Spring Boot!` |
| GET | `/hello/{name}` | `Hello, {name}!` |
| GET | `/greet?name=...&times=...` | `Hi {name}! ({times} times)` |
| GET | `/info` | `{"app":"book-tutorial","version":"...","javaVersion":"..."}` |
| GET | `/h2-console` | H2 데이터베이스 브라우저 콘솔 |

### 데이터베이스
- 기동 시 `BOOK` 테이블 자동 생성 (Hibernate DDL)
- 샘플 도서 4권 자동 삽입 (DataSeeder)
- 메서드 이름 기반 쿼리 동작 — `findByAuthor()`

### 도서 CRUD 엔드포인트
- 아직 없음 → **Step 4에서 추가 예정**

---

## 💡 이 튜토리얼의 특징

- **한국어** — 영문 공식 문서 부담 없이 학습
- **빌드/실행이 모두 검증된 코드** — 각 단계 끝에 실제 실행 결과 캡처
- **"왜?"를 매번 설명** — 어노테이션 하나하나의 의미 풀어서 설명
- **자주 만나는 함정 미리 안내** — 좀비 Java 프로세스, H2 콘솔 JDBC URL 변경 등
- **용어 사전** — 처음 듣는 단어를 매 단계마다 정리

---

## 📖 다음 단계 시작하기

새 세션에서 이어가려면 [**PROGRESS.md**](PROGRESS.md)를 먼저 읽으세요.

거기에는:
- 어디까지 했는지
- 다음 단계에서 무엇을 만들 것인지
- 재개할 때 첫 번째로 확인할 것들

이 모두 정리되어 있습니다.

---

## 📝 라이선스

학습 목적 — 자유롭게 사용/수정/공유 가능.
