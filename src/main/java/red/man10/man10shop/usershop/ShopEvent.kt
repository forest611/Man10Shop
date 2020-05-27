package red.man10.man10shop.usershop

import org.apache.commons.lang.math.NumberUtils
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.data.type.Chest
import org.bukkit.block.data.type.Sign
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.SignChangeEvent
import org.bukkit.event.player.PlayerInteractEvent
import red.man10.man10shop.Man10Shop
import red.man10.man10shop.Man10Shop.Companion.userShop

class ShopEvent:Listener {


    @EventHandler
    fun createChestEvent(e:SignChangeEvent){

        val p = e.player

        if (!p.hasPermission("man10shop.createuser"))return

        if (e.lines[0].indexOf("user:")!= 0)return
        if (!NumberUtils.isNumber(e.lines[1]))return

        val price = e.lines[1].toDouble()

        val isSell = e.lines[0].replace("user:","") == "sell"

        userShop.createShop(p,price,isSell,e.block.location)

        e.setLine(0,"§e§lShop")
        e.setLine(1,"§e§l")

    }

    @EventHandler
    fun clickShop(e:PlayerInteractEvent){

        if (e.action != Action.RIGHT_CLICK_BLOCK)return

        val sign = e.clickedBlock!!.state

        if (sign !is org.bukkit.block.Sign)return


    }


}