package id.satunusa.cafe

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class CafeDb(context: Context) : SQLiteOpenHelper(context, "cafe.db", null, 3) {
 override fun onCreate(db: SQLiteDatabase) {
  db.execSQL("CREATE TABLE IF NOT EXISTS categories(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT UNIQUE,active INTEGER DEFAULT 1)")
  db.execSQL("CREATE TABLE IF NOT EXISTS menu(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT,price INTEGER,category TEXT,description TEXT DEFAULT '',active INTEGER DEFAULT 1)")
  db.execSQL("CREATE TABLE IF NOT EXISTS orders(id INTEGER PRIMARY KEY AUTOINCREMENT,subtotal INTEGER,total INTEGER,payment TEXT,status TEXT,table_no TEXT,discount INTEGER DEFAULT 0,tax INTEGER DEFAULT 0,service INTEGER DEFAULT 0,notes TEXT DEFAULT '',created_at INTEGER)")
  db.execSQL("CREATE TABLE IF NOT EXISTS order_items(id INTEGER PRIMARY KEY AUTOINCREMENT,order_id INTEGER,menu_id INTEGER,menu_name TEXT,qty INTEGER,price INTEGER,note TEXT DEFAULT '')")
  db.execSQL("CREATE TABLE IF NOT EXISTS expenses(id INTEGER PRIMARY KEY AUTOINCREMENT,title TEXT,amount INTEGER,category TEXT DEFAULT 'Operasional',created_at INTEGER)")
  db.execSQL("CREATE TABLE IF NOT EXISTS stock(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT,qty REAL,unit TEXT,min_qty REAL DEFAULT 0)")
  db.execSQL("CREATE TABLE IF NOT EXISTS stock_movements(id INTEGER PRIMARY KEY AUTOINCREMENT,stock_id INTEGER,type TEXT,qty REAL,reference TEXT,created_at INTEGER)")
  db.execSQL("CREATE TABLE IF NOT EXISTS recipes(id INTEGER PRIMARY KEY AUTOINCREMENT,menu_id INTEGER,stock_id INTEGER,qty REAL)")
  db.execSQL("CREATE TABLE IF NOT EXISTS suppliers(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT,phone TEXT,address TEXT)")
  db.execSQL("CREATE TABLE IF NOT EXISTS customers(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT,phone TEXT,points INTEGER DEFAULT 0)")
  db.execSQL("CREATE TABLE IF NOT EXISTS employees(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT,phone TEXT,role TEXT,active INTEGER DEFAULT 1)")
  db.execSQL("CREATE TABLE IF NOT EXISTS shifts(id INTEGER PRIMARY KEY AUTOINCREMENT,employee_id INTEGER,opening_cash INTEGER,closing_cash INTEGER DEFAULT 0,status TEXT,opened_at INTEGER,closed_at INTEGER)")
  db.execSQL("CREATE TABLE IF NOT EXISTS app_settings(key TEXT PRIMARY KEY,value TEXT)")
  db.execSQL("CREATE TABLE IF NOT EXISTS sync_queue(id INTEGER PRIMARY KEY AUTOINCREMENT,entity TEXT,entity_id INTEGER,action TEXT,payload TEXT,created_at INTEGER,synced INTEGER DEFAULT 0)")
  seed(db)
 }
 override fun onUpgrade(db: SQLiteDatabase,oldVersion:Int,newVersion:Int) { onCreate(db) }
 private fun seed(db:SQLiteDatabase) {
  listOf("Minuman","Makanan","Snack").forEach { db.execSQL("INSERT OR IGNORE INTO categories(name) VALUES(?)",arrayOf(it)) }
  db.execSQL("INSERT INTO menu(name,price,category) VALUES('Es Kopi Susu',18000,'Minuman')")
  db.execSQL("INSERT INTO menu(name,price,category) VALUES('Americano',15000,'Minuman')")
  db.execSQL("INSERT INTO menu(name,price,category) VALUES('Nasi Goreng',25000,'Makanan')")
  db.execSQL("INSERT INTO menu(name,price,category) VALUES('Mie Goreng',22000,'Makanan')")
  db.execSQL("INSERT INTO menu(name,price,category) VALUES('Kentang Goreng',18000,'Snack')")
  db.execSQL("INSERT INTO stock(name,qty,unit,min_qty) VALUES('Kopi',1000,'gram',100)")
  db.execSQL("INSERT INTO stock(name,qty,unit,min_qty) VALUES('Susu',10000,'ml',1000)")
  db.execSQL("INSERT INTO stock(name,qty,unit,min_qty) VALUES('Gula',5000,'gram',500)")
  db.execSQL("INSERT INTO stock(name,qty,unit,min_qty) VALUES('Beras',10000,'gram',1000)")
  db.execSQL("INSERT OR IGNORE INTO app_settings(key,value) VALUES('tax_percent','0')")
  db.execSQL("INSERT OR IGNORE INTO app_settings(key,value) VALUES('service_percent','0')")
 }
}