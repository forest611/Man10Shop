package red.man10.man10shop

import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin
import red.man10.man10offlinebank.BankAPI
import red.man10.man10shop.merchant.Commands
import red.man10.man10shop.merchant.MerchantShop
import red.man10.man10shop.merchant.MerchantShop.MerchantShopData
import red.man10.man10shop.merchant.ShopEvent
import red.man10.man10shop.usershop.UserShop
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue

class Man10Shop : JavaPlugin(){

    companion object{

        var merchantShops = ConcurrentHashMap<Int, MerchantShopData>()

        var mysqlQueue = LinkedBlockingQueue<String>()

        lateinit var database: Database

        lateinit var merchantShop: MerchantShop
        lateinit var userShop : UserShop

        lateinit var vault:VaultManager

        lateinit var pl : Man10Shop

        lateinit var bank : BankAPI

        //OPにのみメッセージを送る
        fun sendOP(message:String){
            for (p in Bukkit.getOnlinePlayers()){
                if (!p.hasPermission("man10shop.op"))continue

                p.sendMessage(message)
            }
        }

        val OP = "man10shop.op"
        val USER = "man10shop.user"
        val CREATE = "man10shop.create"

        val USERSHOP = "§a§lUSER SHOP"

        var maxPrice = 10.0

        var pluginEnable = true

        var cost = 10000.0

        var enableWorld = mutableListOf<String>()

        var breakMode = HashMap<Player,Boolean>()

        fun sendMsg(p:Player,msg:String){
            p.sendMessage("§e[Man10Shop]§r$msg")
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

        database = Database()
        merchantShop = MerchantShop()
        userShop = UserShop()

        database.mysqlQueue()

        bank = BankAPI(this)

        vault = VaultManager(this)

        //ショップデータの読み込み
        merchantShop.loadShopData()
        userShop.loadShopData()

        server.pluginManager.registerEvents(ShopEvent(),this)
        server.pluginManager.registerEvents(red.man10.man10shop.usershop.ShopEvent(),this)
        getCommand("man10shop")!!.setExecutor(this)
        getCommand("createshop")!!.setExecutor(Commands())
        getCommand("editshop")!!.setExecutor(red.man10.man10shop.usershop.ShopEvent())

    }

    override fun onDisable() {
        // Plugin shutdown logic
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {

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

        //ユーザーショップのリスト
        if (cmd == "userlist"){

        }

        //adminショップ(看板)のリスト
        if (cmd == "adminlist"){

        }

        //merchantショップのリスト
        if (cmd == "merchantlist"){

        }

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

            Thread(Runnable {

                reloadConfig()

                pluginEnable = config.getBoolean("pluginEnabled",true)
                maxPrice = config.getDouble("maxPrice",100000000.0)
                enableWorld = config.getStringList("enableWorld")
                cost = config.getDouble("cost",10000.0)

                sendMsg(sender,"コンフィグのリロード完了！")

            }).start()

        }

        return false
    }


}