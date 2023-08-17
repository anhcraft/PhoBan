package dev.anhcraft.phoban.gui;

import dev.anhcraft.config.annotations.Configurable;
import dev.anhcraft.palette.ui.Gui;
import dev.anhcraft.phoban.game.Stage;

import java.util.List;
import java.util.Map;

@Configurable(keyNamingStyle = Configurable.NamingStyle.TRAIN_CASE)
public class RoomSelectorGui extends Gui {
    public Map<Stage, List<String>> roomLoreTrailer;

    public List<String> roomLockedTrailer;
}
