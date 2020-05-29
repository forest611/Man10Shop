package red.man10.man10shop

import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin
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

        var pluginEnable = true

        fun sendMsg(p:Player,msg:String){
            p.sendMessage("§e[Man10Shop]§r$msg")
        }

    }

    override fun onEnable() {
        // Plugin startup logic

        saveDefaultConfig()

        pl = this

        pluginEnable = config.getBoolean("pluginEnabled",true)

        database = Database()
        merchantShop = MerchantShop()
        userShop = UserShop()

        database.mysqlQueue()

        vault = VaultManager(this)

        //ショップデータの読み込み
        merchantShop.loadShopData()
        userShop.loadShopData()

        server.pluginManager.registerEvents(ShopEvent(),this)
        server.pluginManager.registerEvents(red.man10.man10shop.usershop.ShopEvent(),this)
        getCommand("createshop")!!.setExecutor(Commands())
        getCommand("shopbal")!!.setExecutor(red.man10.man10shop.usershop.ShopEvent())

    }

    override fun onDisable() {
        // Plugin shutdown logic
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {

        if (label!="man10shop")return false

        if (!sender.hasPermission(OP))return false

        if (sender !is Player)return false

        if (args[0].isEmpty()){
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

        return false
    }


}