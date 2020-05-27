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
import red.man10.man10shop.Man10Shop.Companion.merchantMerchantShop
import red.man10.man10shop.Man10Shop.Companion.merchantShops
import red.man10.man10shop.Man10Shop.Companion.mysqlQueue
import red.man10.man10shop.Man10Shop.Companion.sendOP

class ShopEvent:Listener {

    @EventHandler
    fun setShopEvent(e:SignChangeEvent){

        val p = e.player

        if (!p.hasPermission("man10shop.op"))return

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

        merchantMerchantShop.updateShop(data, id, p)

        e.setLine(0,"§e§lADMIN SHOP")
        e.setLine(1,"§a§l右クリックで開く")
        e.setLine(2,"§b§lRIGHT CLICK!!")

        //ログ
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
                "'PlaceShopSign');")

        sendOP("§a§l${p.name}がショップを設置しました！")

    }

    @EventHandler
    fun clickShopEvent(e:PlayerInteractEvent){

        val p = e.player

        if (!p.hasPermission("man10shop.use"))return

        if (e.action != Action.RIGHT_CLICK_BLOCK)return

        val sign = e.clickedBlock!!.state

        if (sign !is Sign)return

        if (sign.lines.size != 3)return

        if (sign.lines[0] != "§e§lADMIN SHOP")return

        val signLoc = sign.location
        val pLoc = p.location

        val id = merchantMerchantShop.getShop(signLoc,p.server)

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
                "'OpenAdminShop', " +
                "'ID:$id');")
    }

    @EventHandler
    fun breakShop(e:BlockBreakEvent){
        val p = e.player

        val sign = e.block.state

        if (sign !is Sign)return

        if (sign.lines.size != 4)return

        val loc = sign.location

        val id = merchantMerchantShop.getShop(loc,p.server)

        if (id == -1)return

        if (!p.hasPermission("man10shop.op")){
            e.isCancelled = true
            return
        }

        merchantMerchantShop.deleteShop(id,p)

        sendOP("§a§l${p.name}がショップを削除しました！")

    }

    @EventHandler
    fun inventoryClose(e:InventoryCloseEvent){

        val p = e.player as Player

        if (e.view.title != "§e§lMan10ShopOp")return
        if (!p.hasPermission("man10shop.op"))return

        merchantMerchantShop.createShop(e.inventory,p)

    }

}