package red.man10.man10shop

import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import red.man10.man10shop.adminshop.Commands
import red.man10.man10shop.adminshop.ShopData
import red.man10.man10shop.adminshop.ShopEvent
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue

class Man10Shop : JavaPlugin() {

    companion object{

        var shopMap = ConcurrentHashMap<Int,ShopData.AdminShopData>()

        var mysqlQueue = LinkedBlockingQueue<String>()

        lateinit var database: Database
        lateinit var adminShopData: ShopData
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
        adminShopData = ShopData()

        database.mysqlQueue()
        pl = this

        adminShopData.loadShopData()

        server.pluginManager.registerEvents(ShopEvent(),this)
        getCommand("createshop")!!.setExecutor(Commands())



    }

    override fun onDisable() {
        // Plugin shutdown logic
    }
}