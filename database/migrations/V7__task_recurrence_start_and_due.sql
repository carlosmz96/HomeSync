-- HomeSync - V7: horarios y selección de días para recurrencias

ALTER TABLE task_occurrences
    ADD COLUMN starts_at TIMESTAMPTZ;

-- Compatibilidad con posibles ocurrencias existentes.
UPDATE task_occurrences
SET starts_at = due_at
WHERE starts_at IS NULL;

ALTER TABLE task_occurrences
    ALTER COLUMN starts_at SET NOT NULL;

ALTER TABLE task_recurrence_rules
    ADD COLUMN start_time TIME NOT NULL DEFAULT '09:00',
    ADD COLUMN due_time TIME NOT NULL DEFAULT '09:30',
    ADD COLUMN days_of_week SMALLINT[],
    ADD COLUMN day_of_month SMALLINT,
    ADD COLUMN duration_minutes INTEGER NOT NULL DEFAULT 30;

ALTER TABLE task_recurrence_rules
    ADD CONSTRAINT ck_recurrence_duration
        CHECK (duration_minutes > 0),
    ADD CONSTRAINT ck_recurrence_day_of_month
        CHECK (day_of_month IS NULL OR day_of_month BETWEEN 1 AND 31),
    ADD CONSTRAINT ck_recurrence_days_of_week
        CHECK (
            days_of_week IS NULL
            OR (
                cardinality(days_of_week) BETWEEN 1 AND 7
                AND days_of_week <@ ARRAY[1, 2, 3, 4, 5, 6, 7]::SMALLINT[]
            )
        ),
    ADD CONSTRAINT ck_recurrence_time_order
        CHECK (due_time > start_time);