package com.cloudlibrary.bookservice.service;

import com.cloudlibrary.bookservice.dto.BookDTO;
import com.cloudlibrary.bookservice.entity.Book;
import com.cloudlibrary.bookservice.repository.BookRepository;
import com.cloudlibrary.bookservice.storage.CloudStorageService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BookService {

    private final BookRepository bookRepository;
    private final CloudStorageService cloudStorageService;

    public BookService(BookRepository bookRepository, CloudStorageService cloudStorageService) {
        this.bookRepository = bookRepository;
        this.cloudStorageService = cloudStorageService;
    }

    public List<BookDTO> getAllBooks() {
        return bookRepository.findAll()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public BookDTO getBookById(String id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Book not found with id: " + id));
        return toDTO(book);
    }

    public BookDTO createBook(BookDTO dto) {
        Book book = new Book(dto.getTitle(), dto.getAuthor(), dto.getIsbn(), dto.getCategory(), dto.getQuantity());
        Book saved = bookRepository.save(book);
        return toDTO(saved);
    }

    public BookDTO updateBook(String id, BookDTO dto) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Book not found with id: " + id));
        book.setTitle(dto.getTitle());
        book.setAuthor(dto.getAuthor());
        book.setIsbn(dto.getIsbn());
        book.setCategory(dto.getCategory());
        book.setQuantity(dto.getQuantity());
        book.setUpdatedAt(LocalDateTime.now());
        Book updated = bookRepository.save(book);
        return toDTO(updated);
    }

    public void deleteBook(String id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Book not found with id: " + id));
        // Delete cover image from cloud storage if exists
        if (book.getCoverImageUrl() != null) {
            cloudStorageService.deleteFile(book.getCoverImageUrl());
        }
        bookRepository.deleteById(id);
    }

    public BookDTO uploadCoverImage(String id, MultipartFile file) throws IOException {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Book not found with id: " + id));

        // Delete old image if exists
        if (book.getCoverImageUrl() != null) {
            cloudStorageService.deleteFile(book.getCoverImageUrl());
        }

        String imageUrl = cloudStorageService.uploadFile(file);
        book.setCoverImageUrl(imageUrl);
        book.setUpdatedAt(LocalDateTime.now());
        Book updated = bookRepository.save(book);
        return toDTO(updated);
    }

    public List<BookDTO> searchBooks(String query) {
        return bookRepository.findByTitleContainingIgnoreCase(query)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    private BookDTO toDTO(Book book) {
        return new BookDTO(
                book.getId(),
                book.getTitle(),
                book.getAuthor(),
                book.getIsbn(),
                book.getCategory(),
                book.getQuantity(),
                book.getCoverImageUrl()
        );
    }
}
