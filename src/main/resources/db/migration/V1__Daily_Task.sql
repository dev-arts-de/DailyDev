CREATE TABLE daily_tasks
(
    id               UUID         DEFAULT gen_random_uuid() PRIMARY KEY,
    daily_tasks_name VARCHAR(255) NOT NULL,
    jscope_path      VARCHAR(255) NOT NULL,
    category         VARCHAR(255) NOT NULL,
    task_order       INT          NOT NULL,
    processed        BOOLEAN      NOT NULL DEFAULT FALSE
);
