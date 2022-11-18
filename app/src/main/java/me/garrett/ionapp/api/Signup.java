package me.garrett.ionapp.api;

public class Signup {

    private final int blockId, activityId;

    public Signup(int blockId, int activityId) {
        this.blockId = blockId;
        this.activityId = activityId;
    }

    public int getBlockId() {
        return blockId;
    }

    public int getActivityId() {
        return activityId;
    }

}
