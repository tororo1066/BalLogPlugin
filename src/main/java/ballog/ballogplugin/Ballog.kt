package ballog.ballogplugin

import net.ess3.api.events.UserBalanceUpdateEvent
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.event.ClickEvent
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerCommandPreprocessEvent
import org.bukkit.plugin.java.JavaPlugin
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.collections.HashMap

class Ballog : JavaPlugin(), Listener {

    lateinit var es : ExecutorService
    lateinit var plugin : Ballog
    lateinit var mysql : MySQLManager

    private val commandmap = HashMap<String,Pair<String,String>>()
    private val date = SimpleDateFormat("yyyy-MM-dd kk:mm:ss")
    private val prefix = "§f[§e§lBalLog§f]§r"
    private var balanceupdatewait = 0

    override fun onEnable() {
        plugin = this
        saveDefaultConfig()
        balanceupdatewait = config.getInt("balanceupdatewait")
        es = Executors.newCachedThreadPool()
        mysql = MySQLManager(this,"bal_log")
        server.pluginManager.registerEvents(this,this)
    }

    override fun onDisable() {
        es.shutdownNow()
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {

        when(label){
            "ballog"->{
                if (sender !is Player)return true
                if (!sender.hasPermission("ballog.user"))return true
                val page = if (args.isNotEmpty()) args[0].toIntOrNull()?:0 else 0

                sender.sendmsg("§d§l===========電子マネーの履歴==========")
                val list = getLog(sender.uniqueId,page)
                for (data in list){
                    val tag = if (data.isDeposit) "§a[入金]" else "§c[出金]"
                    sender.sendmsg("$tag §e${data.dateFormat} §e§l${format(data.amount)}円 §eserver §e§l${data.server} §eコマンド ${data.command}")
                }
                val previous = if (page!=0) {
                    text("${prefix}§b§l<<==前のページ ").clickEvent(ClickEvent.runCommand("/ballog ${page-1}"))
                }else text(prefix)

                val next = if (list.size == 10){
                    text("§b§l次のページ==>>").clickEvent(ClickEvent.runCommand("/ballog ${page+1}"))
                }else text("")

                sender.sendMessage(previous.append(next))
            }

            "ballogop"->{
                if (!sender.hasPermission("ballog.op"))return true

                val page = if (args.size >= 2) args[1].toIntOrNull()?:0 else 0
                val p = Bukkit.getOfflinePlayer(args[0])

                sender.sendMessage("§d§l===========電子マネーの履歴==========")
                val list = getLog(p.uniqueId,page)
                for (data in list){
                    val tag = if (data.isDeposit) "§a[入金]" else "§c[出金]"
                    sender.sendMessage("$tag §e${data.dateFormat} §e§l${format(data.amount)}円 §eserver §e§l${data.server} §eコマンド ${data.command}")
                }
                val previous = if (page!=0) {
                    text("${prefix}§b§l<<==前のページ ").clickEvent(ClickEvent.runCommand("/ballog ${page-1}"))
                }else text(prefix)

                val next = if (list.size == 10){
                    text("§b§l次のページ==>>").clickEvent(ClickEvent.runCommand("/ballog ${page+1}"))
                }else text("")

                sender.sendMessage(previous.append(next))
            }
        }
        return true
    }


    @EventHandler
    fun balanceupdate(e : UserBalanceUpdateEvent) {
        val cal = Calendar.getInstance()
        cal.time = Date()
        for (i in 1..balanceupdatewait){
            cal.time.seconds = cal.time.seconds-1
            if (commandmap.containsKey(date.format(cal.time))){
                if (commandmap[date.format(cal.time)]?.first != e.player.name)return
                val isdeposit = e.oldBalance.subtract(e.newBalance) >= BigDecimal(0) //trueがdeposit、falseがwithdraw
                es.execute {
                    mysql.execute("INSERT INTO bal_log (player, uuid, amount, server, deposit, command) VALUES " +
                            "('${e.player.name}', '${e.player.uniqueId}', '${if (isdeposit) e.oldBalance.subtract(e.newBalance) else e.newBalance.subtract(e.oldBalance)}', '${server.name}', ${if (isdeposit) 1 else 0}, '${commandmap[date.format(cal.time)]?.second}');")
                }
            }
        }

    }

    @EventHandler
    fun usecommand(e : PlayerCommandPreprocessEvent){
        val cal = Calendar.getInstance()
        cal.time = Date()

        commandmap[date.format(cal.time)] = Pair(e.player.name,e.message)
    }

    fun getLog(player : UUID, page : Int) : MutableList<Log>{
        val rs = mysql.query("select * from bal_log where uuid = '$player' order by id desc Limit 10 offset ${(page)*10};")?:return Collections.emptyList()

        val list = mutableListOf<Log>()

        while (rs.next()){

            val data = Log()

            data.isDeposit = rs.getInt("deposit") == 1
            data.amount = rs.getDouble("amount")
            data.dateFormat = date.format(rs.getTimestamp("date"))
            data.command = rs.getString("command")
            data.server = rs.getString("server")

            list.add(data)
        }
        rs.close()
        mysql.close()

        return list
    }

    fun Player.sendmsg(s : String){
        this.sendMessage(prefix + s)
    }

    fun format(double: Double):String{
        return String.format("%,.0f",double)
    }

}

class Log{
    var isDeposit = true
    var amount = 0.0
    var dateFormat = ""
    var command = ""
    var server = ""
}