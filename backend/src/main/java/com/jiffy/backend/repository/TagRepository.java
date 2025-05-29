package com.jiffy.backend.repository;

import com.jiffy.backend.model.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository // Ensures Spring Boot recognizes this as a repository
public interface TagRepository extends JpaRepository<Tag, Long> {

    // Check if an EPC already exists
    boolean existsByEpc(String epc);

    Optional<Tag> findByEpc(String epc);
}