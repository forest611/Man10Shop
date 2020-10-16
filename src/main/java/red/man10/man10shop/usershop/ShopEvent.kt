package red.man10.man10shop.usershop

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.apache.commons.lang.math.NumberUtils
import org.bukkit.Bukkit
import org.bukkit.Material
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
import org.bukkit.inventory.ItemStack
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
import red.man10.man10shop.Man10Shop.Companion.vault

object ShopEvent : Listener, CommandExecutor {

    val checkMap = HashMap<Player,Int>()
    val isEdit = mutableListOf<Int>()


    //ショップ看板の設置、看板内容の書き換え
    @EventHandler
    fun signEvent(e:SignChangeEvent){

        if (!pluginEnable)return

        val p = e.player

        if (!enableWorld.contains(p.world.name))return

        if (!p.hasPermission(CREATE))return

        val lines = e.lines.clone()

        val id = UserShop.getShop(e.block.location,p.server.name)


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

        if (price <0.1)return

        if (price> maxPrice)return

        val isBuy = lines[0] == "shop" || lines[0] == "shopb"

        //////////ショップ代を支払う
        if (vault.getBalance(p.uniqueId) < cost){
            sendMsg(p,"§c§lショップを作る費用が足りません！")
            return
        }

        vault.withdraw(p.uniqueId, cost)

        GlobalScope.launch {
            UserShop.create(p,e.block.location,price,isBuy)
            sendMsg(p,"§a§l新規ショップを作成しました！")

            if (isBuy){
                sendMsg(p,"§a§l左クリックでショップのコンテナを開き、販売するアイテムを入れてください！")
            }else{
                sendMsg(p,"§a§l左クリックでショップのコンテナを開き、左上に買い取るアイテムを入れてください！")
            }

        }


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

        val shop = UserShop.getShop(sign.location,p.server.name)?:return


        //取引する
        if (e.action == Action.RIGHT_CLICK_BLOCK){

            //自分のショップだった場合リターン
            if (shop.second.ownerUUId == p.uniqueId){
                val item = p.inventory.itemInMainHand

                if (item.hasItemMeta() && item.itemMeta.displayName.indexOf("§a§lman10shop") == 0){

                    val lore = item.lore!!

                    UserShop.updateShop(shop.first,p,lore[1].toDouble(), lore[0] == "b")

                    sign.setLine(0, USERSHOP)
                    sign.setLine(1,"§b§l${p.name}")
                    sign.setLine(2,"${if (lore[0] == "b") "§d§lB" else "§b§lS"}§e§l${lore[1].toDouble()}")
                    sign.setLine(3,lore[2].replace("&","§"))

                    sign.update()
                    p.inventory.removeItem(item)
                    sendMsg(p,"§a§lショップをアップデートしました")
                    return

                }
                sendMsg(p,"§c§lこれは自分のショップです！")

                return
            }

            //ショップが編集中だった場合
            if (isEdit.contains(shop.first)){
                sendMsg(p,"§c§l現在ショップの編集中です！")
                return
            }

            //ショップが空だった場合
            if (shop.second.container.isEmpty()){
                sendMsg(p,"§c§lショップの在庫、もしくは買取アイテムの設定がされていないようです")
                return
            }

            //ショップの確認
            if (checkMap[p] ==null || checkMap[p] != shop.first){


                val item = shop.second.container[shop.second.container.size-1]

                val name = if (!item.hasItemMeta()) item.i18NDisplayName!! else item.itemMeta.displayName

                val lore = if (item.hasItemMeta()&& item.itemMeta.lore != null) item.itemMeta.lore!![0] else "説明無し"

                sendMsg(p,"§e§k§lXXXX§r${if (shop.second.isBuy) "§d§l購入確認" else "§b§l売却確認"}§e§k§lXXXX")
                sendMsg(p,name)
                sendMsg(p,lore)

                checkMap[p] = shop.first
                return
            }

            checkMap.remove(p)

            if (UserShop.tradeItem(shop.first,p, p.isSneaking)){
                sendMsg(p,"§a§l取引成功！")
            }else{
                sendMsg(p,"§c§l取引失敗！")
            }

            return
        }

        //コンテナの中身を見る
        if ((p.uniqueId == shop.second.ownerUUId || p.hasPermission(OP)) && e.action == Action.LEFT_CLICK_BLOCK && !p.isSneaking){

            e.isCancelled = true

            isEdit.add(shop.first)

            UserShop.openContainer(p,shop.first)

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

        val pair = UserShop.getShop(sign.location,p.server.name) ?: return

        if (p.uniqueId != pair.second.ownerUUId &&!p.hasPermission(OP)){
            e.isCancelled = true
            return
        }

        if (!p.isSneaking){
            e.isCancelled = true
            return
        }

        if (UserShop.get(pair.first).container.isNotEmpty()){
            if (p.hasPermission(OP) && breakMode[p] != null && breakMode[p]!!){

                UserShop.deleteShop(pair.first,p)

                sendOP("§a${p.name}がユーザーのショップを強制削除しました")
                return
            }

            sendMsg(p,"§c§lショップの中にアイテムが入っているので破壊できません！")
            e.isCancelled = true
            return
        }

        UserShop.deleteShop(pair.first,p)

        sendMsg(p,"§a§lショップを削除しました！")

    }

    @EventHandler
    fun inventoryClose(e:InventoryCloseEvent){

        val p = e.player

        if (p !is Player)return

        if (e.view.title.indexOf(UserShop.CONTAINER_NAME) == 0){

            val id = e.view.title.replace(UserShop.CONTAINER_NAME,"").toInt()

            UserShop.updateShop(id,p,e.inventory)

            isEdit.remove(id)

        }
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (label != "editshop" && label !="shop")return true

        if (sender !is Player)return true

        if (args.isEmpty()){
            sendMsg(sender,"§d§l/editshop <b/s> <Price/値段> <看板に表示するメッセージ>")
            return true
        }

        //editshop type price text
        if (args.size == 3){

            if (args[0] != "b" && args[0] != "s"){
                sendMsg(sender,"§c§l販売看板なら「b」買取看板なら「s」と入力してください！")
                return true
            }

            if (!NumberUtils.isNumber(args[1])){
                sendMsg(sender,"§c§l数字を入力してください！")
                return true
            }

            val price = args[1].toDouble()

            if (price<0.1|| price> maxPrice){
                sendMsg(sender,"§c§l値段設定に問題があります！")
                sendMsg(sender,"§c§l0.1円以上、$maxPrice 未満に設定してください！")
                return true
            }

            if (vault.getBalance(sender.uniqueId) <10){
                sendMsg(sender,"§c§lショップのアップデート料に10円必要です！")
                return true
            }

            vault.withdraw(sender.uniqueId,10.0)

            val paper = ItemStack(Material.PAPER)

            val meta = paper.itemMeta
            meta.setDisplayName("§a§lman10shop")
            meta.lore = mutableListOf(args[0],args[1],args[2])

            paper.itemMeta = meta

            sender.inventory.addItem(paper)
        }

        return true

    }


}