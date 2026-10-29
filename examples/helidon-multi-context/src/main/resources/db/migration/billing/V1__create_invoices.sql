-- No foreign key to shop_order: the billing context does not depend on the order context's tables.
CREATE TABLE invoices (
    id         UUID PRIMARY KEY,
    order_id   UUID                     NOT NULL UNIQUE,
    item_count INTEGER                  NOT NULL,
    issued_at  TIMESTAMP WITH TIME ZONE NOT NULL
);
