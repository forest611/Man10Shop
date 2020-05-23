package red.man10.man10shop.adminshop

import org.bukkit.block.Sign
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.block.SignChangeEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.player.PlayerInteractEvent
import red.man10.man10shop.Man10Shop
import red.man10.man10shop.Man10Shop.Companion.adminShopData
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

        val data = Man10Shop.shopMap[id]?:return

        e.setLine(0,"§e§lADMIN SHOP")
        e.setLine(1,"§a§l右クリックで開く")
        e.setLine(2,"§b§lRIGHT CLICK THIS SIGN")
        e.setLine(3,"§A§D§M§I§N§S§H§O§P")

        val loc = e.block.location

        data.server = p.server.name
        data.world = loc.world.name
        data.x = loc.blockX
        data.y = loc.blockY
        data.z = loc.blockZ

        adminShopData.updateShopData(data, id, p)

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

        if (!p.hasPermission("man10shop.use"))

        if (e.action != Action.RIGHT_CLICK_BLOCK)return

        val sign = e.clickedBlock!!.state

        if (sign !is Sign)return

        if (sign.lines.size != 4)return

        if (sign.lines[3] != "§A§D§M§I§N§S§H§O§P")return

        val signLoc = sign.location
        val pLoc = p.location

        val id = adminShopData.getShop(signLoc,p.server)

        if (id == -1)return

        p.openMerchant(Man10Shop.shopMap[id]!!.shop,true)

        //ログ
        mysqlQueue.add("INSERT INTO log " +
                "(player, uuid, server, world, locX, locY, locZ, logType, note)" +
                " VALUES (" +
                "'${p.name}', " +
                "'${p.uniqueId}', " +
                "'${p.server.name}', " +
                "'${pLoc.world}', " +
                "${pLoc.x}, " +
                "${pLoc.y}, " +
                "${pLoc.z}, " +
                "'OpenAdminShop', " +
                "'ID:$id);")
    }

    @EventHandler
    fun breakShop(e:BlockBreakEvent){
        val p = e.player

        val sign = e.block.state

        if (sign !is Sign)return

        if (sign.lines.size != 4)return

        if (sign.lines[3] != "§A§D§M§I§N§S§H§O§P")return

        val loc = sign.location

        val id = adminShopData.getShop(loc,p.server)

        if (id == -1)return

        if (!p.hasPermission("man10shop.op")){
            e.isCancelled = true
            return
        }

        adminShopData.deleteShop(id,p)

        sendOP("§a§l${p.name}がショップを削除しました！")

    }

    @EventHandler
    fun inventoryClose(e:InventoryCloseEvent){

        val p = e.player as Player

        if (e.view.title != "§e§lMan10ShopOp")return
        if (!p.hasPermission("man10shop.op"))return

        adminShopData.registerShop(e.inventory,p)

    }


}