package id.satunusa.cafe

import android.content.Context

class SessionManager(context:Context){
 private val prefs=context.getSharedPreferences("cafe_session",Context.MODE_PRIVATE)
 val token:String get()=prefs.getString("token","")?:""
 val role:String get()=prefs.getString("role","OWNER")?:"OWNER"
 val name:String get()=prefs.getString("name","Offline User")?:"Offline User"
 val loggedIn:Boolean get()=token.isNotBlank()
 fun save(token:String,name:String,role:String){prefs.edit().putString("token",token).putString("name",name).putString("role",role).apply()}
 fun clear(){prefs.edit().clear().apply()}
}
