package de.shopitech.repository;

import de.shopitech.model.TaskEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<TaskEntity, UUID> {

    @Query(value = "SELECT * FROM daily_tasks WHERE processed = false ORDER BY RANDOM() LIMIT 1", nativeQuery = true)
    Optional<TaskEntity> findRandomUnprocessed();

    long countByProcessedTrue();

    List<TaskEntity> findByProcessedTrueOrderByProcessedAtAsc(Pageable pageable);

    @Query(value = "SELECT * FROM daily_tasks WHERE processed = true AND category = :category ORDER BY processed_at ASC", nativeQuery = true)
    List<TaskEntity> findProcessedByCategoryOldestFirst(@Param("category") String category, Pageable pageable);
}
