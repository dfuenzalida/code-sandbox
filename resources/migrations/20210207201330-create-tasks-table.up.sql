CREATE TABLE tasks
(id BIGINT AUTO_INCREMENT PRIMARY KEY,
 user_id BIGINT,
 name VARCHAR(300),
 state VARCHAR(30),
 lang VARCHAR(30),
 code VARCHAR(65000),
 stdout VARCHAR(65000),
 sterr VARCHAR(65000),
 exit_code INT,
 created_date TIMESTAMP,
 started_date TIMESTAMP,
 end_date TIMESTAMP,
 foreign key (user_id) references users(id));

