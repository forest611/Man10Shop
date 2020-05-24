package red.man10.man10shop.usershop

import org.apache.commons.lang.math.NumberUtils
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.SignChangeEvent
import red.man10.man10shop.Man10Shop

class ShopEvent:Listener {


    @EventHandler
    fun createChestEvent(e:SignChangeEvent){

        val p = e.player

        if (!p.hasPermission("man10shop.createuser"))return

        if (e.lines[0].indexOf("user:")!= 0)return
        if (!NumberUtils.isNumber(e.lines[1]))return

        val price = e.lines[1].toDouble()

        val isSell = e.lines[0].replace("user:","") == "sell"

        Man10Shop.userShopData.registerShop(p,price,isSell,e.block.location)

        e.setLine(0,"§e§lShop")
        e.setLine(1,"§e§l")

    }


}