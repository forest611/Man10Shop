package red.man10.man10shop.usershop

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Server
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.PlayerInventory
import red.man10.man10shop.Man10Shop
import red.man10.man10shop.Man10Shop.Companion.database
import red.man10.man10shop.Man10Shop.Companion.mysqlQueue
import red.man10.man10shop.Man10Shop.Companion.pl
import red.man10.man10shop.Man10Shop.Companion.userShops
import red.man10.man10shop.MySQLManager
import java.util.*

class ShopData {

    fun loadShopData(){

        val mysql = MySQLManager(pl,"Man10ShopLoadShop")

        val rs = mysql.query("SELECT * FROM money_shop_list WHERE type='User:Sell' or type='User:Buy';")?:return

        while (rs.next()){

            val id = rs.getInt("id")

            val data = UserShopData()

            data.ownerUUID = UUID.fromString(rs.getString("owner_uuid"))

            data.price = rs.getDouble("price")

            data.server = rs.getString("server")
            data.world = rs.getString("world")
            data.x = rs.getInt("locX")
            data.y = rs.getInt("locY")
            data.z = rs.getInt("locZ")

            data.type = rs.getString("type")

            data.item = database.itemFromBase64(rs.getString("shop_item"))

            userShops[id] = data
        }

        rs.close()
        mysql.close()
    }

    ////////////////////////////////
    //新規ショップを登録
    ////////////////////////////////
    fun registerShop(p: Player,price:Double,isSell:Boolean,loc:Location){

        val data = UserShopData()

        data.ownerUUID = p.uniqueId
        data.type = if (isSell) "User:Sell" else "User:Buy"
        data.price = price

        data.server = p.server.name
        data.world = loc.world.name
        data.x = loc.blockX
        data.y = loc.blockY
        data.z = loc.blockZ

        p.sendMessage("§e§l作成中....")

        Thread(Runnable {

            //DBにデータを書き込み

            val mysql = MySQLManager(pl,"Man10ShopCreated")

            mysql.execute("INSERT money_shop_list (" +
                    "owner_player, owner_uuid, server, world, locX, locY, locZ, price, type) " +
                    "VALUES (" +
                    "'${p.name}', " +
                    "'${p.uniqueId}', " +
                    "'${data.server}', " +
                    "${data.world}, " +
                    "${data.x}, " +
                    "${data.y}, " +
                    "${data.z}, " +
                    "$price, " +
                    "'${data.type}');")

            val id:Int

            val rs = mysql.query("SELECT t.*" +
                    "FROM money_shop_list t " +
                    "ORDER BY id DESC " +
                    "LIMIT 501;")?:return@Runnable

            rs.next()

            id = rs.getInt("id")

            rs.close()
            mysql.close()

            userShops[id] = data

            addLog(p,"CreateUserShop","ID:$id price:$price")

            p.sendMessage("§a§l作成完了！売買したいアイテムを持って看板を右クリック！")

        }).start()

    }

    //ショップのデータをアップデート
    fun updateShop(p:Player,item:ItemStack,id:Int){
        val data = userShops[id]?:return

        data.item = item

        userShops[id] = data

        val loc = p.location

        //アップデート
        mysqlQueue.add("UPDATE money_shop_list t SET t.shop_item = '${database.itemToBase64(item)}' WHERE t.id = $id")

        addLog(p,"UpdateUserShop","ID:$id")
    }

    fun deleteShop(p:Player,id:Int){

        userShops.remove(id)

        mysqlQueue.add("DELETE FROM money_shop_list WHERE id = $id;")

        addLog(p,"DeleteUserShop","ID:$id")
    }

    //ログの保存
    fun addLog(p: Player,type:String,note:String){
        val loc = p.location

        mysqlQueue.add("INSERT INTO log " +
                "(player, uuid, server, world, locX, locY, locZ, logType, note)" +
                " VALUES (" +
                "'${p.name}', " +
                "'${p.uniqueId}', " +
                "'${p.server.name}', " +
                "'${loc.world.name}', " +
                "${loc.x}, " +
                "${loc.y}, " +
                "${loc.z}, " +
                "'$type', " +
                "'$note');")

    }

    ///////////////////////////////////////
    //指定したロケーションにショップがあるかどうか
    ///////////////////////////////////////
    fun getShop(loc:Location,server: Server):Int{

        for (shop in userShops){

            val data = shop.value

            if (data.server != server.name)continue
            if (data.world!= loc.world.name)continue
            if (data.x != loc.blockX)continue
            if (data.y != loc.blockY)continue
            if (data.z != loc.blockZ)continue

            return shop.key
        }

        return -1
    }


    class UserShopData{

        var server = "server"
        var world = "world"

        var x = 0
        var y = 0
        var z = 0

        var price = 0.0

        var type = "Buy"//Buy or Sell

        var item : ItemStack? = null

        var ownerUUID: UUID = UUID.randomUUID()

    }


}


