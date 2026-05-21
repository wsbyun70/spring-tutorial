# Step 3 — JPA Entity + H2 데이터베이스

> 이 단계가 끝나면 **클래스를 선언하는 것만으로** 데이터베이스 테이블이 만들어지고,
> **인터페이스를 선언하는 것만으로** CRUD 메서드가 동작합니다.
>
> Spring Data JPA의 마법을 직접 체험하는 단계입니다.

---

## 🎯 이번 단계의 목표

- **`@Entity`** — 클래스 ↔ DB 테이블 자동 매핑 (DDL 자동 생성)
- **`JpaRepository`** — CRUD 메서드를 **구현 없이** 자동 생성
- **메서드 이름 기반 쿼리** — `findByAuthor(...)` 같은 이름만으로 SQL 자동 생성
- **H2 인메모리 DB** + 브라우저 콘솔로 직접 SQL 실행
- **`application.yml` 확장** — DataSource, JPA, H2 설정
- **`CommandLineRunner`** — 기동 시 코드 자동 실행

---

## 📋 사전 확인

- [Step 2](step-02-rest-endpoint.md) 완료 (REST 엔드포인트 4종 동작)
- 포트 8081에 좀비 Java 프로세스 없음:
  ```bash
  netstat -ano | grep ":8081"
  ```

---

## 📂 변경되는 파일

```
e:\workspace\spring-tutorial\
├─ pom.xml                                            ✏️ 의존성 2개 추가
└─ src\main\
   ├─ java\com\example\book\
   │  ├─ BookTutorialApplication.java                 (변경 없음)
   │  ├─ HelloController.java                         (변경 없음)
   │  ├─ Book.java                                    ⭐ 신규 - Entity
   │  ├─ BookRepository.java                          ⭐ 신규 - Repository
   │  └─ DataSeeder.java                              ⭐ 신규 - 기동 시 샘플 삽입
   └─ resources\
      └─ application.yml                              ✏️ DB/JPA/H2 설정 추가
```

---

## 📝 1단계 — `pom.xml`에 의존성 2개 추가

`<dependencies>` 블록에 추가:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>

<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>runtime</scope>
</dependency>
```

### 무엇이 들어오는가?

| 의존성 | 가져오는 것 |
|---|---|
| `spring-boot-starter-data-jpa` | Hibernate (JPA 구현), Spring Data JPA, HikariCP (커넥션 풀), JdbcTemplate, 트랜잭션 관리 |
| `h2` | H2 인메모리 데이터베이스 + H2 콘솔 웹앱 |

### `<scope>runtime</scope>`이 뭔가?

H2는 **컴파일 시점에는 필요 없고 실행 시점에만 필요**합니다.
- 코드에서 `org.h2.Driver`를 직접 import할 일 없음
- Spring이 런타임에 알아서 드라이버 로드

`scope`는 의존성을 어느 단계에서 쓸지 지정 — 사용 가능한 값:
- `compile` (기본값) — 컴파일 + 실행 + 테스트 모두
- `runtime` — 실행 + 테스트만
- `test` — 테스트만
- `provided` — 컴파일 + 테스트, 실행 시엔 외부에서 제공됨

---

## 📝 2단계 — `application.yml` 확장

```yaml
server:
  port: 8081

spring:
  datasource:
    url: jdbc:h2:mem:bookdb;DB_CLOSE_DELAY=-1
    driver-class-name: org.h2.Driver
    username: sa
    password:
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true
    properties:
      hibernate:
        format_sql: true
  h2:
    console:
      enabled: true
      path: /h2-console
```

### 줄별 의미

| 설정 | 의미 |
|---|---|
| `datasource.url: jdbc:h2:mem:bookdb` | 인메모리 H2, DB 이름은 `bookdb` |
| `;DB_CLOSE_DELAY=-1` | 마지막 연결이 닫혀도 DB 유지 (서버 띄어있는 동안) |
| `driver-class-name` | H2 JDBC 드라이버 |
| `username: sa` / `password:` | 기본 계정 (sa = system admin) |
| `jpa.hibernate.ddl-auto: create-drop` | 기동 시 테이블 자동 생성, 종료 시 삭제 |
| `jpa.show-sql: true` | 실행되는 SQL을 콘솔에 출력 (학습에 매우 유용) |
| `format_sql: true` | SQL을 보기 좋게 줄바꿈해 출력 |
| `h2.console.enabled: true` | H2 브라우저 콘솔 활성화 |
| `h2.console.path: /h2-console` | 콘솔 URL — `http://localhost:8081/h2-console` |

