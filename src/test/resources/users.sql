CREATE TABLE public.administration (
    username character varying(100) NOT NULL,
    password character varying(60) NOT NULL
);

CREATE TABLE public.customers (
    email character varying(100) NOT NULL,
    username character varying(100) NOT NULL,
    first_name character varying(100) NOT NULL,
    last_name character varying(100) NOT NULL,
    password character varying(60) NOT NULL
);

CREATE TABLE public.store_managers (
    username character varying(100) NOT NULL,
    password character varying(60) NOT NULL,
    store integer NOT NULL
);

ALTER TABLE ONLY public.administration ADD CONSTRAINT administration_pkey PRIMARY KEY (username);

ALTER TABLE ONLY public.customers ADD CONSTRAINT customers_pkey PRIMARY KEY (email);

ALTER TABLE ONLY public.store_managers ADD CONSTRAINT store_managers_pkey PRIMARY KEY (username);
