-- HomeSync - V3: tareas, asignaciones, recurrencias y ocurrencias
CREATE TABLE tasks (
    id UUID PRIMARY KEY,
    household_id UUID NOT NULL,
    created_by UUID NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    priority VARCHAR(10) NOT NULL DEFAULT 'MEDIUM'
        CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH')),
    status VARCHAR(15) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED')),
    due_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    is_recurring BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    archived_at TIMESTAMPTZ,
    CONSTRAINT uq_tasks_household_id_id UNIQUE (household_id, id),
    CONSTRAINT fk_task_household
        FOREIGN KEY (household_id) REFERENCES households(id) ON DELETE RESTRICT,
    CONSTRAINT fk_task_creator_membership
        FOREIGN KEY (household_id, created_by)
        REFERENCES household_memberships(household_id, user_id) ON DELETE RESTRICT,
    CONSTRAINT ck_tasks_title_nonblank CHECK (length(trim(title)) > 0),
    CONSTRAINT ck_tasks_completion CHECK (
        (status = 'COMPLETED' AND completed_at IS NOT NULL) OR
        (status <> 'COMPLETED' AND completed_at IS NULL)
    )
);
CREATE INDEX ix_tasks_household_status_due
    ON tasks(household_id, status, due_at) WHERE archived_at IS NULL;

-- occurrence_id NULL: responsables predeterminados de la tarea.
-- occurrence_id NOT NULL: responsables específicos de esa ocurrencia.
-- membership_id identifica la pertenencia al hogar; user_id se deriva de ella.
CREATE TABLE task_occurrences (
    id UUID PRIMARY KEY,
    household_id UUID NOT NULL,
    task_id UUID NOT NULL,
    due_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(15) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED')),
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_occurrence_task
        FOREIGN KEY (household_id, task_id)
        REFERENCES tasks(household_id, id) ON DELETE RESTRICT,
    CONSTRAINT uq_occurrence_task_due UNIQUE (task_id, due_at),
    CONSTRAINT uq_occurrence_household_task_id
        UNIQUE (household_id, task_id, id),
    CONSTRAINT ck_occurrence_completion CHECK (
        (status = 'COMPLETED' AND completed_at IS NOT NULL) OR
        (status <> 'COMPLETED' AND completed_at IS NULL)
    )
);
CREATE INDEX ix_occurrences_household_status_due
    ON task_occurrences(household_id, status, due_at);

CREATE TABLE task_assignments (
    id UUID PRIMARY KEY,
    household_id UUID NOT NULL,
    task_id UUID NOT NULL,
    occurrence_id UUID,
    membership_id UUID NOT NULL,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    unassigned_at TIMESTAMPTZ,
    CONSTRAINT fk_assignment_task
        FOREIGN KEY (household_id, task_id)
        REFERENCES tasks(household_id, id) ON DELETE RESTRICT,
    CONSTRAINT fk_assignment_membership
        FOREIGN KEY (household_id, membership_id)
        REFERENCES household_memberships(household_id, id) ON DELETE RESTRICT,
    CONSTRAINT fk_assignment_occurrence_same_task
        FOREIGN KEY (household_id, task_id, occurrence_id)
        REFERENCES task_occurrences(household_id, task_id, id) ON DELETE RESTRICT,
    CONSTRAINT ck_assignment_dates CHECK (
        unassigned_at IS NULL OR unassigned_at >= assigned_at
    )
);
CREATE INDEX ix_task_assignments_membership_active
    ON task_assignments(membership_id, household_id)
    WHERE unassigned_at IS NULL;
CREATE INDEX ix_task_assignments_task_occurrence
    ON task_assignments(task_id, occurrence_id);
CREATE UNIQUE INDEX uq_task_default_assignment_active
    ON task_assignments(task_id, membership_id)
    WHERE occurrence_id IS NULL AND unassigned_at IS NULL;
CREATE UNIQUE INDEX uq_occurrence_assignment_active
    ON task_assignments(occurrence_id, membership_id)
    WHERE occurrence_id IS NOT NULL AND unassigned_at IS NULL;

CREATE TABLE task_recurrence_rules (
    id UUID PRIMARY KEY,
    household_id UUID NOT NULL,
    task_id UUID NOT NULL UNIQUE,
    frequency VARCHAR(10) NOT NULL
        CHECK (frequency IN ('DAILY', 'WEEKLY', 'MONTHLY')),
    interval_value INTEGER NOT NULL DEFAULT 1 CHECK (interval_value > 0),
    start_date DATE NOT NULL,
    end_date DATE,
    next_occurrence_at TIMESTAMPTZ NOT NULL,
    generation_lead_days INTEGER NOT NULL DEFAULT 7
        CHECK (generation_lead_days BETWEEN 0 AND 365),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_recurrence_task
        FOREIGN KEY (household_id, task_id)
        REFERENCES tasks(household_id, id) ON DELETE RESTRICT,
    CONSTRAINT ck_recurrence_dates CHECK (end_date IS NULL OR end_date >= start_date)
);
CREATE INDEX ix_recurrence_due_generation
    ON task_recurrence_rules(next_occurrence_at) WHERE active = TRUE;

-- Reglas de aplicación/transacción:
-- * Para asignar: membresía ACTIVE y hogar no eliminado.
-- * Al retirar miembro: cerrar asignaciones activas con unassigned_at.
-- * Al crear ocurrencia: copiar asignaciones predeterminadas activas en la misma transacción.
-- * Los cambios posteriores de responsables predeterminados no alteran ocurrencias existentes.
-- * El generador debe bloquear la regla (p. ej. SELECT ... FOR UPDATE), insertar idempotentemente
--   la ocurrencia y avanzar next_occurrence_at dentro de una única transacción.
-- * Para tareas no recurrentes, occurrence_id es NULL y las asignaciones son las de la tarea.
-- * Para tareas recurrentes, las asignaciones de ocurrencia se materializan al generarla.
