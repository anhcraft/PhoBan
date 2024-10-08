package dev.anhcraft.phoban.config;

import dev.anhcraft.config.annotations.Configurable;
import dev.anhcraft.config.annotations.Path;
import dev.anhcraft.config.annotations.PostHandler;
import dev.anhcraft.config.annotations.Validation;
import org.bukkit.Location;

import java.util.Collections;
import java.util.List;

@Configurable(keyNamingStyle = Configurable.NamingStyle.TRAIN_CASE)
public class MainConfig {
    public int debugLevel;

    @Path("free-ticket.enabled")
    public boolean freeTicketEnabled;

    @Path("free-ticket.amount")
    public int freeTicketAmount;

    @Path("free-ticket.every")
    public long freeTicketEvery;

    public long roomCreateCooldown;

    @Validation(notNull = true)
    public RoomSettings roomSettings;

    @Configurable(keyNamingStyle = Configurable.NamingStyle.TRAIN_CASE)
    public static class RoomSettings {
        public int waitingTime;

        public int intermissionTime;
    }

    @Validation(notNull = true)
    public Location spawnLocation;

    @Validation(notNull = true, notEmpty = true)
    public List<String> allowedCommands = Collections.emptyList();

    public String infoItemCmd;

    @PostHandler
    private void postHandler() {

    }
}
