package org.jabref.gui.collab;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javafx.util.Duration;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.util.FileUpdateListener;
import org.jabref.model.util.FileUpdateMonitor;
import org.jabref.preferences.PreferencesService;

import org.controlsfx.control.action.Action;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.jabref.gui.Globals.undoManager;

public class DatabaseChangeMonitor implements FileUpdateListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseChangeMonitor.class);

    private final BibDatabaseContext database;
    private final FileUpdateMonitor fileMonitor;
    private final List<DatabaseChangeListener> listeners;
    private final TaskExecutor taskExecutor;
    private final DialogService dialogService;
    private final PreferencesService preferencesService;
    private LibraryTab saveState;

    public DatabaseChangeMonitor(BibDatabaseContext database,
                                 FileUpdateMonitor fileMonitor,
                                 TaskExecutor taskExecutor,
                                 DialogService dialogService,
                                 PreferencesService preferencesService,
                                 LibraryTab.DatabaseNotification notificationPane,
                                 StateManager stateManager) {
        this.database = database;
        this.fileMonitor = fileMonitor;
        this.taskExecutor = taskExecutor;
        this.dialogService = dialogService;
        this.preferencesService = preferencesService;

        this.listeners = new ArrayList<>();

        this.database.getDatabasePath().ifPresent(path -> {
            try {
                fileMonitor.addListenerForFile(path, this);
            } catch (IOException e) {
                LOGGER.error("Error while trying to monitor {}", path, e);
            }
        });

        addListener(changes -> notificationPane.notify(
                IconTheme.JabRefIcons.SAVE.getGraphicNode(),
                Localization.lang("The library has been modified by another program."),
                List.of(new Action(Localization.lang("Dismiss changes"), event -> notificationPane.hide()),
                        new Action(Localization.lang("Review changes"), event -> {
                            DatabaseChangesResolverDialog databaseChangesResolverDialog = new DatabaseChangesResolverDialog(changes, database, Localization.lang("External Changes Resolver"));
                            var areAllChangesResolved = dialogService.showCustomDialogAndWait(databaseChangesResolverDialog);
                            // In case all changes of the file on disk are merged into the current in-memory file, the file on disk does not differ from the in-memory file
                            boolean areAllChangesAccepted = databaseChangesResolverDialog.areAllChangesAccepted();
                            saveState = stateManager.activeTabProperty().get().get();
                            final NamedCompound ce = new NamedCompound(Localization.lang("Merged external changes"));
                            changes.stream().filter(DatabaseChange::isAccepted).forEach(change -> change.applyChange(ce));
                            ce.end();
                            undoManager.addEdit(ce);
                            if (areAllChangesResolved.get()) {
                                if (areAllChangesAccepted) {
                                    saveState.resetChangedProperties();
                                } else {
                                    saveState.markBaseChanged();
                                }
                            }
                            notificationPane.hide();
                        })),
                Duration.ZERO));
    }

    @Override
    public void fileUpdated() {
        synchronized (database) {
            // File on disk has changed, thus look for notable changes and notify listeners in case there are such changes
            ChangeScanner scanner = new ChangeScanner(database, dialogService, preferencesService);
            BackgroundTask.wrap(scanner::scanForChanges)
                          .onSuccess(changes -> {
                              if (!changes.isEmpty()) {
                                  listeners.forEach(listener -> listener.databaseChanged(changes));
                              }
                          })
                          .onFailure(e -> LOGGER.error("Error while watching for changes", e))
                          .executeWith(taskExecutor);
        }
    }

    public void addListener(DatabaseChangeListener listener) {
        listeners.add(listener);
    }

    public void unregister() {
        database.getDatabasePath().ifPresent(file -> fileMonitor.removeListener(file, this));
    }
}
