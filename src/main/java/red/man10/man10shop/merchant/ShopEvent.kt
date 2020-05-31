package red.man10.man10shop.merchant

import org.bukkit.block.Sign
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.SignChangeEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.player.PlayerInteractEvent
import red.man10.man10shop.Man10Shop
import red.man10.man10shop.Man10Shop.Companion.OP
import red.man10.man10shop.Man10Shop.Companion.USER
import red.man10.man10shop.Man10Shop.Companion.database
import red.man10.man10shop.Man10Shop.Companion.merchantShop
import red.man10.man10shop.Man10Shop.Companion.merchantShops
import red.man10.man10shop.Man10Shop.Companion.mysqlQueue
import red.man10.man10shop.Man10Shop.Companion.sendOP

class ShopEvent:Listener {

    val shopTitle = "§e§lADMIN SHOP"

    @EventHandler
    fun setShopEvent(e:SignChangeEvent){

        val p = e.player

        if (!p.hasPermission(OP))return

        if (e.lines.isEmpty())return

        val line = e.lines[0]

        var id = 0

        if (line.indexOf("AdminShop") == 0){
            id = line.replace("AdminShop:","").toInt()
        }

        val data = merchantShops[id]?:return


        val loc = e.block.location

        data.server = p.server.name
        data.world = loc.world.name
        data.x = loc.blockX
        data.y = loc.blockY
        data.z = loc.blockZ

        merchantShops[id] = data

        merchantShop.updateShop(data, id, p)

        e.setLine(0,shopTitle)
        e.setLine(1,"§a§l右クリックで開く")
        e.setLine(2,"§b§lRIGHT CLICK")
        e.setLine(3,"§b§lSIGN")

        database.logOP(p,id,"PlaceShopSign")

        sendOP("§a§l${p.name}がショップを設置しました！")

    }

    @EventHandler
    fun clickShopEvent(e:PlayerInteractEvent){

        if (!Man10Shop.pluginEnable)return

        val p = e.player

        if (!p.hasPermission(USER))return

        if (e.action != Action.RIGHT_CLICK_BLOCK)return

        val sign = e.clickedBlock!!.state

        if (sign !is Sign)return

        if (sign.lines.size != 4)return

        if (sign.lines[0] != shopTitle)return

        val signLoc = sign.location
        val pLoc = p.location

        val id = merchantShop.getShop(signLoc,p.server)

        if (id == -1)return

        p.openMerchant(merchantShops[id]!!.shop,true)

        //ログ
        mysqlQueue.add("INSERT INTO log " +
                "(player, uuid, server, world, locX, locY, locZ, logType, note)" +
                " VALUES (" +
                "'${p.name}', " +
                "'${p.uniqueId}', " +
                "'${p.server.name}', " +
                "'${pLoc.world.name}', " +
                "${pLoc.x}, " +
                "${pLoc.y}, " +
                "${pLoc.z}, " +
                "'OpenMerchantShop', " +
                "'ID:$id');")
    }

    //////////////////////////////
    //ショップを破壊
    ///////////////////////////////
    @EventHandler
    fun breakShop(e:BlockBreakEvent){

        if (!Man10Shop.pluginEnable)return

        val p = e.player

        val sign = e.block.state

        if (sign !is Sign)return

        if (sign.lines.size != 4)return

        val loc = sign.location

        val id = merchantShop.getShop(loc,p.server)

        if (id == -1)return

        if (!p.hasPermission(OP)){
            e.isCancelled = true
            return
        }

        merchantShop.deleteShop(id,p)

        sendOP("§a§l${p.name}がショップを削除しました！")

    }

    /////////////////////////
    //新規ショップを作成
    ////////////////////////
    @EventHandler
    fun inventoryClose(e:InventoryCloseEvent){

        val p = e.player as Player

        if (e.view.title != "§e§lMan10ShopOp")return
        if (!p.hasPermission(OP))return

        merchantShop.createShop(e.inventory,p)

    }

}