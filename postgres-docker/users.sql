CREATE TABLE public.administrators (
    username character varying(100) NOT NULL,
    password character varying(60) NOT NULL,
    CONSTRAINT administrators_pkey PRIMARY KEY (username)
);

CREATE TABLE public.customers (
    email character varying(100) NOT NULL,
    username character varying(100) NOT NULL,
    first_name character varying(100) NOT NULL,
    last_name character varying(100) NOT NULL,
    password character varying(60) NOT NULL,
    CONSTRAINT customers_pkey PRIMARY KEY (email)
);

CREATE TABLE public.store_managers (
    username character varying(100) NOT NULL,
    password character varying(60) NOT NULL,
    store integer NOT NULL,
    CONSTRAINT store_managers_pkey PRIMARY KEY (username)
);
