# ARISTA Native — Audit Kesetaraan Fitur Web vs APK

Tanggal pemeriksaan: 4 November 2026  
Sumber web: branch `main` / `index.html`  
Target native: branch `native-android`  
Aturan keselamatan proyek: jangan ubah branch `main`, website live, atau database produksi selama migrasi native.

Status:
- **Ada**: implementasi native ditemukan di kode.
- **Sebagian**: ada dasar implementasi, tetapi belum setara dengan alur web.
- **Belum**: belum ditemukan implementasi native yang setara.
- **Uji**: harus diuji di perangkat/backend; kode saja belum membuktikan fungsi benar.

| Area | Fitur pada web | Status native saat audit | Pekerjaan tersisa |
|---|---|---|---|
| Akun | Login email/kata sandi dan profil/peran | Sebagian | Persistensi sesi, refresh token, penyimpanan token aman, uji hak akses |
| Akun | Lupa/reset kata sandi | Belum | Alur permintaan tautan reset dan simpan kata sandi baru |
| Pengguna | Pemilik mengelola akun kasir | Sebagian | Cocokkan aksi Edge Function, validasi peran, uji tambah/hapus akun |
| Shift | Buka/tutup shift, saldo awal/akhir, selisih | Sebagian | Riwayat shift dan detail; pastikan data tetap ada setelah aplikasi ditutup |
| Kasir | Menu per kategori dan tambah item ke keranjang | Sebagian | Cocokkan pencarian/kategori, foto produk, dan perilaku kartu dengan web |
| Kasir | Nama pelanggan dan diskon nominal/persen | Sebagian | Verifikasi perhitungan, tampilan, dan data transaksi |
| Pembayaran | Tunai dan QRIS | Sebagian | Uji alur sukses/gagal dan cegah transaksi ganda saat retry |
| Pembayaran | Debit/kredit, transfer, piutang | Belum | Implementasi metode dan alur piutang sesuai backend |
| Pesanan | Simpan, buka, edit, hapus pesanan tertunda | Sebagian | Uji pemulihan data lokal dan perilaku setelah restart |
| Penjualan | Pencatatan transaksi dan stok | Sebagian | Idempotensi stabil saat timeout/retry; uji tanpa transaksi produksi |
| Transaksi | Detail, struk, daftar transaksi | Sebagian | Cocokkan detail/riwayat dan tata letak struk |
| Transaksi | Pembatalan transaksi beserta alasan dan pengembalian stok | Belum | Integrasi RPC pembatalan, izin pemilik, dan audit stok |
| Laporan | Rentang tanggal, omzet, jumlah transaksi, margin | Sebagian | Filter tanggal dan validasi rumus terhadap web |
| Laporan | Ekspor CSV dan HTML | Belum | Ekspor file yang dapat dibuka/dibagikan dari Android |
| Piutang | Daftar nota, bayar nota, bayar sekaligus | Belum | Integrasi tabel pembayaran dan RPC terkait |
| Pengeluaran | Tambah, daftar, hapus, ekspor pengeluaran | Sebagian | Daftar lengkap, hapus, ekspor, filter, validasi izin |
| Menu | Tambah, edit, hapus, kategori, harga, modal, stok | Sebagian | Foto produk, sinkronisasi/cache offline, uji stok |
| Data | Backup dan pemulihan | Belum | Backup/restore lokal lengkap dan validasi berkas |
| Offline | Antrean transaksi dan sinkronisasi ulang | Sebagian | Cache produk, retry terjadwal, status sinkronisasi, uji konflik |
| Printer | Cetak struk web melalui print dialog | Belum | Integrasi Bluetooth native dan uji nyata pada Redmi 14C + CODESOFT HP-M200 + Cleanter; jangan mengasumsikan API tersedia |

## Prioritas pengerjaan

1. **P0 — Keamanan dan keandalan transaksi:** sesi/token, idempotensi retry, hak akses, dan validasi stok.
2. **P1 — Kelengkapan operasional:** metode pembayaran, piutang, pembatalan, shift, laporan, dan pengeluaran.
3. **P2 — Kesetaraan pengalaman:** foto/menu, pesanan tertunda, detail transaksi, dan alur layar.
4. **P3 — Data dan perangkat:** backup/restore, offline lengkap, cetak Bluetooth, APK release bertanda tangan.

## Gerbang selesai

Fitur tidak boleh diberi status 100% hanya karena kode berhasil dikompilasi. Setiap fitur harus:
- cocok dengan perilaku sumber web atau perbedaan Android-nya terdokumentasi;
- lolos build;
- diuji dengan akun/data uji, tanpa membuat transaksi palsu pada data produksi;
- diuji pada perangkat Android untuk alur yang memerlukan perangkat;
- tidak merusak website utama atau data yang sudah ada.

Audit ini adalah baseline kerja dan harus diperbarui setelah tiap kelompok fitur selesai diuji.
