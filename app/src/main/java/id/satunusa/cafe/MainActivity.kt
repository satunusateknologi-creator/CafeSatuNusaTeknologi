package id.satunusa.cafe

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.content.ContentValues
import android.content.Intent
import android.view.Gravity
import android.widget.*
import java.util.Locale

data class CartItem(val id:Long,val name:String,val price:Long,var qty:Int)

class MainActivity:Activity(){
 private lateinit var db:CafeDb
 private val cart=linkedMapOf<Long,CartItem>()
 private var table="Takeaway"
 private var discount=0L
 private var taxPercent=0.0
 private var servicePercent=0.0
 private var cartText:TextView?=null
 private var totalText:TextView?=null
 private fun money(v:Long)="Rp "+String.format(Locale.US,"%,d",v).replace(',','.')
 private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
 private fun tv(s:String,size:Float=16f)=TextView(this).apply{text=s;textSize=size;setTextColor(Color.rgb(35,35,35));setPadding(dp(16),dp(10),dp(16),dp(10))}
 private fun button(s:String,fn:()->Unit)=Button(this).apply{text=s;setOnClickListener{fn()}}
 private fun layout(title:String):LinearLayout{val r=LinearLayout(this);r.orientation=LinearLayout.VERTICAL;r.setBackgroundColor(Color.rgb(248,249,250));val h=TextView(this).apply{text=title;textSize=22f;setTextColor(Color.WHITE);setGravity(Gravity.CENTER_VERTICAL);setPadding(dp(18),dp(18),dp(18),dp(18));setBackgroundColor(Color.rgb(24,28,35))};r.addView(h,LinearLayout.LayoutParams(-1,dp(64)));return r}
 private fun scroll(r:LinearLayout){setContentView(ScrollView(this).apply{addView(r)})}
 override fun onCreate(b:Bundle?){super.onCreate(b);db=CafeDb(this);taxPercent=db.setting("tax_percent","0").toDoubleOrNull()?:0.0;servicePercent=db.setting("service_percent","0").toDoubleOrNull()?:0.0;home()}
 private fun card(r:LinearLayout,title:String,value:String){val x=LinearLayout(this);x.orientation=LinearLayout.VERTICAL;x.setPadding(dp(8),dp(8),dp(8),dp(8));x.setBackgroundColor(Color.WHITE);x.addView(tv(title,13f));x.addView(tv(value,21f));r.addView(x)}
 private fun home(){
  val r=layout("☕ Cafe Satu Nusa")
  val sum=db.readableDatabase.rawQuery("SELECT COUNT(*),COALESCE(SUM(total),0) FROM orders WHERE date(created_at/1000,'unixepoch','localtime')=date('now','localtime')",null);var trx=0L;var omzet=0L;if(sum.moveToFirst()){trx=sum.getLong(0);omzet=sum.getLong(1)};sum.close()
  val ex=db.readableDatabase.rawQuery("SELECT COALESCE(SUM(amount),0) FROM expenses WHERE date(created_at/1000,'unixepoch','localtime')=date('now','localtime')",null);var expense=0L;if(ex.moveToFirst())expense=ex.getLong(0);ex.close()
  val stats=LinearLayout(this);stats.orientation=LinearLayout.VERTICAL;stats.setPadding(dp(10),dp(10),dp(10),dp(10));card(stats,"Transaksi Hari Ini",trx.toString());card(stats,"Omzet Hari Ini",money(omzet));card(stats,"Pengeluaran Hari Ini",money(expense));card(stats,"Estimasi Laba",money(omzet-expense));r.addView(stats)
  r.addView(button("🧾 KASIR / POS"){pos()});r.addView(button("🍳 KITCHEN DISPLAY"){kitchen()});r.addView(button("🪑 MEJA & PESANAN"){orders()});r.addView(button("🍔 MENU & KATEGORI"){menuManager()});r.addView(button("📦 STOK & RESEP"){stockManager()});r.addView(button("👥 PELANGGAN"){customers()});r.addView(button("👨‍💼 KARYAWAN & SHIFT"){employees()});r.addView(button("💸 PENGELUARAN"){expenses()});r.addView(button("📊 LAPORAN & ANALITIK"){reports()});r.addView(button("⚙️ PENGATURAN"){settings()});scroll(r)
 }
 private fun pos(){
  val r=layout("🧾 Kasir / POS");r.addView(button("Meja: $table • Diskon: ${money(discount)}"){chooseTable()})
  val cats=mutableListOf("Semua");db.readableDatabase.rawQuery("SELECT name FROM categories WHERE active=1 ORDER BY name",null).use{c->while(c.moveToNext())cats.add(c.getString(0))}
  cats.forEach{cat->r.addView(button(if(cat=="Semua")"🍽 Semua Menu" else "• $cat"){menuForPos(r,if(cat=="Semua")null else cat)})}
  r.addView(tv("Keranjang",18f));cartText=tv("");r.addView(cartText);totalText=tv("",20f);r.addView(totalText);r.addView(button("💵 BAYAR TUNAI"){pay("Tunai")});r.addView(button("📱 BAYAR QRIS / TRANSFER"){pay("QRIS")});r.addView(button("🗑 Kosongkan Keranjang"){cart.clear();refreshCart()});r.addView(button("Kembali"){home()});scroll(r);refreshCart()
 }
 private fun menuForPos(parent:LinearLayout,cat:String?){
  val list=db.readableDatabase.rawQuery(if(cat==null)"SELECT id,name,price FROM menu WHERE active=1 ORDER BY name" else "SELECT id,name,price FROM menu WHERE active=1 AND category=? ORDER BY name",if(cat==null)null else arrayOf(cat))
  val box=LinearLayout(this);box.orientation=LinearLayout.VERTICAL;box.setBackgroundColor(Color.WHITE)
  list.use{c->while(c.moveToNext()){val id=c.getLong(0);val name=c.getString(1);val price=c.getLong(2);box.addView(button(name+" • "+money(price)){val old=cart[id];if(old==null)cart[id]=CartItem(id,name,price,1) else old.qty++;refreshCart()})}}
  parent.addView(box)
 }
 private fun refreshCart(){
  val lines=cart.values.joinToString("\n"){it.name+" x"+it.qty+" = "+money(it.price*it.qty)}
  val sub=cart.values.sumOf{it.price*it.qty};val net=(sub-discount).coerceAtLeast(0);val tax=Math.round(net*taxPercent/100);val service=Math.round(net*servicePercent/100);val total=net+tax+service
  cartText?.text=if(lines.isBlank())"Keranjang kosong" else lines+"\n\nSubtotal: "+money(sub)+"\nDiskon: "+money(discount)+"\nPajak: "+money(tax)+"\nService: "+money(service);totalText?.text="TOTAL  "+money(total)
 }
 private fun pay(method:String){
  if(cart.isEmpty()){toast("Keranjang kosong");return}
  val sub=cart.values.sumOf{it.price*it.qty};val net=(sub-discount).coerceAtLeast(0);val tax=Math.round(net*taxPercent/100);val service=Math.round(net*servicePercent/100);val total=net+tax+service
  val v=ContentValues().apply{put("subtotal",sub);put("total",total);put("payment",method);put("status","BARU");put("table_no",table);put("discount",discount);put("tax",tax);put("service",service);put("created_at",System.currentTimeMillis())}
  val id=db.writableDatabase.insert("orders",null,v);cart.values.forEach{item->val iv=ContentValues().apply{put("order_id",id);put("menu_id",item.id);put("menu_name",item.name);put("qty",item.qty);put("price",item.price)};db.writableDatabase.insert("order_items",null,iv)}
  cart.clear();discount=0;showReceipt(id,total,method)
 }
 private fun showReceipt(id:Long,total:Long,method:String){
  val lines=StringBuilder()
  lines.append("CAFE SATU NUSA\n")
  lines.append("Order #").append(id).append("\n")
  lines.append("Meja: ").append(table).append("\n")
  lines.append("------------------------------\n")
  db.readableDatabase.rawQuery("SELECT menu_name,qty,price FROM order_items WHERE order_id=? ORDER BY id",arrayOf(id.toString())).use{c->while(c.moveToNext()){lines.append(c.getString(0)).append(" x").append(c.getInt(1)).append("  ").append(money(c.getLong(2)*c.getInt(1))).append("\n")}}
  lines.append("------------------------------\n")
  lines.append("Pembayaran: ").append(method).append("\n")
  lines.append("TOTAL: ").append(money(total)).append("\n")
  lines.append("Terima kasih.\n")
  AlertDialog.Builder(this).setTitle("Struk #"+id).setMessage(lines.toString()).setPositiveButton("Bagikan"){_,_->val send=Intent(Intent.ACTION_SEND);send.type="text/plain";send.putExtra(Intent.EXTRA_TEXT,lines.toString());startActivity(Intent.createChooser(send,"Bagikan Struk"))}.setNegativeButton("Selesai"){_,_->home()}.show()
 }
 private fun discountDialog(){
  val e=EditText(this);e.hint="Nominal diskon";e.inputType=2
  AlertDialog.Builder(this).setTitle("Diskon Transaksi").setMessage("Masukkan nominal diskon dalam rupiah").setView(e).setPositiveButton("Terapkan"){_,_->discount=(e.text.toString().toLongOrNull()?:0).coerceAtLeast(0);refreshCart()}.setNegativeButton("Hapus Diskon"){_,_->discount=0;refreshCart()}.show()
 }
 private fun chooseTable(){val r=layout("🪑 Pilih Meja");r.addView(button("Takeaway"){table="Takeaway";pos()});for(i in 1..12)r.addView(button("Meja $i"){table="Meja $i";pos()});r.addView(button("Kembali"){pos()});scroll(r)}
 private fun orders(){
  val r=layout("🪑 Pesanan & Meja");r.addView(tv("30 pesanan terakhir",18f));db.readableDatabase.rawQuery("SELECT id,table_no,total,payment,status FROM orders ORDER BY id DESC LIMIT 30",null).use{c->while(c.moveToNext()){val id=c.getLong(0);val status=c.getString(4);r.addView(tv("#$id • "+c.getString(1)+" • "+money(c.getLong(2))+" • "+c.getString(3)+" • $status"));if(status!="SELESAI")r.addView(button("Ubah status"){statusDialog(id)})}};r.addView(button("Kembali"){home()});scroll(r)
 }
 private fun statusDialog(id:Long){val items=arrayOf("BARU","DIPROSES","SIAP","SELESAI","BATAL");AlertDialog.Builder(this).setTitle("Status Order #$id").setItems(items){_,which->db.updateStatus(id,items[which]);orders()}.show()}
 private fun kitchen(){
  val r=layout("🍳 Kitchen Display System");r.addView(tv("Antrian dapur",18f));db.readableDatabase.rawQuery("SELECT id,table_no,total,status FROM orders WHERE status IN('BARU','DIPROSES','SIAP') ORDER BY id ASC",null).use{c->while(c.moveToNext()){val id=c.getLong(0);val status=c.getString(3);r.addView(tv("ORDER #$id • "+c.getString(1)+"\n"+money(c.getLong(2))+" • $status",17f));r.addView(button(if(status=="BARU")"Mulai Diproses" else if(status=="DIPROSES")"Tandai Siap" else "Tandai Selesai"){val next=if(status=="BARU")"DIPROSES" else if(status=="DIPROSES")"SIAP" else "SELESAI";db.updateStatus(id,next);kitchen()})}};r.addView(button("Kembali"){home()});scroll(r)
 }
 private fun menuManager(){
  val r=layout("🍔 Menu & Kategori");r.addView(button("＋ Tambah Menu"){menuForm()});db.readableDatabase.rawQuery("SELECT id,name,price,category,active FROM menu ORDER BY category,name",null).use{c->while(c.moveToNext()){val id=c.getLong(0);r.addView(tv(c.getString(3)+" • "+c.getString(1)+" • "+money(c.getLong(2)),16f));r.addView(button("Edit / Nonaktifkan"){menuEdit(id)})}};r.addView(button("＋ Tambah Kategori"){categoryForm()});r.addView(button("Kembali"){home()});scroll(r)
 }
 private fun menuForm(){
  val box=LinearLayout(this);box.orientation=LinearLayout.VERTICAL;val n=EditText(this);n.hint="Nama menu";val p=EditText(this);p.hint="Harga";p.inputType=2;val c=EditText(this);c.hint="Kategori";box.addView(n);box.addView(p);box.addView(c)
  AlertDialog.Builder(this).setTitle("Tambah Menu").setView(box).setPositiveButton("Simpan"){_,_->val v=ContentValues().apply{put("name",n.text.toString());put("price",p.text.toString().toLongOrNull()?:0);put("category",c.text.toString());put("active",1)};db.writableDatabase.insert("menu",null,v);menuManager()}.setNegativeButton("Batal",null).show()
 }
 private fun menuEdit(id:Long){AlertDialog.Builder(this).setTitle("Menu").setItems(arrayOf("Nonaktifkan","Hapus")){_,w->if(w==0)db.writableDatabase.execSQL("UPDATE menu SET active=0 WHERE id=?",arrayOf(id)) else db.writableDatabase.delete("menu","id=?",arrayOf(id.toString()));menuManager()}.show()}
 private fun categoryForm(){val e=EditText(this);e.hint="Nama kategori";AlertDialog.Builder(this).setTitle("Kategori Baru").setView(e).setPositiveButton("Simpan"){_,_->db.writableDatabase.execSQL("INSERT OR IGNORE INTO categories(name) VALUES(?)",arrayOf(e.text.toString()));menuManager()}.setNegativeButton("Batal",null).show()}
 private fun stockManager(){
  val r=layout("📦 Stok & Resep");r.addView(button("＋ Tambah Bahan"){stockForm()});r.addView(button("📥 Stok Masuk"){stockAdjustDialog(true)});r.addView(button("📤 Stok Keluar / Koreksi"){stockAdjustDialog(false)})
  db.readableDatabase.rawQuery("SELECT id,name,qty,unit,min_qty FROM stock ORDER BY name",null).use{c->while(c.moveToNext()){val low=c.getDouble(2)<=c.getDouble(4);r.addView(tv((if(low)"⚠️ " else "• ")+c.getString(1)+" : "+c.getDouble(2)+" "+c.getString(3)+" (min "+c.getDouble(4)+")",16f))}}
  r.addView(button("⚙️ Atur Resep / BOM"){recipeForm()});r.addView(button("Kembali"){home()});scroll(r)
 }
 private fun stockForm(){
  val box=LinearLayout(this);box.orientation=LinearLayout.VERTICAL;val n=EditText(this);n.hint="Nama bahan";val q=EditText(this);q.hint="Saldo awal";q.inputType=2;val u=EditText(this);u.hint="Satuan";val m=EditText(this);m.hint="Minimum";m.inputType=2;box.addView(n);box.addView(q);box.addView(u);box.addView(m)
  AlertDialog.Builder(this).setTitle("Tambah Bahan").setView(box).setPositiveButton("Simpan"){_,_->val v=ContentValues().apply{put("name",n.text.toString());put("qty",q.text.toString().toDoubleOrNull()?:0.0);put("unit",u.text.toString());put("min_qty",m.text.toString().toDoubleOrNull()?:0.0)};db.writableDatabase.insert("stock",null,v);stockManager()}.setNegativeButton("Batal",null).show()
 }
 private fun stockAdjustDialog(incoming:Boolean){
  val names=mutableListOf<Pair<Long,String>>();db.readableDatabase.rawQuery("SELECT id,name FROM stock ORDER BY name",null).use{c->while(c.moveToNext())names.add(c.getLong(0) to c.getString(1))};if(names.isEmpty()){toast("Belum ada bahan");return}
  AlertDialog.Builder(this).setTitle(if(incoming)"Pilih bahan masuk" else "Pilih bahan keluar").setItems(names.map{it.second}.toTypedArray()){_,w->val q=EditText(this);q.hint="Jumlah";q.inputType=2;AlertDialog.Builder(this).setTitle(names[w].second).setView(q).setPositiveButton("Simpan"){_,_->val amount=q.text.toString().toDoubleOrNull()?:0.0;val delta=if(incoming)amount else -amount;db.writableDatabase.execSQL("UPDATE stock SET qty=qty+? WHERE id=?",arrayOf(delta,names[w].first));db.writableDatabase.execSQL("INSERT INTO stock_movements(stock_id,type,qty,reference,created_at) VALUES(?,?,?,?,?)",arrayOf(names[w].first,if(incoming)"IN" else "OUT",delta,"MANUAL",System.currentTimeMillis()));stockManager()}.show()}.show()
 }
 private fun recipeForm(){
  val menus=mutableListOf<Pair<Long,String>>();db.readableDatabase.rawQuery("SELECT id,name FROM menu WHERE active=1 ORDER BY name",null).use{c->while(c.moveToNext())menus.add(c.getLong(0) to c.getString(1))}
  val stocks=mutableListOf<Pair<Long,String>>();db.readableDatabase.rawQuery("SELECT id,name FROM stock ORDER BY name",null).use{c->while(c.moveToNext())stocks.add(c.getLong(0) to c.getString(1))}
  if(menus.isEmpty()||stocks.isEmpty()){toast("Menu dan bahan harus tersedia");return}
  AlertDialog.Builder(this).setTitle("Pilih Menu").setItems(menus.map{it.second}.toTypedArray()){_,mi->AlertDialog.Builder(this).setTitle("Pilih Bahan").setItems(stocks.map{it.second}.toTypedArray()){_,si->val q=EditText(this);q.hint="Jumlah per 1 porsi";q.inputType=2;AlertDialog.Builder(this).setTitle("Resep").setView(q).setPositiveButton("Simpan"){_,_->db.writableDatabase.delete("recipes","menu_id=? AND stock_id=?",arrayOf(menus[mi].first.toString(),stocks[si].first.toString()));db.writableDatabase.execSQL("INSERT INTO recipes(menu_id,stock_id,qty) VALUES(?,?,?)",arrayOf(menus[mi].first,stocks[si].first,q.text.toString().toDoubleOrNull()?:0.0));toast("Resep disimpan")}.show()}.show()}.show()
 }
 private fun customers(){
  val r=layout("👥 Pelanggan");r.addView(button("＋ Tambah Pelanggan"){val box=LinearLayout(this);box.orientation=LinearLayout.VERTICAL;val n=EditText(this);n.hint="Nama";val p=EditText(this);p.hint="No. HP";box.addView(n);box.addView(p);AlertDialog.Builder(this).setTitle("Pelanggan").setView(box).setPositiveButton("Simpan"){_,_->val v=ContentValues().apply{put("name",n.text.toString());put("phone",p.text.toString())};db.writableDatabase.insert("customers",null,v);customers()}.show()});db.readableDatabase.rawQuery("SELECT name,phone,points FROM customers ORDER BY name",null).use{c->while(c.moveToNext())r.addView(tv(c.getString(0)+" • "+c.getString(1)+" • "+c.getInt(2)+" poin"))};r.addView(button("Kembali"){home()});scroll(r)
 }
 private fun employees(){
  val r=layout("👨‍💼 Karyawan & Shift");r.addView(button("＋ Tambah Karyawan"){val n=EditText(this);n.hint="Nama";AlertDialog.Builder(this).setTitle("Karyawan").setView(n).setPositiveButton("Simpan"){_,_->val v=ContentValues().apply{put("name",n.text.toString());put("role","KASIR");put("active",1)};db.writableDatabase.insert("employees",null,v);employees()}.show()});r.addView(button("▶ Buka Shift Kasir"){shiftOpen()});r.addView(button("⏹ Tutup Shift"){shiftClose()});db.readableDatabase.rawQuery("SELECT name,role,active FROM employees ORDER BY name",null).use{c->while(c.moveToNext())r.addView(tv(c.getString(0)+" • "+c.getString(1)+" • "+if(c.getInt(2)==1)"Aktif" else "Nonaktif"))};r.addView(button("Kembali"){home()});scroll(r)
 }
 private fun shiftOpen(){val e=EditText(this);e.hint="Modal kas awal";e.inputType=2;AlertDialog.Builder(this).setTitle("Buka Shift").setView(e).setPositiveButton("Buka"){_,_->val v=ContentValues().apply{put("employee_id",1);put("opening_cash",e.text.toString().toLongOrNull()?:0);put("status","OPEN");put("opened_at",System.currentTimeMillis())};db.writableDatabase.insert("shifts",null,v);toast("Shift dibuka")}.show()}
 private fun shiftClose(){val e=EditText(this);e.hint="Kas akhir";e.inputType=2;AlertDialog.Builder(this).setTitle("Tutup Shift").setView(e).setPositiveButton("Tutup"){_,_->val v=ContentValues().apply{put("closing_cash",e.text.toString().toLongOrNull()?:0);put("status","CLOSED");put("closed_at",System.currentTimeMillis())};val id=db.readableDatabase.rawQuery("SELECT id FROM shifts WHERE status='OPEN' ORDER BY id DESC LIMIT 1",null);if(id.moveToFirst()){val sid=id.getLong(0);db.writableDatabase.update("shifts",v,"id=?",arrayOf(sid.toString()))};id.close();toast("Shift ditutup")}.show()}
 private fun expenses(){
  val r=layout("💸 Pengeluaran");r.addView(button("＋ Catat Pengeluaran"){val box=LinearLayout(this);box.orientation=LinearLayout.VERTICAL;val t=EditText(this);t.hint="Keterangan";val a=EditText(this);a.hint="Nominal";a.inputType=2;val c=EditText(this);c.hint="Kategori";box.addView(t);box.addView(a);box.addView(c);AlertDialog.Builder(this).setTitle("Pengeluaran").setView(box).setPositiveButton("Simpan"){_,_->val v=ContentValues().apply{put("title",t.text.toString());put("amount",a.text.toString().toLongOrNull()?:0);put("category",c.text.toString().ifBlank{"Operasional"});put("created_at",System.currentTimeMillis())};db.writableDatabase.insert("expenses",null,v);expenses()}.show()});db.readableDatabase.rawQuery("SELECT title,amount,category FROM expenses ORDER BY id DESC LIMIT 30",null).use{c->while(c.moveToNext())r.addView(tv(c.getString(0)+" • "+c.getString(2)+" • "+money(c.getLong(1))))};r.addView(button("Kembali"){home()});scroll(r)
 }
 private fun reports(){
  val r=layout("📊 Laporan & Analitik");val q=db.readableDatabase.rawQuery("SELECT COUNT(*),COALESCE(SUM(total),0),COALESCE(SUM(discount),0),COALESCE(SUM(tax),0),COALESCE(SUM(service),0) FROM orders WHERE date(created_at/1000,'unixepoch','localtime')=date('now','localtime')",null);if(q.moveToFirst())r.addView(tv("Hari ini\nTransaksi: "+q.getLong(0)+"\nOmzet: "+money(q.getLong(1))+"\nDiskon: "+money(q.getLong(2))+"\nPajak: "+money(q.getLong(3))+"\nService: "+money(q.getLong(4)),18f));q.close()
  db.readableDatabase.rawQuery("SELECT payment,COUNT(*),COALESCE(SUM(total),0) FROM orders GROUP BY payment ORDER BY 3 DESC",null).use{c->r.addView(tv("Metode Pembayaran",18f));while(c.moveToNext())r.addView(tv(c.getString(0)+" • "+c.getLong(1)+" transaksi • "+money(c.getLong(2))))}
  db.readableDatabase.rawQuery("SELECT menu_name,SUM(qty) FROM order_items GROUP BY menu_id ORDER BY 2 DESC LIMIT 10",null).use{c->r.addView(tv("Menu Terlaris",18f));while(c.moveToNext())r.addView(tv(c.getString(0)+" • "+c.getLong(1)+" terjual"))}
  r.addView(button("Riwayat Semua Order"){orders()});r.addView(button("Kembali"){home()});scroll(r)
 }
 private fun settings(){
  val r=layout("⚙️ Pengaturan")
  val tax=EditText(this);tax.hint="Pajak %";tax.setText(db.setting("tax_percent","0"))
  val service=EditText(this);service.hint="Service charge %";service.setText(db.setting("service_percent","0"))
  val server=EditText(this);server.hint="URL server, contoh https://domain.com";server.setText(db.setting("server_url",""))
  r.addView(tv("Pajak, service dan koneksi server"));r.addView(tax);r.addView(service);r.addView(server)
  r.addView(button("Simpan Pengaturan"){taxPercent=tax.text.toString().toDoubleOrNull()?:0.0;servicePercent=service.text.toString().toDoubleOrNull()?:0.0;db.saveSetting("tax_percent",taxPercent.toString());db.saveSetting("service_percent",servicePercent.toString());db.saveSetting("server_url",server.text.toString().trim());toast("Pengaturan tersimpan")})
  r.addView(button("🌐 Tes Koneksi Server"){val url=server.text.toString().trim();if(url.isBlank()){toast("Isi URL server dahulu")}else{Thread{val result=ApiClient(url).get("health");runOnUiThread{toast(if(result.isSuccess)"Server terhubung" else "Server tidak dapat dihubungi")}}.start()}})
  r.addView(button("Tentang Aplikasi"){AlertDialog.Builder(this).setTitle("Cafe Satu Nusa").setMessage("POS Cafe offline-first\nVersi 2.0\nNative Android + SQLite\nBackend PHP Native + MySQL siap dihubungkan.").setPositiveButton("OK",null).show()})
  r.addView(button("Kembali"){home()});scroll(r)
 }
 private fun toast(s:String)=Toast.makeText(this,s,Toast.LENGTH_SHORT).show()
}
