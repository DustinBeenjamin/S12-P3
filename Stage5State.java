/**
 *  Each time the hardware object is clocked, it needs to save information about
 *  the last stage. To do that, it instantiates one of these objects from the stage 5
 *  information. When clocking is done, the driving class uses an Array List of these objects
 *  to determine performance metrics of the simulation.
 */

public class Stage5State {
    public int pc;
    public Opcodes opcode;

    /**
     * Instatiate an object that holds stage 5 data
     * @param pc the opcode that stage 5 has pipelined
     * @param op the pc counter that loaded the instruction in stage 5
     */ 
    public Stage5State(int pc, Opcodes op) {
        this.pc = pc;
        this.opcode = op;
    }

    /**
     * @return A string that can be printed with details about stage 5 information
     */
    public String toString() {
        return "0x" + Integer.toHexString(pc) + " " + opcode.toString(); 
    }

    /**
     * Determine if two Stage5State objects are equal
     * @param s2 another Stage5State object that this one should be compared to 
     * @return true if this is equal to s2, false otherwise
     */
    public boolean equals(Stage5State s2) {
        return ((this.pc == s2.pc) && (this.opcode.getCode() == s2.opcode.getCode()));
    }
}
