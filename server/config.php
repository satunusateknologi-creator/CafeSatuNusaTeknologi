<?php
declare(strict_types=1);

const DB_HOST='localhost';
const DB_NAME='cafe_satu_nusa';
const DB_USER='root';
const DB_PASS='';
const SESSION_TTL=604800;
const SETUP_KEY='CHANGE_THIS_SETUP_KEY';

function db():PDO{
 static $pdo=null;
 if($pdo instanceof PDO)return $pdo;
 $pdo=new PDO('mysql:host='.DB_HOST.';dbname='.DB_NAME.';charset=utf8mb4',DB_USER,DB_PASS,[
  PDO::ATTR_ERRMODE=>PDO::ERRMODE_EXCEPTION,
  PDO::ATTR_DEFAULT_FETCH_MODE=>PDO::FETCH_ASSOC,
  PDO::ATTR_EMULATE_PREPARES=>false
 ]);
 return $pdo;
}
function json_response(array $data,int $status=200):never{
 http_response_code($status);
 header('Content-Type: application/json; charset=utf-8');
 echo json_encode($data,JSON_UNESCAPED_UNICODE|JSON_UNESCAPED_SLASHES);
 exit;
}
function input_json():array{
 $data=json_decode(file_get_contents('php://input')?:'{}',true);
 return is_array($data)?$data:[];
}
function bearer():string{
 $h=$_SERVER['HTTP_AUTHORIZATION']??'';
 if(preg_match('/Bearer\\s+(.+)/i',$h,$m))return trim($m[1]);
 return '';
}
function auth_user():array{
 $token=bearer();
 if($token==='')json_response(['ok'=>false,'message'=>'Unauthorized'],401);
 $hash=hash('sha256',$token);
 $st=db()->prepare("SELECT u.id,u.name,u.username,u.role FROM sessions s JOIN users u ON u.id=s.user_id WHERE s.token_hash=? AND s.expires_at>NOW() AND u.active=1 LIMIT 1");
 $st->execute([$hash]);$u=$st->fetch();
 if(!$u)json_response(['ok'=>false,'message'=>'Session expired'],401);
 return $u;
}
function require_role(array $user,array $roles):void{
 if(!in_array($user['role'],$roles,true))json_response(['ok'=>false,'message'=>'Forbidden'],403);
}
function new_token():string{
 return bin2hex(random_bytes(32));
}
