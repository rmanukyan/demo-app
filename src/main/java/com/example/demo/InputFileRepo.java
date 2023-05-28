package com.example.demo;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InputFileRepo extends JpaRepository<InputFile, Long> {
    boolean existsByName(String name);
    boolean existsByNameAndProcessed(String name, Boolean processed);
    Optional<InputFile> findByName(String name);
}
