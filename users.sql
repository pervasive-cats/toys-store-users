CREATE TABLE public.administrators (
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

ALTER TABLE ONLY public.administrators ADD CONSTRAINT administrators_pkey PRIMARY KEY (username);

ALTER TABLE ONLY public.customers ADD CONSTRAINT customers_pkey PRIMARY KEY (email);

ALTER TABLE ONLY public.store_managers ADD CONSTRAINT store_managers_pkey PRIMARY KEY (username);

INSERT INTO public.administrators(
  username, password)
  VALUES ('elena', 'Efda!dWQ');