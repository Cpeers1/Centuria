package org.asf.emuferal.enums.shops;

public enum ItemBuyStatus {

    SUCCESS(1), // successful purchase
    UNAVAILABLE(2), // item unavailable
    FULL_INVENTORY(3), // inventory full
    UNAFFORDABLE(4), // player cannot afford this
    LEVEL_LOCKED(6), // the player needs to be at a specific level to unlock this item
    UNKNOWN_ERROR(-1);

    private int status;

    private ItemBuyStatus(int status) {
        this.status = status;
    }

    public static ItemBuyStatus getByStatus(int status) {
        for (ItemBuyStatus state : ItemBuyStatus.values()) {
            if (state.status == status)
                return state;
        }
        return ItemBuyStatus.UNKNOWN_ERROR;
    }

    public int getStatus() {
        return status;
    }

}