### `ddl-auto` 옵션 (중요!)

| 값 | 동작 | 용도 |
|---|---|---|
| `none` | 아무것도 안 함 | 운영 환경 (DDL은 별도 마이그레이션 도구로) |
| `validate` | 엔티티 ↔ 테이블 구조 검증만 | 운영 환경 (안전 확인) |
| `update` | 부족한 컬럼/테이블 추가 (삭제는 안 함) | 개발 환경 |
| `create` | 기동 시 테이블 모두 drop 후 새로 생성 | 테스트 |
| `create-drop` | `create` + 종료 시 drop | **튜토리얼/테스트용** |

> ⚠️ **운영 환경에서는 절대 `create-drop`이나 `create` 쓰지 말 것.**
> 기동할 때마다 모든 데이터가 사라집니다.

---

## 📝 3단계 — `Book.java` (Entity)

```java
package com.example.book;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String author;
    private Integer publishedYear;

    protected Book() {
    }

    public Book(String title, String author, Integer publishedYear) {
        this.title = title;
        this.author = author;
        this.publishedYear = publishedYear;
    }

    // getters / setters ...
}
```

### 핵심 어노테이션

#### `@Entity`
- "이 클래스는 DB 테이블과 매핑됩니다"
- Spring Boot 기동 시 JPA가 발견하여 **`BOOK` 테이블 자동 생성** (ddl-auto가 활성화된 경우)
- 테이블명은 기본적으로 클래스명을 소문자/snake_case로 (`Book` → `book`)
- 다른 이름을 쓰려면: `@Entity @Table(name = "books")`

#### `@Id`
- "이 필드가 Primary Key(기본키)입니다"
- 모든 Entity는 정확히 하나의 `@Id` 필드가 있어야 함

#### `@GeneratedValue(strategy = GenerationType.IDENTITY)`
- "이 PK 값은 DB가 자동으로 채워줍니다 (AUTO_INCREMENT)"
- 우리가 `book.setId(...)` 안 해도 INSERT 후에 자동 채워짐

**`strategy` 옵션**:
| 값 | 동작 |
|---|---|
| `IDENTITY` | DB의 AUTO_INCREMENT 사용 (MySQL, H2, SQL Server) |
| `SEQUENCE` | 시퀀스 객체 사용 (Oracle, PostgreSQL) |
| `TABLE` | 별도 테이블에 카운터 저장 (느림, 거의 안 씀) |
| `AUTO` | JPA가 DB에 맞게 자동 선택 |

#### 컬럼 매핑 (자동)
```java
private String title;       // VARCHAR 컬럼 자동 생성
private Integer publishedYear;  // INTEGER 컬럼 자동 생성
```
- 필드명 → 컬럼명 (`publishedYear` → `published_year`, snake_case)
- 타입 → SQL 타입 자동 매핑
- 명시하려면: `@Column(name = "pub_year", length = 4, nullable = false)`

> **왜 `year`가 아니라 `publishedYear`?**
> `YEAR`는 일부 DB의 예약어. 호환성을 위해 풀어서 명명.

### 왜 `protected Book()` 기본 생성자가 필요한가?
JPA는 객체를 만들 때 **리플렉션으로 기본 생성자를 호출**한 뒤 필드에 값을 채워넣습니다.
이 때문에 **인자 없는 생성자가 반드시 필요**합니다.

`protected`로 만들면 외부에서 잘못 호출하지 못하게 막을 수 있습니다 (JPA는 protected도 호출 가능).

---

## 📝 4단계 — `BookRepository.java` (Repository)

```java
package com.example.book;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BookRepository extends JpaRepository<Book, Long> {

    List<Book> findByAuthor(String author);
}
```

### **인터페이스만 선언!** 구현 클래스 없음!

이게 Spring Data JPA의 핵심 마법입니다. Spring이 기동 시 자동으로 **프록시 구현체를 생성**해서 Bean으로 등록.

