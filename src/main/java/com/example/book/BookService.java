package com.example.book;

import java.util.List;

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
    public Book findOne(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new BookNotFoundException(id));
    }

    public Book create(Book book) {
        return repository.save(book);
    }

    public Book update(Long id, Book changes) {
        Book existing = repository.findById(id)
            .orElseThrow(() -> new BookNotFoundException(id));
        existing.setTitle(changes.getTitle());
        existing.setAuthor(changes.getAuthor());
        existing.setPublishedYear(changes.getPublishedYear());
        return existing;
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new BookNotFoundException(id);
        }
        repository.deleteById(id);
    }
}
