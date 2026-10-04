package id.satunusa.cafe

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.ViewGroup
import android.widget.*
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class CafeMenu(val id:Int,val name:String,val price:Long,val category:String)

class CafeDb(context:Context):SQLiteOpenHelper(context,"cafe.db",null,2){
 override fun onCreate(db:SQLiteDatabase){
  db.execSQL("CREATE TABLE menu(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT,price INTEGER,category TEXT)")
  db.execSQL("CREATE TABLE orders(id INTEGER PRIMARY KEY AUTOINCREMENT,total INTEGER,payment TEXT,status TEXT,table_no TEXT,created_at INTEGER)")
  db.execSQL("CREATE TABLE order_items(id INTEGER PRIMARY KEY AUTOINCREMENT,order_id INTEGER,menu_id INTEGER,qty INTEGER,price INTEGER)")
  db.execSQL("CREATE TABLE expenses(id INTEGER PRIMARY KEY AUTOINCREMENT,title TEXT,amount INTEGER,created_at INTEGER)")
  db.execSQL("CREATE TABLE stock(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT,qty REAL,unit TEXT)")
  seed(db)
 }
 override fun onUpgrade(db:SQLiteDatabase,oldVersion:Int,newVersion:Int){
  if(oldVersion<2){db.execSQL("ALTER TABLE orders ADD COLUMN status TEXT DEFAULT 'BARU'");db.execSQL("ALTER TABLE orders ADD COLUMN table_no TEXT DEFAULT ''");db.execSQL("CREATE TABLE expenses(id INTEGER PRIMARY KEY AUTOINCREMENT,title TEXT,amount INTEGER,created_at INTEGER)");db.execSQL("CREATE TABLE stock(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT,qty REAL,unit TEXT)")}
 }
 private fun seed(db:SQLiteDatabase){
  db.execSQL("INSERT INTO menu(name,price,category) VALUES('Es Kopi Susu',18000,'Minuman'),('Americano',15000,'Minuman'),('Nasi Goreng',25000,'Makanan'),('Mie Goreng',22000,'Makanan'),('Kentang Goreng',18000,'Snack')")
  db.execSQL("INSERT INTO stock(name,qty,unit) VALUES('Kopi',1000,'gram'),('Susu',10000,'ml'),('Gula',5000,'gram'),('Beras',10000,'gram')")
 }
 fun menus():List<CafeMenu>{val a=mutableListOf<CafeMenu>();readableDatabase.rawQuery("SELECT id,name,price,category FROM menu ORDER BY category,name",null).use{c->while(c.moveToNext())a.add(CafeMenu(c.getInt(0),c.getString(1),c.getLong(2),c.getString(3)))};return a}
 fun saveOrder(cart:Map<CafeMenu,Int>,payment:String,table:String):Long{val db=writableDatabase;val total=cart.entries.sumOf{e->e.key.price*e.value};db.beginTransaction();try{val cv=android.content.ContentValues().apply{put("total",total);put("payment",payment);put("status","BARU");put("table_no",table);put("created_at",System.currentTimeMillis())};val id=db.insert("orders",null,cv);cart.forEach{e->val v=android.content.ContentValues().apply{put("order_id",id);put("menu_id",e.key.id);put("qty",e.value);put("price",e.key.price)};db.insert("order_items",null,v)};db.setTransactionSuccessful();return id}finally{db.endTransaction()}}
 fun updateStatus(id:Long,status:String){writableDatabase.execSQL("UPDATE orders SET status=? WHERE id=?",arrayOf(status,id))}
}

