package red.man10.man10shop.usershop

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import red.man10.man10shop.Man10Shop
import red.man10.man10shop.Man10Shop.Companion.database
import red.man10.man10shop.Man10Shop.Companion.mysqlQueue
import red.man10.man10shop.Man10Shop.Companion.pl
import red.man10.man10shop.Man10Shop.Companion.sendMsg
import red.man10.man10shop.Man10Shop.Companion.vault
import red.man10.man10shop.MySQLManager
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class UserShop {

    private val userShop = ConcurrentHashMap<Int,UserShopData>()
    private var mysql : MySQLManager = MySQLManager(pl,"man10Shop")

    val CONTAINER_NAME = "§6§lショップコンテナ"

    /**
     * ショップデータの読み込み
     */
    fun loadShopData(){

        userShop.clear()

        val mysql = MySQLManager(pl,"Man10ShopLoad")

        val rs = mysql.query("SELECT * FROM user_shop_list;")?:return

        while (rs.next()){

            val id = rs.getInt("id")

            val data = UserShopData()

            data.ownerUUId = UUID.fromString(rs.getString("uuid"))

            data.server = rs.getString("server")
            data.world = rs.getString("world")

            data.loc = Triple(rs.getInt("locX"),rs.getInt("locY"),rs.getInt("locZ"))

            data.isBuy = rs.getInt("buy")==1

            data.container = database.itemStackArrayFromBase64(rs.getString("shop_container"))

            data.price = rs.getDouble("price")

            userShop[id] = data

            Bukkit.getLogger().info("Loaded user shop ID:$id")

        }

        rs.close()
        mysql.close()

    }

    /**
     * 新規ショップを作成する
     *
     * @param p crate user
     * @param location sign location
     * @param price shop price
     * @param isBuy sell shop is false
     *
     */
    @Synchronized
    fun create(p: Player,location: Location,price:Double,isBuy:Boolean){

        sendMsg(p,"§e§l新規ショップを作成中")

        val data = UserShopData()

        data.ownerUUId = p.uniqueId

        data.server = p.server.name
        data.world = location.world.name

        data.loc = Triple(location.blockX,location.blockY,location.blockZ)

        data.isBuy = isBuy

        data.price = price

        Thread(Runnable {

            mysql.execute("INSERT INTO user_shop_list " +
                    "(player, uuid, server, world, " +
                    "locX, locY, locZ, buy, price,shop_container) " +
                    "VALUES (" +
                    "'${p.name}', " +
                    "'${p.uniqueId}', " +
                    "'${p.server.name}', " +
                    "'${location.world.name}', " +
                    "${data.loc.first}, " +
                    "${data.loc.second}, " +
                    "${data.loc.third}, " +
                    "${if (isBuy) 1 else 0}, $price," +
                    "'${database.itemStackArrayToBase64(data.container.toTypedArray())}');")

            val rs = mysql.query("SELECT t.*" +
                    "FROM user_shop_list t " +
                    "ORDER BY id DESC " +
                    "LIMIT 501; ")?:return@Runnable

            rs.next()

            val id = rs.getInt("id")

            userShop[id] = data

            database.logNormal(p,"CreateNewShop (${if (isBuy) "buy" else "sell"})",price)

            sendMsg(p,"§a§l作成完了")

        }).start()

    }

    /**
     * ショップのロケーションとコンテナの中身をアップデートする
     *
     * @param id shop id
     * @param container shop container
     */
    fun updateShop(id:Int,p:Player, container:Inventory){

        val data = userShop[id]?:return

        val list = mutableListOf<ItemStack>()

        for (i in 0..53){
            val item = container.getItem(i)
            if (item == null || item.type == Material.AIR)continue
            list.add(item)
        }

        data.container = list

        set(id,data)

        mysqlQueue.add("UPDATE user_shop_list t SET " +
                "t.shop_container = '${database.itemStackArrayToBase64(data.container.toTypedArray())}' " +
                "WHERE t.id = $id;")

        database.logNormal(p,"UpdateShop ID:$id",0.0)

    }

    /**
     * ショップの削除
     */
    fun deleteShop(id:Int,p:Player){

        userShop.remove(id)

        mysqlQueue.add("DELETE FROM user_shop_list WHERE id = $id;")

        database.logNormal(p,"DeleteShop ID:$id",0.0)
    }

    /**
     * ロケーションからショップを特定
     *
     * @return id ショップのデータ
     */
    fun getShop(location: Location,server:String): Pair<Int, UserShopData>? {

        for (data in userShop){

            val value = data.value

            if (value.server != server)continue
            if (value.world != location.world.name)continue

            if (value.loc.first !=location.blockX)continue
            if (value.loc.second !=location.blockY)continue
            if (value.loc.third !=location.blockZ)continue

            return Pair(data.key,data.value)
        }

        return null

    }

    /**
     * アイテムを購入、買取する
     *
     * @return 取引成功したら true 失敗したら false
     */
    fun tradeItem(id:Int,p:Player,stack:Boolean):Boolean{

        val data = get(id)

        if(data.container.isEmpty())return false

        ///////////////購入//////////////////
        if (data.isBuy) {

            val container = data.container
            val item = container[container.size-1]

            if (container.isEmpty()) return false
            if (container[0].type == Material.AIR)return false

            //まとめて取引する場合
            if (stack) {

                val price = data.price * item.amount

                if (vault.getBalance(p.uniqueId) < price) return false

                vault.withdraw(p.uniqueId, price)

                //オーナーにお金を送金
                addProfit(data.ownerUUId, price)

                p.inventory.addItem(item.clone())

                container.remove(item)

                data.container = container

                set(id, data)

                database.logNormal(p, "BuyItem x ${item.amount} ID:$id", price)
                return true
            }

            //一つだけの場合
            if (vault.getBalance(p.uniqueId) < data.price) return false

            vault.withdraw(p.uniqueId, data.price)

            addProfit(data.ownerUUId, data.price)

            val pItem = item.clone()

            pItem.amount = 1

            item.amount--

            if (item.amount == 0){
                container.removeAt(container.size-1)
            }else{
                container[container.size-1] = item
            }

            data.container = container

            set(id, data)

            p.inventory.addItem(pItem)

            database.logNormal(p, "BuyItem x 1 ID:$id", data.price)

            return true

        }

        ///////////買取/////////////////

        val inv = p.inventory

        val sellItem = data.container[0]

        if (sellItem.type == Material.AIR)return false

        //スタックで買い取ってもらう
        if (stack){

            for (item in inv){

                if (item == null)continue

                if (!equeal(item,sellItem))continue

                val price = item.amount * data.price

                if (vault.getBalance(data.ownerUUId) < price)return false

                //ショップオーナーからお金を引き出す
                vault.withdraw(data.ownerUUId,price)

                p.inventory.removeItem(item)

                data.container.add(item)

                addProfit(p.uniqueId,price)

                database.logNormal(p, "SellItem x ${item.amount} ID:$id", price)

                return true
            }

            return false

        }

        //一つのアイテム
        for (item in inv){

            if (item == null)continue

            if (!equeal(item,sellItem))continue

            val pItem = item.clone()
            pItem.amount = 1

            val price = data.price

            if (vault.getBalance(data.ownerUUId) < price)return false

            //ショップオーナーからお金を引き出す
            vault.withdraw(data.ownerUUId,price)

            p.inventory.removeItem(pItem)

            data.container.add(pItem)

            addProfit(p.uniqueId,price)

            database.logNormal(p, "SellItem x ${item.amount} ID:$id", price)

            return true
        }

        return false
    }

    /**
     * ２つのItemStackが同じものかを識別する
     */
    fun equeal(itemA:ItemStack,itemB:ItemStack):Boolean{

        val anItemA = itemA.clone()
        anItemA.amount = 1

        val anItemB = itemB.clone()
        anItemB.amount = 1

        if (anItemA.toString() == anItemB.toString())return true

        return false

    }

    /**
     * コンテナを開く
     */
    fun openContainer(p:Player,id: Int){

        val data = get(id)

        val inv = Bukkit.createInventory(null,54,CONTAINER_NAME+id)

        for (item in data.container){
            if (item.type == Material.AIR)continue
            inv.addItem(item.clone())
        }

        p.openInventory(inv)

        database.logNormal(p,"OpenShopContainer ID:$id",0.0)
    }

    fun get(id:Int):UserShopData{
        return userShop[id]?:UserShopData()
    }

    fun set(id:Int,data:UserShopData){
        userShop[id] = data
    }

    fun addProfit(uuid:UUID,amount: Double){

        val p = Bukkit.getOfflinePlayer(uuid)

        if (p.isOnline){
            sendMsg(p.player!!,"§e§l入金情報 : $$amount")
        }

        mysqlQueue.add("INSERT INTO user_index (uuid, player, profit) " +
                "VALUES ('$uuid', '${p.name}', '$amount');")

    }

    /**
     * 利益の確認
     */
    fun getProfit(p:Player):Double{
        val mysql = MySQLManager(pl,"mreGetProfit")
        var profit = 0.0
        val rs = mysql.query("SELECT `profit` FROM `user_index` " +
                "WHERE `uuid`='${p.uniqueId}' AND `received`='0';")?:return 0.0

        while (rs.next()){
            profit += rs.getDouble("profit")
        }
        return profit
    }

    /**
     * 利益の取り出し
     */
    fun takeProfit(p:Player){
        vault.deposit(p.uniqueId,getProfit(p))

        mysqlQueue.add("UPDATE `user_index` SET `received`='1' WHERE `uuid`='${p.uniqueId}';")
    }

    class UserShopData{

        var ownerUUId = UUID.randomUUID()

        var server = "server"
        var world = "world"

        var loc = Triple(0,0,0)//X Y Z

        var container = mutableListOf<ItemStack>()

        var isBuy = false

        var price = 0.0

    }
}