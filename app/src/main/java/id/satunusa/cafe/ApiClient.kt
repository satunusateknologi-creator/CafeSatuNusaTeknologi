package id.satunusa.cafe

import java.net.HttpURLConnection
import java.net.URL

class ApiClient(private val baseUrl:String){
 fun get(action:String):Result<String>{ return request("GET",action,null) }
 fun post(action:String,json:String):Result<String>{ return request("POST",action,json) }
 private fun request(method:String,action:String,body:String?):Result<String>{
  return try{
   val url=URL(baseUrl.trimEnd('/')+"/api.php?action="+action)
   val c=url.openConnection() as HttpURLConnection
   c.requestMethod=method
   c.connectTimeout=8000
   c.readTimeout=10000
   c.setRequestProperty("Accept","application/json")
   if(body!=null){
    c.doOutput=true
    c.setRequestProperty("Content-Type","application/json; charset=utf-8")
    c.outputStream.use{it.write(body.toByteArray(Charsets.UTF_8))}
   }
   val code=c.responseCode
   val stream=if(code in 200..299)c.inputStream else c.errorStream
   val text=stream?.bufferedReader()?.use{it.readText()} ?: ""
   c.disconnect()
   if(code in 200..299) Result.success(text) else Result.failure(Exception("HTTP "+code+": "+text))
  }catch(e:Exception){Result.failure(e)}
 }
}