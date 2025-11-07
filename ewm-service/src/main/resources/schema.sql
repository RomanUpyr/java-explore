
-- Таблица категорий событий
CREATE TABLE IF NOT EXISTS categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Таблица пользователей
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(250) NOT NULL,
    email VARCHAR(254) NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Таблица событий
CREATE TABLE IF NOT EXISTS events (
    id BIGSERIAL PRIMARY KEY,
    annotation VARCHAR(2000) NOT NULL,
    category_id BIGINT NOT NULL,
    confirmed_requests INTEGER DEFAULT 0,
    created_on TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    description VARCHAR(7000),
    event_date TIMESTAMP NOT NULL,
    initiator_id BIGINT NOT NULL,
    lat REAL,
    lon REAL,
    paid BOOLEAN DEFAULT FALSE,
    participant_limit INTEGER DEFAULT 0,
    published_on TIMESTAMP NULL,
    request_moderation BOOLEAN DEFAULT TRUE,
    state event_state DEFAULT 'PENDING',
    title VARCHAR(120) NOT NULL,

    -- Внешние ключи
    FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE RESTRICT,
    FOREIGN KEY (initiator_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Индексы для таблицы events
CREATE INDEX IF NOT EXISTS idx_events_category ON events(category_id);
CREATE INDEX IF NOT EXISTS idx_events_initiator ON events(initiator_id);
CREATE INDEX IF NOT EXISTS idx_events_state ON events(state);
CREATE INDEX IF NOT EXISTS idx_events_event_date ON events(event_date);
CREATE INDEX IF NOT EXISTS idx_events_published ON events(published_on);

-- Таблица заявок на участие в событиях
CREATE TABLE IF NOT EXISTS requests (
    id BIGSERIAL PRIMARY KEY,
    created TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    event_id BIGINT NOT NULL,
    requester_id BIGINT NOT NULL,
    status request_status DEFAULT 'PENDING',

    -- Внешние ключи
    FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE,
    FOREIGN KEY (requester_id) REFERENCES users(id) ON DELETE CASCADE,

    -- Уникальный индекс для предотвращения дублирования заявок
    UNIQUE (event_id, requester_id)
);

-- Индексы для таблицы requests
CREATE INDEX IF NOT EXISTS idx_requests_event ON requests(event_id);
CREATE INDEX IF NOT EXISTS idx_requests_requester ON requests(requester_id);
CREATE INDEX IF NOT EXISTS idx_requests_status ON requests(status);

-- Таблица подборок событий
CREATE TABLE IF NOT EXISTS compilations (
    id BIGSERIAL PRIMARY KEY,
    pinned BOOLEAN DEFAULT FALSE NOT NULL,
    title VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Связующая таблица для связи подборок и событий
CREATE TABLE IF NOT EXISTS compilation_events (
    compilation_id BIGINT NOT NULL,
    event_id BIGINT NOT NULL,

    -- Внешние ключи
    FOREIGN KEY (compilation_id) REFERENCES compilations(id) ON DELETE CASCADE,
    FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE,

    -- Составной первичный ключ
    PRIMARY KEY (compilation_id, event_id)
);

-- Индекс для таблицы compilation_events
CREATE INDEX IF NOT EXISTS idx_compilation_events_event ON compilation_events(event_id);

