package red.man10.man10shop.usershop

import org.apache.commons.lang.math.NumberUtils
import org.bukkit.Bukkit
import org.bukkit.block.Sign
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.SignChangeEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.player.PlayerInteractEvent
import red.man10.man10shop.Man10Shop.Companion.CREATE
import red.man10.man10shop.Man10Shop.Companion.OP
import red.man10.man10shop.Man10Shop.Companion.USER
import red.man10.man10shop.Man10Shop.Companion.USERSHOP
import red.man10.man10shop.Man10Shop.Companion.breakMode
import red.man10.man10shop.Man10Shop.Companion.cost
import red.man10.man10shop.Man10Shop.Companion.enableWorld
import red.man10.man10shop.Man10Shop.Companion.maxPrice
import red.man10.man10shop.Man10Shop.Companion.pluginEnable
import red.man10.man10shop.Man10Shop.Companion.sendMsg
import red.man10.man10shop.Man10Shop.Companion.sendOP
import red.man10.man10shop.Man10Shop.Companion.userShop
import red.man10.man10shop.Man10Shop.Companion.vault
import kotlin.math.cos

class ShopEvent : Listener{

    val checkMap = mutableListOf<Pair<Player,Int>>()


    //ショップ看板の設置、看板内容の書き換え
    @EventHandler
    fun signEvent(e:SignChangeEvent){

        if (!pluginEnable)return

        val p = e.player

        if (!enableWorld.contains(p.world.name))return

        if (!p.hasPermission(CREATE))return

        val lines = e.lines.clone()

        val id = userShop.getShop(e.block.location,p.server.name)


        //////同じ場所にショップがあったら復元
        if (id!=null){
            e.setLine(0, USERSHOP)
            e.setLine(1,"§b§l${Bukkit.getOfflinePlayer(id.second.ownerUUId).name}")
            e.setLine(2,"${if (id.second.isBuy) "§d§lB" else "§b§lS"}§e§l${id.second.price}")

            sendMsg(p,"§a§lショップが復元されました！")
            return

        }

        if (lines[0].indexOf("shop") != 0)return

        if (!NumberUtils.isNumber(lines[1]))return

        val price = lines[1].toDouble()

        if (price <0)return

        if (price> maxPrice)return

        val isBuy = lines[0].replace("shop","") == "b"

        //////////ショップ代を支払う
        if (vault.getBalance(p.uniqueId) < cost){
            sendMsg(p,"§c§lショップを作る費用が足りません！")
            return
        }

        vault.withdraw(p.uniqueId, cost)

        userShop.create(p,e.block.location,price,isBuy)
        sendMsg(p,"§a§l新規ショップを作成しました！")

        e.setLine(0, USERSHOP)
        e.setLine(1,"§b§l${p.name}")
        e.setLine(2,"${if (isBuy) "§d§lB" else "§b§lS"}§e§l${price}")
        e.setLine(3,lines[2].replace("&","§"))

    }

    @EventHandler
    fun signClickEvent(e:PlayerInteractEvent){

        if (!pluginEnable)return

        val p = e.player

        if (!enableWorld.contains(p.world.name))return

        if (!p.hasPermission(USER))return

        if (e.action != Action.RIGHT_CLICK_BLOCK && e.action != Action.LEFT_CLICK_BLOCK)return

        val sign = e.clickedBlock!!.state

        if (sign !is Sign)return

        val shop = userShop.getShop(sign.location,p.server.name)?:return


        //取引する
        if (e.action == Action.RIGHT_CLICK_BLOCK){

            //自分のショップだった場合リターン
            if (shop.second.ownerUUId == p.uniqueId){
                sendMsg(p,"§c§lこれは自分のショップです！")
                return
            }

            //ショップの確認
            if (!checkMap.contains(Pair(p,shop.first))){

                if (shop.second.container.isEmpty()){
                    sendMsg(p,"§c§lショップの在庫、もしくは買取アイテムの設定がされていないようです")
                    return
                }

                val item = shop.second.container[shop.second.container.size-1]

                val name = if (!item.hasItemMeta()) item.i18NDisplayName!! else item.itemMeta.displayName

                val lore = if (item.hasItemMeta()&& item.itemMeta.lore != null) item.itemMeta.lore!![0] else "説明無し"

                sendMsg(p,"§e§k§lXXXX§r${if (shop.second.isBuy) "§d§l購入確認" else "§b§l売却確認"}§e§k§lXXXX")
                sendMsg(p,name)
                sendMsg(p,lore)

                checkMap.add(Pair(p,shop.first))
                return
            }

            checkMap.remove(Pair(p,shop.first))

            if (userShop.tradeItem(shop.first,p, p.isSneaking)){
                sendMsg(p,"§a§l取引成功！")
            }else{
                sendMsg(p,"§c§l取引失敗！")
            }

            return
        }

        //コンテナの中身を見る
        if (p.uniqueId == shop.second.ownerUUId && e.action == Action.LEFT_CLICK_BLOCK && !p.isSneaking){

            e.isCancelled = true

            userShop.openContainer(p,shop.first)

            return
        }


    }

    //ショップ削除
    @EventHandler
    fun signBreakEvent(e:BlockBreakEvent){

        val p = e.player

        if (!enableWorld.contains(p.world.name))return

        val sign = e.block.state

        if (sign !is Sign)return

        val pair = userShop.getShop(sign.location,p.server.name) ?: return

        if (p.uniqueId != pair.second.ownerUUId &&!p.hasPermission(OP)){
            e.isCancelled = true
            return
        }

        if (!p.isSneaking){
            e.isCancelled = true
            return
        }

        if (userShop.get(pair.first).container.isNotEmpty()){
            if (p.hasPermission(OP) && breakMode[p] != null && breakMode[p]!!){

                userShop.deleteShop(pair.first,p)

                sendOP("§a${p.name}がユーザーのショップを強制削除しました")
                return
            }

            sendMsg(p,"§c§lショップの中にアイテムが入っているので破壊できません！")
            e.isCancelled = true
            return
        }

        userShop.deleteShop(pair.first,p)

        sendMsg(p,"§a§lショップを削除しました！")

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