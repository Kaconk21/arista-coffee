-- Jalankan di Supabase: SQL Editor > New query > Run.
create table if not exists public.sales (
  id uuid primary key,
  sale_date date not null,
  sale_time text,
  items jsonb not null,
  total numeric not null check (total >= 0),
  method text not null check (method in ('Tunai', 'QRIS', 'Debit/Kredit', 'Transfer')),
  paid numeric not null,
  change_amount numeric not null default 0,
  cashier_name text,
  created_at timestamptz not null default now()
);

alter table public.sales enable row level security;

-- Hanya pengguna yang sudah login yang boleh menulis dan membaca transaksi.
create policy "login boleh menambah transaksi"
  on public.sales for insert to authenticated with check (true);
create policy "login boleh membaca transaksi"
  on public.sales for select to authenticated using (true);
