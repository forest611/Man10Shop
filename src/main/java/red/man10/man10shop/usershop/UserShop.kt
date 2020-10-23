package red.man10.man10shop.usershop

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import red.man10.man10shop.Database
import red.man10.man10shop.Man10Shop.Companion.bank
import red.man10.man10shop.Man10Shop.Companion.mysqlQueue
import red.man10.man10shop.Man10Shop.Companion.pl
import red.man10.man10shop.Man10Shop.Companion.sendMsg
import red.man10.man10shop.Man10Shop.Companion.vault
import red.man10.man10shop.MySQLManager
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object UserShop {

    private val userShop = ConcurrentHashMap<Int,UserShopData>()
    private var mysql : MySQLManager = MySQLManager(pl,"man10Shop")

    const val CONTAINER_NAME = "§6§lショップコンテナ"

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

            data.price = rs.getDouble("price")

            userShop[id] = data

            containerInventory(id,Database.itemStackArrayFromBase64(rs.getString("shop_container")))

//            Bukkit.getLogger().info("Loaded user shop ID:$id")

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


        val data = UserShopData()

        data.ownerUUId = p.uniqueId

        data.server = p.server.name
        data.world = location.world.name

        data.loc = Triple(location.blockX,location.blockY,location.blockZ)

        data.isBuy = isBuy

        data.price = price
//
//        data.container = Bukkit.createInventory(null,54,CONTAINER_NAME+id)

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
                "null);")
//                "'${Database.itemStackArrayToBase64(data.container.storageContents)}');")

        val rs = mysql.query("SELECT t.*" +
                "FROM user_shop_list t " +
                "ORDER BY id DESC " +
                "LIMIT 1; ")?:return

        rs.next()

        val id = rs.getInt("id")

        userShop[id] = data

        containerInventory(id,null)

        Database.logNormal(p,"CreateNewShop (${if (isBuy) "buy" else "sell"})",price)


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

//        data.container.storageContents= list.toTypedArray()

        containerInventory(id,list)

//        set(id,data)

        mysqlQueue.add("UPDATE user_shop_list t SET " +
                "t.shop_container = '${Database.itemStackArrayToBase64(list.toTypedArray())}' " +
                "WHERE t.id = $id;")

        Database.logNormal(p,"UpdateShop ID:$id",0.0)

    }

