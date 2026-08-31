BEGIN;

CREATE TEMP TABLE demo_users AS
SELECT
  n AS user_no,
  (substr(md5('demo-user-' || n), 1, 8) || '-' || substr(md5('demo-user-' || n), 9, 4) || '-' || substr(md5('demo-user-' || n), 13, 4) || '-' || substr(md5('demo-user-' || n), 17, 4) || '-' || substr(md5('demo-user-' || n), 21, 12))::uuid AS id,
  'Usuario ' || n AS name,
  'usuario' || n || '@usuario' || n || '.com' AS email
FROM generate_series(1, 10) AS n;

CREATE TEMP TABLE demo_projects AS
SELECT
  p AS project_no,
  ((p - 1) / 2 + 1)::int AS owner_no,
  CASE WHEN p % 2 = 1 THEN (((((p - 1) / 2 + 1)::int) % 10) + 1) ELSE (((((p - 1) / 2 + 2)::int) % 10) + 1) END AS member_no,
  (substr(md5('demo-project-' || p), 1, 8) || '-' || substr(md5('demo-project-' || p), 9, 4) || '-' || substr(md5('demo-project-' || p), 13, 4) || '-' || substr(md5('demo-project-' || p), 17, 4) || '-' || substr(md5('demo-project-' || p), 21, 12))::uuid AS id,
  'Projeto Demo ' || lpad(p::text, 2, '0') AS name
FROM generate_series(1, 20) AS p;

CREATE TEMP TABLE demo_tasks AS
SELECT
  p.project_no,
  t AS task_no,
  (substr(md5('demo-task-' || p.project_no || '-' || t), 1, 8) || '-' || substr(md5('demo-task-' || p.project_no || '-' || t), 9, 4) || '-' || substr(md5('demo-task-' || p.project_no || '-' || t), 13, 4) || '-' || substr(md5('demo-task-' || p.project_no || '-' || t), 17, 4) || '-' || substr(md5('demo-task-' || p.project_no || '-' || t), 21, 12))::uuid AS id,
  p.id AS project_id,
  p.owner_no,
  p.member_no,
  CASE
    WHEN t BETWEEN 1 AND 10 THEN 'IN_PROGRESS'
    WHEN t % 3 = 0 THEN 'DONE'
    ELSE 'TODO'
  END AS status,
  CASE (t + p.project_no) % 4
    WHEN 0 THEN 'LOW'
    WHEN 1 THEN 'MEDIUM'
    WHEN 2 THEN 'HIGH'
    ELSE 'CRITICAL'
  END AS priority,
  CASE
    WHEN t % 10 IN (0, 7) THEN NULL
    WHEN t BETWEEN 1 AND 5 THEN owner_user.id
    WHEN t BETWEEN 6 AND 10 THEN member_user.id
    WHEN t % 2 = 0 THEN owner_user.id
    ELSE member_user.id
  END AS assignee_id
FROM demo_projects p
CROSS JOIN generate_series(1, 50) AS t
JOIN demo_users owner_user ON owner_user.user_no = p.owner_no
JOIN demo_users member_user ON member_user.user_no = p.member_no;

CREATE TEMP TABLE demo_notifications AS
SELECT
  (substr(md5('demo-notification-project-' || p.project_no), 1, 8) || '-' || substr(md5('demo-notification-project-' || p.project_no), 9, 4) || '-' || substr(md5('demo-notification-project-' || p.project_no), 13, 4) || '-' || substr(md5('demo-notification-project-' || p.project_no), 17, 4) || '-' || substr(md5('demo-notification-project-' || p.project_no), 21, 12))::uuid AS id,
  'PROJECT_ADDED' AS type,
  'Voce foi adicionado ao ' || p.name AS message,
  p.id AS project_id,
  NULL::uuid AS task_id,
  owner_user.id AS created_by,
  member_user.id AS recipient_id,
  p.project_no AS sort_no,
  now() - ((p.project_no % 30) || ' days')::interval AS created_at
FROM demo_projects p
JOIN demo_users owner_user ON owner_user.user_no = p.owner_no
JOIN demo_users member_user ON member_user.user_no = p.member_no
UNION ALL
SELECT
  (substr(md5('demo-notification-task-' || t.project_no || '-' || t.task_no), 1, 8) || '-' || substr(md5('demo-notification-task-' || t.project_no || '-' || t.task_no), 9, 4) || '-' || substr(md5('demo-notification-task-' || t.project_no || '-' || t.task_no), 13, 4) || '-' || substr(md5('demo-notification-task-' || t.project_no || '-' || t.task_no), 17, 4) || '-' || substr(md5('demo-notification-task-' || t.project_no || '-' || t.task_no), 21, 12))::uuid AS id,
  'TASK_ASSIGNED' AS type,
  'Voce recebeu a tarefa Demo ' || t.project_no || '-' || t.task_no AS message,
  t.project_id,
  t.id AS task_id,
  actor_user.id AS created_by,
  t.assignee_id AS recipient_id,
  (1000 + t.project_no * 100 + t.task_no) AS sort_no,
  now() - (((t.project_no + t.task_no) % 30) || ' days')::interval AS created_at
