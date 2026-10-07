package ru.dualupa.clicker;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ClickerCommand implements CommandExecutor {
    private final DuaLupaClicker plugin;
    private final ClickerGUI gui;

    public ClickerCommand(DuaLupaClicker plugin, ClickerGUI gui) {
        this.plugin = plugin;
        this.gui = gui;
    }

    @Override
    public boolean onCommand(CommandSender s, Command cmd, String label, String[] args) {
        if (!(s instanceof Player p)) return true;
        if (plugin.getManager().get(p) == null) { p.sendMessage("§cСначала зарегистрируйся на сайте DUA LUPA."); return true; }
        gui.open(p);
        return true;
    }
}
