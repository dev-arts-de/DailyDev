package de.shopitech.service;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.MessageCreateParams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
@Slf4j
public class AnthropicService {

    @Value("${dailyDev.anthropic.apiKey}")
    private String apiKey;

    public String generateMarkdownContent(String topicName, String category) {
        AnthropicClient client = AnthropicOkHttpClient.builder()
                .apiKey(apiKey)
                .build();

        var message = client.messages().create(
                MessageCreateParams.builder()
                        .model("claude-haiku-4-5-20251001")
                        .maxTokens(2048)
                        .addUserMessage(buildPrompt(topicName, category))
                        .build()
        );

        String content = message.content().stream()
                .filter(b -> b.text().isPresent())
                .map(b -> b.text().get().text())
                .collect(Collectors.joining());

        log.debug("Generated content for topic: {}", topicName);
        return content;
    }

    private String buildPrompt(String topicName, String category) {
        return """
                Du bist ein erfahrener Java-Entwickler und erstellst lernoptimierte Notizen für andere Java-Entwickler.
                Schreibe KEIN Fließtext-Wuste. Nutze kurze Absätze, Hervorhebungen, strukturierte Abschnitte.

                Thema: %s
                Kategorie: %s

                Antworte NUR mit folgendem Format (keine weiteren Erklärungen):

                DIFFICULTY: easy|medium|hard

                ## Zusammenfassung

                > [1-2 Sätze: Was ist das? Wofür braucht man es?]

                ## Kernkonzept

                [Erkläre das Konzept in 2-3 kurzen, präzisen Absätzen. Kein Fließtext-Block. Nutze **Fettdruck** für Schlüsselbegriffe.]

                ## Code-Beispiel

                ```java
                [Praxisnahes, kommentiertes Java-Beispiel]
                ```

                ## Wichtige Punkte

                - [konkreter Punkt]
                - [konkreter Punkt]
                - [konkreter Punkt]
                - [konkreter Punkt]
                - [konkreter Punkt]

                ## Klassische Fragen

                ### [Frage]?
                [Präzise Antwort in 2-4 Sätzen]

                ---

                ### [Frage]?
                [Präzise Antwort in 2-4 Sätzen]

                ---

                ### [Frage]?
                [Präzise Antwort in 2-4 Sätzen]

                ---

                ## Wusstest du schon?

                [Ein überraschender, witziger oder unbekannter Fakt zum Thema — gerne auch historisch oder kurios]
                """.formatted(topicName, category);
    }
}
