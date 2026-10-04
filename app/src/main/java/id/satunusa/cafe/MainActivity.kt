package id.satunusa.cafe
import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.widget.*
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class CafeMenu(val id:Int,val name:String,val price:Long,val category:String)
class CafeDb(context:Context):SQLiteOpenHelper(context,"cafe.db",null,1){
 override fun onCreate(db:SQLiteDatabase){
  db.execSQL("CREATE TABLE menu(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT,price INTEGER,category TEXT)")
  db.execSQL("CREATE TABLE orders(id INTEGER PRIMARY KEY AUTOINCREMENT,total INTEGER,payment TEXT,created_at INTEGER)")
  db.execSQL("CREATE TABLE order_items(id INTEGER PRIMARY KEY AUTOINCREMENT,order_id INTEGER,menu_id INTEGER,qty INTEGER,price INTEGER)")
  db.execSQL("INSERT INTO menu(name,price,category) VALUES('Es Kopi Susu',18000,'Minuman'),('Americano',15000,'Minuman'),('Nasi Goreng',25000,'Makanan'),('Mie Goreng',22000,'Makanan')")
 }
 override fun onUpgrade(db:SQLiteDatabase,oldVersion:Int,newVersion:Int){}
 fun menus():List<CafeMenu>{ val out=mutableListOf<CafeMenu>(); readableDatabase.rawQuery("SELECT id,name,price,category FROM menu ORDER BY category,name",null).use{c->while(c.moveToNext())out.add(CafeMenu(c.getInt(0),c.getString(1),c.getLong(2),c.getString(3)))};return out }
 fun saveOrder(cart:Map<CafeMenu,Int>,payment:String):Long{val db=writableDatabase;val total=cart.entries.sumOf{e->e.key.price*e.value};val cv=android.content.ContentValues();cv.put("total",total);cv.put("payment",payment);cv.put("created_at",System.currentTimeMillis());return db.insert("orders",null,cv)}
}
class MainActivity:Activity(){
 lateinit var db:CafeDb;val cart=linkedMapOf<CafeMenu,Int>();lateinit var cartView:TextView;lateinit var totalView:TextView
 fun rp(v:Long)="Rp "+String.format("%,d",v).replace(',','.')
 override fun onCreate(b:Bundle?){super.onCreate(b);db=CafeDb(this);home()}
 fun root(title:String):LinearLayout{val r=LinearLayout(this);r.orientation=LinearLayout.VERTICAL;r.setBackgroundColor(Color.WHITE);val h=TextView(this);h.text=title;h.textSize=23f;h.setTextColor(Color.WHITE);h.setBackgroundColor(Color.rgb(35,35,35));h.setPadding(24,32,24,32);r.addView(h);return r}
 fun btn(s:String,f:()->Unit)=Button(this).apply{text=s;setOnClickListener{f()}}
 fun home(){val r=root("☕ Cafe Satu Nusa");r.addView(TextView(this).apply{text="Dashboard\n\nKelola operasional cafe dari satu aplikasi.";textSize=18f;setPadding(24,28,24,20)});r.addView(btn("🧾 Kasir / POS"){pos()});r.addView(btn("🍳 Kitchen"){kitchen()});r.addView(btn("📦 Menu"){menu()});r.addView(btn("📊 Laporan"){report()});setContentView(r)}
 fun pos(){val r=root("Kasir / POS");db.menus().forEach{m->r.addView(btn(m.name+" • "+rp(m.price)){cart[m]=(cart[m]?:0)+1;refresh()})};cartView=TextView(this);cartView.textSize=16f;cartView.setPadding(24,18,24,8);r.addView(cartView);totalView=TextView(this);totalView.textSize=21f;totalView.setPadding(24,8,24,8);r.addView(totalView);r.addView(btn("💵 Bayar Tunai"){pay("Tunai")});r.addView(btn("📱 Bayar QRIS / Transfer"){pay("QRIS")});r.addView(btn("Kembali"){home()});setContentView(r);refresh()}
 fun refresh(){val lines=cart.entries.joinToString("\n"){e->e.key.name+" x"+e.value};val t=cart.entries.sumOf{e->e.key.price*e.value};cartView.text=if(lines.isBlank())"Keranjang kosong" else "Pesanan:\n"+lines;totalView.text="TOTAL  "+rp(t)}
 fun pay(p:String){if(cart.isEmpty()){Toast.makeText(this,"Keranjang kosong",Toast.LENGTH_SHORT).show();return};val id=db.saveOrder(cart,p);cart.clear();Toast.makeText(this,"Transaksi #"+id+" berhasil",Toast.LENGTH_LONG).show();refresh()}
 fun menu(){val r=root("Menu");db.menus().forEach{m->r.addView(TextView(this).apply{text=m.category+" • "+m.name+" • "+rp(m.price);textSize=17f;setPadding(24,14,24,14)})};r.addView(btn("Kembali"){home()});setContentView(r)}
 fun kitchen(){val r=root("Kitchen Display");r.addView(TextView(this).apply{text="Modul Kitchen Display akan dikembangkan pada tahap berikutnya.";textSize=18f;setPadding(24,28,24,28)});r.addView(btn("Kembali"){home()});setContentView(r)}
 fun report(){val r=root("Laporan");db.readableDatabase.rawQuery("SELECT COUNT(*),COALESCE(SUM(total),0) FROM orders",null).use{c->if(c.moveToFirst())r.addView(TextView(this).apply{text="Total transaksi: "+c.getInt(0)+"\nOmzet: "+rp(c.getLong(1));textSize=20f;setPadding(24,28,24,28)})};r.addView(btn("Kembali"){home()});setContentView(r)}
}