package com.jiffy.backend.controller;

import com.jiffy.backend.model.Tag;
import com.jiffy.backend.repository.TagRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/tags")
@CrossOrigin(origins = "*")
public class TagController {
    
    private static final Logger logger = LoggerFactory.getLogger(TagController.class); // Improved logging

    private final TagRepository tagRepository;

    @Autowired
    public TagController(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }
    
    @PostMapping
    public ResponseEntity<?> addTag(@RequestBody Tag tag) {
        // Ensure timestamp is set if missing
        if (tag.getTimestamp() == null) {
            tag.setTimestamp(LocalDateTime.now());
        }

        try {
            tagRepository.save(tag);
            logger.info("Tag saved successfully: {}", tag.getEpc());
            return ResponseEntity.ok("Tag recorded successfully: " + tag.getEpc());
        } catch (Exception e) {
            logger.error("Error saving tag [{}]: {}", tag.getEpc(), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to save tag: " + tag.getEpc());
        }
    }
    
    @GetMapping
    public ResponseEntity<List<Tag>> getTags() {
        try {
            List<Tag> tags = tagRepository.findAll();
            return ResponseEntity.ok(tags);
        } catch (Exception e) {
            logger.error("Error retrieving tags: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
    
    @DeleteMapping
    public ResponseEntity<String> deleteAllTags() {
        try {
            long deletedCount = tagRepository.count(); // Show number of deleted tags
            tagRepository.deleteAll();
            return ResponseEntity.ok("Deleted " + deletedCount + " tags successfully.");
        } catch (Exception e) {
            logger.error("Error deleting tags: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to delete tags");
        }
    }
}