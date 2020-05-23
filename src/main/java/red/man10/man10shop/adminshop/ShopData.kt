package red.man10.man10shop.adminshop

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Server
import org.bukkit.entity.Player
import org.bukkit.inventory.*
import red.man10.man10shop.Man10Shop
import red.man10.man10shop.Man10Shop.Companion.database
import red.man10.man10shop.Man10Shop.Companion.mysqlQueue
import red.man10.man10shop.Man10Shop.Companion.pl
import red.man10.man10shop.MySQLManager

class ShopData {

    //新規ショップを作成
    fun registerShop(inv:Inventory,p: Player){

        val shopList = inventoryToItemStackList(inv)

        val data = AdminShopData()

        data.shop = itemToMerchant(shopList)

        p.sendMessage("§e§l作成中....")

        Thread(Runnable {

            val id = registerShopData(p,shopList)

            if (id == -1){
                p.sendMessage("§3§l作成失敗")
                return@Runnable
            }

            p.sendMessage("§a§l作成成功！看板にAdminShop:$id と入力してください！")

            Man10Shop.shopMap[id] = data

        }).start()

        Man10Shop.sendOP("§e§l${p.name}が新規ショップを登録しました！")

    }

    //サバ起動時にショップデータを読み込む
    fun loadShopData(){
        val mysql = MySQLManager(pl,"Man10ShopLoadShop")

        val rs = mysql.query("SELECT * FROM `merchant_shop_list`;")?:return

        while (rs.next()){

            val id = rs.getInt("id")

            val data = AdminShopData()

            data.server = rs.getString("server")
            data.world = rs.getString("world")

            data.x = rs.getInt("locX")
            data.y = rs.getInt("locY")
            data.z = rs.getInt("locZ")

            data.shop = Man10Shop.adminShopData.itemToMerchant(database.itemStackArrayFromBase64(rs.getString("shop_item")))

            Man10Shop.shopMap[id] = data
        }

        rs.close()
        mysql.close()

        Bukkit.getLogger().info("[Man10Shop]Loaded Al Shop")
    }

    //////////////////////////////////
    //新規ショップデータの作成とログの保存
    //////////////////////////////////
    fun registerShopData(creator:Player, items:MutableList<ItemStack>):Int{

        val mysql = MySQLManager(pl,"Man10ShopCreated")

        mysql.execute("INSERT INTO merchant_shop_list " +
                "(create_player, uuid, shop_item)" +
                " VALUES (" +
                "'${creator.name}', " +
                "'${creator.uniqueId}', " +
                "'${database.itemStackArrayToBase64(items.toTypedArray())}');")

        var id = -1

        val rs = mysql.query("SELECT t.*\n" +
                "           FROM man10shop.op_log t\n" +
                "           ORDER BY id DESC\n" +
                "           LIMIT 501;")?:return id

        rs.next()

        id = rs.getInt("id")

        rs.close()
        mysql.close()

        val loc = creator.location

        mysql.execute("INSERT INTO op_log " +
                "( player, uuid, server, world, locX, locY, locZ, shop_id, note)" +
                " VALUES (" +
                "'${creator.name}', " +
                "'${creator.uniqueId}', " +
                "'${creator.server.name}', " +
                "'${creator.world.name}', " +
                "${loc.x}, " +
                "${loc.y}, " +
                "${loc.z}, " +
                "${id}, " +
                "'register');")

        return id
    }

    /////////////////////////////
    //ショップデータをアップデート
    /////////////////////////////
    fun updateShopData(data:AdminShopData,id:Int,p:Player){

        mysqlQueue.add("UPDATE merchant_shop_list t SET " +
                "t.server = '${data.server}', " +
                "t.world = '${data.world}', " +
                "t.locX = ${data.x}, " +
                "t.locY = ${data.y}, " +
                "t.locZ = ${data.z} " +
                "WHERE t.id = $id")

        val loc = p.location

        mysqlQueue.add("INSERT INTO op_log " +
                "( player, uuid, server, world, locX, locY, locZ, shop_id, note)" +
                " VALUES (" +
                "'${p.name}', " +
                "'${p.uniqueId}', " +
                "'${p.server.name}', " +
                "'${p.world.name}', " +
                "${loc.x}, " +
                "${loc.y}, " +
                "${loc.z}, " +
                "${id}, " +
                "'update');")

    }

    //////////////////////////
    //ショップの削除
    //////////////////////////
    fun deleteShop(id:Int,p:Player){

        mysqlQueue.add("DELETE FROM log WHERE id = 1;")

        val loc = p.location

        mysqlQueue.add("INSERT INTO op_log " +
                "( player, uuid, server, world, locX, locY, locZ, shop_id, note)" +
                " VALUES (" +
                "'${p.name}', " +
                "'${p.uniqueId}', " +
                "'${p.server.name}', " +
                "'${p.world.name}', " +
                "${loc.x}, " +
                "${loc.y}, " +
                "${loc.z}, " +
                "${id}, " +
                "'delete');")

    }

    ///////////////////////////////////////
    //指定したロケーションにショップがあるかどうか
    ///////////////////////////////////////
    fun getShop(loc:Location,server:Server):Int{

        for (shop in Man10Shop.shopMap){

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

    //インベントリからアイテムのリストを作る
    //result , ing1 , ing2
    fun inventoryToItemStackList(inv:Inventory):MutableList<ItemStack>{

        val list = inv.toList()

        val itemStackList = mutableListOf<ItemStack>()

        for (i in 0..8){

            if (list.size < i+18)break

            itemStackList.add(list[i])
            itemStackList.add(list[i+9])
            itemStackList.add(list[i+18])

        }

        return itemStackList
    }

    /////////////////////////////////////////
    //ItemStackのリストからショップのレシピを作る
    /////////////////////////////////////////
    fun itemToMerchant(list: MutableList<ItemStack>): Merchant {

        val merchant = Bukkit.createMerchant("AdminShop")
        val merchantList = mutableListOf<MerchantRecipe>()

        for (i in 0 until list.size){

            val recipe = MerchantRecipe(list[i],1000000)

            recipe.addIngredient(list[i+1])
            recipe.addIngredient(list[i+2])

            recipe.setExperienceReward(false)

            merchantList.add(recipe)

            i+2

        }

        merchant.recipes = merchantList

        return merchant
    }


    class AdminShopData{

        var server = "server"
        var world = "world"

        var x = 0
        var y = 0
        var z = 0

        var shop = Bukkit.createMerchant("AdminShop")
    }

}