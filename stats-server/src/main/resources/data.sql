-- Очистка таблицы
DELETE FROM endpoint_hits;

-- Тестовые данные для статистики
INSERT INTO endpoint_hits (app, uri, ip, timestamp) VALUES
-- Данные для /events (основной тест)
('ewm-main-service', '/events', '192.168.1.1', '2025-11-06 10:00:00'),
('ewm-main-service', '/events', '192.168.1.2', '2025-11-06 10:05:00'),
('ewm-main-service', '/events', '192.168.1.3', '2025-11-06 10:10:00'),

-- Данные для конкретных event ID
('ewm-main-service', '/events/1', '192.168.1.10', '2025-11-06 11:00:00'),
('ewm-main-service', '/events/1', '192.168.1.11', '2025-11-06 11:05:00'),
('ewm-main-service', '/events/1', '192.168.1.12', '2025-11-06 11:10:00'),

('ewm-main-service', '/events/2', '192.168.1.20', '2025-11-06 12:00:00'),
('ewm-main-service', '/events/2', '192.168.1.21', '2025-11-06 12:05:00'),

('ewm-main-service', '/events/3', '192.168.1.30', '2025-11-06 13:00:00'),
('ewm-main-service', '/events/3', '192.168.1.31', '2025-11-06 13:05:00'),
('ewm-main-service', '/events/3', '192.168.1.32', '2025-11-06 13:10:00'),
('ewm-main-service', '/events/3', '192.168.1.33', '2025-11-06 13:15:00'),

-- Данные для сортировки
('ewm-main-service', '/events/9', '192.168.1.90', '2025-11-06 14:00:00'),
('ewm-main-service', '/events/9', '192.168.1.91', '2025-11-06 14:05:00'),

('ewm-main-service', '/events/10', '192.168.1.100', '2025-11-06 15:00:00'),
('ewm-main-service', '/events/10', '192.168.1.101', '2025-11-06 15:05:00'),
('ewm-main-service', '/events/10', '192.168.1.102', '2025-11-06 15:10:00');