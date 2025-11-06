DELETE FROM endpoint_hits;

-- Тестовые данные для статистики
INSERT INTO endpoint_hits (app, uri, ip, timestamp) VALUES
-- Данные для /events (основной тест)
('ewm-main-service', '/events', '192.168.1.1', '2025-11-06 10:00:00'),
('ewm-main-service', '/events', '192.168.1.2', '2025-11-06 10:05:00'),
('ewm-main-service', '/events', '192.168.1.1', '2025-11-06 10:10:00'),
('ewm-main-service', '/events', '192.168.1.3', '2025-11-06 10:15:00'),

-- Данные для /events/1
('ewm-main-service', '/events/1', '192.168.1.10', '2025-11-06 11:00:00'),
('ewm-main-service', '/events/1', '192.168.1.11', '2025-11-06 11:05:00'),

-- Данные для /events/2
('ewm-main-service', '/events/2', '192.168.1.20', '2025-11-06 12:00:00'),
('ewm-main-service', '/events/2', '192.168.1.21', '2025-11-06 12:05:00'),
('ewm-main-service', '/events/2', '192.168.1.22', '2025-11-06 12:10:00'),

-- Данные для /events/3
('ewm-main-service', '/events/3', '192.168.1.30', '2025-11-06 13:00:00');