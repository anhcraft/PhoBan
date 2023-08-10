package dev.anhcraft.phoban;

import dev.anhcraft.config.annotations.Configurable;

@Configurable(keyNamingStyle = Configurable.NamingStyle.TRAIN_CASE)
public class MessageConfig {
    public String prefix;

    public MailNotification mailNotification;

    @Configurable(keyNamingStyle = Configurable.NamingStyle.TRAIN_CASE)
    public static class MailNotification {
        public String[] onJoinPresent;
        public String[] onJoinEmpty;
        public String onReceive;
        public String onRead;
    }

    public String insufficientBalance;
    public String tooManyItemsAttached;
    public String titleTooLong;
    public String subjectTooLong;
    public String contentTooLong;
}
