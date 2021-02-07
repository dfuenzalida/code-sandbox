CREATE TABLE tokens
(user_id BIGINT,
 token VARCHAR(36),
 created TIMESTAMP,
 foreign key (user_id) references users(id));

