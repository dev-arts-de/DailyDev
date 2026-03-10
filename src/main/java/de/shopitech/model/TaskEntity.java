package de.shopitech.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Data
@Table(name = "daily_tasks")
public class TaskEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "daily_tasks_name", nullable = false)
    private String name;

    @Column(name = "jscope_path", nullable = false)
    private String slug;

    @Column(name = "category", nullable = false)
    private String category;

    @Column(name = "task_order", nullable = false)
    private int order;

    @Column(name = "processed", nullable = false)
    private boolean processed;

    @Column(name = "processed_at")
    private LocalDate processedAt;
}
