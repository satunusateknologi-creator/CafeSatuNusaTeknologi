<?php
declare(strict_types=1);
require __DIR__.'/config.php';

$action=$_GET['action']??'health';
try{
 $pdo=db();

 if($action==='health')json_response(['ok'=>true,'app'=>'Cafe Satu Nusa','version'=>'2.1']);

 if($action==='setup' && $_SERVER['REQUEST_METHOD']==='POST'){
  $in=input_json();
  if(!hash_equals(SETUP_KEY,(string)($in['setup_key']??'')))json_response(['ok'=>false,'message'=>'Setup key salah'],403);
  if((int)$pdo->query('SELECT COUNT(*) FROM users')->fetchColumn()>0)json_response(['ok'=>false,'message'=>'Setup sudah pernah dilakukan'],409);
  $username=trim((string)($in['username']??'owner'));$password=(string)($in['password']??'');
  if($username===''||strlen($password)<8)json_response(['ok'=>false,'message'=>'Username wajib dan password minimal 8 karakter'],422);
  $st=$pdo->prepare('INSERT INTO users(name,username,password_hash,role) VALUES(?,?,?,"OWNER")');
  $st->execute([trim((string)($in['name']??'Owner')),$username,password_hash($password,PASSWORD_DEFAULT)]);
  json_response(['ok'=>true,'message'=>'Owner berhasil dibuat']);
 }

 if($action==='login' && $_SERVER['REQUEST_METHOD']==='POST'){
  $in=input_json();
  $st=$pdo->prepare('SELECT id,name,username,password_hash,role FROM users WHERE username=? AND active=1 LIMIT 1');
  $st->execute([trim((string)($in['username']??''))]);$u=$st->fetch();
  if(!$u||!password_verify((string)($in['password']??''),$u['password_hash']))json_response(['ok'=>false,'message'=>'Username atau password salah'],401);
  $pdo->prepare('DELETE FROM sessions WHERE expires_at<=NOW() OR user_id=?')->execute([$u['id']]);
  $token=new_token();$hash=hash('sha256',$token);
  $st=$pdo->prepare('INSERT INTO sessions(user_id,token_hash,expires_at) VALUES(?,?,DATE_ADD(NOW(),INTERVAL ? SECOND))');
  $st->execute([$u['id'],$hash,SESSION_TTL]);
  unset($u['password_hash']);
  json_response(['ok'=>true,'token'=>$token,'expires_in'=>SESSION_TTL,'user'=>$u]);
 }

 $user=auth_user();

 if($action==='menus'){
  $rows=$pdo->query('SELECT id,name,category_id,price,description,active,updated_at FROM menus WHERE active=1 ORDER BY name')->fetchAll();
  json_response(['ok'=>true,'data'=>$rows]);
 }
 if($action==='categories'){
  $rows=$pdo->query('SELECT id,name,active FROM categories WHERE active=1 ORDER BY name')->fetchAll();
  json_response(['ok'=>true,'data'=>$rows]);
 }
 if($action==='stock'){
  require_role($user,['OWNER','ADMIN','KITCHEN']);
  $rows=$pdo->query('SELECT id,name,qty,unit,min_qty,updated_at FROM stock ORDER BY name')->fetchAll();
  json_response(['ok'=>true,'data'=>$rows]);
 }
 if($action==='customers'){
  $rows=$pdo->query('SELECT id,name,phone,points,updated_at FROM customers ORDER BY name')->fetchAll();
  json_response(['ok'=>true,'data'=>$rows]);
 }
 if($action==='orders' && $_SERVER['REQUEST_METHOD']==='POST'){
  require_role($user,['OWNER','ADMIN','KASIR']);
  $in=input_json();$local=$in['local_id']??null;
  $pdo->beginTransaction();
  try{
   if($local!==null){
    $check=$pdo->prepare('SELECT id FROM orders WHERE local_id=? LIMIT 1');$check->execute([$local]);$existing=$check->fetchColumn();
    if($existing){$pdo->commit();json_response(['ok'=>true,'server_id'=>(int)$existing,'duplicate'=>true]);}
   }
   $st=$pdo->prepare('INSERT INTO orders(local_id,table_no,subtotal,total,payment,status,discount,tax,service,customer_id,notes,created_at,sync_status) VALUES(?,?,?,?,?,?,?,?,?,?,?,NOW(),"SYNCED")');
   $st->execute([$local,$in['table_no']??'Takeaway',$in['subtotal']??0,$in['total']??0,$in['payment']??'Tunai',$in['status']??'BARU',$in['discount']??0,$in['tax']??0,$in['service']??0,$in['customer_id']??null,$in['notes']??'']);
   $orderId=(int)$pdo->lastInsertId();
   $it=$pdo->prepare('INSERT INTO order_items(order_id,menu_id,menu_name,qty,price,note) VALUES(?,?,?,?,?,?)');
   foreach(($in['items']??[]) as $item)$it->execute([$orderId,$item['menu_id']??null,$item['menu_name']??'',$item['qty']??1,$item['price']??0,$item['note']??'']);
   $pdo->prepare('INSERT INTO audit_logs(user_id,action,entity_name,entity_id,detail) VALUES(?,?,?,?,?)')->execute([$user['id'],'SALE','ORDER',$orderId,'payment='.($in['payment']??'Tunai')]);
   $pdo->commit();json_response(['ok'=>true,'server_id'=>$orderId]);
  }catch(Throwable $e){if($pdo->inTransaction())$pdo->rollBack();throw $e;}
 }
 if($action==='dashboard'){
  require_role($user,['OWNER','ADMIN']);
  $sales=$pdo->query("SELECT COUNT(*) transactions,COALESCE(SUM(total),0) omzet FROM orders WHERE DATE(created_at)=CURDATE() AND status NOT IN('BATAL','REFUND')")->fetch();
  $expense=$pdo->query("SELECT COALESCE(SUM(amount),0) total FROM expenses WHERE DATE(created_at)=CURDATE()")->fetch();
  $low=$pdo->query("SELECT COUNT(*) total FROM stock WHERE qty<=min_qty")->fetch();
  json_response(['ok'=>true,'sales'=>$sales,'expense'=>$expense,'low_stock'=>(int)$low['total']]);
 }
 if($action==='me')json_response(['ok'=>true,'user'=>$user]);

 json_response(['ok'=>false,'message'=>'Action tidak dikenal'],404);
}catch(Throwable $e){
 error_log((string)$e);
 json_response(['ok'=>false,'message'=>'Server error'],500);
}