### `JpaRepository<Book, Long>`이 무료로 주는 메서드

| 메서드 | 동작 |
|---|---|
| `save(book)` | INSERT 또는 UPDATE |
| `findById(id)` | PK로 조회 → `Optional<Book>` |
| `findAll()` | 모든 row → `List<Book>` |
| `findAll(Pageable)` | 페이지네이션 조회 |
| `deleteById(id)` | PK로 삭제 |
| `delete(book)` | 객체로 삭제 |
| `count()` | 전체 개수 |
| `existsById(id)` | 존재 여부 |
| `saveAll(books)` | 한꺼번에 여러 개 저장 |
| ... | 그 외 다수 |

타입 파라미터:
- `<Book>` — 어떤 Entity를 다루는지
- `<Long>` — PK 타입

### 🪄 메서드 이름 기반 쿼리 — `findByAuthor`

```java
List<Book> findByAuthor(String author);
```

**이 한 줄이 SQL `SELECT * FROM book WHERE author = ?`를 자동 생성합니다.**

Spring Data JPA는 메서드 이름을 파싱:
- `findBy` → SELECT 쿼리
- `Author` → `author` 컬럼
- `(String author)` 파라미터 → WHERE 조건 값

**더 많은 패턴**:
```java
List<Book> findByAuthorAndPublishedYear(String author, Integer year);
// WHERE author = ? AND published_year = ?

List<Book> findByTitleContaining(String keyword);
// WHERE title LIKE %?%

List<Book> findByPublishedYearGreaterThan(Integer year);
// WHERE published_year > ?

List<Book> findByAuthorOrderByPublishedYearDesc(String author);
// WHERE author = ? ORDER BY published_year DESC

Optional<Book> findFirstByAuthor(String author);
// LIMIT 1
```

키워드: `And`, `Or`, `Between`, `LessThan`, `GreaterThan`, `Like`, `Containing`, `OrderBy`, `Asc`, `Desc`, `Not`, `IsNull`, `In`, ...

> ⚠️ **메서드 이름이 너무 길어지면**:
> 복잡한 조건은 **`@Query`** 어노테이션으로 직접 JPQL/SQL 작성하는 게 더 명확합니다.
> ```java
> @Query("SELECT b FROM Book b WHERE b.author = :a AND b.publishedYear >= :y")
> List<Book> searchAuthorSince(@Param("a") String author, @Param("y") Integer year);
> ```

---

## 📝 5단계 — `DataSeeder.java` (기동 시 데이터 삽입)

```java
package com.example.book;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final BookRepository repository;

    public DataSeeder(BookRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(String... args) {
        repository.save(new Book("Clean Code", "Robert C. Martin", 2008));
        repository.save(new Book("Effective Java", "Joshua Bloch", 2017));
        repository.save(new Book("Clean Architecture", "Robert C. Martin", 2017));
        repository.save(new Book("Spring in Action", "Craig Walls", 2022));

        log.info("=== Loaded all books ===");
        repository.findAll().forEach(b ->
            log.info("  id={}, title={}, author={}, year={}",
                b.getId(), b.getTitle(), b.getAuthor(), b.getPublishedYear()));

        log.info("=== findByAuthor('Robert C. Martin') ===");
        repository.findByAuthor("Robert C. Martin").forEach(b ->
            log.info("  -> {}", b.getTitle()));
    }
}
```

### 핵심 개념

#### `@Component`
- "이 클래스를 Spring Bean으로 등록해주세요"
- Component Scan에 잡혀서 자동 등록
- `@RestController`, `@Service`, `@Repository`도 모두 `@Component`의 특수 형태

#### `CommandLineRunner`
- Spring Boot가 **기동 완료 직후 자동으로 `run()` 메서드를 호출**해주는 인터페이스
- 초기 데이터 삽입, 워밍업, 외부 시스템 연결 검증 등에 사용
- 비슷한 `ApplicationRunner`도 있음 (인자 받는 방식만 다름)

#### 생성자 주입 (Constructor Injection)
```java
public DataSeeder(BookRepository repository) {
    this.repository = repository;
}
```
- Spring이 `BookRepository` Bean을 **생성자 인자로 자동 주입**
- `@Autowired`도 가능하지만, **생성자 주입이 권장**되는 이유:
  - 불변(`final`) 선언 가능
  - 테스트 시 모킹 쉬움 (`new DataSeeder(mockRepo)`)
  - 의존성이 컴파일 타임에 명확

