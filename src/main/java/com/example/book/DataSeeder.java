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
