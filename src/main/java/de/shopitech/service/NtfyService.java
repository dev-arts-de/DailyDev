package de.shopitech.service;

import de.shopitech.model.TaskEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

@Service
@Slf4j
public class NtfyService {

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Value("${dailyDev.ntfy.topic}")
    private String topic;

    @Value("${dailyDev.ntfy.baseUrl:https://ntfy.sh}")
    private String baseUrl;

    public void sendNewTopicNotification(String taskName, String vercelUrl) throws Exception {
        post(ascii(taskName), "default", "books,computer", vercelUrl, vercelUrl);
        log.info("ntfy: new topic sent — {}", taskName);
    }

    public void sendReviewNotification(List<TaskEntity> reviews, String vercelBaseUrl) throws Exception {
        if (reviews.isEmpty()) return;

        String body = reviews.stream()
                .map(t -> "• " + t.getName() + "\n  " + vercelBaseUrl + "/topics/" + t.getCategory() + "/" + t.getSlug() + ".html")
                .reduce("", (a, b) -> a.isBlank() ? b : a + "\n\n" + b);

        String clickUrl = vercelBaseUrl + "/topics/" + reviews.get(0).getCategory() + "/" + reviews.get(0).getSlug() + ".html";
        post("Wiederholung: " + reviews.size() + " Themen", "low", "repeat,books", clickUrl, body);
        log.info("ntfy: review notification sent for {} topics", reviews.size());
    }

    public void sendHealthCheckNotification(long processed, long total) throws Exception {
        long percent = total > 0 ? (processed * 100 / total) : 0;
        String body = processed + " von " + total + " Topics verarbeitet (" + percent + "% fertig)";
        post("DailyDev Status", "low", "white_check_mark", baseUrl, body);
        log.info("ntfy: health check sent — {}/{}", processed, total);
    }

    public void sendAllProcessedNotification(long total) throws Exception {
        String body = "Alle " + total + " Topics wurden verarbeitet. Die Wissensdatenbank ist vollstandig!";
        post("DailyDev abgeschlossen", "high", "tada,books", baseUrl, body);
        log.info("ntfy: all-processed notification sent — {} topics total", total);
    }

    private void post(String title, String priority, String tags, String clickUrl, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/" + topic))
                .header("Title", ascii(title))
                .header("Priority", priority)
                .header("Tags", tags)
                .header("Click", clickUrl)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new RuntimeException("ntfy request failed: " + response.statusCode());
        }
    }

    private String ascii(String input) {
        return input.replaceAll("[^\\x20-\\x7E]", "").trim();
    }
}
