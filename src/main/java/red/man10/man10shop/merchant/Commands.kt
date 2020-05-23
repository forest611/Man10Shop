package red.man10.man10shop.merchant

import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class Commands:CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {

        if (sender !is Player)return false

        if (label != "createshop")return false
        if (!sender.hasPermission("man10shop.op"))return false

        sender.openInventory(Bukkit.createInventory(null,27,"§e§lMan10ShopOp"))


        return false
    }
}