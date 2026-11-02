-- HomeSync - V5: notificaciones, preferencias y actividad
CREATE TABLE notification_preferences (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    household_id UUID REFERENCES households(id) ON DELETE CASCADE,
    channel VARCHAR(10) NOT NULL CHECK (channel IN ('IN_APP', 'EMAIL', 'PUSH')),
    notification_type VARCHAR(60) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX uq_notification_preference_global
    ON notification_preferences(user_id, channel, notification_type)
    WHERE household_id IS NULL;
CREATE UNIQUE INDEX uq_notification_preference_household
    ON notification_preferences(user_id, household_id, channel, notification_type)
    WHERE household_id IS NOT NULL;

CREATE TABLE notifications (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    household_id UUID REFERENCES households(id) ON DELETE RESTRICT,
    channel VARCHAR(10) NOT NULL CHECK (channel IN ('IN_APP', 'EMAIL', 'PUSH')),
    type VARCHAR(60) NOT NULL,
    title VARCHAR(200) NOT NULL,
    body TEXT NOT NULL,
    delivery_status VARCHAR(15) NOT NULL DEFAULT 'PENDING'
        CHECK (delivery_status IN ('PENDING', 'SENT', 'FAILED', 'CANCELLED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_at TIMESTAMPTZ,
    sent_at TIMESTAMPTZ,
    retry_count INTEGER NOT NULL DEFAULT 0 CHECK (retry_count >= 0),
    idempotency_key VARCHAR(160) UNIQUE,
    CONSTRAINT ck_notification_sent_at CHECK (
        (delivery_status = 'SENT' AND sent_at IS NOT NULL) OR
        (delivery_status <> 'SENT' AND sent_at IS NULL)
    ),
    CONSTRAINT ck_notification_read_channel CHECK (
        read_at IS NULL OR channel = 'IN_APP'
    )
);
CREATE INDEX ix_notifications_user_delivery_created
    ON notifications(user_id, delivery_status, created_at DESC);
CREATE INDEX ix_notifications_user_unread
    ON notifications(user_id, created_at DESC)
    WHERE channel = 'IN_APP' AND read_at IS NULL;

CREATE TABLE activity_events (
    id UUID PRIMARY KEY,
    household_id UUID NOT NULL REFERENCES households(id) ON DELETE RESTRICT,
    actor_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    event_type VARCHAR(80) NOT NULL,
    entity_type VARCHAR(60) NOT NULL,
    entity_id UUID NOT NULL,
    metadata JSONB NOT NULL DEFAULT '{}'::JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX ix_activity_household_created
    ON activity_events(household_id, created_at DESC);
CREATE INDEX ix_activity_entity
    ON activity_events(entity_type, entity_id);
