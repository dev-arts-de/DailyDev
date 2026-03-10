package de.shopitech.service;

import de.shopitech.model.TaskEntity;
import de.shopitech.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DailyTaskService {

    private static final int REVIEW_THRESHOLD = 10;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final TaskRepository taskRepository;
    private final AnthropicService anthropicService;
    private final GitService gitService;
    private final NtfyService ntfyService;

    @Value("${dailyDev.vercel.baseUrl}")
    private String vercelBaseUrl;

    public void executeDailyTask() {
        log.info("Starting daily task execution...");

        TaskEntity task = taskRepository.findRandomUnprocessed().orElse(null);
        if (task == null) {
            long total = taskRepository.count();
            log.info("All tasks processed — nothing to do.");
            try {
                ntfyService.sendAllProcessedNotification(total);
            } catch (Exception e) {
                log.error("Failed to send all-processed notification: {}", e.getMessage(), e);
            }
            return;
        }

        log.info("Processing task: {} ({})", task.getName(), task.getCategory());

        try {
            String aiContent = anthropicService.generateMarkdownContent(task.getName(), task.getCategory());
            LocalDate today = LocalDate.now();
            String markdown = buildMarkdown(task.getName(), today, aiContent);
            String filePath = gitService.commitAndPush(task.getCategory(), task.getSlug(), markdown);
            String vercelUrl = vercelBaseUrl + "/topics" + filePath + ".html";

            task.setProcessed(true);
            task.setProcessedAt(today);
            taskRepository.save(task);

            ntfyService.sendNewTopicNotification(task.getName(), vercelUrl);
            log.info("Task done: {} -> {}", task.getSlug(), vercelUrl);

            long processed = taskRepository.countByProcessedTrue();
            long total = taskRepository.count();
            ntfyService.sendHealthCheckNotification(processed, total);

            sendReviewsIfEligible(task);

        } catch (Exception e) {
            log.error("Failed to process task '{}': {}", task.getSlug(), e.getMessage(), e);
        }
    }

    private void sendReviewsIfEligible(TaskEntity newTask) {
        long processedCount = taskRepository.countByProcessedTrue();
        if (processedCount <= REVIEW_THRESHOLD) {
            log.info("Only {} processed tasks — review skipped (threshold: {})", processedCount, REVIEW_THRESHOLD);
            return;
        }

        List<TaskEntity> reviews = selectReviewTopics(newTask);
        if (reviews.isEmpty()) return;

        try {
            ntfyService.sendReviewNotification(reviews, vercelBaseUrl);
        } catch (Exception e) {
            log.error("Failed to send review notification: {}", e.getMessage(), e);
        }
    }

    private List<TaskEntity> selectReviewTopics(TaskEntity newTask) {
        // Prefer same category for reinforcement learning
        List<TaskEntity> sameCategory = taskRepository
                .findProcessedByCategoryOldestFirst(newTask.getCategory(), PageRequest.of(0, 2))
                .stream()
                .filter(t -> !t.getId().equals(newTask.getId()))
                .toList();

        if (sameCategory.size() >= 2) {
            log.info("Review: 2 topics from same category '{}'", newTask.getCategory());
            return sameCategory;
        }

        // Fill remaining slots with oldest processed overall
        List<TaskEntity> result = new ArrayList<>(sameCategory);
        taskRepository.findByProcessedTrueOrderByProcessedAtAsc(PageRequest.of(0, 5))
                .stream()
                .filter(t -> !t.getId().equals(newTask.getId()))
                .filter(t -> result.stream().noneMatch(r -> r.getId().equals(t.getId())))
                .limit(2 - result.size())
                .forEach(result::add);

        log.info("Review: {} same-category + {} oldest overall", sameCategory.size(), result.size() - sameCategory.size());
        return result;
    }

    private String escapeVueTemplateSyntax(String content) {
        StringBuilder result = new StringBuilder();
        String[] lines = content.split("\n", -1);
        boolean inCodeBlock = false;

        for (String line : lines) {
            if (line.startsWith("```")) {
                inCodeBlock = !inCodeBlock;
            }
            if (inCodeBlock) {
                result.append(line);
            } else {
                result.append(line
                        .replace("{{", "&#123;&#123;")
                        .replace("}}", "&#125;&#125;"));
            }
            result.append("\n");
        }

        return result.toString().stripTrailing();
    }

    private String buildMarkdown(String title, LocalDate date, String aiContent) {
        String[] lines = aiContent.split("\n", 3);
        String difficulty = "medium";
        String body = aiContent;

        if (lines.length > 0 && lines[0].startsWith("DIFFICULTY:")) {
            difficulty = lines[0].replace("DIFFICULTY:", "").trim().toLowerCase();
            body = lines.length > 2 ? lines[2] : "";
        }

        body = escapeVueTemplateSyntax(body);

        String difficultyLabel = switch (difficulty) {
            case "easy" -> "Einfach";
            case "hard" -> "Fortgeschritten";
            default -> "Mittel";
        };

        String difficultyEmoji = switch (difficulty) {
            case "easy" -> "🟢";
            case "hard" -> "🔴";
            default -> "🟡";
        };

        return """
                ---
                outline: deep
                ---

                # %s

                <div class="meta">
                  <span class="difficulty %s">%s %s</span>
                  <span class="status">Bearbeitet ☑️</span>
                  <span class="date">%s</span>
                </div>

                ---

                %s

                <style>
                .meta {
                  display: flex;
                  gap: 0.75rem;
                  margin-top: 0.5rem;
                  flex-wrap: wrap;
                  align-items: center;
                }
                .meta span {
                  display: inline-block;
                  padding: 4px 12px;
                  border-radius: 6px;
                  font-size: 0.85em;
                }
                .difficulty {
                  font-weight: 600;
                }
                .difficulty.easy {
                  background: #064e3b;
                  color: #6ee7b7;
                }
                .difficulty.medium {
                  background: #78350f;
                  color: #fcd34d;
                }
                .difficulty.hard {
                  background: #7f1d1d;
                  color: #fca5a5;
                }
                .status {
                  background: #1e3a5f;
                  color: #93c5fd;
                }
                .date {
                  background: #2a2a2a;
                  color: #aaa;
                  font-size: 0.8em;
                }
                </style>
                """.formatted(title, difficulty, difficultyEmoji, difficultyLabel, date.format(DATE_FORMAT), body.trim());
    }
}
