package ru.dualupa.clicker;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ClickerManager {
    private final DuaLupaClicker plugin;
    private final Database db;
    private final Map<UUID, ClickerPlayer> cache = new ConcurrentHashMap<>();

    public boolean bossActive = false;
    public final Map<UUID, Integer> bossHits = new ConcurrentHashMap<>();
    private boolean weeklyGiven = false;

    public ClickerManager(DuaLupaClicker plugin, Database db) {
        this.plugin = plugin;
        this.db = db;
    }

    public ClickerPlayer get(Player p) { return cache.get(p.getUniqueId()); }

    public ClickerPlayer load(Player p) {
        try {
            String userId = null;
            try (PreparedStatement ps = db.prepare("SELECT id FROM users WHERE LOWER(nickname)=LOWER(?)")) {
                ps.setString(1, p.getName());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) userId = rs.getString(1);
            }
            if (userId == null) return null;

            ClickerPlayer cp = new ClickerPlayer();
            cp.userId = userId;
            cp.nickname = p.getName();

            try (PreparedStatement ps = db.prepare("SELECT * FROM clicker WHERE user_id=?")) {
                ps.setString(1, userId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    cp.coins = rs.getLong("coins");
                    cp.totalEarned = rs.getLong("total_earned");
                    cp.pickaxeLevel = rs.getInt("pickaxe_level");
                    cp.autoLevel = rs.getInt("auto_level");
                    cp.luckLevel = rs.getInt("luck_level");
                    cp.chestLevel = rs.getInt("chest_level");
                    cp.prestige = rs.getInt("prestige");
                    cp.offlineHours = rs.getInt("offline_hours");
                    cp.weeklyEarned = rs.getLong("weekly_earned");
                    cp.weekStart = rs.getDate("week_start") != null ? rs.getDate("week_start").toString() : null;
                    cp.dailyStreak = rs.getInt("daily_streak");
                    cp.lastDaily = rs.getDate("last_daily") != null ? rs.getDate("last_daily").toString() : null;
                    cp.lastSeen = rs.getTimestamp("last_seen");
                } else {
                    try (PreparedStatement ins = db.prepare("INSERT INTO clicker (user_id, week_start) VALUES (?, CURRENT_DATE)")) {
                        ins.setString(1, userId);
                        ins.executeUpdate();
                    }
                    cp.lastSeen = new Timestamp(System.currentTimeMillis());
                }
            }

            String thisWeek = getWeekStart();
            if (cp.weekStart == null || !cp.weekStart.equals(thisWeek)) {
                cp.weeklyEarned = 0;
                cp.weekStart = thisWeek;
                cp.dirty = true;
            }

            if (cp.lastSeen != null && cp.autoLevel > 0) {
                long sec = (System.currentTimeMillis() - cp.lastSeen.getTime()) / 1000L;
                long reward = cp.offlineReward(sec);
                if (reward > 0) {
                    cp.coins += reward;
                    cp.totalEarned += reward;
                    cp.weeklyEarned += reward;
                }
            }
            cp.lastSeen = new Timestamp(System.currentTimeMillis());
            cp.dirty = true;

            cache.put(p.getUniqueId(), cp);
            return cp;
        } catch (Exception e) {
            plugin.getLogger().warning("load error: " + e.getMessage());
            return null;
        }
    }

    public void unload(Player p) {
        save(p.getUniqueId());
        cache.remove(p.getUniqueId());
    }

    public void save(UUID id) {
        ClickerPlayer cp = cache.get(id);
        if (cp == null || !cp.dirty) return;
        try (PreparedStatement ps = db.prepare(
            "UPDATE clicker SET coins=?, total_earned=?, pickaxe_level=?, auto_level=?, luck_level=?, chest_level=?, prestige=?, offline_hours=?, weekly_earned=?, week_start=?, daily_streak=?, last_daily=?, last_seen=NOW() WHERE user_id=?")) {
            ps.setLong(1, cp.coins);
            ps.setLong(2, cp.totalEarned);
            ps.setInt(3, cp.pickaxeLevel);
            ps.setInt(4, cp.autoLevel);
            ps.setInt(5, cp.luckLevel);
            ps.setInt(6, cp.chestLevel);
            ps.setInt(7, cp.prestige);
            ps.setInt(8, cp.offlineHours);
            ps.setLong(9, cp.weeklyEarned);
            ps.setDate(10, cp.weekStart != null ? java.sql.Date.valueOf(cp.weekStart) : java.sql.Date.valueOf(LocalDate.now()));
            ps.setInt(11, cp.dailyStreak);
            ps.setDate(12, cp.lastDaily != null ? java.sql.Date.valueOf(cp.lastDaily) : null);
            ps.setString(13, cp.userId);
            ps.executeUpdate();
            cp.dirty = false;
        } catch (Exception e) {
            plugin.getLogger().warning("save error: " + e.getMessage());
        }
    }

    public void saveAll() {
        for (UUID id : cache.keySet()) save(id);
    }

    public long handleClick(Player p) {
        ClickerPlayer cp = get(p);
        if (cp == null) return 0;
        long base = cp.clickReward();
        boolean crit = Math.random() * 100 < cp.critChancePercent();
        long gain = crit ? base * 2 : base;
        if (Math.random() < 0.0001) giveRubles(p, 10);
        cp.coins += gain;
        cp.totalEarned += gain;
        cp.weeklyEarned += gain;
        cp.dirty = true;
        return gain;
    }

    private void giveRubles(Player p, int amount) {
        try (PreparedStatement ps = db.prepare(
            "UPDATE users SET donate_balance = donate_balance + ? WHERE LOWER(nickname)=LOWER(?)")) {
            ps.setInt(1, amount);
            ps.setString(2, p.getName());
            ps.executeUpdate();
            p.sendMessage("§d§l✦ ЗОЛОТОЙ БЛОК! §f+10 ₽ на сайт!");
        } catch (Exception ignored) {}
    }

    public boolean upgrade(Player p, String type) {
        ClickerPlayer cp = get(p);
        if (cp == null) return false;
        long price;
        switch (type) {
            case "pickaxe": if (cp.pickaxeLevel >= ClickerPlayer.MAX_PICKAXE) return false; price = cp.pricePickaxe(); break;
            case "auto":    if (cp.autoLevel >= ClickerPlayer.MAX_AUTO) return false; price = cp.priceAuto(); break;
            case "luck":    if (cp.luckLevel >= ClickerPlayer.MAX_LUCK) return false; price = cp.priceLuck(); break;
            case "chest":   if (cp.chestLevel >= ClickerPlayer.MAX_CHEST) return false; price = cp.priceChest(); break;
            default: return false;
        }
        if (cp.coins < price) return false;
        cp.coins -= price;
        switch (type) {
            case "pickaxe": cp.pickaxeLevel++; break;
            case "auto": cp.autoLevel++; break;
            case "luck": cp.luckLevel++; break;
            case "chest": cp.chestLevel++; break;
        }
        cp.dirty = true;
        return true;
    }

    public boolean prestige(Player p) {
        ClickerPlayer cp = get(p);
        if (cp == null) return false;
        if (cp.coins < ClickerPlayer.PRESTIGE_THRESHOLD) return false;
        cp.prestige++;
        cp.coins = 0;
        cp.pickaxeLevel = 1;
        cp.autoLevel = 0;
        cp.luckLevel = 0;
        cp.chestLevel = 0;
        cp.dirty = true;
        return true;
    }

    public int claimDaily(Player p) {
        ClickerPlayer cp = get(p);
        if (cp == null) return -1;
        String today = LocalDate.now().toString();
        if (today.equals(cp.lastDaily)) return -1;
        LocalDate yesterday = LocalDate.now().minusDays(1);
        if (cp.lastDaily != null && cp.lastDaily.equals(yesterday.toString())) {
            cp.dailyStreak = Math.min(cp.dailyStreak + 1, 7);
        } else {
            cp.dailyStreak = 1;
        }
        if (cp.dailyStreak == 7) {
            grantCase(cp.userId, "rare");
            cp.dailyStreak = 0;
            cp.lastDaily = today;
            cp.dirty = true;
            return -2;
        }
        long[] rewards = {100, 250, 500, 1000, 2500, 5000};
        long r = rewards[Math.min(cp.dailyStreak - 1, rewards.length - 1)];
        cp.coins += r;
        cp.totalEarned += r;
        cp.lastDaily = today;
        cp.dirty = true;
        return (int) r;
    }

    public boolean claimDailyAvailable(Player p) {
        ClickerPlayer cp = get(p);
        if (cp == null) return false;
        return !LocalDate.now().toString().equals(cp.lastDaily);
    }

    private void grantCase(String userId, String caseType) {
        try (PreparedStatement ps = db.prepare(
            "INSERT INTO user_cases (user_id, case_type, source) VALUES (?, ?, 'clicker')")) {
            ps.setString(1, userId);
            ps.setString(2, caseType);
            ps.executeUpdate();
        } catch (Exception e) {
            plugin.getLogger().warning("grantCase error: " + e.getMessage());
        }
    }

    public void checkWeeklyReward() {
        LocalDate now = LocalDate.now();
        if (now.getDayOfWeek().getValue() != 7) return;
        java.time.LocalTime time = java.time.LocalTime.now();
        if (time.getHour() != 23 || time.getMinute() < 58) return;
        if (weeklyGiven) return;
        weeklyGiven = true;

        try (PreparedStatement ps = db.prepare(
            "SELECT user_id FROM clicker WHERE weekly_earned > 0 ORDER BY weekly_earned DESC LIMIT 3")) {
            ResultSet rs = ps.executeQuery();
            int place = 1;
            while (rs.next()) {
                String uid = rs.getString(1);
                if (place == 1) grantCase(uid, "epic");
                else if (place == 2) grantCase(uid, "rare");
                else { grantCase(uid, "common"); grantCase(uid, "common"); }
                place++;
            }
        } catch (Exception e) {}

        try (PreparedStatement ps = db.prepare("UPDATE clicker SET weekly_earned=0, week_start=CURRENT_DATE")) {
            ps.executeUpdate();
        } catch (Exception ignored) {}
    }

    public void startBossEvent() {
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage("§d§l⚔ КАМЕННЫЙ ГОЛЕМ ПОЯВИЛСЯ!");
        Bukkit.broadcastMessage("§7Кликай по нему 30 секунд. Топ-1 — редкий кейс.");
        Bukkit.broadcastMessage("§7Заходи: §f/clicker");
        Bukkit.broadcastMessage("");
        bossActive = true;

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            bossActive = false;
            if (!bossHits.isEmpty()) {
                UUID top = bossHits.entrySet().stream().max(Map.Entry.comparingByValue()).get().getKey();
                Player p = Bukkit.getPlayer(top);
                if (p != null && cache.containsKey(top)) {
                    grantCase(cache.get(top).userId, "rare");
                    p.sendMessage("§d§l🏆 Ты победил босса! Редкий кейс ждёт на сайте.");
                    p.playSound(p.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1, 1);
                }
                bossHits.clear();
            }
        }, 20L * 30);
    }

    public String getWeekStart() {
        LocalDate now = LocalDate.now();
        LocalDate monday = now.minusDays(now.getDayOfWeek().getValue() - 1);
        return monday.toString();
    }

    public String buildTopText() {
        StringBuilder sb = new StringBuilder();
        try (PreparedStatement ps = db.prepare(
            "SELECT u.nickname, c.weekly_earned FROM clicker c JOIN users u ON u.id=c.user_id WHERE c.weekly_earned>0 ORDER BY c.weekly_earned DESC LIMIT 10")) {
            ResultSet rs = ps.executeQuery();
            int i = 1;
            while (rs.next()) {
                sb.append("§e").append(i).append(". §f").append(rs.getString(1)).append(" §7— §6").append(rs.getLong(2)).append("\n");
                i++;
            }
            if (i == 1) sb.append("§7Пока никого нет");
        } catch (Exception e) {
            sb.append("§7Ошибка загрузки");
        }
        return sb.toString();
    }
}
