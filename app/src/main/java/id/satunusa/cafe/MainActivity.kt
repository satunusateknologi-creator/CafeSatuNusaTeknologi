package id.satunusa.cafe

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.view.Gravity
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import android.widget.*
import java.util.Locale
import org.json.JSONObject

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
 private var menuArea:LinearLayout?=null
 private var menuSearch:EditText?=null
 private var cartBoxView:LinearLayout?=null
 private var selectedCategory:String?=null
 private val backupRequest=9101
 private val restoreRequest=9102
 private lateinit var session:SessionManager
 private fun money(v:Long)="Rp "+String.format(Locale.US,"%,d",v).replace(',','.')
 private val navy=Color.rgb(19,32,52)
 private val accent=Color.rgb(15,118,110)
 private val surface=Color.rgb(248,250,252)
 private val ink=Color.rgb(15,23,42)
 private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
 private fun bg(color:Int,radius:Int=18)=GradientDrawable().apply{setColor(color);cornerRadius=dp(radius).toFloat()}
 private fun tv(s:String,size:Float=16f)=TextView(this).apply{text=s;textSize=size;setTextColor(ink);setPadding(dp(4),dp(6),dp(4),dp(6))}
 private fun button(s:String,fn:()->Unit)=Button(this).apply{text=s;textSize=14f;isAllCaps=false;setTextColor(navy);typeface=Typeface.DEFAULT_BOLD;background=bg(Color.WHITE,16);stateListAnimator=null;setPadding(dp(14),dp(10),dp(14),dp(10));setOnClickListener{fn()}}
 private fun primaryButton(s:String,fn:()->Unit)=Button(this).apply{text=s;textSize=15f;isAllCaps=false;setTextColor(Color.WHITE);typeface=Typeface.DEFAULT_BOLD;background=bg(accent,16);stateListAnimator=null;setPadding(dp(14),dp(10),dp(14),dp(10));setOnClickListener{fn()}}
 private fun layout(title:String):LinearLayout{
  val r=LinearLayout(this);r.orientation=LinearLayout.VERTICAL;r.setBackgroundColor(surface);r.setPadding(dp(16),0,dp(16),dp(18))
  val h=TextView(this);h.text=title;h.textSize=25f;h.setTextColor(navy);h.typeface=Typeface.DEFAULT_BOLD;h.setPadding(dp(4),dp(20),dp(4),dp(16));r.addView(h)
  return r
 }
 private fun section(s:String)=TextView(this).apply{text=s;textSize=15f;setTextColor(navy);typeface=Typeface.DEFAULT_BOLD;setPadding(dp(4),dp(18),dp(4),dp(8))}
 private fun addGap(r:LinearLayout,h:Int=8){r.addView(Space(this),LinearLayout.LayoutParams(1,dp(h)))}
 private fun field(hint:String,number:Boolean=false)=EditText(this).apply{
  this.hint=hint;setTextSize(15f);setTextColor(ink);setHintTextColor(Color.rgb(100,116,139));setPadding(dp(14),dp(10),dp(14),dp(10));background=bg(Color.WHITE,14)
  if(number)inputType=2
 }
 private fun listCard(title:String,subtitle:String="",status:String?=null):LinearLayout{
  val box=LinearLayout(this);box.orientation=LinearLayout.VERTICAL;box.setPadding(dp(14),dp(12),dp(14),dp(12));box.background=bg(Color.WHITE,16)
  val head=LinearLayout(this);head.gravity=Gravity.CENTER_VERTICAL
  val t=TextView(this);t.text=title;t.textSize=16f;t.setTextColor(navy);t.typeface=Typeface.DEFAULT_BOLD;head.addView(t,LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1f))
  if(status!=null){val s=TextView(this);s.text=status;s.textSize=11f;s.setTextColor(accent);s.typeface=Typeface.DEFAULT_BOLD;s.setPadding(dp(9),dp(5),dp(9),dp(5));s.background=bg(Color.rgb(226,245,242),12);head.addView(s)}
  box.addView(head)
  if(subtitle.isNotBlank()){val st=TextView(this);st.text=subtitle;st.textSize=13f;st.setTextColor(Color.rgb(71,85,105));st.setPadding(0,dp(5),0,0);box.addView(st)}
  return box
 }
 private fun scroll(r:LinearLayout){setContentView(ScrollView(this).apply{isFillViewport=true;addView(r)})}
 private fun login(){
  val box=LinearLayout(this);box.orientation=LinearLayout.VERTICAL
  val u=field("Username")
  val p=field("Password");p.inputType=0x81
  box.addView(tv("Login Server",20f));box.addView(u);box.addView(p)
  AlertDialog.Builder(this).setTitle("Cafe Satu Nusa").setMessage("Masuk sebagai pengguna server").setView(box).setPositiveButton("Login"){_,_->
   val url=db.setting("server_url","").trim()
   if(url.isBlank()){toast("URL server belum diisi");home();return@setPositiveButton}
   Thread{
    val body=JSONObject().put("username",u.text.toString().trim()).put("password",p.text.toString()).toString()
    val result=ApiClient(url).post("login",body)
    runOnUiThread{
     if(result.isSuccess){
      try{
       val o=JSONObject(result.getOrThrow());val usr=o.getJSONObject("user")
       session.save(o.getString("token"),usr.getString("name"),usr.getString("role"));toast("Login berhasil");home()
      }catch(e:Exception){toast("Respons server tidak valid")}
     }else toast("Login gagal")
    }
   }.start()
  }.setNegativeButton("Mode Offline"){_,_->session.clear();home()}.show()
 }
 private fun backupDatabase(){
  val intent=Intent(Intent.ACTION_CREATE_DOCUMENT).apply{
   addCategory(Intent.CATEGORY_OPENABLE);type="application/octet-stream";putExtra(Intent.EXTRA_TITLE,"cafe-satu-nusa-backup.db")
  }
  startActivityForResult(intent,backupRequest)
 }
 private fun restoreDatabase(){
  val intent=Intent(Intent.ACTION_OPEN_DOCUMENT).apply{addCategory(Intent.CATEGORY_OPENABLE);type="application/octet-stream"}
  startActivityForResult(intent,restoreRequest)
 }
 override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?){
  super.onActivityResult(requestCode,resultCode,data)
  if(resultCode!=RESULT_OK||data?.data==null)return
  val uri=data.data!!
  try{
   val target=File(applicationInfo.dataDir+"/databases/cafe.db")
   if(requestCode==backupRequest){
    db.close()
    FileInputStream(target).use{input->contentResolver.openOutputStream(uri)?.use{output->input.copyTo(output)}?:throw IllegalStateException("Tidak dapat membuka file tujuan")}
    db=CafeDb(this)
    toast("Backup database berhasil")
   }else if(requestCode==restoreRequest){
    db.close()
    File(target.parentFile,"cafe.db-wal").delete()
    File(target.parentFile,"cafe.db-shm").delete()
    contentResolver.openInputStream(uri)?.use{input->FileOutputStream(target).use{output->input.copyTo(output)}}?:throw IllegalStateException("Tidak dapat membaca backup")
    db=CafeDb(this)
    taxPercent=db.setting("tax_percent","0").toDoubleOrNull()?:0.0
    servicePercent=db.setting("service_percent","0").toDoubleOrNull()?:0.0
    toast("Restore berhasil. Data lokal telah dipulihkan")
   }
  }catch(e:Exception){
   db=CafeDb(this)
   toast("Backup/restore gagal: "+(e.message?:"kesalahan file"))
  }
 }
 override fun onCreate(b:Bundle?){
  super.onCreate(b);session=SessionManager(this);db=CafeDb(this)
  taxPercent=db.setting("tax_percent","0").toDoubleOrNull()?:0.0
  servicePercent=db.setting("service_percent","0").toDoubleOrNull()?:0.0
  if(db.setting("server_url","").isNotBlank()&&!session.loggedIn)login() else home()
 }
 private fun card(r:LinearLayout,title:String,value:String){val x=LinearLayout(this);x.orientation=LinearLayout.VERTICAL;x.setPadding(dp(8),dp(8),dp(8),dp(8));x.setBackgroundColor(Color.WHITE);x.addView(tv(title,13f));x.addView(tv(value,21f));r.addView(x)}
 private fun home(){
  val r=layout("☕ Cafe Satu Nusa")
  val greet=TextView(this);greet.text="Halo, "+session.name;greet.textSize=14f;greet.setTextColor(Color.DKGRAY);r.addView(greet)
  val role=TextView(this);role.text=session.role+" • Operasional Hari Ini";role.textSize=12f;role.setTextColor(Color.GRAY);r.addView(role);addGap(r,12)
  val sum=db.readableDatabase.rawQuery("SELECT COUNT(*),COALESCE(SUM(total),0) FROM orders WHERE date(created_at/1000,'unixepoch','localtime')=date('now','localtime')",null);var trx=0L;var omzet=0L;if(sum.moveToFirst()){trx=sum.getLong(0);omzet=sum.getLong(1)};sum.close()
  val ex=db.readableDatabase.rawQuery("SELECT COALESCE(SUM(amount),0) FROM expenses WHERE date(created_at/1000,'unixepoch','localtime')=date('now','localtime')",null);var expense=0L;if(ex.moveToFirst())expense=ex.getLong(0);ex.close()
  r.addView(section("Ringkasan Hari Ini"))
  val s1=LinearLayout(this);s1.orientation=LinearLayout.HORIZONTAL;s1.addView(statCard("Transaksi",trx.toString()),LinearLayout.LayoutParams(0,dp(92),1f));s1.addView(statCard("Omzet",money(omzet)),LinearLayout.LayoutParams(0,dp(92),1f));r.addView(s1);addGap(r,8)
  val s2=LinearLayout(this);s2.orientation=LinearLayout.HORIZONTAL;s2.addView(statCard("Pengeluaran",money(expense)),LinearLayout.LayoutParams(0,dp(92),1f));s2.addView(statCard("Estimasi Laba",money(omzet-expense)),LinearLayout.LayoutParams(0,dp(92),1f));r.addView(s2)
  r.addView(section("Menu Utama"))
  val grid=GridLayout(this);grid.columnCount=2
  fun addMenu(label:String,fn:()->Unit){val v=button(label,fn);val lp=GridLayout.LayoutParams();lp.width=0;lp.height=dp(68);lp.columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f);lp.setMargins(dp(4),dp(4),dp(4),dp(4));grid.addView(v,lp)}
  if(session.role in listOf("OWNER","ADMIN","KASIR"))addMenu("🧾\nKasir / POS"){pos()}
  if(session.role in listOf("OWNER","ADMIN","KASIR","KITCHEN"))addMenu("🍳\nKitchen"){kitchen()}
  if(session.role in listOf("OWNER","ADMIN","KASIR"))addMenu("🪑\nPesanan"){orders()}
  if(session.role in listOf("OWNER","ADMIN"))addMenu("🍔\nMenu"){menuManager()}
  if(session.role in listOf("OWNER","ADMIN","KITCHEN"))addMenu("📦\nStok"){stockManager()}
  if(session.role in listOf("OWNER","ADMIN","KASIR"))addMenu("👥\nPelanggan"){customers()}
  if(session.role in listOf("OWNER","ADMIN"))addMenu("👨‍💼\nKaryawan"){employees()}
  if(session.role in listOf("OWNER","ADMIN"))addMenu("💸\nPengeluaran"){expenses()}
  if(session.role in listOf("OWNER","ADMIN"))addMenu("📊\nLaporan"){reports()}
  if(session.role in listOf("OWNER","ADMIN"))addMenu("🛠️\nLanjutan"){advancedManager()}
  if(session.role in listOf("OWNER","ADMIN"))addMenu("⚙️\nPengaturan"){settings()}
  r.addView(grid);if(session.loggedIn){addGap(r,8);r.addView(button("🚪 Keluar dari Server"){session.clear();home()})};scroll(r)
 }
 private fun statCard(title:String,value:String):TextView=TextView(this).apply{text=title+"\n"+value;textSize=13f;setTextColor(navy);typeface=Typeface.DEFAULT_BOLD;gravity=Gravity.CENTER_VERTICAL;setPadding(dp(14),dp(10),dp(10),dp(10));background=bg(Color.WHITE,18)}
 private fun pos(){
  val r=layout("🧾 Kasir / POS")
  val top=LinearLayout(this);top.orientation=LinearLayout.HORIZONTAL;top.addView(button("🪑 "+table){chooseTable()},LinearLayout.LayoutParams(0,dp(52),1f));top.addView(button("🏷 "+money(discount)){discountDialog()},LinearLayout.LayoutParams(0,dp(52),1f));r.addView(top)
  r.addView(section("Pilih Menu"))
  menuSearch=field("🔎 Cari menu")
  r.addView(menuSearch)
  menuSearch!!.addTextChangedListener(object:android.text.TextWatcher{
   override fun beforeTextChanged(s:CharSequence?,start:Int,count:Int,after:Int){}
   override fun onTextChanged(s:CharSequence?,start:Int,before:Int,count:Int){menuForPos(r,selectedCategory)}
   override fun afterTextChanged(s:android.text.Editable?){}
  })
  val cats=mutableListOf("Semua");db.readableDatabase.rawQuery("SELECT name FROM categories WHERE active=1 ORDER BY name",null).use{c0->while(c0.moveToNext())cats.add(c0.getString(0))}
  val catBar=LinearLayout(this);catBar.orientation=LinearLayout.HORIZONTAL
  cats.forEach{cat->catBar.addView(button(cat){selectedCategory=if(cat=="Semua")null else cat;menuForPos(r,selectedCategory)},LinearLayout.LayoutParams(dp(120),dp(48)))}
  val catScroll=HorizontalScrollView(this).apply{isHorizontalScrollBarEnabled=false;addView(catBar)}
  r.addView(catScroll)
  menuArea=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
  r.addView(menuArea)
  r.addView(section("Keranjang"))
  val cartBox=LinearLayout(this);cartBoxView=cartBox;cartBox.orientation=LinearLayout.VERTICAL;cartBox.setPadding(dp(14),dp(12),dp(14),dp(12));cartBox.background=bg(Color.WHITE,18);cartText=tv("");cartBox.addView(cartText);r.addView(cartBox)
  totalText=TextView(this);totalText!!.textSize=24f;totalText!!.typeface=Typeface.DEFAULT_BOLD;totalText!!.setTextColor(accent);totalText!!.gravity=Gravity.CENTER;r.addView(totalText);addGap(r,6)
  val pay=LinearLayout(this);pay.orientation=LinearLayout.HORIZONTAL;pay.addView(primaryButton("💵 Tunai"){pay("Tunai")},LinearLayout.LayoutParams(0,dp(56),1f));pay.addView(primaryButton("📱 QRIS"){pay("QRIS")},LinearLayout.LayoutParams(0,dp(56),1f));r.addView(pay);addGap(r,6)
  r.addView(button("🗑 Kosongkan Keranjang"){cart.clear();refreshCart()});addGap(r,4);r.addView(button("‹ Kembali"){home()});scroll(r);refreshCart()
 }
 private fun menuForPos(parent:LinearLayout,cat:String?){
  val search=menuSearch?.text?.toString()?.trim()?:""
  val sql=StringBuilder("SELECT id,name,price FROM menu WHERE active=1")
  val args=mutableListOf<String>()
  if(cat!=null){sql.append(" AND category=?");args.add(cat)}
  if(search.isNotBlank()){sql.append(" AND name LIKE ?");args.add("%"+search+"%")}
  sql.append(" ORDER BY name")
  val list=db.readableDatabase.rawQuery(sql.toString(),if(args.isEmpty())null else args.toTypedArray())
  val grid=GridLayout(this);grid.columnCount=2;grid.setPadding(dp(2),dp(6),dp(2),dp(6))
  var count=0
  list.use{c0->while(c0.moveToNext()){
   count++
   val id=c0.getLong(0);val name=c0.getString(1);val price=c0.getLong(2)
   val v=button(name+"\n"+money(price)){val old=cart[id];if(old==null)cart[id]=CartItem(id,name,price,1) else old.qty++;refreshCart()}
   val lp=GridLayout.LayoutParams();lp.width=0;lp.height=dp(82);lp.columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f);lp.setMargins(dp(4),dp(4),dp(4),dp(4));grid.addView(v,lp)
  }}
  menuArea?.removeAllViews()
  if(count==0)menuArea?.addView(tv("Menu tidak ditemukan.",14f)) else menuArea?.addView(grid)
 }
 private fun refreshCart(){
  val sub=cart.values.sumOf{it.price*it.qty};val net=(sub-discount).coerceAtLeast(0);val tax=Math.round(net*taxPercent/100);val service=Math.round(net*servicePercent/100);val total=net+tax+service
  val box=cartBoxView ?: return
  box.removeAllViews()
  if(cart.isEmpty()){box.addView(tv("Keranjang kosong",14f))}else{
   cart.values.toList().forEach{item->
    val row=LinearLayout(this);row.gravity=Gravity.CENTER_VERTICAL
    val info=TextView(this);info.text=item.name+"\n"+money(item.price)+" × "+item.qty;info.textSize=14f;info.setTextColor(ink);info.setPadding(0,dp(5),dp(6),dp(5))
    row.addView(info,LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1f))
    row.addView(button("−"){item.qty--;if(item.qty<=0)cart.remove(item.id);refreshCart()},LinearLayout.LayoutParams(dp(48),dp(44)))
    val q=TextView(this);q.text=item.qty.toString();q.textSize=15f;q.typeface=Typeface.DEFAULT_BOLD;q.gravity=Gravity.CENTER;q.setTextColor(navy);row.addView(q,LinearLayout.LayoutParams(dp(34),dp(44)))
    row.addView(primaryButton("+"){item.qty++;refreshCart()},LinearLayout.LayoutParams(dp(48),dp(44)))
    box.addView(row)
   }
   box.addView(tv("Subtotal: "+money(sub),13f))
   box.addView(tv("Diskon: "+money(discount)+" • Pajak: "+money(tax)+" • Service: "+money(service),12f))
  }
  totalText?.text="TOTAL  "+money(total)
 }
 private fun pay(method:String){
  if(cart.isEmpty()){toast("Keranjang kosong");return}
  val sub=cart.values.sumOf{it.price*it.qty}
  val net=(sub-discount).coerceAtLeast(0)
  val tax=Math.round(net*taxPercent/100)
  val service=Math.round(net*servicePercent/100)
  val total=net+tax+service
  val d=db.writableDatabase
  d.beginTransaction()
  var orderId=-1L
  try{
   val v=ContentValues().apply{
    put("subtotal",sub);put("total",total);put("payment",method);put("status","BARU")
    put("table_no",table);put("discount",discount);put("tax",tax);put("service",service)
    put("created_at",System.currentTimeMillis());put("sync_status","PENDING")
   }
   orderId=d.insertOrThrow("orders",null,v)
   for(item in cart.values){
    val iv=ContentValues().apply{put("order_id",orderId);put("menu_id",item.id);put("menu_name",item.name);put("qty",item.qty);put("price",item.price)}
    d.insertOrThrow("order_items",null,iv)
    d.rawQuery("SELECT stock_id,qty FROM recipes WHERE menu_id=?",arrayOf(item.id.toString())).use{c->
     while(c.moveToNext()){
      val stockId=c.getLong(0)
      val used=c.getDouble(1)*item.qty
      val available=d.rawQuery("SELECT qty FROM stock WHERE id=?",arrayOf(stockId.toString())).use{s->if(s.moveToFirst())s.getDouble(0)else 0.0}
      if(available+0.000001<used)throw IllegalStateException("Stok tidak cukup untuk "+item.name)
      d.execSQL("UPDATE stock SET qty=qty-? WHERE id=? AND qty>=?",arrayOf(used,stockId,used))
      d.execSQL("INSERT INTO stock_movements(stock_id,type,qty,reference,created_at) VALUES(?,?,?,?,?)",arrayOf(stockId,"SALE",used,"ORDER #"+orderId,System.currentTimeMillis()))
     }
    }
   }
   d.execSQL("INSERT INTO sync_queue(entity,entity_id,action,payload,created_at,synced) VALUES('ORDER',?,?,?, ?,0)",arrayOf(orderId,"CREATE","LOCAL_ORDER",System.currentTimeMillis()))
   db.logAudit("SALE","ORDER",orderId,"total=$total;payment=$method")
   d.setTransactionSuccessful()
  }catch(e:Exception){
   toast("Transaksi gagal: "+(e.message?:"kesalahan database"))
   return
  }finally{d.endTransaction()}
  cart.clear();discount=0
  showReceipt(orderId,total,method)
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
  val e=field("Nominal diskon",true)
  AlertDialog.Builder(this).setTitle("Diskon Transaksi").setMessage("Masukkan nominal diskon dalam rupiah").setView(e).setPositiveButton("Terapkan"){_,_->discount=(e.text.toString().toLongOrNull()?:0).coerceAtLeast(0);refreshCart()}.setNegativeButton("Hapus Diskon"){_,_->discount=0;refreshCart()}.show()
 }
 private fun chooseTable(){val r=layout("🪑 Pilih Meja");r.addView(button("Takeaway"){table="Takeaway";pos()});for(i in 1..12)r.addView(button("Meja $i"){table="Meja $i";pos()});r.addView(button("Kembali"){pos()});scroll(r)}
 private fun orders(){
  val r=layout("🪑 Pesanan & Meja");r.addView(tv("30 pesanan terakhir",14f));db.readableDatabase.rawQuery("SELECT id,table_no,total,payment,status FROM orders ORDER BY id DESC LIMIT 30",null).use{c->while(c.moveToNext()){val id=c.getLong(0);val status=c.getString(4);val row=listCard("#$id • "+c.getString(1),money(c.getLong(2))+" • "+c.getString(3),status);r.addView(row,LinearLayout.LayoutParams(-1,LinearLayout.LayoutParams.WRAP_CONTENT).apply{setMargins(0,dp(5),0,dp(5))});if(status!="SELESAI")r.addView(button("Ubah status"){statusDialog(id)})}};r.addView(button("Kembali"){home()});scroll(r)
 }
 private fun statusDialog(id:Long){val items=arrayOf("BARU","DIPROSES","SIAP","SELESAI","BATAL");AlertDialog.Builder(this).setTitle("Status Order #$id").setItems(items){_,which->db.updateStatus(id,items[which]);orders()}.show()}
 private fun kitchen(){
  val r=layout("🍳 Kitchen Display System");r.addView(tv("Antrian dapur",18f));db.readableDatabase.rawQuery("SELECT id,table_no,total,status FROM orders WHERE status IN('BARU','DIPROSES','SIAP') ORDER BY id ASC",null).use{c->while(c.moveToNext()){val id=c.getLong(0);val status=c.getString(3);r.addView(tv("ORDER #$id • "+c.getString(1)+"\n"+money(c.getLong(2))+" • $status",17f));r.addView(button(if(status=="BARU")"Mulai Diproses" else if(status=="DIPROSES")"Tandai Siap" else "Tandai Selesai"){val next=if(status=="BARU")"DIPROSES" else if(status=="DIPROSES")"SIAP" else "SELESAI";db.updateStatus(id,next);kitchen()})}};r.addView(button("Kembali"){home()});scroll(r)
 }
 private fun menuManager(){
  val r=layout("🍔 Menu & Kategori");r.addView(button("＋ Tambah Menu"){menuForm()});db.readableDatabase.rawQuery("SELECT id,name,price,category,active FROM menu ORDER BY category,name",null).use{c->while(c.moveToNext()){val id=c.getLong(0);r.addView(tv(c.getString(3)+" • "+c.getString(1)+" • "+money(c.getLong(2)),16f));r.addView(button("Edit / Nonaktifkan"){menuEdit(id)})}};r.addView(button("＋ Tambah Kategori"){categoryForm()});r.addView(button("Kembali"){home()});scroll(r)
 }
 private fun menuForm(){
  val box=LinearLayout(this);box.orientation=LinearLayout.VERTICAL;addGap(box,4);val n=field("Nama menu");val p=field("Harga",true);val c=field("Kategori");box.addView(n);addGap(box,6);box.addView(p);addGap(box,6);box.addView(c)
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
  val box=LinearLayout(this);box.orientation=LinearLayout.VERTICAL;val n=field("Nama bahan");val q=field("Saldo awal",true);val u=field("Satuan");val m=field("Minimum",true);box.addView(n);addGap(box,6);box.addView(q);addGap(box,6);box.addView(u);addGap(box,6);box.addView(m)
  AlertDialog.Builder(this).setTitle("Tambah Bahan").setView(box).setPositiveButton("Simpan"){_,_->val v=ContentValues().apply{put("name",n.text.toString());put("qty",q.text.toString().toDoubleOrNull()?:0.0);put("unit",u.text.toString());put("min_qty",m.text.toString().toDoubleOrNull()?:0.0)};db.writableDatabase.insert("stock",null,v);stockManager()}.setNegativeButton("Batal",null).show()
 }
 private fun stockAdjustDialog(incoming:Boolean){
  val names=mutableListOf<Pair<Long,String>>();db.readableDatabase.rawQuery("SELECT id,name FROM stock ORDER BY name",null).use{c->while(c.moveToNext())names.add(c.getLong(0) to c.getString(1))};if(names.isEmpty()){toast("Belum ada bahan");return}
  AlertDialog.Builder(this).setTitle(if(incoming)"Pilih bahan masuk" else "Pilih bahan keluar").setItems(names.map{it.second}.toTypedArray()){_,w->val q=field("Jumlah",true);AlertDialog.Builder(this).setTitle(names[w].second).setView(q).setPositiveButton("Simpan"){_,_->val amount=q.text.toString().toDoubleOrNull()?:0.0;val delta=if(incoming)amount else -amount;db.writableDatabase.execSQL("UPDATE stock SET qty=qty+? WHERE id=?",arrayOf(delta,names[w].first));db.writableDatabase.execSQL("INSERT INTO stock_movements(stock_id,type,qty,reference,created_at) VALUES(?,?,?,?,?)",arrayOf(names[w].first,if(incoming)"IN" else "OUT",delta,"MANUAL",System.currentTimeMillis()));stockManager()}.show()}.show()
 }
 private fun recipeForm(){
  val menus=mutableListOf<Pair<Long,String>>();db.readableDatabase.rawQuery("SELECT id,name FROM menu WHERE active=1 ORDER BY name",null).use{c->while(c.moveToNext())menus.add(c.getLong(0) to c.getString(1))}
  val stocks=mutableListOf<Pair<Long,String>>();db.readableDatabase.rawQuery("SELECT id,name FROM stock ORDER BY name",null).use{c->while(c.moveToNext())stocks.add(c.getLong(0) to c.getString(1))}
  if(menus.isEmpty()||stocks.isEmpty()){toast("Menu dan bahan harus tersedia");return}
  AlertDialog.Builder(this).setTitle("Pilih Menu").setItems(menus.map{it.second}.toTypedArray()){_,mi->AlertDialog.Builder(this).setTitle("Pilih Bahan").setItems(stocks.map{it.second}.toTypedArray()){_,si->val q=field("Jumlah per 1 porsi",true);AlertDialog.Builder(this).setTitle("Resep").setView(q).setPositiveButton("Simpan"){_,_->db.writableDatabase.delete("recipes","menu_id=? AND stock_id=?",arrayOf(menus[mi].first.toString(),stocks[si].first.toString()));db.writableDatabase.execSQL("INSERT INTO recipes(menu_id,stock_id,qty) VALUES(?,?,?)",arrayOf(menus[mi].first,stocks[si].first,q.text.toString().toDoubleOrNull()?:0.0));toast("Resep disimpan")}.show()}.show()}.show()
 }
 private fun advancedManager(){
  val r=layout("🛠️ Fitur Lanjutan")
  r.addView(button("➕ Tambah Modifier / Topping"){val b=LinearLayout(this);b.orientation=LinearLayout.VERTICAL;val n=EditText(this);n.hint="Nama topping";val p=EditText(this);p.hint="Harga";p.inputType=2;b.addView(n);b.addView(p);AlertDialog.Builder(this).setTitle("Modifier / Topping").setView(b).setPositiveButton("Simpan"){_,_->db.addModifier(n.text.toString(),p.text.toString().toLongOrNull()?:0);toast("Modifier disimpan")}.setNegativeButton("Batal",null).show()})
  r.addView(button("🏭 Supplier"){supplierManager()})
  r.addView(button("↩ Refund Order"){refundDialog()})
  r.addView(button("💾 Backup Database"){backupDatabase()})
  r.addView(button("♻️ Restore Database"){restoreDatabase()})
  r.addView(button("🔄 Sinkronisasi Server"){syncNow()})
  r.addView(button("Kembali"){home()});scroll(r)
 }
 private fun supplierManager(){
  val r=layout("🏭 Supplier")
  r.addView(button("🛒 Catat Pembelian / Stok Masuk"){purchaseForm()})
  r.addView(button("＋ Tambah Supplier"){val b=LinearLayout(this);b.orientation=LinearLayout.VERTICAL;val n=field("Nama");val p=field("Telepon");val a=field("Alamat");b.addView(n);b.addView(p);b.addView(a);AlertDialog.Builder(this).setTitle("Supplier").setView(b).setPositiveButton("Simpan"){_,_->db.addSupplier(n.text.toString(),p.text.toString(),a.text.toString());supplierManager()}.show()});db.readableDatabase.rawQuery("SELECT name,phone,address FROM suppliers WHERE active=1 ORDER BY name",null).use{c->while(c.moveToNext())r.addView(tv(c.getString(0)+" • "+(c.getString(1)?:"")+" • "+(c.getString(2)?:""))) };r.addView(button("Kembali"){advancedManager()});scroll(r)
 }
 private fun purchaseForm(){
  val suppliers=mutableListOf<Pair<Long,String>>()
  db.readableDatabase.rawQuery("SELECT id,name FROM suppliers WHERE active=1 ORDER BY name",null).use{c->while(c.moveToNext())suppliers.add(c.getLong(0) to c.getString(1))}
  val stocks=mutableListOf<Pair<Long,String>>()
  db.readableDatabase.rawQuery("SELECT id,name FROM stock ORDER BY name",null).use{c->while(c.moveToNext())stocks.add(c.getLong(0) to c.getString(1))}
  if(suppliers.isEmpty()){toast("Tambahkan supplier terlebih dahulu");return}
  if(stocks.isEmpty()){toast("Tambahkan bahan terlebih dahulu");return}
  AlertDialog.Builder(this).setTitle("Pilih Supplier").setItems(suppliers.map{it.second}.toTypedArray()){_,si->
   AlertDialog.Builder(this).setTitle("Pilih Bahan").setItems(stocks.map{it.second}.toTypedArray()){_,bi->
    val box=LinearLayout(this);box.orientation=LinearLayout.VERTICAL
    val qty=field("Jumlah",true)
    val price=field("Harga satuan",true)
    box.addView(qty);box.addView(price)
    AlertDialog.Builder(this).setTitle("Pembelian").setView(box).setPositiveButton("Simpan"){_,_->
     val q=qty.text.toString().toDoubleOrNull()?:0.0
     val unit=price.text.toString().toLongOrNull()?:0L
     if(q<=0||unit<0){
      toast("Jumlah/harga tidak valid")
     }else{
      val total=Math.round(q*unit)
      val d=db.writableDatabase;d.beginTransaction()
      try{
       val pv=ContentValues().apply{put("supplier_id",suppliers[si].first);put("total",total);put("status","RECEIVED");put("created_at",System.currentTimeMillis())}
       val purchaseId=d.insertOrThrow("purchases",null,pv)
       val iv=ContentValues().apply{put("purchase_id",purchaseId);put("stock_id",stocks[bi].first);put("qty",q);put("unit_price",unit)}
       d.insertOrThrow("purchase_items",null,iv)
       d.execSQL("UPDATE stock SET qty=qty+? WHERE id=?",arrayOf(q,stocks[bi].first))
       d.execSQL("INSERT INTO stock_movements(stock_id,type,qty,reference,created_at) VALUES(?,?,?,?,?)",arrayOf(stocks[bi].first,"PURCHASE",q,"PURCHASE #"+purchaseId,System.currentTimeMillis()))
       db.logAudit("PURCHASE","PURCHASE",purchaseId,"supplier="+suppliers[si].first+";stock="+stocks[bi].first+";qty="+q+";total="+total)
       d.setTransactionSuccessful()
       toast("Pembelian tersimpan dan stok bertambah")
      }catch(e:Exception){toast("Pembelian gagal: "+(e.message?:"database error"))}finally{d.endTransaction()}
      supplierManager()
     }
    }.setNegativeButton("Batal",null).show()
   }.show()
  }.show()
 }
 private fun refundDialog(){
  val e=field("ID Order",true)
  AlertDialog.Builder(this).setTitle("Refund").setMessage("Refund akan menandai order sebagai REFUND.").setView(e).setPositiveButton("Lanjut"){_,_->val id=e.text.toString().toLongOrNull()?:0;refundReason(id)}.setNegativeButton("Batal",null).show()
 }
 private fun refundReason(id:Long){
  val b=field("Alasan refund")
  AlertDialog.Builder(this).setTitle("Alasan Refund").setView(b)
   .setPositiveButton("Proses"){_,_->
    val q=db.readableDatabase.rawQuery("SELECT total,refund_amount,status FROM orders WHERE id=?",arrayOf(id.toString()))
    var amount=0L
    var refunded=0L
    var status=""
    if(q.moveToFirst()){amount=q.getLong(0);refunded=q.getLong(1);status=q.getString(2)}
    q.close()
    if(amount<=0){toast("Order tidak ditemukan");return@setPositiveButton}
    if(status=="REFUND"){toast("Order sudah refund");return@setPositiveButton}
    val available=amount-refunded
    if(available<=0){toast("Sisa refund sudah habis");return@setPositiveButton}
    try{db.refund(id,available,b.text.toString());toast("Refund order #"+id+" berhasil")}catch(e:Exception){toast(e.message?:"Refund gagal")}
   }
   .setNegativeButton("Batal",null).show()
 }
 private fun syncNow(){
  val url=db.setting("server_url","").trim()
  if(url.isBlank()){toast("Isi URL server di Pengaturan");return}
  Thread{
   val result=ApiClient(url,session.token).get("health")
   runOnUiThread{toast(if(result.isSuccess)"Server siap disinkronkan" else "Koneksi server gagal")}
  }.start()
 }
 private fun customers(){
  val r=layout("👥 Pelanggan");r.addView(button("＋ Tambah Pelanggan"){val box=LinearLayout(this);box.orientation=LinearLayout.VERTICAL;val n=field("Nama");val p=field("No. HP");box.addView(n);addGap(box,6);box.addView(p);AlertDialog.Builder(this).setTitle("Pelanggan").setView(box).setPositiveButton("Simpan"){_,_->val v=ContentValues().apply{put("name",n.text.toString());put("phone",p.text.toString())};db.writableDatabase.insert("customers",null,v);customers()}.show()});db.readableDatabase.rawQuery("SELECT name,phone,points FROM customers ORDER BY name",null).use{c->while(c.moveToNext())r.addView(tv(c.getString(0)+" • "+c.getString(1)+" • "+c.getInt(2)+" poin"))};r.addView(button("Kembali"){home()});scroll(r)
 }
 private fun employees(){
  val r=layout("👨‍💼 Karyawan & Shift");r.addView(button("＋ Tambah Karyawan"){val n=field("Nama");AlertDialog.Builder(this).setTitle("Karyawan").setView(n).setPositiveButton("Simpan"){_,_->val v=ContentValues().apply{put("name",n.text.toString());put("role","KASIR");put("active",1)};db.writableDatabase.insert("employees",null,v);employees()}.show()});r.addView(button("▶ Buka Shift Kasir"){shiftOpen()});r.addView(button("⏹ Tutup Shift"){shiftClose()});db.readableDatabase.rawQuery("SELECT name,role,active FROM employees ORDER BY name",null).use{c->while(c.moveToNext())r.addView(tv(c.getString(0)+" • "+c.getString(1)+" • "+if(c.getInt(2)==1)"Aktif" else "Nonaktif"))};r.addView(button("Kembali"){home()});scroll(r)
 }
 private fun shiftOpen(){val e=field("Modal kas awal",true);AlertDialog.Builder(this).setTitle("Buka Shift").setView(e).setPositiveButton("Buka"){_,_->val v=ContentValues().apply{put("employee_id",1);put("opening_cash",e.text.toString().toLongOrNull()?:0);put("status","OPEN");put("opened_at",System.currentTimeMillis())};db.writableDatabase.insert("shifts",null,v);toast("Shift dibuka")}.show()}
 private fun shiftClose(){val e=field("Kas akhir",true);AlertDialog.Builder(this).setTitle("Tutup Shift").setView(e).setPositiveButton("Tutup"){_,_->val v=ContentValues().apply{put("closing_cash",e.text.toString().toLongOrNull()?:0);put("status","CLOSED");put("closed_at",System.currentTimeMillis())};val id=db.readableDatabase.rawQuery("SELECT id FROM shifts WHERE status='OPEN' ORDER BY id DESC LIMIT 1",null);if(id.moveToFirst()){val sid=id.getLong(0);db.writableDatabase.update("shifts",v,"id=?",arrayOf(sid.toString()))};id.close();toast("Shift ditutup")}.show()}
 private fun expenses(){
  val r=layout("💸 Pengeluaran");r.addView(button("＋ Catat Pengeluaran"){val box=LinearLayout(this);box.orientation=LinearLayout.VERTICAL;val t=field("Keterangan");val a=field("Nominal",true);val c=field("Kategori");box.addView(t);addGap(box,6);box.addView(a);addGap(box,6);box.addView(c);AlertDialog.Builder(this).setTitle("Pengeluaran").setView(box).setPositiveButton("Simpan"){_,_->val v=ContentValues().apply{put("title",t.text.toString());put("amount",a.text.toString().toLongOrNull()?:0);put("category",c.text.toString().ifBlank{"Operasional"});put("created_at",System.currentTimeMillis())};db.writableDatabase.insert("expenses",null,v);expenses()}.show()});db.readableDatabase.rawQuery("SELECT title,amount,category FROM expenses ORDER BY id DESC LIMIT 30",null).use{c->while(c.moveToNext())r.addView(tv(c.getString(0)+" • "+c.getString(2)+" • "+money(c.getLong(1))))};r.addView(button("Kembali"){home()});scroll(r)
 }
 private fun reports(){
  val r=layout("📊 Laporan & Analitik");val q=db.readableDatabase.rawQuery("SELECT COUNT(*),COALESCE(SUM(total),0),COALESCE(SUM(discount),0),COALESCE(SUM(tax),0),COALESCE(SUM(service),0) FROM orders WHERE date(created_at/1000,'unixepoch','localtime')=date('now','localtime')",null);if(q.moveToFirst())r.addView(tv("Hari ini\nTransaksi: "+q.getLong(0)+"\nOmzet: "+money(q.getLong(1))+"\nDiskon: "+money(q.getLong(2))+"\nPajak: "+money(q.getLong(3))+"\nService: "+money(q.getLong(4)),18f));q.close()
  db.readableDatabase.rawQuery("SELECT payment,COUNT(*),COALESCE(SUM(total),0) FROM orders GROUP BY payment ORDER BY 3 DESC",null).use{c->r.addView(tv("Metode Pembayaran",18f));while(c.moveToNext())r.addView(tv(c.getString(0)+" • "+c.getLong(1)+" transaksi • "+money(c.getLong(2))))}
  db.readableDatabase.rawQuery("SELECT menu_name,SUM(qty) FROM order_items GROUP BY menu_id ORDER BY 2 DESC LIMIT 10",null).use{c->r.addView(tv("Menu Terlaris",18f));while(c.moveToNext())r.addView(tv(c.getString(0)+" • "+c.getLong(1)+" terjual"))}
  r.addView(button("Riwayat Semua Order"){orders()});r.addView(button("Kembali"){home()});scroll(r)
 }
 private fun settings(){
  val r=layout("⚙️ Pengaturan")
  val tax=field("Pajak %",true);tax.setText(db.setting("tax_percent","0"))
  val service=field("Service charge %",true);service.setText(db.setting("service_percent","0"))
  val server=field("URL server, contoh https://domain.com");server.setText(db.setting("server_url",""))
  r.addView(tv("Pajak, service dan koneksi server"));r.addView(tax);r.addView(service);r.addView(server)
  r.addView(button("Simpan Pengaturan"){taxPercent=tax.text.toString().toDoubleOrNull()?:0.0;servicePercent=service.text.toString().toDoubleOrNull()?:0.0;db.saveSetting("tax_percent",taxPercent.toString());db.saveSetting("service_percent",servicePercent.toString());db.saveSetting("server_url",server.text.toString().trim());toast("Pengaturan tersimpan")})
  r.addView(button("🌐 Tes Koneksi Server"){val url=server.text.toString().trim();if(url.isBlank()){toast("Isi URL server dahulu")}else{Thread{val result=ApiClient(url).get("health");runOnUiThread{toast(if(result.isSuccess)"Server terhubung" else "Server tidak dapat dihubungi")}}.start()}})
  r.addView(button("Tentang Aplikasi"){AlertDialog.Builder(this).setTitle("Cafe Satu Nusa").setMessage("POS Cafe offline-first\nVersi 2.0\nNative Android + SQLite\nBackend PHP Native + MySQL siap dihubungkan.").setPositiveButton("OK",null).show()})
  r.addView(button("Kembali"){home()});scroll(r)
 }
 private fun toast(s:String)=Toast.makeText(this,s,Toast.LENGTH_SHORT).show()
}