class MainActivity:Activity(){
 lateinit var db:CafeDb;val cart=linkedMapOf<CafeMenu,Int>();lateinit var cartView:TextView;lateinit var totalView:TextView;var selectedTable="Takeaway"
 fun rp(v:Long)="Rp "+String.format("%,d",v).replace(',','.')
 override fun onCreate(b:Bundle?){super.onCreate(b);db=CafeDb(this);home()}
 fun root(title:String):LinearLayout{val r=LinearLayout(this);r.orientation=LinearLayout.VERTICAL;r.setBackgroundColor(Color.WHITE);val h=TextView(this);h.text=title;h.textSize=22f;h.setTextColor(Color.WHITE);h.setBackgroundColor(Color.rgb(30,30,30));h.setPadding(24,30,24,30);r.addView(h);return r}
 fun btn(s:String,f:()->Unit)=Button(this).apply{text=s;setOnClickListener{f()}}
 fun home(){val r=root("☕ Cafe Satu Nusa");r.addView(TextView(this).apply{text="Dashboard\n\nSistem POS Cafe offline-first";textSize=18f;setPadding(24,24,24,16)});r.addView(btn("🧾 Kasir / POS"){pos()});r.addView(btn("🪑 Meja Cafe"){tables()});r.addView(btn("🍳 Kitchen Display"){kitchen()});r.addView(btn("📦 Menu & Stok"){inventory()});r.addView(btn("💸 Pengeluaran"){expense()});r.addView(btn("📊 Laporan"){report()});setContentView(r)}
 fun pos(){val r=root("Kasir / POS");r.addView(btn("Meja: $selectedTable"){tables()});db.menus().forEach{m->r.addView(btn(m.name+" • "+rp(m.price)){cart[m]=(cart[m]?:0)+1;refresh()})};cartView=TextView(this);cartView.textSize=16f;cartView.setPadding(24,15,24,5);r.addView(cartView);totalView=TextView(this);totalView.textSize=20f;totalView.setPadding(24,5,24,5);r.addView(totalView);r.addView(btn("💵 Bayar Tunai"){pay("Tunai")});r.addView(btn("📱 Bayar QRIS / Transfer"){pay("QRIS")});r.addView(btn("Kembali"){home()});setContentView(r);refresh()}
 fun refresh(){val lines=cart.entries.joinToString("\n"){e->e.key.name+" x"+e.value};val t=cart.entries.sumOf{e->e.key.price*e.value};cartView.text=if(lines.isBlank())"Keranjang kosong" else "Pesanan:\n"+lines;totalView.text="TOTAL "+rp(t)}
 fun pay(p:String){if(cart.isEmpty()){Toast.makeText(this,"Keranjang kosong",Toast.LENGTH_SHORT).show();return};val id=db.saveOrder(cart,p,selectedTable);cart.clear();Toast.makeText(this,"Order #$id tersimpan • $p",Toast.LENGTH_LONG).show();refresh()}
 fun tables(){val r=root("🪑 Pilih Meja");r.addView(btn("Takeaway"){selectedTable="Takeaway";pos()});for(i in 1..12)r.addView(btn("Meja $i"){selectedTable="Meja $i";pos()});r.addView(btn("Kembali"){home()});setContentView(r)}
 fun kitchen(){val r=root("🍳 Kitchen Display");r.addView(TextView(this).apply{text="Pesanan masuk dari kasir akan diproses di sini.";textSize=17f;setPadding(24,18,24,10)});val c=db.readableDatabase.rawQuery("SELECT id,table_no,total,status FROM orders WHERE status!='SELESAI' ORDER BY id DESC",null);c.use{while(it.moveToNext()){val id=it.getLong(0);val box=LinearLayout(this);box.orientation=LinearLayout.VERTICAL;box.setPadding(20,12,20,12);box.addView(TextView(this).apply{text="Order #$id • "+it.getString(1)+"\n"+rp(it.getLong(2))+"\nStatus: "+it.getString(3);textSize=16f});box.addView(btn("Tandai DIPROSES"){db.updateStatus(id,"DIPROSES");kitchen()});box.addView(btn("Tandai SIAP"){db.updateStatus(id,"SIAP");kitchen()});box.addView(btn("Tandai SELESAI"){db.updateStatus(id,"SELESAI");kitchen()});r.addView(box)}};r.addView(btn("Kembali"){home()});setContentView(r)}
 fun inventory(){val r=root("📦 Menu & Stok");r.addView(TextView(this).apply{text="MENU";textSize=18f;setPadding(24,18,24,5)});db.menus().forEach{m->r.addView(TextView(this).apply{text=m.category+" • "+m.name+" • "+rp(m.price);textSize=16f;setPadding(24,10,24,10)})};r.addView(TextView(this).apply{text="STOK BAHAN";textSize=18f;setPadding(24,20,24,5)});db.readableDatabase.rawQuery("SELECT name,qty,unit FROM stock ORDER BY name",null).use{c->while(c.moveToNext())r.addView(TextView(this).apply{text=c.getString(0)+" : "+c.getDouble(1)+" "+c.getString(2);textSize=16f;setPadding(24,9,24,9)})};r.addView(btn("Kembali"){home()});setContentView(r)}
 fun expense(){val r=root("💸 Pengeluaran");val title=EditText(this);title.hint="Nama pengeluaran";r.addView(title);val amount=EditText(this);amount.hint="Nominal";amount.inputType=2;r.addView(amount);r.addView(btn("Simpan Pengeluaran"){val cv=android.content.ContentValues();cv.put("title",title.text.toString());cv.put("amount",amount.text.toString().toLongOrNull()?:0);cv.put("created_at",System.currentTimeMillis());db.writableDatabase.insert("expenses",null,cv);Toast.makeText(this,"Pengeluaran tersimpan",Toast.LENGTH_SHORT).show();home()});r.addView(btn("Kembali"){home()});setContentView(r)}
 fun report(){val r=root("📊 Laporan");db.readableDatabase.rawQuery("SELECT COUNT(*),COALESCE(SUM(total),0) FROM orders WHERE status='SELESAI' OR status='BARU' OR status='DIPROSES' OR status='SIAP'",null).use{c->if(c.moveToFirst())r.addView(TextView(this).apply{text="Total transaksi: "+c.getInt(0)+"\nOmzet: "+rp(c.getLong(1));textSize=20f;setPadding(24,25,24,20)})};db.readableDatabase.rawQuery("SELECT COALESCE(SUM(amount),0) FROM expenses",null).use{c->if(c.moveToFirst())r.addView(TextView(this).apply{text="Total pengeluaran: "+rp(c.getLong(0));textSize=18f;setPadding(24,10,24,20)})};r.addView(btn("Kembali"){home()});setContentView(r)}
}