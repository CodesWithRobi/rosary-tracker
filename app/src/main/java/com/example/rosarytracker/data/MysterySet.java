package com.example.rosarytracker.data;

/**
 * Represents the four sets of Rosary mysteries.
 * Order matters: JOYFUL(0-4), LUMINOUS(5-9), SORROWFUL(10-14), GLORIOUS(15-19).
 */
public enum MysterySet {

    JOYFUL("Joyful", new String[]{
            "The Annunciation",
            "The Visitation",
            "The Nativity",
            "The Presentation in the Temple",
            "The Finding in the Temple"
    }),
    LUMINOUS("Luminous", new String[]{
            "The Baptism of Christ",
            "The Wedding Feast at Cana",
            "Proclamation of the Kingdom",
            "The Transfiguration",
            "Institution of the Eucharist"
    }),
    SORROWFUL("Sorrowful", new String[]{
            "The Agony in the Garden",
            "The Scourging at the Pillar",
            "The Crowning with Thorns",
            "The Carrying of the Cross",
            "The Crucifixion and Death"
    }),
    GLORIOUS("Glorious", new String[]{
            "The Resurrection",
            "The Ascension",
            "The Descent of the Holy Spirit",
            "The Assumption",
            "The Coronation of Mary"
    });

    private final String displayName;
    private final String[] mysteries;

    MysterySet(String displayName, String[] mysteries) {
        this.displayName = displayName;
        this.mysteries = mysteries;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns the 5 mystery names for this set.
     */
    public String[] getMysteries() {
        return mysteries;
    }

    /**
     * Returns the mystery name at the given local index (0-4).
     */
    public String getMystery(int index) {
        return mysteries[index];
    }

    /**
     * Returns the total number of mysteries across all sets (always 20).
     */
    public static int getTotalCount() {
        return values().length * 5;
    }

    /**
     * Returns the MysterySet for a given global index (0-19).
     * 0-4→JOYFUL, 5-9→LUMINOUS, 10-14→SORROWFUL, 15-19→GLORIOUS.
     */
    public static MysterySet fromGlobalIndex(int globalIndex) {
        return values()[globalIndex / 5];
    }

    /**
     * Returns the local index (0-4) within a set for a given global index (0-19).
     */
    public static int toLocalIndex(int globalIndex) {
        return globalIndex % 5;
    }

    /**
     * Returns the global index (0-19) from a set and local index.
     */
    public static int toGlobalIndex(MysterySet set, int localIndex) {
        return set.ordinal() * 5 + localIndex;
    }
}
