-- Reference schema, kept in sync with the JPA entities in com.brm.app.entity.
--
-- You do NOT need to run this: the app creates and updates these tables itself
-- (spring.jpa.hibernate.ddl-auto: update). It is here so the data model can be read in one place,
-- and as a starting point if you later switch to ddl-auto: validate with migrations (Flyway/Liquibase).

CREATE TABLE users (
    id          UUID PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    email       VARCHAR(150) NOT NULL UNIQUE,         -- stored lower-case
    password    VARCHAR(255) NOT NULL,                -- BCrypt hash
    role        VARCHAR(20)  NOT NULL,                -- ROOM_ADMIN | ROOM_MEMBER (label only, see note below)
    created_at  TIMESTAMP    NOT NULL
);
-- Note: admin rights are decided per room (rooms.admin_id), not by users.role. Someone who admins
-- one room is just a member of any other room they join.

CREATE TABLE rooms (
    id           UUID PRIMARY KEY,
    name         VARCHAR(150) NOT NULL,
    admin_id     UUID NOT NULL REFERENCES users(id),
    invite_code  VARCHAR(20) UNIQUE,
    created_at   TIMESTAMP NOT NULL
);

CREATE TABLE room_members (
    id         UUID PRIMARY KEY,
    room_id    UUID NOT NULL REFERENCES rooms(id),
    user_id    UUID NOT NULL REFERENCES users(id),
    joined_at  TIMESTAMP NOT NULL,
    UNIQUE (room_id, user_id)
);

-- ---------- Expenses & money ----------

CREATE TABLE expenses (
    id            UUID PRIMARY KEY,
    room_id       UUID NOT NULL REFERENCES rooms(id),
    paid_by       UUID NOT NULL REFERENCES users(id),
    amount        NUMERIC(12, 2) NOT NULL,
    description   VARCHAR(255) NOT NULL,
    split_type    VARCHAR(20) NOT NULL,               -- EQUAL | CUSTOM
    expense_date  DATE NOT NULL,
    created_at    TIMESTAMP NOT NULL
);

-- One row per person per expense. Shares always add up to expenses.amount.
CREATE TABLE expense_shares (
    id            UUID PRIMARY KEY,
    expense_id    UUID NOT NULL REFERENCES expenses(id),
    user_id       UUID NOT NULL REFERENCES users(id),
    share_amount  NUMERIC(12, 2) NOT NULL
);

-- A member says they paid someone (normally the admin); only CONFIRMED rows change balances.
CREATE TABLE payments (
    id            UUID PRIMARY KEY,
    room_id       UUID NOT NULL REFERENCES rooms(id),
    from_user_id  UUID NOT NULL REFERENCES users(id),
    to_user_id    UUID NOT NULL REFERENCES users(id),
    amount        NUMERIC(12, 2) NOT NULL,
    note          VARCHAR(255),
    status        VARCHAR(20) NOT NULL,               -- PENDING | CONFIRMED | REJECTED
    created_at    TIMESTAMP NOT NULL,
    resolved_at   TIMESTAMP
);
-- Balance per person = paid - owed + payments sent - payments received (confirmed only).

-- ---------- Tasks & chores ----------

CREATE TABLE recurring_chores (
    id                  UUID PRIMARY KEY,
    room_id             UUID NOT NULL REFERENCES rooms(id),
    created_by          UUID NOT NULL REFERENCES users(id),
    title               VARCHAR(200) NOT NULL,
    description         VARCHAR(2000),
    day_of_week         VARCHAR(12) NOT NULL,         -- MONDAY .. SUNDAY
    assigned_to         UUID REFERENCES users(id),    -- who is NEXT UP
    rotate_assignee     BOOLEAN NOT NULL,
    active              BOOLEAN NOT NULL,
    last_generated_date DATE,                         -- makes midnight generation idempotent
    created_at          TIMESTAMP NOT NULL
);

CREATE TABLE tasks (
    id            UUID PRIMARY KEY,
    room_id       UUID NOT NULL REFERENCES rooms(id),
    assigned_to   UUID REFERENCES users(id),
    created_by    UUID NOT NULL REFERENCES users(id),
    title         VARCHAR(200) NOT NULL,
    description   VARCHAR(2000),
    status        VARCHAR(20) NOT NULL,               -- PENDING | DONE
    due_date      DATE,
    is_recurring  BOOLEAN NOT NULL,                   -- true when generated from a recurring chore
    chore_id      UUID REFERENCES recurring_chores(id),
    completed_at  TIMESTAMP,
    created_at    TIMESTAMP NOT NULL
);

-- ---------- Board / media ----------

CREATE TABLE media_posts (
    id               UUID PRIMARY KEY,
    room_id          UUID NOT NULL REFERENCES rooms(id),
    user_id          UUID NOT NULL REFERENCES users(id),
    caption          VARCHAR(1000),
    file_url         VARCHAR(500),                    -- null for text-only posts
    stored_filename  VARCHAR(100),
    content_type     VARCHAR(100),
    file_type        VARCHAR(20),                     -- IMAGE | VIDEO
    posted_at        TIMESTAMP NOT NULL
);
