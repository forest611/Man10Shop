package red.man10.man10shop

import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import red.man10.man10shop.merchant.Commands
import red.man10.man10shop.merchant.ShopData
import red.man10.man10shop.merchant.ShopData.MerchantShopData
import red.man10.man10shop.merchant.ShopEvent
import red.man10.man10shop.usershop.ShopData.UserShopData
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue

class Man10Shop : JavaPlugin() {

    companion object{

        var merchantShops = ConcurrentHashMap<Int, MerchantShopData>()
        var userShops = ConcurrentHashMap<Int, UserShopData>()

        var mysqlQueue = LinkedBlockingQueue<String>()

        lateinit var database: Database
        lateinit var merchantShopData: ShopData
        lateinit var userShopData : red.man10.man10shop.usershop.ShopData
        lateinit var pl : Man10Shop

        //OPにのみメッセージを送る
        fun sendOP(message:String){
            for (p in Bukkit.getOnlinePlayers()){
                if (!p.hasPermission("man10shop.op"))continue

                p.sendMessage(message)
            }
        }
    }


    override fun onEnable() {
        // Plugin startup logic

        saveDefaultConfig()

        database = Database()
        merchantShopData = ShopData()
        userShopData = red.man10.man10shop.usershop.ShopData()

        database.mysqlQueue()
        pl = this

        //ショップデータの読み込み
        merchantShopData.loadShopData()
        userShopData.loadShopData()

        server.pluginManager.registerEvents(ShopEvent(),this)
        getCommand("createshop")!!.setExecutor(Commands())



    }

    override fun onDisable() {
        // Plugin shutdown logic
    }
}