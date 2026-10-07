package ru.dualupa.clicker;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ClickerAdminCommand implements CommandExecutor {
    private final DuaLupaClicker plugin;
    private final ClickerManager manager;

    public ClickerAdminCommand(DuaLupaClicker plugin, ClickerManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender s, Command c, String l, String[] args) {
        if (args.length == 0) { s.sendMessage("/cadmin boss — босс"); s.sendMessage("/cadmin weekly — награды недели"); return true; }
        if (args[0].equals("boss")) { manager.startBossEvent(); s.sendMessage("§aБосс запущен"); }
        else if (args[0].equals("weekly")) { manager.checkWeeklyReward(); s.sendMessage("§aНаграды выданы"); }
        return true;
    }
}
