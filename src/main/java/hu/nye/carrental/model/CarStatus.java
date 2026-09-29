package hu.nye.carrental.model;

public enum CarStatus {
    AVAILABLE("Available", "text-bg-success"),
    RENTED("Rented", "text-bg-warning"),
    MAINTENANCE("Maintenance", "text-bg-secondary");

    private final String label;
    private final String badgeClass;

    CarStatus(String label, String badgeClass) {
        this.label = label;
        this.badgeClass = badgeClass;
    }

    public String getLabel() {
        return label;
    }

    public String getBadgeClass() {
        return badgeClass;
    }
}