> Spring 4.3+ 부터 **생성자가 1개**면 `@Autowired`를 안 적어도 자동 주입됩니다.

---

## ▶️ 빌드 & 실행

### 컴파일

```bash
mvn -f e:/workspace/spring-tutorial/pom.xml compile
```

**기대 출력 일부**:
```
[INFO] Compiling 5 source files to e:\workspace\spring-tutorial\target\classes
[INFO] BUILD SUCCESS
```

> 처음에는 의존성 다운로드로 시간이 좀 더 걸립니다 (Hibernate, H2 등).

### 서버 기동

```bash
mvn -f e:/workspace/spring-tutorial/pom.xml spring-boot:run
```

---

## 🧪 검증 — 콘솔 로그 확인

### 1) H2 콘솔 자동 등록 메시지
```
INFO ... H2ConsoleAutoConfiguration : H2 console available at '/h2-console'. Database available at 'jdbc:h2:mem:bookdb'
```

### 2) Hibernate가 DDL 자동 실행
```
Hibernate: 
    create table book (
        id bigint generated by default as identity,
        author varchar(255),
        published_year integer,
        title varchar(255),
        primary key (id)
    )
```

**우리가 SQL을 안 썼는데** Hibernate가 Entity 클래스를 보고 자동 생성!

### 3) `save()` → INSERT 4번
```
Hibernate: 
    insert 
    into
        book
        (author, published_year, title, id) 
    values
        (?, ?, ?, default)
```

### 4) `findAll()` → DataSeeder 로그
```
INFO ... DataSeeder : === Loaded all books ===
INFO ... DataSeeder :   id=1, title=Clean Code, author=Robert C. Martin, year=2008
INFO ... DataSeeder :   id=2, title=Effective Java, author=Joshua Bloch, year=2017
INFO ... DataSeeder :   id=3, title=Clean Architecture, author=Robert C. Martin, year=2017
INFO ... DataSeeder :   id=4, title=Spring in Action, author=Craig Walls, year=2022
```

### 5) `findByAuthor()` — 메서드 이름으로 SQL 자동 생성!
```
INFO ... DataSeeder : === findByAuthor('Robert C. Martin') ===
Hibernate: 
    select ... from book where author = ?
INFO ... DataSeeder :   -> Clean Code
INFO ... DataSeeder :   -> Clean Architecture
```

4개 중 **Robert C. Martin이 쓴 2권만 정확히 필터링**됨!

---

## 🌐 H2 브라우저 콘솔로 직접 SQL 실행

서버가 기동된 상태에서 브라우저로:

```
http://localhost:8081/h2-console
```

**접속 화면에서**:
| 필드 | 값 |
|---|---|
| Saved Settings | `Generic H2 (Embedded)` |
| Driver Class | `org.h2.Driver` |
| **JDBC URL** | **`jdbc:h2:mem:bookdb`** ⚠️ 기본값(`jdbc:h2:~/test`)에서 반드시 변경 |
| User Name | `sa` |
| Password | (비움) |

**Connect 클릭** → 좌측 트리에 `BOOK` 테이블이 보임 → SQL 실행 가능:

```sql
-- 모든 책 조회
SELECT * FROM book;

-- 2017년 이후 출판
SELECT * FROM book WHERE published_year >= 2017;

-- 새 책 추가
INSERT INTO book (title, author, published_year) VALUES ('Refactoring', 'Martin Fowler', 2018);

-- 다시 조회
SELECT * FROM book;
```

> **인메모리 DB이므로** 서버 재시작하면 모든 데이터가 사라지고 `DataSeeder`가 다시 처음 4권만 넣습니다.

---

## ✅ 검증 체크리스트

