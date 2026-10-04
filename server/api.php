<?php
declare(strict_types=1);
require __DIR__.'/config.php';

$action=$_GET['action'] ?? 'health';
try {
    $pdo=db();

    if($action==='health'){
        json_response(['ok'=>true,'app'=>'Cafe Satu Nusa','version'=>'2.0']);
    }

    if($action==='login'){
        $in=input_json();
        $st=$pdo->prepare('SELECT id,name,username,password_hash,role FROM users WHERE username=? AND active=1 LIMIT 1');
        $st->execute([$in['username'] ?? '']);
        $u=$st->fetch();
        if(!$u || !password_verify((string)($in['password'] ?? ''),$u['password_hash'])){
            json_response(['ok'=>false,'message'=>'Username atau password salah'],401);
        }
        unset($u['password_hash']);
        json_response(['ok'=>true,'user'=>$u]);
    }

    if($action==='menus'){
        $rows=$pdo->query('SELECT id,name,category_id,price,description,active,updated_at FROM menus WHERE active=1 ORDER BY name')->fetchAll();
        json_response(['ok'=>true,'data'=>$rows]);
    }

    if($action==='categories'){
        $rows=$pdo->query('SELECT id,name,active FROM categories WHERE active=1 ORDER BY name')->fetchAll();
        json_response(['ok'=>true,'data'=>$rows]);
    }

    if($action==='stock'){
        $rows=$pdo->query('SELECT id,name,qty,unit,min_qty,updated_at FROM stock ORDER BY name')->fetchAll();
        json_response(['ok'=>true,'data'=>$rows]);
    }

    if($action==='customers'){
        $rows=$pdo->query('SELECT id,name,phone,points,updated_at FROM customers ORDER BY name')->fetchAll();
        json_response(['ok'=>true,'data'=>$rows]);
    }

    if($action==='orders' && $_SERVER['REQUEST_METHOD']==='POST'){
        $in=input_json();
        $pdo->beginTransaction();
        $st=$pdo->prepare('INSERT INTO orders(local_id,table_no,subtotal,total,payment,status,discount,tax,service,customer_id,notes,created_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)');
        $st->execute([
            $in['local_id'] ?? null,$in['table_no'] ?? 'Takeaway',$in['subtotal'] ?? 0,$in['total'] ?? 0,
            $in['payment'] ?? 'Tunai',$in['status'] ?? 'BARU',$in['discount'] ?? 0,$in['tax'] ?? 0,
            $in['service'] ?? 0,$in['customer_id'] ?? null,$in['notes'] ?? '',date('Y-m-d H:i:s')
        ]);
        $orderId=(int)$pdo->lastInsertId();
        foreach(($in['items'] ?? []) as $item){
            $it=$pdo->prepare('INSERT INTO order_items(order_id,menu_id,menu_name,qty,price,note) VALUES(?,?,?,?,?,?)');
            $it->execute([$orderId,$item['menu_id'] ?? null,$item['menu_name'] ?? '',$item['qty'] ?? 1,$item['price'] ?? 0,$item['note'] ?? '']);
        }
        $pdo->commit();
        json_response(['ok'=>true,'server_id'=>$orderId]);
    }

    if($action==='dashboard'){
        $sales=$pdo->query("SELECT COUNT(*) transactions,COALESCE(SUM(total),0) omzet FROM orders WHERE DATE(created_at)=CURDATE()")->fetch();
        $expense=$pdo->query("SELECT COALESCE(SUM(amount),0) total FROM expenses WHERE DATE(created_at)=CURDATE()")->fetch();
        $low=$pdo->query("SELECT COUNT(*) total FROM stock WHERE qty<=min_qty")->fetch();
        json_response(['ok'=>true,'sales'=>$sales,'expense'=>$expense,'low_stock'=>$low['total']]);
    }

    json_response(['ok'=>false,'message'=>'Action tidak dikenal'],404);
} catch(Throwable $e) {
    json_response(['ok'=>false,'message'=>'Server error','detail'=>$e->getMessage()],500);
}
