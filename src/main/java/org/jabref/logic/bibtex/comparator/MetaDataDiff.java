package org.jabref.logic.bibtex.comparator;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.jabref.model.metadata.MetaData;
import org.jabref.preferences.PreferencesService;

public class MetaDataDiff {
    public enum DifferenceType {
        CONTENT_SELECTOR,
        DEFAULT_KEY_PATTERN,
        ENCODING,
        GENERAL_FILE_DIRECTORY,
        GROUPS_ALTERED,
        KEY_PATTERNS,
        LATEX_FILE_DIRECTORY,
        MODE,
        PROTECTED,
        SAVE_ACTIONS,
        SAVE_SORT_ORDER,
        USER_FILE_DIRECTORY
    }

    public record Difference(DifferenceType differenceType, Object originalObject, Object newObject) {
    }

    private final Optional<GroupDiff> groupDiff;
    private final MetaData originalMetaData;
    private final MetaData newMetaData;

    private MetaDataDiff(MetaData originalMetaData, MetaData newMetaData) {
        this.originalMetaData = originalMetaData;
        this.newMetaData = newMetaData;
        this.groupDiff = GroupDiff.compare(originalMetaData, newMetaData);
    }

    public static Optional<MetaDataDiff> compare(MetaData originalMetaData, MetaData newMetaData) {
        if (originalMetaData.equals(newMetaData)) {
            return Optional.empty();
        } else {
            return Optional.of(new MetaDataDiff(originalMetaData, newMetaData));
        }
    }

    private void addToListIfDiff(List<Difference> changes, DifferenceType differenceType, Object originalObject, Object newObject) {
        if (!Objects.equals(originalObject, newObject)) {
            changes.add(new Difference(differenceType, originalObject, newObject));
        }
    }

    /**
     * Should be kept in sync with {@link MetaData#equals(Object)}
     */
    public List<Difference> getDifferences(PreferencesService preferences) {
        List<Difference> changes = new ArrayList<>();
        addToListIfDiff(changes, DifferenceType.PROTECTED, originalMetaData.isProtected(), newMetaData.isProtected());
        addToListIfDiff(changes, DifferenceType.GROUPS_ALTERED, originalMetaData.getGroups(), newMetaData.getGroups());
        addToListIfDiff(changes, DifferenceType.ENCODING, originalMetaData.getEncoding(), newMetaData.getEncoding());
        addToListIfDiff(changes, DifferenceType.SAVE_SORT_ORDER, originalMetaData.getSaveOrder(), newMetaData.getSaveOrder());
        addToListIfDiff(changes, DifferenceType.KEY_PATTERNS,
                originalMetaData.getCiteKeyPatterns(preferences.getCitationKeyPatternPreferences().getKeyPatterns()),
                newMetaData.getCiteKeyPatterns(preferences.getCitationKeyPatternPreferences().getKeyPatterns()));
        addToListIfDiff(changes, DifferenceType.USER_FILE_DIRECTORY, originalMetaData.getUserFileDirectories(), newMetaData.getUserFileDirectories());
        addToListIfDiff(changes, DifferenceType.LATEX_FILE_DIRECTORY, originalMetaData.getLatexFileDirectories(), newMetaData.getLatexFileDirectories());
        addToListIfDiff(changes, DifferenceType.DEFAULT_KEY_PATTERN, originalMetaData.getDefaultCiteKeyPattern(), newMetaData.getDefaultCiteKeyPattern());
        addToListIfDiff(changes, DifferenceType.SAVE_ACTIONS, originalMetaData.getSaveActions(), newMetaData.getSaveActions());
        addToListIfDiff(changes, DifferenceType.MODE, originalMetaData.getMode(), newMetaData.getMode());
        addToListIfDiff(changes, DifferenceType.GENERAL_FILE_DIRECTORY, originalMetaData.getDefaultFileDirectory(), newMetaData.getDefaultFileDirectory());
        addToListIfDiff(changes, DifferenceType.CONTENT_SELECTOR, originalMetaData.getContentSelectors(), newMetaData.getContentSelectors());
        return changes;
    }

    public MetaData getNewMetaData() {
        return newMetaData;
    }

    public Optional<GroupDiff> getGroupDifferences() {
        return groupDiff;
    }

    @Override
    public String toString() {
        return "MetaDataDiff{" +
                "groupDiff=" + groupDiff +
                ", originalMetaData=" + originalMetaData +
                ", newMetaData=" + getNewMetaData() +
                '}';
    }
}
