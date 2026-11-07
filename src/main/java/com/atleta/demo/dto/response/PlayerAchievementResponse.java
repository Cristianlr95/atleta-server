package com.atleta.demo.dto.response;

import com.atleta.demo.enums.AchievementTier;

/** A live, career-based competitive achievement and its next milestone. */
public class PlayerAchievementResponse {
    private String code;
    private String title;
    private String description;
    private String metricLabel;
    private long currentValue;
    private int nextThreshold;
    private int progressPercent;
    private AchievementTier tier;
    private boolean unlocked;

    public PlayerAchievementResponse() { }

    public PlayerAchievementResponse(String code, String title, String description, String metricLabel,
                                     long currentValue, int nextThreshold, int progressPercent,
                                     AchievementTier tier, boolean unlocked) {
        this.code = code;
        this.title = title;
        this.description = description;
        this.metricLabel = metricLabel;
        this.currentValue = currentValue;
        this.nextThreshold = nextThreshold;
        this.progressPercent = progressPercent;
        this.tier = tier;
        this.unlocked = unlocked;
    }

    public String getCode() { return code; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getMetricLabel() { return metricLabel; }
    public long getCurrentValue() { return currentValue; }
    public int getNextThreshold() { return nextThreshold; }
    public int getProgressPercent() { return progressPercent; }
    public AchievementTier getTier() { return tier; }
    public boolean isUnlocked() { return unlocked; }
}
