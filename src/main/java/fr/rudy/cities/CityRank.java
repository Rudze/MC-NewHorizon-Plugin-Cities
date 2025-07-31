package fr.rudy.cities;

public enum CityRank {
    LEADER("Chef"),
    COLEADER("Sous-chef"),
    MEMBER("Membre");

    private final String displayName;

    CityRank(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static CityRank fromString(String value) {
        for (CityRank rank : values()) {
            if (rank.name().equalsIgnoreCase(value)) {
                return rank;
            }
        }
        return null;
    }
}