package io.mundt;

public class CPU {
    public final Memory memory;

    public short pc, sp;

    public byte a, x, y;

    public boolean carryFlag, zeroFlag, interruptDisableFlag, decimalModeFlag, breakCommandFlag, overflowFlag, negativeFlag;

    public CPU(Memory memory) {
        this.memory = memory;
    }

    public void reset() {
        pc = memory.readWord((short) 0xFFFC);
        sp = (short) 0x01FF;
        a = x = y = 0;
        carryFlag = zeroFlag = interruptDisableFlag = decimalModeFlag = breakCommandFlag = overflowFlag = negativeFlag = false;
    }

    public byte fetchByte() {
        byte data = memory.readByte(pc);
        pc++;
        return data;
    }

    public int step() {
        byte opcode = fetchByte();
        switch (opcode) {
            default -> throw new RuntimeException("Unknown opcode: " + opcode);
        }
    }
}
