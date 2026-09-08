INSERT INTO system_users (id, username, password_hash, role, active)
VALUES
    (
        1,
        'admin',
        '120000:2OjWAxHWMJa/7kO9FeEq3w==:7LBmVkUFrLP4+hj5DEANKqCIkURTueupF+3GAfEIaiU=',
        'ADMIN',
        TRUE
    ),
    (
        2,
        'operator1',
        '120000:vCjtAoJ3dbrYo+R7DU6H9Q==:Ctxx1oMZsn2uqWUzFvjD5h89aiBg4DiqVDiwzUe+DnA=',
        'OPERATOR',
        TRUE
    );

INSERT INTO clients (id, first_name, last_name, phone, email, birth_date)
VALUES
    (1, 'Иван', 'Иванов', '+79991111111', 'ivan.ivanov@example.com', DATE '1998-04-12'),
    (2, 'Мария', 'Петрова', '+79992222222', 'maria.petrova@example.com', DATE '2001-09-23'),
    (3, 'Алексей', 'Смирнов', '+79993333333', 'alexey.smirnov@example.com', DATE '1995-01-30'),
    (4, 'Ольга', 'Соколова', '+79994444444', NULL, DATE '1989-12-05'),
    (5, 'Дмитрий', 'Кузнецов', '+79995555555', 'dmitry.kuznetsov@example.com', DATE '1992-07-18'),
    (6, 'Елена', 'Попова', '+79996666666', 'elena.popova@example.com', DATE '1999-11-02'),
    (7, 'Сергей', 'Волков', '+79997777777', NULL, DATE '1987-03-14'),
    (8, 'Анна', 'Морозова', '+79998888888', 'anna.morozova@example.com', DATE '2003-06-27');

INSERT INTO visits (
    id,
    client_id,
    visit_date,
    start_time,
    duration_minutes,
    lane_number,
    status,
    created_by
)
VALUES
    (1, 1, CURRENT_DATE - 30, TIME '10:00', 60, 1, 'COMPLETED', 1),
    (2, 2, CURRENT_DATE - 25, TIME '12:00', 90, 2, 'COMPLETED', 2),
    (3, 3, CURRENT_DATE - 20, TIME '09:00', 45, 3, 'CANCELLED', 1),
    (4, 4, CURRENT_DATE - 15, TIME '18:00', 60, 4, 'COMPLETED', 2),
    (5, 5, CURRENT_DATE - 10, TIME '07:30', 120, 5, 'COMPLETED', 1),
    (6, 6, CURRENT_DATE - 7, TIME '16:00', 30, 6, 'CANCELLED', 2),
    (7, 7, CURRENT_DATE - 3, TIME '11:00', 90, 7, 'COMPLETED', 1),
    (8, 8, CURRENT_DATE - 1, TIME '14:00', 60, 8, 'COMPLETED', 2),
    (9, 1, CURRENT_DATE, TIME '08:00', 60, 1, 'IN_PROGRESS', 1),
    (10, 2, CURRENT_DATE, TIME '10:00', 45, 2, 'PLANNED', 2),
    (11, 3, CURRENT_DATE + 1, TIME '09:00', 60, 3, 'PLANNED', 1),
    (12, 4, CURRENT_DATE + 1, TIME '11:00', 90, 4, 'PLANNED', 2),
    (13, 5, CURRENT_DATE + 2, TIME '13:00', 120, 5, 'PLANNED', 1),
    (14, 6, CURRENT_DATE + 3, TIME '15:00', 30, 6, 'PLANNED', 2),
    (15, 7, CURRENT_DATE + 4, TIME '17:00', 45, 7, 'PLANNED', 1),
    (16, 8, CURRENT_DATE + 5, TIME '19:00', 60, 8, 'PLANNED', 2),
    (17, 1, CURRENT_DATE + 6, TIME '07:00', 90, 2, 'PLANNED', 1),
    (18, 2, CURRENT_DATE + 7, TIME '08:30', 120, 3, 'PLANNED', 2),
    (19, 3, CURRENT_DATE + 8, TIME '12:30', 60, 4, 'PLANNED', 1),
    (20, 4, CURRENT_DATE + 9, TIME '16:30', 45, 5, 'PLANNED', 2);

SELECT setval('system_users_id_seq', (SELECT max(id) FROM system_users), true);
SELECT setval('clients_id_seq', (SELECT max(id) FROM clients), true);
SELECT setval('visits_id_seq', (SELECT max(id) FROM visits), true);
