CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    user_name VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL
);

-- Пароли test_password_1 и test_password_2
INSERT INTO users (user_name, password) VALUES
('user1', '$2a$12$/TqnqqpO4aODaFB689Lp2u9OKTfHIxhItprIFsgRoUEm/QyA9Eujy'),
('user2', '$2a$12$qxrkeP3LtcyGYI/IPlfvQOHjgRdlCAhFqZ2LQpToWRuymgCxCtlbu');

ALTER TABLE orders
ADD COLUMN user_id BIGINT NULL,
ADD CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES users(id);