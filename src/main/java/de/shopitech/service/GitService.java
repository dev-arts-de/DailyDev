package de.shopitech.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
public class GitService {

    @Value("${dailyDev.git.repoUrl}")
    private String repoUrl;

    @Value("${dailyDev.git.localPath}")
    private String localPath;

    @Value("${dailyDev.git.token}")
    private String token;

    @Value("${dailyDev.git.branch:main}")
    private String branch;

    public String commitAndPush(String category, String slug, String content) throws Exception {
        File repoDir = new File(localPath);

        if (!repoDir.exists()) {
            cloneRepo(repoDir);
        } else {
            pull(repoDir);
        }

        File categoryDir = new File(repoDir, "docs/topics/" + category);
        categoryDir.mkdirs();

        File mdFile = new File(categoryDir, slug + ".md");
        Files.writeString(mdFile.toPath(), content);
        log.info("Wrote markdown file: {}", mdFile.getAbsolutePath());

        runGit(repoDir, "add", mdFile.getAbsolutePath());
        runGit(repoDir, "commit", "-m", "feat(daily): add " + slug);
        runGit(repoDir, "push", getAuthenticatedUrl(), branch);
        log.info("Pushed commit for: {}", slug);

        return "/" + category + "/" + slug;
    }

    private void cloneRepo(File repoDir) throws Exception {
        log.info("Cloning repository to {}", localPath);
        runGit(null, "clone", getAuthenticatedUrl(), repoDir.getAbsolutePath());
        runGit(repoDir, "remote", "set-url", "origin", getAuthenticatedUrl());
        configureUser(repoDir);
    }

    private void pull(File repoDir) throws Exception {
        log.info("Pulling latest changes");
        runGit(repoDir, "pull", "--rebase", getAuthenticatedUrl(), branch);
    }

    private void configureUser(File repoDir) throws Exception {
        runGit(repoDir, "config", "user.email", "dailydev@bot.local");
        runGit(repoDir, "config", "user.name", "DailyDev Bot");
    }

    private String getAuthenticatedUrl() {
        return repoUrl.replace("https://", "https://x-access-token:" + token + "@");
    }

    private void runGit(File workingDir, String... args) throws Exception {
        List<String> command = new ArrayList<>();
        command.add("git");
        command.addAll(Arrays.asList(args));

        ProcessBuilder pb = new ProcessBuilder(command);
        if (workingDir != null) {
            pb.directory(workingDir);
        }
        pb.redirectErrorStream(true);

        Process process = pb.start();
        String output = new String(process.getInputStream().readAllBytes());
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new RuntimeException("Git command failed (exit %d): %s".formatted(exitCode, output));
        }
        log.debug("git {}: {}", args[0], output.isBlank() ? "ok" : output.trim());
    }
}