- [ ] `pom.xml`에 `starter-data-jpa`, `h2` 의존성 추가됨
- [ ] `application.yml`에 DB/JPA/H2 설정 추가됨
- [ ] `Book.java`, `BookRepository.java`, `DataSeeder.java` 작성 완료
- [ ] `mvn compile` → `Compiling 5 source files`, `BUILD SUCCESS`
- [ ] 서버 기동 로그에 `create table book` 보임
- [ ] 서버 기동 로그에 `insert` 4번 보임
- [ ] DataSeeder 로그에 `id=1, title=Clean Code, ...` 등 4행 출력
- [ ] `findByAuthor('Robert C. Martin')` 결과 2권만 출력
- [ ] `http://localhost:8081/h2-console` 접속 → `SELECT * FROM book` 4행 반환

---

## 📚 핵심 개념 정리

### 1. JPA 아키텍처 (Spring Boot 기준)

```
Application Code
    ↓ uses
BookRepository (interface — JpaRepository)
    ↓ implemented by
Spring Data JPA Proxy (자동 생성)
    ↓ delegates to
EntityManager (JPA 표준 API)
    ↓ implemented by
Hibernate (JPA 구현체)
    ↓ generates
SQL
    ↓ via
JDBC Driver (H2)
    ↓
H2 Database
```

우리가 만진 건 맨 위 두 줄뿐. 나머지는 Spring Boot가 자동 구성.

### 2. ORM (Object-Relational Mapping) 이란?

| 객체 세계 | 관계형 DB 세계 |
|---|---|
| 클래스 (`Book`) | 테이블 (`book`) |
| 필드 (`title`) | 컬럼 (`title`) |
| 객체 인스턴스 (`new Book(...)`) | row (`(1, 'Clean Code', ...)`) |
| 객체 참조 (`book.getAuthor()`) | JOIN 또는 FK |
| 객체 컬렉션 (`List<Book>`) | 쿼리 결과 집합 |

JPA = 이 두 세계 사이의 자동 변환 표준.

### 3. Spring Data JPA의 3대 마법

1. **자동 DDL** — Entity 클래스만으로 `CREATE TABLE` 생성
2. **Proxy Repository** — 인터페이스만 선언, 구현은 런타임에 자동 생성
3. **Derived Query** — 메서드 이름 파싱으로 WHERE 절 자동 생성

### 4. 트랜잭션 자동 관리
- `JpaRepository`의 메서드 호출은 **자동으로 트랜잭션** 안에서 실행됨
- 예외 발생 시 자동 롤백
- `@Transactional` 어노테이션으로 명시적 제어도 가능 (다음 단계에서 등장)

---

## ⚠️ 자주 만나는 에러

### "No qualifying bean of type 'BookRepository' available"
**원인**: `BookRepository` 인터페이스가 Component Scan 범위 밖에 있거나, `JpaRepository`를 상속하지 않음.
**해결**:
- 패키지가 `com.example.book` 또는 그 하위인지 확인
- `extends JpaRepository<Book, Long>` 정확히 적었는지 확인

### "Unknown entity: com.example.book.Book"
**원인**: `@Entity` 어노테이션 누락.
**해결**: 클래스 위에 `@Entity` 추가.

### "Table 'book' not found"
**원인**: `ddl-auto`가 `none`이거나 schema 초기화 실패.
**해결**: `application.yml`에 `jpa.hibernate.ddl-auto: create-drop` 설정 확인.

### H2 콘솔 접속 시 "Database 'mem:test' not found"
**원인**: 콘솔 화면의 **JDBC URL이 기본값(`jdbc:h2:~/test`)** 그대로.
**해결**: JDBC URL을 **`jdbc:h2:mem:bookdb`**로 수정.

### "No default constructor for entity"
**원인**: Entity 클래스에 기본 생성자 없음.
**해결**: `public` 또는 `protected` 기본 생성자 추가.

### "Object References an unsaved transient instance"
**원인**: 연관 관계의 다른 엔티티가 아직 영속화 안 됨.
**해결**: 부모를 먼저 `save()` 한 후 자식 저장. (이번 단계엔 해당 없음 — Step 5 이후 관련)

---

## 🔤 용어 사전 (이번 단계 신규)

