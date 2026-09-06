INSERT INTO clients (id, first_name, last_name, phone, email, birth_date)
VALUES
    (1, 'Иван', 'Иванов', '+79991111111', 'ivan.ivanov@example.com', DATE '1998-04-12'),
    (2, 'Мария', 'Петрова', '+79992222222', 'maria.petrova@example.com', DATE '2001-09-23'),
    (3, 'Алексей', 'Смирнов', '+79993333333', 'alexey.smirnov@example.com', DATE '1995-01-30'),
    (4, 'Ольга', 'Соколова', '+79994444444', NULL, DATE '1989-12-05');

INSERT INTO visits (id, client_id, visit_date, start_time, duration_minutes, lane_number, status)
VALUES
    (1, 1, CURRENT_DATE - INTERVAL '10 days', TIME '10:00', 60, 1, 'COMPLETED'),
    (2, 1, CURRENT_DATE + INTERVAL '1 day', TIME '12:00', 90, 2, 'PLANNED'),
    (3, 2, CURRENT_DATE, TIME '09:30', 45, 3, 'IN_PROGRESS'),
    (4, 3, CURRENT_DATE - INTERVAL '3 days', TIME '18:00', 60, 2, 'CANCELLED'),
    (5, 4, CURRENT_DATE + INTERVAL '2 days', TIME '16:00', 120, 4, 'PLANNED');

SELECT setval('clients_id_seq', (SELECT max(id) FROM clients), true);
SELECT setval('visits_id_seq', (SELECT max(id) FROM visits), true);
