package com.example.tunevaultfx.chrome;

import com.example.tunevaultfx.core.Song;
import com.example.tunevaultfx.search.SearchRecentItem;
import com.example.tunevaultfx.util.CellStyleKit;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** List cell rendering for the top-bar search typeahead. */
final class SearchDropdownListCell extends ListCell<Object> {

    SearchDropdownListCell() {}

    @Override
    protected void updateItem(Object item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            setText(null);
            setGraphic(null);
            setStyle("-fx-background-color: transparent;");
            return;
        }
        if (item == SearchDropdownRows.CLEAR_SENTINEL) {
            setText(null);
            Label clear = new Label("Clear recent searches");
            clear.getStyleClass().add("search-dropdown-clear-label");
            HBox row = new HBox(clear);
            row.getStyleClass().add("search-dropdown-clear-row");
            row.setMaxWidth(Double.MAX_VALUE);
            row.setPickOnBounds(true);
            setGraphic(row);
            setStyle("-fx-background-color: transparent;");
            return;
        }
        if (item == SearchDropdownRows.NO_RESULTS_SENTINEL) {
            setText(null);
            Label l = new Label("No matches");
            l.getStyleClass().add("search-dropdown-no-results-label");
            setGraphic(l);
            setStyle("-fx-background-color: transparent;");
            return;
        }
        if (item instanceof SearchRecentItem recent) {
            boolean isSong = recent.getType() == SearchRecentItem.Type.SONG;
            StackPane icon =
                    CellStyleKit.iconBox(
                            isSong ? "♫" : "◎",
                            isSong ? CellStyleKit.Palette.PURPLE : CellStyleKit.Palette.ROSE,
                            !isSong);
            VBox text =
                    isSong
                            ? CellStyleKit.songTextBoxWithKind(
                                    recent.getPrimaryText(),
                                    recent.getSecondaryText(),
                                    null,
                                    null)
                            : CellStyleKit.textBox(
                                    recent.getPrimaryText(), recent.getSecondaryText());
            HBox.setHgrow(text, Priority.ALWAYS);
            HBox row = CellStyleKit.row(icon, text);
            CellStyleKit.addHover(row);
            setText(null);
            setGraphic(row);
            setStyle("-fx-background-color: transparent; -fx-padding: 2 0 2 0;");
            return;
        }
        if (item instanceof Song song) {
            StackPane icon = CellStyleKit.iconBox("♫", CellStyleKit.Palette.PURPLE, false);
            VBox text =
                    CellStyleKit.songTextBoxWithKind(
                            song.title(), song.artist(), song.genre(), null);
            HBox.setHgrow(text, Priority.ALWAYS);
            HBox row = CellStyleKit.row(icon, text);
            CellStyleKit.addHover(row);
            setText(null);
            setGraphic(row);
            setStyle("-fx-background-color: transparent; -fx-padding: 2 0 2 0;");
            return;
        }
        if (item instanceof SearchDropdownRows.ArtistHit ah) {
            StackPane icon = CellStyleKit.iconBox("◎", CellStyleKit.Palette.ROSE, true);
            VBox text = CellStyleKit.textBox(ah.name(), "Artist");
            HBox.setHgrow(text, Priority.ALWAYS);
            HBox row = CellStyleKit.row(icon, text);
            CellStyleKit.addHover(row);
            setText(null);
            setGraphic(row);
            setStyle("-fx-background-color: transparent; -fx-padding: 2 0 2 0;");
        }
    }
}
