# ARISTA Native Android — Migration Plan

## Tujuan

Membuat aplikasi Android native (bukan WebView) yang mempertahankan fungsi ARISTA web saat ini. Website pada branch `main` tetap menjadi cadangan dan tidak diubah. Semua pengembangan native dilakukan pada branch `native-android`.

## Teknologi

- Kotlin
- Jetpack Compose untuk antarmuka
- Room untuk penyimpanan lokal
- Supabase Auth dan PostgREST/RPC untuk akun dan sinkronisasi cloud
- Android Bluetooth APIs untuk jalur cetak struk native; protokol printer harus diuji pada CODESOFT HP-M200 sebelum diputuskan.
- GitHub Actions + Gradle untuk build APK dari cloud, sehingga pemilik proyek tidak memerlukan laptop.

## Inventaris fitur yang ditemukan pada source web (branch main)

### Akun dan akses
- Login email/kata sandi Supabase.
- Pemulihan kata sandi.
- Peran Pemilik dan Kasir.
- Pemilik dapat membuat dan menghapus akun Kasir melalui Edge Function `kelola-kasir`.

### Operasional kasir
- Buka shift dengan saldo awal.
- Tutup shift dengan uang terhitung, hitung selisih, dan riwayat shift.
- Menu berdasarkan kategori.
- Keranjang, ubah jumlah item, nama customer, diskon nominal/persen.
- Pembayaran Tunai, QRIS, Debit/Kredit, Transfer, dan Piutang.
- Simpan order tertunda berdasarkan nama customer, buka kembali, ubah, dan hapus.
- Struk 80 mm saat ini menggunakan `window.print()` (belum Bluetooth native).

### Produk dan laporan
- Tambah, edit, hapus menu; kategori, harga jual, harga modal, stok.
- Sinkronisasi produk melalui tabel `products`.
- Laporan omzet, jumlah transaksi, margin kotor, pengeluaran, dan piutang.
- Ekspor CSV dan HTML.
- Pembatalan transaksi oleh Pemilik melalui RPC `batalkan_penjualan`, termasuk alasan pembatalan dan pengembalian stok.
- Pengeluaran tersimpan di tabel `expenses`.
- Pembayaran piutang melalui tabel `pembayaran` dan RPC `bayar_sekaligus` / `bayar_nota`.

### Data lokal dan sinkronisasi
- Browser menggunakan localStorage key `arista_pos_v1`.
- Data lokal meliputi session, products, sales, outbox, menuDirty/menuPull, pesanan tertunda, shift, dan riwayat shift.
- Transaksi baru dimasukkan ke outbox dan dikirim ke RPC `catat_penjualan`.
- Pesanan tertunda dan shift pada source yang diperiksa disimpan lokal; belum ditemukan sinkronisasi cloud untuk keduanya.
- Backup JSON saat ini hanya berisi products dan sales. Format backup tersebut tidak cukup untuk memulihkan semua data operasional.

## Peta migrasi native

| Area | Implementasi native yang direncanakan |
|---|---|
| Login/session | Supabase Auth; token disimpan dengan aman |
| Produk/menu | Room cache + sinkronisasi Supabase |
| Keranjang/pembayaran | Logika transaksi Kotlin; transaksi disimpan lokal dahulu |
| Outbox | Tabel Room untuk antrean sinkronisasi dan retry |
| Order tertunda | Room lokal, dengan aturan pemulihan yang jelas |
| Shift dan riwayat | Room lokal; cloud hanya jika skema backend mendukung |
| Laporan/ekspor | Perhitungan Kotlin dari transaksi lokal/cloud |
| Piutang/pengeluaran | Integrasi RPC/tabel yang sudah ada setelah kontrak backend diverifikasi |
| Struk | Layout native 80 mm dan koneksi Bluetooth setelah pengujian perangkat |
| Backup/restore | Backup lengkap database lokal, bukan hanya produk dan penjualan |

## Penghalang integrasi backend yang perlu diverifikasi

File `schema.sql` yang ada di repository hanya membuat tabel `sales` beserta dua policy. Source web juga memanggil objek backend berikut yang definisinya tidak ditemukan pada file SQL tersebut:

- Tabel `profiles`
- Tabel `products`
- Tabel `expenses`
- Tabel `pembayaran`
- RPC `catat_penjualan`
- RPC `bayar_sekaligus`
- RPC `bayar_nota`
- RPC `batalkan_penjualan`
- Edge Function `kelola-kasir`

Sebelum integrasi native, definisi kolom, tipe data, foreign key, RLS policies, dan perilaku RPC/Edge Function harus diperoleh dan diperiksa. Jangan menyalin data pelanggan atau kredensial rahasia ke repository. Jangan pernah meminta service-role key.

## Build awal

Workflow `.github/workflows/build-native-apk.yml` berhasil menjalankan `assembleDebug` pada 4 November 2026. Artifact awal adalah APK kerangka Compose, bukan aplikasi kasir lengkap dan belum layak dipakai untuk transaksi.

## Gerbang rilis

1. Backend schema dan hak akses diverifikasi.
2. Fitur native utama selesai dan diuji.
3. Migrasi data/backup dan sinkronisasi diuji tanpa kehilangan data.
4. Cetak Bluetooth diuji pada HP-M200.
5. APK release ditandatangani dan diuji instalasi di Redmi 14C.
6. Website tetap tersedia sebagai cadangan.

## Status implementasi native (4 November 2026)

Sudah ada di cabang `native-android`:
- Login Supabase dan pembacaan profil/peran.
- Kasir dasar, keranjang, Tunai/QRIS, validasi uang diterima dan kembalian.
- Pencatatan transaksi melalui RPC `catat_penjualan`.
- Beranda dan laporan dasar dari transaksi Supabase.
- Pengelolaan menu untuk Pemilik (tambah, ubah, hapus).
- Pengelolaan akun Kasir melalui `swift-worker`.
- Room lokal untuk pesanan tertunda, antrean sinkronisasi, dan shift.
- Simpan/buka/hapus pesanan tertunda di perangkat; pesanan tetap ada sampai pembayaran berhasil.
- Antrean transaksi saat Android mendeteksi tidak ada koneksi internet; percobaan sinkronisasi ulang saat login.
- Buka/tutup shift lokal dengan modal awal, kas akhir, dan selisih.
- Laporan estimasi modal/laba kotor dan form catat pengeluaran untuk Pemilik.

## Belum selesai / belum teruji
- Pemulihan sesi login dan pengamanan token lokal.
- Riwayat shift dan laporan yang lengkap dengan filter tanggal, ekspor CSV, dan piutang.
- Alur pembayaran debit/transfer/piutang dan pencatatan pembayaran piutang.
- Pembatalan transaksi dan penyesuaian stok melalui RPC yang sudah ada.
- Cache produk dan sinkronisasi perubahan menu saat offline.
- Backup/restore database lokal secara lengkap.
- Cetak Bluetooth native untuk CODESOFT HP-M200; harus diuji pada Redmi 14C dan printer fisik.
- Uji end-to-end pada akun uji agar tidak membuat transaksi sungguhan pada data produksi.
- APK release final, pemeriksaan instalasi, dan uji perangkat.

Build KSP/Room berhasil pada commit `ff6642f7046ccaada14eba7c89f30db691364e00`. Perubahan setelah commit tersebut masih menunggu build dan verifikasi.
