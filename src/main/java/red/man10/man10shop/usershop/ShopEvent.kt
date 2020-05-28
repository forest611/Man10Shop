package red.man10.man10shop.usershop

import org.apache.commons.lang.math.NumberUtils
import org.bukkit.block.data.type.Sign
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.SignChangeEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.player.PlayerInteractEvent
import red.man10.man10shop.Man10Shop.Companion.CREATE
import red.man10.man10shop.Man10Shop.Companion.USER
import red.man10.man10shop.Man10Shop.Companion.USERSHOP
import red.man10.man10shop.Man10Shop.Companion.pluginEnable
import red.man10.man10shop.Man10Shop.Companion.userShop

class ShopEvent : Listener{


    //ショップ看板の設置、看板内容の書き換え
    @EventHandler
    fun signEvent(e:SignChangeEvent){

        if (!pluginEnable)return

        val p = e.player

        if (!p.hasPermission(CREATE))return

        val lines = e.lines

        if (lines[0].indexOf("usershop") != 0)return

        if (!NumberUtils.isNumber(lines[1]))return

        val isBuy = lines[0].replace("usershop","") == "buy"

        userShop.create(p,e.block.location,lines[1].toDouble(),isBuy)

        e.setLine(0, USERSHOP)
        e.setLine(1,"§b§l${p.name}")
        e.setLine(2,"${if (isBuy) "§d§l販売中:" else "§b§l買取中:"}§e§l${lines[1]}")
        e.setLine(3,lines[2])

    }

    @EventHandler
    fun signClickEvent(e:PlayerInteractEvent){

        if (!pluginEnable)return

        val p = e.player

        if (!p.hasPermission(USER))return

        if (e.action != Action.RIGHT_CLICK_BLOCK && e.action != Action.LEFT_CLICK_BLOCK)return

        val sign = e.clickedBlock!!.state

        if (sign !is Sign)return

        val shop = userShop.getShop(e.clickedBlock!!.location)?:return

        //取引する
        if (e.action == Action.RIGHT_CLICK_BLOCK){
            userShop.tradeItem(shop.first,p, p.isSneaking)
            return
        }

        //コンテナの中身を見る
        if (p.uniqueId == shop.second.ownerUUId && e.action == Action.LEFT_CLICK_BLOCK && p.isSneaking){

            userShop.openContainer(p,shop.first)

            return
        }


    }

    //ショップ削除
    @EventHandler
    fun signBreakEvent(e:BlockBreakEvent){

        val p = e.player

        val sign = e.block

        if (sign !is Sign)return

        val pair = userShop.getShop(sign.location) ?: return

        if (p.uniqueId != pair.second.ownerUUId)return

        userShop.deleteShop(pair.first,p)

        p.sendMessage("§a§lショップを削除しました！")

    }

    @EventHandler
    fun inventoryClose(e:InventoryCloseEvent){

        val p = e.player

        if (p !is Player)return

        if (e.view.title.indexOf(userShop.CONTAINER_NAME) == 0){

            val id = e.view.title.replace(userShop.CONTAINER_NAME,"").toInt()

            userShop.updateShop(id,p,e.inventory)

        }
    }

}