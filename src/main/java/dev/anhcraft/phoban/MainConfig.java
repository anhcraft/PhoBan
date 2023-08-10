package dev.anhcraft.phoban;

import dev.anhcraft.config.annotations.Configurable;

@Configurable(keyNamingStyle = Configurable.NamingStyle.TRAIN_CASE)
public class MainConfig {
    public String prefix;
    public int debugLevel;
    public PlayerMailSettings playerMail;

    @Configurable(keyNamingStyle = Configurable.NamingStyle.TRAIN_CASE)
    public static class PlayerMailSettings {
        public int maxItemsAttachment;
        public int baseCost;
        public int costPerItem;
        public int costPerTitleCharacter;
        public int costPerSubjectCharacter;
        public int costPerContentCharacter;
    }

    public NotificationSettings notification;

    @Configurable(keyNamingStyle = Configurable.NamingStyle.TRAIN_CASE)
    public static class NotificationSettings {
        public boolean noticeReceiveMail;
        public boolean noticeReadMail;
    }
}
