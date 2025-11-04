-- Создание базы данных для сервиса событий
CREATE DATABASE IF NOT EXISTS event_service;
USE event_service;

-- Таблица категорий событий
CREATE TABLE categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
)

-- Таблица пользователей (инициаторов событий)
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(250) NOT NULL,
    email VARCHAR(254) NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
)

-- Таблица событий
CREATE TABLE events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    annotation VARCHAR(2000) NOT NULL,
    category_id BIGINT NOT NULL,
    confirmed_requests INT DEFAULT 0,
    created_on TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    description VARCHAR(7000),
    event_date TIMESTAMP NOT NULL,
    initiator_id BIGINT NOT NULL,
    lat FLOAT,
    lon FLOAT,
    paid BOOLEAN DEFAULT FALSE,
    participant_limit INT DEFAULT 0,
    published_on TIMESTAMP NULL,
    request_moderation BOOLEAN DEFAULT TRUE,
    state ENUM('PENDING', 'PUBLISHED', 'CANCELED') DEFAULT 'PENDING',
    title VARCHAR(120) NOT NULL,

    -- Внешние ключи
    FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE RESTRICT,
    FOREIGN KEY (initiator_id) REFERENCES users(id) ON DELETE CASCADE,

    -- Индексы для оптимизации запросов
    INDEX idx_events_category (category_id),
    INDEX idx_events_initiator (initiator_id),
    INDEX idx_events_state (state),
    INDEX idx_events_event_date (event_date),
    INDEX idx_events_published (published_on)
)

-- Таблица заявок на участие в событиях
CREATE TABLE requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    created TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    event_id BIGINT NOT NULL,
    requester_id BIGINT NOT NULL,
    status ENUM('PENDING', 'CONFIRMED', 'REJECTED', 'CANCELED') DEFAULT 'PENDING',

    -- Внешние ключи
    FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE,
    FOREIGN KEY (requester_id) REFERENCES users(id) ON DELETE CASCADE,

    -- Уникальный индекс для предотвращения дублирования заявок
    UNIQUE KEY unique_event_requester (event_id, requester_id),

    -- Индексы для оптимизации
    INDEX idx_requests_event (event_id),
    INDEX idx_requests_requester (requester_id),
    INDEX idx_requests_status (status)
)

-- Таблица подборок событий
CREATE TABLE compilations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    pinned BOOLEAN DEFAULT FALSE NOT NULL,
    title VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
)

-- Связующая таблица для связи подборок и событий (многие-ко-многим)
CREATE TABLE compilation_events (
    compilation_id BIGINT NOT NULL,
    event_id BIGINT NOT NULL,

    -- Внешние ключи
    FOREIGN KEY (compilation_id) REFERENCES compilations(id) ON DELETE CASCADE,
    FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE,

    -- Составной первичный ключ
    PRIMARY KEY (compilation_id, event_id),

    -- Индексы для оптимизации
    INDEX idx_compilation_events_event (event_id)
)