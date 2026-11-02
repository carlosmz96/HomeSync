-- HomeSync - V2: hogares, membresías e invitaciones
CREATE TABLE households (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    timezone VARCHAR(64) NOT NULL DEFAULT 'Europe/Madrid',
    created_by UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT ck_households_name_nonblank CHECK (length(trim(name)) > 0)
);

CREATE INDEX ix_households_active ON households(id) WHERE deleted_at IS NULL;

CREATE TABLE household_memberships (
    id UUID PRIMARY KEY,
    household_id UUID NOT NULL REFERENCES households(id) ON DELETE RESTRICT,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    role VARCHAR(10) NOT NULL CHECK (role IN ('ADMIN', 'MEMBER')),
    status VARCHAR(10) NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('ACTIVE', 'REMOVED')),
    joined_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    removed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_membership_household_user UNIQUE (household_id, user_id),
    CONSTRAINT uq_membership_household_id_id UNIQUE (household_id, id),
    CONSTRAINT uq_membership_household_id_user UNIQUE (household_id, id, user_id),
    CONSTRAINT ck_membership_status_dates CHECK (
        (status = 'ACTIVE' AND removed_at IS NULL) OR
        (status = 'REMOVED' AND removed_at IS NOT NULL)
    )
);
CREATE INDEX ix_memberships_user_status ON household_memberships(user_id, status);
CREATE INDEX ix_memberships_household_status
    ON household_memberships(household_id, status, role);

CREATE TABLE household_invitations (
    id UUID PRIMARY KEY,
    household_id UUID NOT NULL,
    created_by UUID NOT NULL,
    token_hash TEXT NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMPTZ NOT NULL DEFAULT (CURRENT_TIMESTAMP + INTERVAL '7 days'),
    consumed_at TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ,
    CONSTRAINT fk_invitation_household
        FOREIGN KEY (household_id) REFERENCES households(id) ON DELETE RESTRICT,
    CONSTRAINT fk_invitation_creator_membership
        FOREIGN KEY (household_id, created_by)
        REFERENCES household_memberships(household_id, user_id) ON DELETE RESTRICT,
    CONSTRAINT ck_invitation_expiry CHECK (expires_at > created_at),
    CONSTRAINT ck_invitation_terminal_state
        CHECK (NOT (consumed_at IS NOT NULL AND revoked_at IS NOT NULL))
);
CREATE INDEX ix_invitations_household_expiry
    ON household_invitations(household_id, expires_at)
    WHERE consumed_at IS NULL AND revoked_at IS NULL;
