//an enum for the possible states of the fsm that manages stalling

public enum StoreFsmState {

    RESTARTING(0x01), 
    CASCADING(0x02),
    STALLING(0x04);

    private final int code; //binary encoding of the state

    /**
     * Creates enum
     * @param code integer value of the state binary encoding
     */
    StoreFsmState(int code) {
        this.code = code;
    }

    /**
     * @return int encoding of the state
     */
    public int getCode() {
        return code;
    }
}
