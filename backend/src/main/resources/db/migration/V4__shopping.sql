-- HomeSync - V4: listas y artículos de compra
CREATE TABLE shopping_lists (
    id UUID PRIMARY KEY,
    household_id UUID NOT NULL,
    created_by UUID NOT NULL,
    name VARCHAR(120) NOT NULL,
    status VARCHAR(10) NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('ACTIVE', 'ARCHIVED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    archived_at TIMESTAMPTZ,
    CONSTRAINT uq_shopping_lists_household_id_id UNIQUE (household_id, id),
    CONSTRAINT fk_shopping_list_household
        FOREIGN KEY (household_id) REFERENCES households(id) ON DELETE RESTRICT,
    CONSTRAINT fk_shopping_list_creator
        FOREIGN KEY (household_id, created_by)
        REFERENCES household_memberships(household_id, user_id) ON DELETE RESTRICT,
    CONSTRAINT ck_shopping_list_name CHECK (length(trim(name)) > 0),
    CONSTRAINT ck_shopping_list_archive CHECK (
        (status = 'ARCHIVED' AND archived_at IS NOT NULL) OR
        (status = 'ACTIVE' AND archived_at IS NULL)
    )
);
CREATE INDEX ix_shopping_lists_household_status
    ON shopping_lists(household_id, status);

CREATE TABLE shopping_items (
    id UUID PRIMARY KEY,
    household_id UUID NOT NULL,
    shopping_list_id UUID NOT NULL,
    name VARCHAR(200) NOT NULL,
    quantity NUMERIC(10,2),
    unit VARCHAR(30),
    category VARCHAR(60),
    note TEXT,
    is_purchased BOOLEAN NOT NULL DEFAULT FALSE,
    created_by UUID NOT NULL,
    purchased_by UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    purchased_at TIMESTAMPTZ,
    CONSTRAINT fk_item_list
        FOREIGN KEY (household_id, shopping_list_id)
        REFERENCES shopping_lists(household_id, id) ON DELETE RESTRICT,
    CONSTRAINT fk_item_creator
        FOREIGN KEY (household_id, created_by)
        REFERENCES household_memberships(household_id, user_id) ON DELETE RESTRICT,
    CONSTRAINT fk_item_purchaser
        FOREIGN KEY (household_id, purchased_by)
        REFERENCES household_memberships(household_id, user_id) ON DELETE RESTRICT,
    CONSTRAINT ck_item_name CHECK (length(trim(name)) > 0),
    CONSTRAINT ck_item_quantity CHECK (quantity IS NULL OR quantity > 0),
    CONSTRAINT ck_item_purchase_state CHECK (
        (is_purchased = TRUE AND purchased_at IS NOT NULL AND purchased_by IS NOT NULL) OR
        (is_purchased = FALSE AND purchased_at IS NULL AND purchased_by IS NULL)
    )
);
CREATE INDEX ix_shopping_items_list_purchased
    ON shopping_items(shopping_list_id, is_purchased, created_at);
