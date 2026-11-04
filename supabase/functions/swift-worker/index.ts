// ARISTA Edge Function: kelola akun kasir.
// Dipanggil oleh aplikasi native melalui endpoint /functions/v1/swift-worker.
// Hanya pengguna dengan profil Pemilik yang boleh mengelola akun.

const CORS = {
  "Access-Control-Allow-Origin": "https://kaconk21.github.io",
  "Access-Control-Allow-Headers": "authorization, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

const json = (status: number, obj: unknown) =>
  new Response(JSON.stringify(obj), {
    status,
    headers: { ...CORS, "Content-Type": "application/json" },
  });

Deno.serve(async (req: Request) => {
  if (req.method === "OPTIONS") {
    return new Response(null, { status: 204, headers: CORS });
  }
  if (req.method !== "POST") {
    return json(405, { error: "Metode tidak diizinkan" });
  }

  const SUPA = Deno.env.get("SUPABASE_URL");
  const SRV = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
  const ANON = Deno.env.get("SUPABASE_ANON_KEY");
  if (!SUPA || !SRV || !ANON) {
    return json(500, { error: "Konfigurasi server belum lengkap" });
  }

  const auth = req.headers.get("Authorization") || "";
  if (!auth.startsWith("Bearer ")) {
    return json(401, { error: "Perlu masuk terlebih dahulu" });
  }

  // Validasi identitas pemanggil menggunakan token sesi.
  const me = await fetch(`${SUPA}/auth/v1/user`, {
    headers: { apikey: ANON, Authorization: auth },
  });
  if (!me.ok) {
    return json(401, { error: "Sesi tidak valid. Masuk lagi." });
  }
  const callerId = (await me.json()).id;

  // Hak admin hanya digunakan di server setelah memverifikasi Pemilik.
  const admin = {
    apikey: SRV,
    Authorization: `Bearer ${SRV}`,
    "Content-Type": "application/json",
  };
  const pr = await fetch(
    `${SUPA}/rest/v1/profiles?id=eq.${callerId}&select=peran`,
    { headers: admin },
  );
  const prow = pr.ok ? await pr.json() : [];
  if (!prow.length || prow[0].peran !== "Pemilik") {
    return json(403, {
      error: "Hanya Pemilik yang boleh mengelola pengguna.",
    });
  }

  const body = await req.json().catch(() => null);
  if (!body || typeof body !== "object") {
    return json(400, { error: "Permintaan tidak valid" });
  }

  // Daftar akun.
  if (body.aksi === "daftar") {
    const pr2 = await fetch(
      `${SUPA}/rest/v1/profiles?select=id,nama,peran&order=nama.asc`,
      { headers: admin },
    );
    if (!pr2.ok) {
      return json(500, { error: "Gagal mengambil daftar pengguna" });
    }
    const profiles = await pr2.json();

    const ur = await fetch(
      `${SUPA}/auth/v1/admin/users?per_page=1000`,
      { headers: admin },
    );
    if (!ur.ok) {
      return json(500, { error: "Gagal mengambil data akun pengguna" });
    }
    const ud = await ur.json();
    const list = Array.isArray(ud) ? ud : (ud.users || []);
    const emailOf: Record<string, string> = {};
    for (const u of list) emailOf[u.id] = u.email || "";

    return json(
      200,
      profiles.map((p: { id: string; nama: string; peran: string }) => ({
        id: p.id,
        nama: p.nama,
        peran: p.peran,
        email: emailOf[p.id] || "",
      })),
    );
  }

  // Tambah akun kasir baru.
  if (body.aksi === "tambah") {
    const nama = String(body.nama || "").trim();
    const email = String(body.email || "").trim().toLowerCase();
    const password = String(body.password || "");
    if (!nama) return json(400, { error: "Nama wajib diisi" });
    if (!/^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$/.test(email)) {
      return json(400, { error: "Email tidak valid" });
    }
    if (password.length < 6) {
      return json(400, { error: "Kata sandi minimal 6 karakter" });
    }

    const cr = await fetch(`${SUPA}/auth/v1/admin/users`, {
      method: "POST",
      headers: admin,
      body: JSON.stringify({ email, password, email_confirm: true }),
    });
    const cd = await cr.json().catch(() => ({}));
    if (!cr.ok) {
      return json(400, {
        error: cd.msg || cd.message || "Gagal membuat akun",
      });
    }

    const uid = cd.id;
    const ins = await fetch(`${SUPA}/rest/v1/profiles`, {
      method: "POST",
      headers: { ...admin, Prefer: "return=minimal" },
      body: JSON.stringify({ id: uid, nama, peran: "Kasir" }),
    });
    if (!ins.ok) {
      // Rollback akun Auth jika pembuatan profil gagal.
      await fetch(`${SUPA}/auth/v1/admin/users/${uid}`, {
        method: "DELETE",
        headers: admin,
      });
      return json(500, {
        error: "Profil gagal dibuat. Akun dibatalkan, coba lagi.",
      });
    }
    return json(200, { id: uid, nama, email, peran: "Kasir" });
  }

  // Hapus akun kasir. Tidak boleh menghapus akun sendiri.
  if (body.aksi === "hapus") {
    const uid = String(body.id || "");
    if (!/^[0-9a-f-]{36}$/i.test(uid)) {
      return json(400, { error: "ID tidak valid" });
    }
    if (uid === callerId) {
      return json(400, { error: "Tidak bisa menghapus akun sendiri" });
    }

    // Batasi target hanya akun Kasir, bukan Pemilik lain.
    const target = await fetch(
      `${SUPA}/rest/v1/profiles?id=eq.${uid}&select=peran`,
      { headers: admin },
    );
    const targetRows = target.ok ? await target.json() : [];
    if (!target.ok || !targetRows.length || targetRows[0].peran !== "Kasir") {
      return json(400, { error: "Akun kasir tidak ditemukan" });
    }

    const dr = await fetch(`${SUPA}/auth/v1/admin/users/${uid}`, {
      method: "DELETE",
      headers: admin,
    });
    if (!dr.ok) {
      return json(400, { error: "Gagal menghapus akun" });
    }
    return json(200, { ok: true });
  }

  return json(400, { error: "Aksi tidak dikenal" });
});