//    /**
//     * ショップのコンテナのみをアップデート
//     *
//     * @param id shop id
//     * @param container shop container
//     */
//    fun updateShop(id:Int,p:Player, container:MutableList<ItemStack>){
//
////        val data = userShop[id]?:return
//
//        mysqlQueue.add("UPDATE user_shop_list t SET " +
//                "t.shop_container = '${Database.itemStackArrayToBase64(container.toTypedArray())}' " +
//                "WHERE t.id = $id;")
//
//        Database.logNormal(p,"UpdateShop ID:$id",0.0)
//
//    }
    fun updateShop(id:Int,p:Player,price: Double,isBuy: Boolean){

        val data = userShop[id]?:return

        data.isBuy = isBuy
        data.price = price

        set(id,data)

        mysqlQueue.add("UPDATE user_shop_list t SET t.buy = ${if (isBuy) 1 else 0}, t.price = $price WHERE t.id = $id")

        Database.logNormal(p,"UpdateShop ID:$id",price)
    }

    /**
     * ショップの削除
     */
    fun deleteShop(id:Int,p:Player){

        userShop.remove(id)

        mysqlQueue.add("DELETE FROM user_shop_list WHERE id = $id;")

        Database.logNormal(p,"DeleteShop ID:$id",0.0)
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


    fun buy(id:Int,p:Player,stack: Boolean):Boolean{

        val data = get(id)

        if (p.inventory.firstEmpty() == -1){
            sendMsg(p,"§cインベントリに空きがない可能性があります")

            return false
        }

        val container = data.container
        val item = getTradeItem(id)?:return false

        if (container.isEmpty) return false
        if (item.type == Material.AIR)return false

        //まとめて取引する場合
        if (stack) {

            val price = data.price * item.amount

            if (vault.getBalance(p.uniqueId) < price) {
                sendMsg(p,"§cお金が足りません！")
                return false
            }

            vault.withdraw(p.uniqueId, price)

            //オーナーにお金を送金
            bank.deposit(data.ownerUUId,price, "ShopProfit")

            p.inventory.addItem(item.clone())

            container.remove(item)

            data.container = container

            set(id, data)

            updateShop(id,p,container)

            Database.logNormal(p, "BuyItem x ${item.amount} ID:$id", price)
            return true
        }

        //一つだけの場合
        if (vault.getBalance(p.uniqueId) < data.price){
            sendMsg(p,"§cお金が足りません！")
            return false
        }

        vault.withdraw(p.uniqueId, data.price)

        bank.deposit(data.ownerUUId,data.price, "ShopProfit")

        val pItem = item.clone()

        pItem.amount = 1

        item.amount--

//        if (item.amount == 0){
//            container.removeItem()
//        }else{
//            container[container.size-1] = item
//        }

        data.container = container

        set(id, data)

        updateShop(id,p,container)

        p.inventory.addItem(pItem)

        Database.logNormal(p, "BuyItem x 1 ID:$id", data.price)

        return true

    }

    fun sell(id:Int,p:Player,stack: Boolean):Boolean{

        val data = get(id)

//        if (data.container.firstEmpty() == -1){
//            sendMsg(p,"§c§lショップのコンテナが満タンの可能性があります")
//            return false
//        }

        val inv = p.inventory

        val sellItem =  getTradeItem(id)?:return false

        if (sellItem.type == Material.AIR)return false

        //スタックで買い取ってもらう
        if (stack){

            if (data.container.firstEmpty() == -1){
                sendMsg(p,"§c§lショップのコンテナが満タンの可能性があります")
                return false
            }

            for (item in inv){

                if (item == null)continue

                if (!sellItem.isSimilar(item))continue

                val price = item.amount * data.price

                //ショップオーナーからお金を引き出す
                if (!bank.withdraw(data.ownerUUId,price,"ShopPurchase")){
                    sendMsg(p,"§cショップのオーナーがお金を持っていないようです！")
                    return false
                }
                Database.logNormal(p, "SellItem x ${item.amount} ID:$id", price)

//                p.inventory.removeItem(item)

                val containerItem = item.clone()
                item.amount = 0

                data.container.addItem(containerItem)

                updateShop(id,p,data.container)

                bank.deposit(p.uniqueId,price,"ShopProfit")

                return true
            }

            return false

        }

        if (data.container.last().amount>=data.container.last().maxStackSize){
            sendMsg(p,"§c§lショップのコンテナが満タンの可能性があります")
            return false
        }

        //一つのアイテム
        for (item in inv){

            if (item == null)continue

            if (!sellItem.isSimilar(item))continue

            val pItem = item.clone()
            pItem.amount = 1

            val price = data.price

            //ショップオーナーからお金を引き出す
            if (!bank.withdraw(data.ownerUUId,price,"ShopPurchase")){
                sendMsg(p,"§cショップのオーナーがお金を持っていないようです！")
                return false
            }
            Database.logNormal(p, "SellItem x ${item.amount} ID:$id", price)

            item.amount =  item.amount -1

            data.container.addItem(pItem)

            updateShop(id,p,data.container)

            bank.deposit(p.uniqueId,price,"ShopProfit")


            return true
        }

        return false

    }

    fun sellAll(id:Int,p:Player):Boolean{

        val data = get(id)

        if (data.container.firstEmpty() == -1){
            sendMsg(p,"§c§lショップのコンテナが満タンの可能性があります")
            return false
        }

        val inv = p.inventory

        val sellItem =  getTradeItem(id)?:return false

        if (sellItem.type == Material.AIR)return false

        var totalPrice = 0.0
        var totalItem = 0

        //スタックで買い取ってもらう
        for (item in inv){

            if (item == null)continue

            if (!sellItem.isSimilar(item))continue

            val price = item.amount * data.price

            //ショップオーナーからお金を引き出す
            if (!bank.withdraw(data.ownerUUId,price,"ShopPurchase")){
                sendMsg(p,"§cショップのオーナーのお金がなくなったようです！")
                break
            }

            if (data.container.firstEmpty() == -1){
                sendMsg(p,"§c§lショップのコンテナが満タンになりました！")
                break
            }

            Database.logNormal(p, "SellItem x ${item.amount} ID:$id", price)

//                p.inventory.removeItem(item)

            val containerItem = item.clone()
            item.amount = 0

            data.container.addItem(containerItem)

            updateShop(id,p,data.container)

            totalPrice += price
            totalItem += containerItem.amount
        }

        if (totalPrice == 0.0){
            return false
        }

        bank.deposit(p.uniqueId,totalPrice,"ShopProfit")
        p.sendMessage("§e売った数:$totalItem")
        p.sendMessage("§e入金額:$totalPrice")

        return true

    }

    /**
     * コンテナを開く
     */
    fun openContainer(p:Player,id: Int){

        val data = get(id)

        p.openInventory(data.container)

        Database.logNormal(p,"OpenShopContainer ID:$id",0.0)
    }

    fun containerInventory(id:Int,list:MutableList<ItemStack>?){

        val data = get(id)

        data.container = Bukkit.createInventory(null,54,CONTAINER_NAME+id)

        if (list == null){
            set(id,data)
            updateShop(id,Bukkit.getPlayer(data.ownerUUId)!!,data.container)
            return
        }

        for (item in list){
            if (item.type == Material.AIR)continue
            data.container.addItem(item)
        }

        set(id,data)

    }

    fun get(id:Int):UserShopData{
        return userShop[id]?:UserShopData()
    }

    fun set(id:Int,data:UserShopData){
        userShop[id] = data
    }

    fun getTradeItem(id:Int): ItemStack? {
        val data = get(id)

        return if (data.isBuy){
            data.container.getItem(data.container.firstEmpty()-1)
        }else{
            data.container.getItem(0)
        }
    }


    class UserShopData{

        var ownerUUId = UUID.randomUUID()

        var server = "server"
        var world = "world"

        var loc = Triple(0,0,0)//X Y Z

//        var container = mutableListOf<ItemStack>()
        lateinit var container : Inventory

        var isBuy = false

        var price = 0.0

    }
}