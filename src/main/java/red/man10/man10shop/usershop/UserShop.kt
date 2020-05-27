package red.man10.man10shop.usershop

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.Chest
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import red.man10.man10shop.Man10Shop
import red.man10.man10shop.Man10Shop.Companion.mysqlQueue
import red.man10.man10shop.MySQLManager
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class UserShop {

    val shopData = ConcurrentHashMap<Int,UserShopData>()


    fun loadShops(){

        val mysql = MySQLManager(Man10Shop.pl,"Man10ShopLoadShop")

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

            data.item = Man10Shop.database.itemFromBase64(rs.getString("shop_item"))

            shopData[id] = data
        }

        rs.close()
        mysql.close()
    }


    /**
     * 新規ショップを作成
     * @param p 作者
     * @param price ショップの値段
     * @param isSell trueなら買取、falseなら販売
     * @param loc ショップのロケーション
     */
    fun createShop(p: Player, price:Double, isSell:Boolean, loc: Location){

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

            val mysql = MySQLManager(Man10Shop.pl,"Man10ShopCreated")

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


            shopData[id] = data

            addLog(p,"CreateUserShop","ID:$id price:$price")

            p.sendMessage("§a§l作成完了！売買したいアイテムを持って看板を右クリック！")

        }).start()

    }

    /**
     * ショップデータを保存する
     *
     */
    fun set(p:Player,id:Int,data:UserShopData){

        shopData[id] = data

        mysqlQueue.add("UPDATE money_shop_list t SET t.shop_item = '${Man10Shop.database.itemToBase64(data.item!!)}' WHERE t.id = $id")

        addLog(p,"UpdateUserShop","ID:$id")

    }

    fun deleteShop(p:Player,id:Int){

        shopData.remove(id)

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

