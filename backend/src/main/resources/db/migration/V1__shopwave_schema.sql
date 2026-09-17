CREATE TABLE users (
  id BINARY(16) NOT NULL,
  email VARCHAR(254) NOT NULL,
  password_hash VARCHAR(100) NOT NULL,
  first_name VARCHAR(80) NOT NULL,
  last_name VARCHAR(80) NOT NULL,
  mobile VARCHAR(20) NOT NULL,
  role VARCHAR(16) NOT NULL,
  token_version INT NOT NULL DEFAULT 0,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP(6) NOT NULL,
  updated_at TIMESTAMP(6) NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE TABLE categories (
  id BINARY(16) NOT NULL,
  name VARCHAR(80) NOT NULL,
  slug VARCHAR(80) NOT NULL,
  level INT NOT NULL,
  parent_id BINARY(16) NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_category_parent_slug UNIQUE (parent_id, slug),
  CONSTRAINT fk_category_parent FOREIGN KEY (parent_id) REFERENCES categories(id)
);

CREATE TABLE products (
  id BINARY(16) NOT NULL,
  title VARCHAR(160) NOT NULL,
  description VARCHAR(4000) NOT NULL,
  brand VARCHAR(80) NOT NULL,
  color VARCHAR(40) NOT NULL,
  image_url VARCHAR(1000) NOT NULL,
  price_minor BIGINT NOT NULL,
  sale_price_minor BIGINT NOT NULL,
  discount_percent INT NOT NULL,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  version BIGINT NOT NULL DEFAULT 0,
  created_at TIMESTAMP(6) NOT NULL,
  updated_at TIMESTAMP(6) NOT NULL,
  category_id BINARY(16) NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT fk_product_category FOREIGN KEY (category_id) REFERENCES categories(id)
);

CREATE TABLE product_variants (
  id BINARY(16) NOT NULL,
  product_id BINARY(16) NOT NULL,
  label VARCHAR(30) NOT NULL,
  label_normalized VARCHAR(30) NOT NULL,
  stock INT NOT NULL,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  version BIGINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  CONSTRAINT uk_variant_product_label UNIQUE (product_id, label_normalized),
  CONSTRAINT fk_variant_product FOREIGN KEY (product_id) REFERENCES products(id)
);

CREATE TABLE carts (
  id BINARY(16) NOT NULL,
  user_id BINARY(16) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  CONSTRAINT uk_cart_user UNIQUE (user_id),
  CONSTRAINT fk_cart_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE cart_items (
  id BINARY(16) NOT NULL,
  cart_id BINARY(16) NOT NULL,
  variant_id BINARY(16) NOT NULL,
  quantity INT NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_cart_variant UNIQUE (cart_id, variant_id),
  CONSTRAINT fk_cart_item_cart FOREIGN KEY (cart_id) REFERENCES carts(id),
  CONSTRAINT fk_cart_item_variant FOREIGN KEY (variant_id) REFERENCES product_variants(id)
);

CREATE TABLE addresses (
  id BINARY(16) NOT NULL,
  user_id BINARY(16) NOT NULL,
  first_name VARCHAR(80) NOT NULL,
  last_name VARCHAR(80) NOT NULL,
  street_address VARCHAR(200) NOT NULL,
  city VARCHAR(80) NOT NULL,
  department VARCHAR(40) NOT NULL,
  postal_code VARCHAR(20) NULL,
  mobile VARCHAR(20) NOT NULL,
  country VARCHAR(2) NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT fk_address_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE orders (
  id BINARY(16) NOT NULL,
  order_number VARCHAR(40) NOT NULL,
  user_id BINARY(16) NOT NULL,
  status VARCHAR(20) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  created_at TIMESTAMP(6) NOT NULL,
  delivered_at TIMESTAMP(6) NULL,
  idempotency_key VARCHAR(80) NOT NULL,
  request_hash VARCHAR(64) NOT NULL,
  ship_first_name VARCHAR(80) NOT NULL,
  ship_last_name VARCHAR(80) NOT NULL,
  ship_street VARCHAR(200) NOT NULL,
  ship_city VARCHAR(80) NOT NULL,
  ship_department VARCHAR(40) NOT NULL,
  ship_postal_code VARCHAR(20) NULL,
  ship_mobile VARCHAR(20) NOT NULL,
  ship_country VARCHAR(2) NOT NULL,
  subtotal_minor BIGINT NOT NULL,
  discount_minor BIGINT NOT NULL,
  total_minor BIGINT NOT NULL,
  total_quantity INT NOT NULL,
  currency VARCHAR(3) NOT NULL,
  payment_method VARCHAR(16) NOT NULL,
  payment_status VARCHAR(16) NOT NULL,
  payment_reference VARCHAR(80) NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_order_number UNIQUE (order_number),
  CONSTRAINT uk_order_user_idempotency UNIQUE (user_id, idempotency_key),
  CONSTRAINT fk_order_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX ix_products_active_price ON products(active, sale_price_minor);
CREATE INDEX ix_products_category ON products(category_id, active);
CREATE INDEX ix_orders_user_created ON orders(user_id, created_at);

CREATE TABLE order_items (
  id BINARY(16) NOT NULL,
  order_id BINARY(16) NOT NULL,
  product_id BINARY(16) NOT NULL,
  variant_id BINARY(16) NOT NULL,
  title VARCHAR(160) NOT NULL,
  image_url VARCHAR(1000) NOT NULL,
  variant_label VARCHAR(30) NOT NULL,
  quantity INT NOT NULL,
  unit_price_minor BIGINT NOT NULL,
  unit_sale_price_minor BIGINT NOT NULL,
  line_total_minor BIGINT NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT fk_order_item_order FOREIGN KEY (order_id) REFERENCES orders(id)
);

CREATE INDEX ix_order_items_order ON order_items(order_id);
