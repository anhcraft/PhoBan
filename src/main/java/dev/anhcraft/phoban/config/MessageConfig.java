package dev.anhcraft.phoban.config;

import dev.anhcraft.config.annotations.Configurable;
import dev.anhcraft.config.annotations.Validation;
import dev.anhcraft.phoban.game.Difficulty;
import dev.anhcraft.phoban.game.Stage;

import java.util.Map;

@Configurable(keyNamingStyle = Configurable.NamingStyle.TRAIN_CASE)
public class MessageConfig {
    @Validation(notNull = true)
    public String prefix;

    @Validation(notNull = true)
    public Map<Stage, String> stage;

    @Validation(notNull = true)
    public Map<Difficulty, String> difficulty;

    public String alreadyJoined;
    public String notJoined;
    public String joinMessage;
    public String leaveMessage;
    public String waitingCooldown;
    public String gameStarted;
    public String endingCooldown;
}
