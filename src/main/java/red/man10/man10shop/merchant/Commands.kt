package red.man10.man10shop.merchant

import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import red.man10.man10shop.Man10Shop
import red.man10.man10shop.Man10Shop.Companion.OP

class Commands:CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {

        if (sender !is Player)return false

        if (label != "createshop")return false
        if (!sender.hasPermission(OP))return false

        if (!Man10Shop.pluginEnable)return false


        sender.openInventory(Bukkit.createInventory(null,27,"§e§lMan10ShopOp"))


        return false
    }
}