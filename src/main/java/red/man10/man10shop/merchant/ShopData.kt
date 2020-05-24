package red.man10.man10shop.merchant

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Server
import org.bukkit.entity.Player
import org.bukkit.inventory.*
import red.man10.man10shop.Man10Shop
import red.man10.man10shop.Man10Shop.Companion.database
import red.man10.man10shop.Man10Shop.Companion.mysqlQueue
import red.man10.man10shop.Man10Shop.Companion.pl
import red.man10.man10shop.MySQLManager

class ShopData {

    //サバ起動時にショップデータを読み込む
    fun loadShopData(){
        val mysql = MySQLManager(pl,"Man10ShopLoadShop")

        val rs = mysql.query("SELECT * FROM `merchant_shop_list`;")?:return

        while (rs.next()){

            val id = rs.getInt("id")

            val data = MerchantShopData()

            data.server = rs.getString("server")
            data.world = rs.getString("world")

            data.x = rs.getInt("locX")
            data.y = rs.getInt("locY")
            data.z = rs.getInt("locZ")

            data.shop = Man10Shop.merchantShopData.itemToMerchant(database.itemStackArrayFromBase64(rs.getString("shop_item")))

            Man10Shop.merchantShops[id] = data

            Bukkit.getLogger().info("Loaded merchant shop ID:$id")
        }

        rs.close()
        mysql.close()

        Bukkit.getLogger().info("[Man10Shop]Loaded Al Shop")
    }

    ////////////////////////////////////
    //新規ショップを作成
    /////////////////////////////////////
    fun registerShop(inv:Inventory,p: Player){

        val shopList = inventoryToItemStackList(inv)

        val data = MerchantShopData()

        data.shop = itemToMerchant(shopList)

        p.sendMessage("§e§l作成中....")

        Thread(Runnable {

            //DBにショップデータを書き込み

            val mysql = MySQLManager(pl,"Man10ShopCreated")

            mysql.execute("INSERT INTO merchant_shop_list " +
                    "(create_player, uuid, shop_item)" +
                    " VALUES (" +
                    "'${p.name}', " +
                    "'${p.uniqueId}', " +
                    "'${database.itemStackArrayToBase64(shopList.toTypedArray())}');")

            val id: Int

            val rs = mysql.query("SELECT t.*" +
                    "FROM merchant_shop_list t " +
                    "ORDER BY id DESC " +
                    "LIMIT 501; ")?:return@Runnable

            rs.next()

            id = rs.getInt("id")

            rs.close()
            mysql.close()

            addLog(p,id,"register")

            if (id == -1){
                p.sendMessage("§3§l作成失敗")
                return@Runnable
            }

            p.sendMessage("§a§l作成成功！看板にAdminShop:$id と入力してください！")

            Man10Shop.merchantShops[id] = data

        }).start()

        Man10Shop.sendOP("§e§l${p.name}が新規ショップを登録しました！")

    }


    /////////////////////////////
    //ショップデータをアップデート
    /////////////////////////////
    fun updateShopData(data:MerchantShopData, id:Int, p:Player){

        mysqlQueue.add("UPDATE merchant_shop_list t SET " +
                "t.server = '${data.server}', " +
                "t.world = '${data.world}', " +
                "t.locX = ${data.x}, " +
                "t.locY = ${data.y}, " +
                "t.locZ = ${data.z} " +
                "WHERE t.id = $id")

        addLog(p,id,"update")

    }

    //////////////////////////
    //ショップの削除
    //////////////////////////
    fun deleteShop(id:Int,p:Player){

        Man10Shop.merchantShops.remove(id)

        mysqlQueue.add("DELETE FROM merchant_shop_list WHERE id = $id;")

        addLog(p,id,"delete")
    }

    //ログの保存
    fun addLog(p:Player,id:Int,note:String){

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
                "'$note');")

    }


    ///////////////////////////////////////
    //指定したロケーションにショップがあるかどうか
    ///////////////////////////////////////
    fun getShop(loc:Location,server:Server):Int{

        for (shop in Man10Shop.merchantShops){

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


        val itemStackList = mutableListOf<ItemStack>()

        for (i in 0..8){

            if (inv.getItem(i) ==null ||inv.getItem(i)!!.type == Material.AIR)break

            itemStackList.add(inv.getItem(i)!!)
            if (inv.getItem(i+9) !=null){
                itemStackList.add(inv.getItem(i+9)!!)
            }else{
                itemStackList.add(ItemStack(Material.AIR))
            }
            itemStackList.add(inv.getItem(i+18)!!)

        }

        return itemStackList
    }

    /////////////////////////////////////////
    //ItemStackのリストからショップのレシピを作る
    /////////////////////////////////////////
    fun itemToMerchant(list: MutableList<ItemStack>): Merchant {

        val merchant = Bukkit.createMerchant("AdminShop")
        val merchantList = mutableListOf<MerchantRecipe>()

        var i = 0
        while (list.size > i+2){

            val recipe = MerchantRecipe(list[i+2],1000000)

            recipe.addIngredient(list[i])
            recipe.addIngredient(list[i+1])

            recipe.setExperienceReward(false)

            merchantList.add(recipe)

            i +=3
        }

        merchant.recipes = merchantList

        return merchant
    }


    class MerchantShopData{

        var server = "server"
        var world = "world"

        var x = 0
        var y = 0
        var z = 0

        var shop = Bukkit.createMerchant("AdminShop")
    }

}