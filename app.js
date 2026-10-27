const STORE_KEY = "arista_coffee_v1";
const rupiah = n => new Intl.NumberFormat("id-ID",{style:"currency",currency:"IDR",maximumFractionDigits:0}).format(Number(n)||0);
const todayKey = () => { const d=new Date(); return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,"0")}-${String(d.getDate()).padStart(2,"0")}`; };
const uid = () => "p"+Date.now().toString(36)+Math.random().toString(36).slice(2,6);
const defaults = [
 {id:"d1",name:"Espresso",category:"Espresso Based",price:16000,desc:"Shot kopi dengan karakter pekat dan seimbang."},
 {id:"d2",name:"Americano",category:"Espresso Based",price:18000,desc:"Espresso dan air, bersih dengan aftertaste panjang."},
 {id:"d3",name:"Cappuccino",category:"Espresso Based",price:22000,desc:"Espresso, susu steamed, dan foam lembut."},
 {id:"d4",name:"Café Latte",category:"Espresso Based",price:22000,desc:"Espresso dengan susu creamy dan tekstur halus."},
 {id:"d5",name:"Aren Latte",category:"Signature",price:24000,desc:"Latte susu dengan manis gula aren yang hangat."},
 {id:"d6",name:"Sea Salt Latte",category:"Signature",price:26000,desc:"Latte dengan sentuhan krim asin yang lembut."},
 {id:"d7",name:"Manual Brew",category:"Manual Brew",price:25000,desc:"Seduhan pilihan sesuai ketersediaan biji."},
 {id:"d8",name:"Matcha Latte",category:"Matcha",price:24000,desc:"Matcha dengan susu lembut dan rasa earthy."},
 {id:"d9",name:"Thai Tea",category:"Tea",price:18000,desc:"Teh khas Thailand dengan rasa creamy."},
 {id:"d10",name:"Chocolate Cloud",category:"Non-Coffee",price:23000,desc:"Cokelat lembut dengan topping foam."},
 {id:"d11",name:"Orange Jasmine",category:"Tea",price:22000,desc:"Jasmine tea dengan aroma citrus yang segar."},
 {id:"d12",name:"Yuzu Americano",category:"Signature",price:25000,desc:"Espresso, citrus yuzu, dan sensasi menyegarkan."}
];
let state = loadState();
let cart = [];
let activeCategory = "Semua";
let deferredPrompt = null;
function loadState(){
 try{
  const saved=JSON.parse(localStorage.getItem(STORE_KEY));
  if(saved && Array.isArray(saved.products) && Array.isArray(saved.sales)) return saved;
 }catch(e){}
 return {products:defaults,sales:[]};
}
function saveState(){localStorage.setItem(STORE_KEY,JSON.stringify(state));}
function toast(msg){const el=document.getElementById("toast");el.textContent=msg;el.classList.add("show");setTimeout(()=>el.classList.remove("show"),2400);}
function esc(s){return String(s??"").replace(/[&<>"']/g,c=>({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#39;"}[c]));}
function iconFor(cat){return ({"Espresso Based":"☕","Signature":"✦","Manual Brew":"◉","Non-Coffee":"♨","Matcha":"🍵","Tea":"❋"})[cat]||"☕";}
function switchView(id){
 document.querySelectorAll(".view").forEach(v=>v.classList.toggle("active",v.id===id));
 document.querySelectorAll(".tab").forEach(t=>t.classList.toggle("active",t.dataset.view===id));
 if(id==="reportView")renderReports();
 if(id==="manageView")renderManage();
 if(id==="cashierView")renderCart();
 window.scrollTo({top:0,behavior:"smooth"});
}
document.querySelectorAll(".tab").forEach(b=>b.addEventListener("click",()=>switchView(b.dataset.view)));
function renderFilters(){
 const cats=["Semua",...new Set(state.products.map(p=>p.category))];
 const el=document.getElementById("categoryFilters");
 if(!cats.includes(activeCategory))activeCategory="Semua";
 el.innerHTML=cats.map(c=>`<button class="filter ${c===activeCategory?"active":""}" data-cat="${esc(c)}">${esc(c)}</button>`).join("");
 el.querySelectorAll("button").forEach(b=>b.addEventListener("click",()=>{activeCategory=b.dataset.cat;renderFilters();renderMenu();}));
}
function renderMenu(){
 const list=state.products.filter(p=>activeCategory==="Semua"||p.category===activeCategory);
 document.getElementById("menuCount").textContent=`${list.length} pilihan`;
 document.getElementById("menuGrid").innerHTML=list.length?list.map(p=>`<article class="menu-card"><div class="drink-icon">${iconFor(p.category)}</div><div class="category">${esc(p.category)}</div><h3>${esc(p.name)}</h3><p>${esc(p.desc||"Dibuat segar saat dipesan.")}</p><div class="menu-card-bottom"><span class="price">${rupiah(p.price)}</span><button class="button button-gold" data-add="${p.id}">+ Tambah</button></div></article>`).join(""):`<div class="empty-state">Belum ada menu pada kategori ini.</div>`;
 document.querySelectorAll("[data-add]").forEach(b=>b.addEventListener("click",()=>addToCart(b.dataset.add)));
}
function addToCart(id){
 const p=state.products.find(x=>x.id===id);if(!p)return;
 const item=cart.find(x=>x.id===id);if(item)item.qty++;else cart.push({...p,qty:1});
 renderCart();toast(`${p.name} ditambahkan`);
}
function subtotal(){return cart.reduce((s,x)=>s+x.price*x.qty,0);}
function discount(){return Math.min(Math.max(0,Number(document.getElementById("discount").value)||0),subtotal());}
function total(){return Math.max(0,subtotal()-discount());}
function renderCart(){
 document.getElementById("cartBadge").textContent=cart.reduce((s,x)=>s+x.qty,0);
 document.getElementById("cartEmpty").classList.toggle("hidden",cart.length>0);
 document.getElementById("cartItems").innerHTML=cart.map(x=>`<div class="cart-row"><div><strong>${esc(x.name)}</strong><small>${rupiah(x.price)} / item</small><div class="cart-controls"><button class="qty-btn" data-dec="${x.id}" aria-label="Kurangi">−</button><span>${x.qty}</span><button class="qty-btn" data-inc="${x.id}" aria-label="Tambah">+</button><button class="button button-quiet" data-remove="${x.id}">Hapus</button></div></div><div class="cart-line-price">${rupiah(x.price*x.qty)}</div></div>`).join("");
 document.querySelectorAll("[data-inc]").forEach(b=>b.onclick=()=>{const x=cart.find(i=>i.id===b.dataset.inc);if(x)x.qty++;renderCart();});
 document.querySelectorAll("[data-dec]").forEach(b=>b.onclick=()=>{const x=cart.find(i=>i.id===b.dataset.dec);if(x)x.qty--;cart=cart.filter(i=>i.qty>0);renderCart();});
 document.querySelectorAll("[data-remove]").forEach(b=>b.onclick=()=>{cart=cart.filter(i=>i.id!==b.dataset.remove);renderCart();});
 document.getElementById("subtotal").textContent=rupiah(subtotal());
 document.getElementById("total").textContent=rupiah(total());
 updateChange();
}
function updateChange(){
 const paid=Number(document.getElementById("paidAmount").value)||0;
 document.getElementById("changeAmount").textContent=rupiah(Math.max(0,paid-total()));
}
document.getElementById("discount").addEventListener("input",()=>{renderCart();});
document.getElementById("paidAmount").addEventListener("input",updateChange);
document.getElementById("clearCartBtn").addEventListener("click",()=>{if(cart.length&&!confirm("Kosongkan semua item di keranjang?"))return;cart=[];document.getElementById("discount").value=0;renderCart();});
document.getElementById("checkoutBtn").addEventListener("click",()=>{
 if(!cart.length){toast("Keranjang masih kosong");return;}
 const method=document.getElementById("paymentMethod").value;
 const paid=Number(document.getElementById("paidAmount").value)||0;
 if(method==="Tunai"&&paid<total()){toast("Uang diterima belum mencukupi");return;}
 const sale={id:"AR"+Date.now().toString().slice(-8),time:new Date().toISOString(),date:todayKey(),customer:document.getElementById("customerName").value.trim(),items:cart.map(x=>({...x})),subtotal:subtotal(),discount:discount(),total:total(),method,paid:method==="Tunai"?paid:total(),change:method==="Tunai"?Math.max(0,paid-total()):0};
 state.sales.unshift(sale);saveState();renderReceipt(sale);cart=[];document.getElementById("discount").value=0;document.getElementById("paidAmount").value="";document.getElementById("customerName").value="";renderCart();toast("Transaksi tersimpan");renderReports();
});
function renderReceipt(s){
 const el=document.getElementById("receiptArea");el.classList.remove("hidden");
 el.innerHTML=`<div class="receipt-brand">ARISTA</div><div class="muted">COFFEE & ROASTERY</div><hr><div class="receipt-row"><span>No. transaksi</span><strong>${esc(s.id)}</strong></div><div class="receipt-row"><span>Waktu</span><span>${new Date(s.time).toLocaleString("id-ID")}</span></div>${s.customer?`<div class="receipt-row"><span>Pelanggan</span><span>${esc(s.customer)}</span></div>`:""}<hr>${s.items.map(x=>`<div class="receipt-row"><span>${esc(x.name)} × ${x.qty}</span><strong>${rupiah(x.price*x.qty)}</strong></div>`).join("")}<hr><div class="receipt-row"><span>Subtotal</span><span>${rupiah(s.subtotal)}</span></div><div class="receipt-row"><span>Diskon</span><span>− ${rupiah(s.discount)}</span></div><div class="receipt-row"><strong>Total</strong><strong>${rupiah(s.total)}</strong></div><div class="receipt-row"><span>Pembayaran</span><span>${esc(s.method)}</span></div>${s.method==="Tunai"?`<div class="receipt-row"><span>Diterima / Kembali</span><span>${rupiah(s.paid)} / ${rupiah(s.change)}</span></div>`:""}<hr><p class="tiny">Terima kasih sudah mampir. Semoga harimu sebaik kopimu.</p><div class="receipt-actions"><button class="button button-gold" id="printReceiptBtn">Cetak struk</button><button class="button button-outline" id="hideReceiptBtn">Tutup</button></div>`;
 document.getElementById("printReceiptBtn").onclick=()=>window.print();
 document.getElementById("hideReceiptBtn").onclick=()=>el.classList.add("hidden");
}
function renderReports(){
 const today=state.sales.filter(s=>s.date===todayKey());
 document.getElementById("todayRevenue").textContent=rupiah(today.reduce((a,s)=>a+s.total,0));
 document.getElementById("todayCount").textContent=today.length;
 document.getElementById("allCount").textContent=state.sales.length;
 const el=document.getElementById("salesHistory");
 el.innerHTML=state.sales.length?state.sales.slice(0,100).map(s=>`<div class="history-item"><div><strong>${esc(s.id)} · ${esc(s.customer||"Pelanggan")}</strong><small>${new Date(s.time).toLocaleString("id-ID")} · ${esc(s.method)} · ${s.items.reduce((n,x)=>n+x.qty,0)} item</small></div><div class="history-right"><strong>${rupiah(s.total)}</strong><small><button class="button button-quiet" data-viewsale="${s.id}">Lihat struk</button></small></div></div>`).join(""):`<div class="empty-state">Belum ada transaksi yang tercatat.</div>`;
 el.querySelectorAll("[data-viewsale]").forEach(b=>b.onclick=()=>{const s=state.sales.find(x=>x.id===b.dataset.viewsale);if(s){switchView("cashierView");renderReceipt(s);document.getElementById("receiptArea").scrollIntoView({behavior:"smooth"});}});
}
document.getElementById("clearSalesBtn").addEventListener("click",()=>{
 if(!state.sales.length){toast("Belum ada riwayat");return;}
 if(confirm("Hapus seluruh riwayat penjualan dari perangkat ini? Sebaiknya ekspor backup terlebih dahulu.")){state.sales=[];saveState();renderReports();toast("Riwayat dihapus");}
});
document.getElementById("exportBtn").addEventListener("click",()=>{
 const rows=[["ID Transaksi","Waktu","Pelanggan","Item","Subtotal","Diskon","Total","Pembayaran"],...state.sales.map(s=>[s.id,new Date(s.time).toLocaleString("id-ID"),s.customer,s.items.map(x=>`${x.name} x${x.qty}`).join("; "),s.subtotal,s.discount,s.total,s.method])];
 downloadFile("arista-penjualan-"+todayKey()+".csv","\ufeff"+rows.map(r=>r.map(v=>`"${String(v??"").replace(/"/g,'""')}"`).join(",")).join("\n"),"text/csv;charset=utf-8");
});
function downloadFile(name,data,type){const blob=new Blob([data],{type});const url=URL.createObjectURL(blob);const a=document.createElement("a");a.href=url;a.download=name;a.click();URL.revokeObjectURL(url);}
function renderManage(){
 const el=document.getElementById("manageList");
 el.innerHTML=state.products.length?state.products.map(p=>`<div class="manage-item"><div><strong>${esc(p.name)} · ${rupiah(p.price)}</strong><small>${esc(p.category)}${p.desc?" — "+esc(p.desc):""}</small></div><div class="manage-actions"><button class="button button-outline" data-edit="${p.id}">Edit</button><button class="button" data-del="${p.id}">Hapus</button></div></div>`).join(""):`<div class="empty-state">Menu masih kosong.</div>`;
 el.querySelectorAll("[data-edit]").forEach(b=>b.onclick=()=>editProduct(b.dataset.edit));
 el.querySelectorAll("[data-del]").forEach(b=>b.onclick=()=>deleteProduct(b.dataset.del));
}
function resetProductForm(){document.getElementById("productForm").reset();document.getElementById("productId").value="";document.getElementById("formTitle").textContent="Tambah menu baru";document.getElementById("cancelEditBtn").classList.add("hidden");}
function editProduct(id){
 const p=state.products.find(x=>x.id===id);if(!p)return;
 document.getElementById("productId").value=p.id;document.getElementById("productName").value=p.name;document.getElementById("productCategory").value=p.category;document.getElementById("productPrice").value=p.price;document.getElementById("productDesc").value=p.desc||"";
 document.getElementById("formTitle").textContent="Edit menu";document.getElementById("cancelEditBtn").classList.remove("hidden");document.getElementById("productForm").scrollIntoView({behavior:"smooth"});
}
function deleteProduct(id){const p=state.products.find(x=>x.id===id);if(!p)return;if(confirm(`Hapus menu "${p.name}"?`)){state.products=state.products.filter(x=>x.id!==id);saveState();renderAll();toast("Menu dihapus");}}
document.getElementById("productForm").addEventListener("submit",e=>{
 e.preventDefault();const id=document.getElementById("productId").value;
 const p={id:id||uid(),name:document.getElementById("productName").value.trim(),category:document.getElementById("productCategory").value,price:Math.max(0,Number(document.getElementById("productPrice").value)||0),desc:document.getElementById("productDesc").value.trim()};
 if(!p.name){toast("Nama menu wajib diisi");return;}
 if(id)state.products=state.products.map(x=>x.id===id?p:x);else state.products.push(p);
 saveState();resetProductForm();renderAll();toast("Menu berhasil disimpan");
});
document.getElementById("cancelEditBtn").addEventListener("click",resetProductForm);
document.getElementById("backupBtn").addEventListener("click",()=>downloadFile("arista-backup-"+todayKey()+".json",JSON.stringify(state,null,2),"application/json"));
document.getElementById("restoreInput").addEventListener("change",async e=>{
 const file=e.target.files[0];if(!file)return;
 try{const obj=JSON.parse(await file.text());if(!Array.isArray(obj.products)||!Array.isArray(obj.sales))throw new Error("Format tidak sesuai");if(confirm("Pulihkan data dari backup? Data saat ini akan diganti.")){state=obj;saveState();cart=[];renderAll();toast("Backup berhasil dipulihkan");}}catch(err){toast("File backup tidak valid");}e.target.value="";
});
function renderPremiumHome(){
 const today=state.sales.filter(s=>s.date===todayKey());
 const revenue=today.reduce((sum,s)=>sum+(Number(s.total)||0),0);
 const rev=document.getElementById("premiumTodayRevenue");
 const count=document.getElementById("premiumTodayCount");
 const date=document.getElementById("premiumTodayLabel");
 const recent=document.getElementById("premiumRecentActivity");
 if(rev)rev.textContent=rupiah(revenue);
 if(count)count.textContent=String(today.length);
 if(date)date.textContent=new Date().toLocaleDateString("id-ID",{day:"2-digit",month:"short"}).toUpperCase();
 if(recent){
  const rows=state.sales.slice(0,3);
  recent.innerHTML=rows.length?rows.map(s=>`<div class="premium-activity-row"><span class="premium-activity-icon">☕</span><div class="premium-activity-copy"><strong>${esc(s.customer||"Pelanggan umum")}</strong><small>${esc(s.id)} · ${new Date(s.time).toLocaleTimeString("id-ID",{hour:"2-digit",minute:"2-digit"})}</small></div><strong class="premium-activity-total">${rupiah(s.total)}</strong></div>`).join(""):'<div class="premium-activity-empty">Belum ada transaksi hari ini. Transaksi selesai akan muncul di sini.</div>';
 }
}

