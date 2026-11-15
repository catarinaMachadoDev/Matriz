CREATE TABLE app_user (
 id uuid PRIMARY KEY, username varchar(80) NOT NULL UNIQUE,
 display_name varchar(120) NOT NULL, password_hash varchar(100) NOT NULL
);
CREATE TABLE project (
 id uuid PRIMARY KEY, name varchar(100) NOT NULL CHECK(length(trim(name))>0),
 description varchar(1000) NOT NULL DEFAULT '', manager_id uuid NOT NULL REFERENCES app_user(id),
 version bigint NOT NULL DEFAULT 0
);
CREATE TABLE project_member (
 project_id uuid REFERENCES project(id) ON DELETE CASCADE,
 user_id uuid REFERENCES app_user(id), PRIMARY KEY(project_id,user_id)
);
CREATE TABLE task (
 id uuid PRIMARY KEY, project_id uuid NOT NULL REFERENCES project(id) ON DELETE CASCADE,
 title varchar(120) NOT NULL CHECK(length(trim(title))>0),
 description varchar(2000) NOT NULL CHECK(length(trim(description))>0),
 status varchar(20) NOT NULL DEFAULT 'todo' CHECK(status IN ('todo','in_progress','done')),
 priority varchar(10) NOT NULL DEFAULT 'medium' CHECK(priority IN ('low','medium','high')),
 version bigint NOT NULL DEFAULT 0, UNIQUE(id,project_id)
);
CREATE TABLE task_assignee (
 task_id uuid NOT NULL, project_id uuid NOT NULL, user_id uuid NOT NULL,
 PRIMARY KEY(task_id,user_id),
 FOREIGN KEY(task_id,project_id) REFERENCES task(id,project_id) ON DELETE CASCADE,
 FOREIGN KEY(project_id,user_id) REFERENCES project_member(project_id,user_id) ON DELETE CASCADE
);
CREATE TABLE task_comment (
 id uuid PRIMARY KEY, task_id uuid NOT NULL REFERENCES task(id) ON DELETE CASCADE,
 author_id uuid NOT NULL REFERENCES app_user(id), content varchar(2000) NOT NULL CHECK(length(trim(content))>0),
 created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX task_project_idx ON task(project_id);
CREATE INDEX membership_user_idx ON project_member(user_id);
CREATE INDEX comment_task_idx ON task_comment(task_id,created_at);
