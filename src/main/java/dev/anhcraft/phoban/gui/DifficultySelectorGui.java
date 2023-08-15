package dev.anhcraft.phoban.gui;

import dev.anhcraft.config.annotations.Configurable;
import dev.anhcraft.palette.ui.Gui;

import java.util.List;

@Configurable(keyNamingStyle = Configurable.NamingStyle.TRAIN_CASE)
public class DifficultySelectorGui extends Gui {
    public List<String> roomLoreTrailer;
}
