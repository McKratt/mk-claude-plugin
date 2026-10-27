CREATE TABLE orders (
    id        UUID PRIMARY KEY,
    placed_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE order_lines (
    order_id UUID        NOT NULL REFERENCES orders (id),
    sku      VARCHAR(64) NOT NULL,
    quantity INTEGER     NOT NULL
);
