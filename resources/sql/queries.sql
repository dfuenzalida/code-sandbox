-- :name create-user! :! :n
-- :doc creates a new user record
INSERT INTO users
(id, first_name, last_name, email, pass)
VALUES (:id, :first_name, :last_name, :email, :pass)

-- :name update-user! :! :n
-- :doc updates an existing user record
UPDATE users
SET first_name = :first_name, last_name = :last_name, email = :email
WHERE id = :id

-- :name get-user :? :1
-- :doc retrieves a user record given the id
SELECT * FROM users
WHERE id = :id

-- :name get-user-by-email :? :1
-- :doc retrieves a user record given their email
SELECT * FROM users
WHERE email = :email

-- :name delete-user! :! :n
-- :doc deletes a user record given the id
DELETE FROM users
WHERE id = :id

-- :name create-token! :! :n
-- :doc creates a new token for a given user id
INSERT INTO tokens
(user_id, token, created)
VALUES (:user_id, :token, now())

-- :name get-token :? :1
-- :doc finds a token by its token column
SELECT * FROM tokens
WHERE token = :token

-- :name username-by-token! :? :1
-- :doc retrieve the username for a given token, if any
SELECT email FROM users
WHERE id in (SELECT user_id FROM tokens WHERE token = :token)

-- :name create-task! :i! :raw
-- :doc creates a new task for a given user id and data
INSERT INTO tasks
(user_id, name, state, lang, code, created_date)
VALUES (:user_id, :name, '0', :lang, :code, now())

-- :name get-tasks-for-user :? :*
-- :doc find the tasks for a given user-id
SELECT * FROM tasks
WHERE user_id = :user_id

-- :name all-tasks :? :*
-- :doc get all tasks
SELECT * from tasks
