create table if not exists prd_equity(symbol varchar(20) primary key, product_type char(10));
create table if not exists prd_option(symbol varchar(20) primary key, product_type char(10), put_call char(4), strike_price NUMERIC(20, 2), maturity_year integer, maturity_month integer, underlying_symbol varchar(20));