FROM demo_tasks t
JOIN demo_users actor_user ON actor_user.user_no = t.owner_no
WHERE t.assignee_id IS NOT NULL AND t.task_no <= 15;

DELETE FROM user_notifications
WHERE notification_id IN (SELECT id FROM demo_notifications)
   OR user_id IN (SELECT id FROM demo_users);

DELETE FROM notifications
WHERE id IN (SELECT id FROM demo_notifications)
   OR project_id IN (SELECT id FROM demo_projects)
   OR task_id IN (SELECT id FROM demo_tasks)
   OR created_by IN (SELECT id FROM demo_users);

DELETE FROM task_logs
WHERE project_id IN (SELECT id FROM demo_projects)
   OR task_id IN (SELECT id FROM demo_tasks)
   OR actor_id IN (SELECT id FROM demo_users);

DELETE FROM tasks
WHERE project_id IN (SELECT id FROM demo_projects)
   OR id IN (SELECT id FROM demo_tasks);

DELETE FROM project_members
WHERE project_id IN (SELECT id FROM demo_projects)
   OR user_id IN (SELECT id FROM demo_users);

DELETE FROM projects
WHERE id IN (SELECT id FROM demo_projects)
   OR owner_id IN (SELECT id FROM demo_users);

DELETE FROM users
WHERE id IN (SELECT id FROM demo_users)
   OR email LIKE 'usuario%@usuario%.com';

INSERT INTO users (id, name, email, password, created_at, updated_at)
SELECT
  id,
  name,
  email,
  '$2b$12$EHfs.CVVRvGwr8/Aho47Kuw1MMwecVv2QznhL2LFHqocOclGAq7TC',
  now() - ((user_no + 20) || ' days')::interval,
  now() - ((user_no + 10) || ' days')::interval
FROM demo_users;

INSERT INTO projects (id, name, description, owner_id, created_at, updated_at, deleted_at)
SELECT
  p.id,
  p.name,
  'Projeto demonstrativo criado pelo seed local ' || p.project_no,
  u.id,
  now() - ((p.project_no + 15) || ' days')::interval,
  now() - ((p.project_no + 5) || ' days')::interval,
  NULL
FROM demo_projects p
JOIN demo_users u ON u.user_no = p.owner_no;

INSERT INTO project_members (id, project_id, user_id, role, joined_at)
SELECT
  (substr(md5('demo-member-admin-' || p.project_no), 1, 8) || '-' || substr(md5('demo-member-admin-' || p.project_no), 9, 4) || '-' || substr(md5('demo-member-admin-' || p.project_no), 13, 4) || '-' || substr(md5('demo-member-admin-' || p.project_no), 17, 4) || '-' || substr(md5('demo-member-admin-' || p.project_no), 21, 12))::uuid,
  p.id,
  u.id,
  'ADMIN',
  now() - ((p.project_no + 14) || ' days')::interval
FROM demo_projects p
JOIN demo_users u ON u.user_no = p.owner_no
UNION ALL
SELECT
  (substr(md5('demo-member-member-' || p.project_no), 1, 8) || '-' || substr(md5('demo-member-member-' || p.project_no), 9, 4) || '-' || substr(md5('demo-member-member-' || p.project_no), 13, 4) || '-' || substr(md5('demo-member-member-' || p.project_no), 17, 4) || '-' || substr(md5('demo-member-member-' || p.project_no), 21, 12))::uuid,
  p.id,
  u.id,
  'MEMBER',
  now() - ((p.project_no + 12) || ' days')::interval
FROM demo_projects p
JOIN demo_users u ON u.user_no = p.member_no;

INSERT INTO tasks (id, project_id, title, description, status, priority, user_id, due_date, created_at, updated_at, deleted_at)
SELECT
  id,
  project_id,
  'Task Demo ' || project_no || '-' || lpad(task_no::text, 2, '0'),
  'Tarefa demonstrativa ' || task_no || ' do projeto ' || project_no,
  status,
  priority,
  assignee_id,
  now() + (((task_no % 21) - 7) || ' days')::interval,
  now() - (((project_no + task_no) % 20 + 1) || ' days')::interval,
  now() - (((project_no + task_no) % 7) || ' days')::interval,
  NULL
FROM demo_tasks;

INSERT INTO task_logs (id, project_id, task_id, actor_id, action, from_status, to_status, created_at)
SELECT
  (substr(md5('demo-log-created-' || t.project_no || '-' || t.task_no), 1, 8) || '-' || substr(md5('demo-log-created-' || t.project_no || '-' || t.task_no), 9, 4) || '-' || substr(md5('demo-log-created-' || t.project_no || '-' || t.task_no), 13, 4) || '-' || substr(md5('demo-log-created-' || t.project_no || '-' || t.task_no), 17, 4) || '-' || substr(md5('demo-log-created-' || t.project_no || '-' || t.task_no), 21, 12))::uuid,
  t.project_id,
  t.id,
  actor.id,
  'TASK_CREATED',
  NULL,
  NULL,
  now() - (((t.project_no + t.task_no) % 20 + 1) || ' days')::interval
