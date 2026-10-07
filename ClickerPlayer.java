package ru.dualupa.clicker;

import java.sql.Timestamp;

public class ClickerPlayer {
    public String userId;
    public String nickname;
    public long coins;
    public long totalEarned;
    public int pickaxeLevel = 1;
    public int autoLevel = 0;
    public int luckLevel = 0;
    public int chestLevel = 0;
    public int prestige = 0;
    public int offlineHours = 8;
    public long weeklyEarned = 0;
    public String weekStart;
    public int dailyStreak = 0;
    public String lastDaily;
    public Timestamp lastSeen;
    public boolean dirty = false;

    public static final int MAX_PICKAXE = 50;
    public static final int MAX_AUTO = 50;
    public static final int MAX_LUCK = 20;
    public static final int MAX_CHEST = 20;
    public static final long PRESTIGE_THRESHOLD = 100000L;

    public double bonus() { return 1.0 + prestige * 0.1; }

    public long clickReward() {
        long base = pickaxeLevel * 10L;
        return Math.round(base * bonus());
    }

    public int critChancePercent() {
        return Math.min(60, luckLevel * 3);
    }

    public long offlineReward(long secondsAway) {
        if (autoLevel <= 0) return 0;
        long cap = offlineHours * 3600L;
        long sec = Math.min(secondsAway, cap);
        return Math.round(sec * autoLevel * bonus());
    }

    public String oreMaterial() {
        int l = pickaxeLevel;
        if (l <= 6)  return "STONE";
        if (l <= 13) return "COAL_ORE";
        if (l <= 19) return "IRON_ORE";
        if (l <= 25) return "GOLD_ORE";
        if (l <= 31) return "DIAMOND_ORE";
        if (l <= 38) return "EMERALD_ORE";
        if (l <= 45) return "ANCIENT_DEBRIS";
        return "CRYING_OBSIDIAN";
    }

    public long pricePickaxe() { return (long)(pickaxeLevel + 1) * (pickaxeLevel + 1) * 100L; }
    public long priceAuto()    { return (long)(autoLevel + 1) * (autoLevel + 1) * 200L; }
    public long priceLuck()    { return (long)(luckLevel + 1) * (luckLevel + 1) * 500L; }
    public long priceChest()   { return (long)(chestLevel + 1) * (chestLevel + 1) * 1000L; }
}
