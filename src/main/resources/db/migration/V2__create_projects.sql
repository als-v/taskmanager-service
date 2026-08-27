CREATE TABLE projects (
    id UUID NOT NULL,
    name VARCHAR(150) NOT NULL,
    description VARCHAR(2000),
    owner_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT pk_projects PRIMARY KEY (id),
    CONSTRAINT fk_projects_owner FOREIGN KEY (owner_id) REFERENCES users (id)
);
