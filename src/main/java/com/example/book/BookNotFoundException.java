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