| 용어 | 의미 |
|---|---|
| **JPA** | Java Persistence API — Java 표준 ORM 명세 |
| **Hibernate** | JPA 명세를 구현한 가장 유명한 ORM 라이브러리 |
| **Spring Data JPA** | JPA를 한층 더 추상화한 Spring 모듈 (`JpaRepository` 등) |
| **Entity** | DB 테이블과 매핑되는 클래스 |
| **DDL** | Data Definition Language — `CREATE TABLE`, `ALTER` 등 |
| **DML** | Data Manipulation Language — `INSERT`, `UPDATE`, `DELETE`, `SELECT` |
| **PK (Primary Key)** | 테이블에서 각 행을 유일하게 식별하는 값 |
| **AUTO_INCREMENT / IDENTITY** | DB가 PK 값을 자동으로 1, 2, 3... 채워주는 기능 |
| **Repository (Spring 용어)** | DB 접근 코드를 담는 계층의 이름 |
| **JPQL** | JPA Query Language — SQL과 비슷하지만 엔티티 이름 기준 |
| **Proxy** | 인터페이스의 구현체를 런타임에 동적 생성한 것 |
| **Persistence Context** | JPA가 관리하는 엔티티 캐시 (1차 캐시) |
| **DataSource** | DB 연결 정보 + 커넥션 풀 추상화 |
| **Connection Pool** | DB 연결을 미리 만들어두고 재사용하는 풀 (HikariCP 기본) |

---

## 🧠 한 번 더 강조 — 우리가 작성한 코드 vs 자동 동작

| 우리가 작성 | Spring Boot가 자동으로 한 일 |
|---|---|
| `@Entity public class Book {...}` (35줄) | `CREATE TABLE book (...)` SQL 실행, 컬럼 타입 매핑 |
| `interface BookRepository extends JpaRepository<Book, Long>` (1줄) | save/findAll/findById/delete 등 **모든 CRUD 메서드 구현** |
| `List<Book> findByAuthor(String author);` (1줄) | `SELECT * FROM book WHERE author = ?` SQL 자동 생성 + 결과 매핑 |
| `application.yml` 16줄 | DataSource 생성, 커넥션 풀(HikariCP) 설정, Hibernate 설정, 트랜잭션 매니저 설정 |
| `@Component`만 붙임 | Bean 자동 등록, 의존성 자동 주입, 기동 시 `run()` 자동 호출 |

전통적인 JDBC 코드로 같은 일을 하려면 **수백 줄의 보일러플레이트**가 필요합니다.

---

## ➡️ 다음 단계 미리보기 — Step 4

**Service 계층 추가 + 완전한 REST CRUD**

```java
@Service
public class BookService {
    private final BookRepository repository;
    
    public Book create(Book book) { return repository.save(book); }
    public Book findOne(Long id) { ... }
    public Book update(Long id, Book book) { ... }
    public void delete(Long id) { ... }
}

@RestController
@RequestMapping("/api/books")
public class BookController {
    @GetMapping        public List<Book> list() { ... }
    @PostMapping       public Book create(@RequestBody Book book) { ... }
    @GetMapping("/{id}") public Book get(@PathVariable Long id) { ... }
    @PutMapping("/{id}") public Book update(@PathVariable Long id, @RequestBody Book book) { ... }
    @DeleteMapping("/{id}") public void delete(@PathVariable Long id) { ... }
}
```

이때 등장할 새 개념:
- **3계층 아키텍처** — Controller → Service → Repository
- `@Service` — 비즈니스 로직 계층
- `@RequestBody` — JSON 요청 본문 → 객체 자동 변환
- `@RequestMapping("/api/books")` — 클래스 레벨 공통 경로
- HTTP 상태 코드 제어 — `ResponseEntity`
- `@Transactional` — 명시적 트랜잭션 경계

---

## 📦 이 단계의 산출물

```
e:\workspace\spring-tutorial\
├─ pom.xml                                            ✏️ +10 줄 (의존성)
├─ src\main\java\com\example\book\
│  ├─ BookTutorialApplication.java                    (변경 없음)
│  ├─ HelloController.java                            (변경 없음)
│  ├─ Book.java                                       ⭐ 신규 +55 줄
│  ├─ BookRepository.java                             ⭐ 신규 +10 줄
│  └─ DataSeeder.java                                 ⭐ 신규 +30 줄
└─ src\main\resources\
   └─ application.yml                                 ✏️ +15 줄
```

**총 추가 코드량: 약 120 줄.**

이 정도로 **완전히 동작하는 영속화 계층**(DB 연결 + 테이블 생성 + CRUD + 쿼리)이 구축됐습니다.
