CREATE TABLE users
(id BIGINT AUTO_INCREMENT PRIMARY KEY,
 first_name VARCHAR(30),
 last_name VARCHAR(30),
 email VARCHAR(30),
 admin BOOLEAN,
 last_login TIMESTAMP,
 is_active BOOLEAN,
 pass VARCHAR(300));
--;;
INSERT INTO users VALUES
(null,'first','last','demo@example.com',false,null,true,null);
