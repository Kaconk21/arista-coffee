# ARISTA Coffee & Roastery — starter app

Aplikasi awal berupa PWA (Progressive Web App) yang juga dapat dibungkus menjadi APK Android memakai Capacitor.

## Fitur yang sudah tersedia
- Menu digital dengan kategori.
- Contoh menu awal yang bisa diedit.
- Kasir: keranjang, jumlah item, diskon, metode pembayaran, uang diterima dan kembalian.
- Struk transaksi dan fungsi cetak browser.
- Riwayat transaksi, ringkasan omzet hari ini, ekspor CSV.
- Kelola menu (tambah/edit/hapus).
- Backup dan restore JSON.
- PWA manifest + service worker untuk instalasi/akses offline setelah dimuat.

## Catatan penting
- Data disimpan di localStorage pada browser/perangkat yang digunakan. Belum ada sinkronisasi lintas perangkat, akun staf, server, atau database cloud.
- QRIS hanya dicatat sebagai metode pembayaran; aplikasi tidak memproses pembayaran atau memverifikasi notifikasi bank.
- Data browser dapat hilang bila data aplikasi/browser dibersihkan. Gunakan backup JSON.
- Ini starter/prototipe, lakukan uji transaksi dan backup sebelum digunakan untuk operasional sungguhan.

## Menjalankan sebagai website
PWA dan service worker butuh HTTPS atau localhost. Untuk tes di komputer:
1. Pasang Node.js LTS.
2. Di folder proyek, jalankan server lokal sederhana, misalnya `npx serve .`
3. Buka alamat localhost yang ditampilkan.
4. Untuk akses online dan QR code pelanggan, deploy folder ini ke hosting HTTPS (misalnya hosting static web). Buat QR code yang menuju URL website tersebut.

## Membungkus menjadi APK dengan Capacitor
Prasyarat: Node.js LTS, Android Studio, Android SDK/JDK yang direkomendasikan Android Studio.

1. Ekstrak folder proyek ini.
2. Buka terminal pada folder proyek dan jalankan:

   ```bash
   npm init -y
   npm install @capacitor/core @capacitor/cli @capacitor/android
   npx cap init "ARISTA Coffee & Roastery" "id.arista.coffee" --web-dir .
   npx cap add android
   npx cap sync android
   npx cap open android
   ```

3. Android Studio akan membuka proyek `android`. Tunggu Gradle sync selesai.
4. Untuk uji di HP, aktifkan Developer options dan USB debugging, sambungkan HP, lalu tekan Run.
5. Untuk membuat APK: Android Studio → **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
6. Biasanya APK debug tersedia di `android/app/build/outputs/apk/debug/app-debug.apk`. Untuk distribusi publik, buat signed release APK melalui **Build → Generate Signed Bundle / APK**.

### Penting untuk build offline
Capacitor menampilkan `index.html` lokal di APK, tetapi localStorage tetap terikat pada instalasi aplikasi itu. Website dan APK tidak otomatis berbagi data. Untuk kasir multi-device, tambahkan backend/database dan autentikasi sebelum digunakan.

## Branding
Warna utama: espresso `#171513`, panel `#211e1a`, emas `#c5a16a`, krem `#f4ede2`.
Menu, harga, kategori, dan deskripsi contoh dapat diubah dari tab **Kelola** setelah aplikasi dibuka.
