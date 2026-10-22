package entity;

public class ClubGrowthRow {
    private final int month; // 1-12
    private final int newMembers;
    private final int cumulative;
    private final double growthPercent;

    public ClubGrowthRow(int month, int newMembers, int cumulative, double growthPercent) {
        this.month = month;
        this.newMembers = newMembers;
        this.cumulative = cumulative;
        this.growthPercent = growthPercent;
    }

    public int getMonth() { return month; }
    public int getNewMembers() { return newMembers; }
    public int getCumulative() { return cumulative; }
    public double getGrowthPercent() { return growthPercent; }
}
