package red.man10.man10shop

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.md_5.bungee.api.chat.ClickEvent
import net.md_5.bungee.api.chat.ComponentBuilder
import net.md_5.bungee.api.chat.HoverEvent
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import red.man10.man10offlinebank.BankAPI
import red.man10.man10shop.merchant.Commands
import red.man10.man10shop.merchant.MerchantShop
import red.man10.man10shop.merchant.MerchantShop.MerchantShopData
import red.man10.man10shop.merchant.ShopEvent
import red.man10.man10shop.usershop.UserShop
import java.lang.Exception
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue

class Man10Shop : JavaPlugin(){

    companion object{

        var merchantShops = ConcurrentHashMap<Int, MerchantShopData>()

        var mysqlQueue = LinkedBlockingQueue<String>()

        val checkMap = HashMap<Player,Int>()

        lateinit var vault:VaultManager

        lateinit var pl : Man10Shop

        lateinit var bank : BankAPI

        const val OP = "man10shop.op"
        const val USER = "man10shop.user"
        const val CREATE = "man10shop.create"

        const val USERSHOP = "§a§lUSER SHOP"

        var maxPrice = 10.0

        var pluginEnable = true

        var cost = 10000.0

        var enableWorld = mutableListOf<String>()

        var breakMode = HashMap<Player,Boolean>()

        fun sendMsg(p: Player, msg:String){
            p.sendMessage("§e[Man10Shop]§r$msg")
        }

        fun sendHoverText(p: Player, text: String, hoverText: String, command: String) {
            //////////////////////////////////////////
            //      ホバーテキストとイベントを作成する
            val hoverEvent = HoverEvent(HoverEvent.Action.SHOW_TEXT, ComponentBuilder(hoverText).create())

            //////////////////////////////////////////
            //   クリックイベントを作成する
            val clickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, "/$command")
            val message = ComponentBuilder(text).event(hoverEvent).event(clickEvent).create()
            p.spigot().sendMessage(*message)
        }

        //OPにのみメッセージを送る
        fun sendOP(message:String){
            for (p in Bukkit.getOnlinePlayers()){
                if (!p.hasPermission(OP))continue

                p.sendMessage(message)
            }
        }
    }

    override fun onEnable() {
        // Plugin startup logic

        saveDefaultConfig()

        pl = this

        pluginEnable = config.getBoolean("pluginEnabled",true)
        maxPrice = config.getDouble("maxPrice",100000000.0)
        enableWorld = config.getStringList("enableWorld")
        cost = config.getDouble("cost",10000.0)

        Database.mysqlQueue()

        bank = BankAPI(this)

        vault = VaultManager(this)

        //ショップデータの読み込み
        MerchantShop.loadShopData()
        UserShop.loadShopData()

        server.pluginManager.registerEvents(ShopEvent,this)
        server.pluginManager.registerEvents(red.man10.man10shop.usershop.ShopEvent,this)
        getCommand("man10shop")!!.setExecutor(this)
        getCommand("createshop")!!.setExecutor(Commands)
        getCommand("editshop")!!.setExecutor(red.man10.man10shop.usershop.ShopEvent)

    }

    override fun onDisable() {
        // Plugin shutdown logic
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {

        if (label=="usershop"){

            val cmd = args[0]

            if (sender !is Player)return false

            if (!sender.hasPermission(USER))return false

            if (!pluginEnable)return false

            if (cmd == "buyusershop"){//man10shop buyusershop <id> <is stack>

                try {

                    val id = args[1].toInt()

                    //ショップが編集中だった場合
                    if (red.man10.man10shop.usershop.ShopEvent.isEdit.contains(id)){
                        sendMsg(sender,"§c§l現在ショップの編集中です！")
                        return false
                    }

                    GlobalScope.launch {
                        if (UserShop.buy(id,sender,args[2].toBoolean())){
                            sendMsg(sender,"§a取引成功")
                        }else{
                            sendMsg(sender,"§c取引失敗")
                        }

                    }

                }catch (e:Exception){
                    sendMsg(sender,"§c§lERROR:${e.message}")
                }

                return true

            }

            if (cmd == "sellusershop"){

                try {

                    val id = args[1].toInt()

                    //ショップが編集中だった場合
                    if (red.man10.man10shop.usershop.ShopEvent.isEdit.contains(id)){
                        sendMsg(sender,"§c§l現在ショップの編集中です！")
                        return false
                    }

                    GlobalScope.launch {
                        if (args[2] == "all"){

                            if (checkMap[sender]==null || checkMap[sender]!=id ){

                                sendMsg(sender,"§a本当にすべて売却しますか？")
                                sendHoverText(sender,"§c§l[売っちゃう！]","","usershop sellusershop $id all")
                                checkMap[sender] = id
                                return@launch
                            }

                            checkMap.remove(sender)

                            if (UserShop.sellAll(id,sender)){
                                sendMsg(sender,"§a取引成功")
                                return@launch
                            }
                            sendMsg(sender,"§c取引失敗")
                            return@launch
                        }

                        if (UserShop.sell(id,sender,args[2].toBoolean())){
                            sendMsg(sender,"§a取引成功")
                        }else{
                            sendMsg(sender,"§c取引失敗")
                        }
                    }
                    return true


                }catch (e:Exception){
                    sendMsg(sender,"§c§lERROR:${e.message}")
                }

                return true

            }

            return true
        }

        if (label!="man10shop")return false

        if (!sender.hasPermission(OP))return false

        if (sender !is Player)return false

        if (args.isEmpty()){

            sendMsg(sender,"/man10shop on : プラグインをonにする")
            sendMsg(sender,"/man10shop off : プラグインをoffにする")
            sendMsg(sender,"/man10shop enable add <world> : 指定ワールドでショップの利用を許可する")
            sendMsg(sender,"/man10shop enable remove <world> : 指定ワールドでショップの利用を許可を外す")
            sendMsg(sender,"/man10shop breakmode : ユーザーショップの破壊モードのON/OFF")

            return true
        }

        val cmd = args[0]

        if (cmd == "off"){
            config.set("pluginEnabled",false)
            sender.sendMessage("ショップをOFFにしました")
            pluginEnable = false
        }

        if (cmd == "on"){
            config.set("pluginEnabled",true)
            sender.sendMessage("ショップをONにしました")
            pluginEnable = true
        }

        if (cmd == "enable" && args.size == 3){

            if (args[1] == "add"){

                enableWorld.add(args[2])
                config.set("enableWorld", enableWorld)
                saveConfig()
                sendMsg(sender,"追加完了！")

            }

            if (args[1] == "remove"){
                enableWorld.remove(args[2])
                config.set("enableWorld", enableWorld)
                saveConfig()
                sendMsg(sender,"削除完了！")

            }

        }

        if (cmd == "breakmode"){

            if (breakMode[sender] == null){
                breakMode[sender] = true
                return true
            }

            breakMode[sender] = !breakMode[sender]!!
        }

        if (cmd == "reloadconfig"){

            Thread {

                reloadConfig()

                pluginEnable = config.getBoolean("pluginEnabled", true)
                maxPrice = config.getDouble("maxPrice", 100000000.0)
                enableWorld = config.getStringList("enableWorld")
                cost = config.getDouble("cost", 10000.0)

                sendMsg(sender, "コンフィグのリロード完了！")

            }.start()

        }

        return false
    }


}