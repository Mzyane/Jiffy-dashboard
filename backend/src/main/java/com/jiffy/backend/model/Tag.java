package com.jiffy.backend.model;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tags")
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String epc;
    private LocalDateTime timestamp;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEpc() { return epc; }
    public void setEpc(String epc) { this.epc = epc; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    @Override
    public String toString() {
        return "Tag{" +
                "id=" + id +
                ", epc='" + epc + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}