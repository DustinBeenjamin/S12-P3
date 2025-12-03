/**
 * An enum for opcode related information. Makes actual code more legible, as reader does not need to know the binary representation of each opcode.
 */

public enum Opcodes {
    JMP(0x00),
    JN(0x01),
    JZ(0x02),
    LOAD(0x04),
    STORE(0x05),
    LOADI(0x06),
    STOREI(0x07),
    AND(0x08),
    OR(0x09),
    ADD(0x0A),
    SUB(0x0B),
    HALT(0x0F);

    //Binary representation of the opcode
    private final int code;

    Opcodes(int code) {
        this.code = code;
    }

    /**
     * get opcode binary from an enum object
     * @return opcode binary as int
     */
    public int getCode() {
        return code;
    }

    /**
     * get opcode object from opcode binary
     * @param code opcode binary as int
     * @return corresponding opcode object
     */
    public static Opcodes fromCode(int code) {
        for (Opcodes op : Opcodes.values()) {
            if (op.code == code) {
                return op;
            }
        }
        return null;
    }

    /**
     * @param code opcode binary as int
     * @return opcode description as string
     */
    public static String getDescription(int code) {
        Opcodes op = fromCode(code);
        return (op != null) ? op.name() : String.format("Unknown opcode: 0x%02X", code);
    }

    /**
     * @param code opcode binary as int
     * @return true if code corresponds to an opcode, false otherwise
     */
    public static boolean isValidOpcode(int code) {
        for (Opcodes op : Opcodes.values()) {
            if (op.code == code) {
                return true;
            }
        }
        return false;
    }

}