FROM demo_tasks t
JOIN demo_users actor ON actor.user_no = t.owner_no
UNION ALL
SELECT
  (substr(md5('demo-log-status-' || t.project_no || '-' || t.task_no), 1, 8) || '-' || substr(md5('demo-log-status-' || t.project_no || '-' || t.task_no), 9, 4) || '-' || substr(md5('demo-log-status-' || t.project_no || '-' || t.task_no), 13, 4) || '-' || substr(md5('demo-log-status-' || t.project_no || '-' || t.task_no), 17, 4) || '-' || substr(md5('demo-log-status-' || t.project_no || '-' || t.task_no), 21, 12))::uuid,
  t.project_id,
  t.id,
  actor.id,
  'STATUS_CHANGED',
  'TODO',
  t.status,
  now() - (((t.project_no + t.task_no) % 10 + 1) || ' days')::interval
FROM demo_tasks t
JOIN demo_users actor ON actor.user_no = t.owner_no
WHERE t.status <> 'TODO'
UNION ALL
SELECT
  (substr(md5('demo-log-priority-' || t.project_no || '-' || t.task_no), 1, 8) || '-' || substr(md5('demo-log-priority-' || t.project_no || '-' || t.task_no), 9, 4) || '-' || substr(md5('demo-log-priority-' || t.project_no || '-' || t.task_no), 13, 4) || '-' || substr(md5('demo-log-priority-' || t.project_no || '-' || t.task_no), 17, 4) || '-' || substr(md5('demo-log-priority-' || t.project_no || '-' || t.task_no), 21, 12))::uuid,
  t.project_id,
  t.id,
  actor.id,
  'PRIORITY_CHANGED',
  NULL,
  NULL,
  now() - (((t.project_no + t.task_no) % 8 + 1) || ' days')::interval
FROM demo_tasks t
JOIN demo_users actor ON actor.user_no = t.owner_no
WHERE t.priority IN ('HIGH', 'CRITICAL')
UNION ALL
SELECT
  (substr(md5('demo-log-assignee-' || t.project_no || '-' || t.task_no), 1, 8) || '-' || substr(md5('demo-log-assignee-' || t.project_no || '-' || t.task_no), 9, 4) || '-' || substr(md5('demo-log-assignee-' || t.project_no || '-' || t.task_no), 13, 4) || '-' || substr(md5('demo-log-assignee-' || t.project_no || '-' || t.task_no), 17, 4) || '-' || substr(md5('demo-log-assignee-' || t.project_no || '-' || t.task_no), 21, 12))::uuid,
  t.project_id,
  t.id,
  actor.id,
  'ASSIGNEE_CHANGED',
  NULL,
  NULL,
  now() - (((t.project_no + t.task_no) % 6 + 1) || ' days')::interval
FROM demo_tasks t
JOIN demo_users actor ON actor.user_no = t.owner_no
WHERE t.assignee_id IS NOT NULL AND t.task_no <= 20;

INSERT INTO notifications (id, type, message, project_id, task_id, created_by, created_at)
SELECT id, type, message, project_id, task_id, created_by, created_at
FROM demo_notifications;

INSERT INTO user_notifications (user_id, notification_id, read_at)
SELECT
  recipient_id,
  id,
  CASE WHEN sort_no % 3 = 0 THEN created_at + interval '1 day' ELSE NULL END
FROM demo_notifications;

DO $$
DECLARE
  user_count integer;
  project_count integer;
  task_count integer;
  wip_violations integer;
BEGIN
  SELECT count(*) INTO user_count FROM users WHERE id IN (SELECT id FROM demo_users);
  SELECT count(*) INTO project_count FROM projects WHERE id IN (SELECT id FROM demo_projects);
  SELECT count(*) INTO task_count FROM tasks WHERE id IN (SELECT id FROM demo_tasks);
  SELECT count(*) INTO wip_violations
  FROM (
    SELECT project_id, user_id, count(*) AS total
    FROM tasks
    WHERE id IN (SELECT id FROM demo_tasks)
      AND status = 'IN_PROGRESS'
      AND user_id IS NOT NULL
    GROUP BY project_id, user_id
    HAVING count(*) > 5
  ) violations;

  IF user_count <> 10 OR project_count <> 20 OR task_count <> 1000 OR wip_violations <> 0 THEN
    RAISE EXCEPTION 'Invalid seed result: users %, projects %, tasks %, wip violations %', user_count, project_count, task_count, wip_violations;
  END IF;
END $$;

COMMIT;