function renderCashierHome(){
 const today=state.sales.filter(s=>s.date===todayKey());
 const revenue=today.reduce((sum,s)=>sum+(Number(s.total)||0),0);
 const count=document.getElementById("cashierHomeCount"),rev=document.getElementById("cashierHomeRevenue"),date=document.getElementById("cashierHomeDate");
 if(count)count.textContent=String(today.length);
 if(rev)rev.textContent=rupiah(revenue);
 if(date)date.textContent=new Date().toLocaleDateString("id-ID",{day:"2-digit",month:"short"});
 const recent=document.getElementById("cashierHomeRecent");
 if(recent){const rows=state.sales.slice(0,3);recent.innerHTML=rows.length?rows.map(s=>`<div class="cashier-home-row"><span>☕</span><div><strong>${esc(s.customer||"Pelanggan umum")}</strong><small>${esc(s.id||"Transaksi")} · ${new Date(s.time||Date.now()).toLocaleTimeString("id-ID",{hour:"2-digit",minute:"2-digit"})}</small></div><strong>${rupiah(s.total)}</strong></div>`).join(""):'<p>Belum ada transaksi yang tercatat.</p>';}
 const pending=document.getElementById("cashierHomePending");
 if(pending){const rows=Array.isArray(state.pendingOrders)?state.pendingOrders:[];pending.innerHTML=rows.length?rows.slice(0,3).map((o,i)=>`<div class="cashier-home-row"><span>◷</span><div><strong>${esc(o.customer||o.name||("Pesanan "+(i+1)))}</strong><small>${esc(o.id||"Menunggu pembayaran")}</small></div><span class="pending-pill">Tertunda</span></div>`).join(""):'<p>Tidak ada pesanan tertunda.</p>';}
}

function renderAll(){renderFilters();renderMenu();renderCart();renderReports();renderManage();renderPremiumHome();renderCashierHome();}
window.addEventListener("beforeinstallprompt",e=>{e.preventDefault();deferredPrompt=e;document.getElementById("installBtn").classList.remove("hidden");});
document.getElementById("installBtn").addEventListener("click",async()=>{if(!deferredPrompt){toast("Gunakan menu browser: Tambahkan ke layar utama");return;}deferredPrompt.prompt();await deferredPrompt.userChoice;deferredPrompt=null;document.getElementById("installBtn").classList.add("hidden");});
if("serviceWorker" in navigator && (location.protocol==="https:"||location.hostname==="localhost"))navigator.serviceWorker.register("./sw.js").catch(()=>{});
renderAll();
