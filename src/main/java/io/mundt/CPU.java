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

    public short fetchWord() {
        short data = memory.readWord(pc);
        pc += 2;
        return data;
    }

    public int step() {
        byte opcode = fetchByte();
        switch (opcode) {
            case (byte) 0xA9 -> { // LDA #nn
                a = fetchByte();
                zeroFlag = a == 0;
                negativeFlag = (a & 0x80) != 0;
                return 2;
            }
            case (byte) 0xA5 -> { // LDA nn
                a = memory.readByte(fetchByte());
                zeroFlag = a == 0;
                negativeFlag = (a & 0x80) != 0;
                return 3;
            }
            case (byte) 0xB5 -> { // LDA nn,X
                a = memory.readByte((short) (fetchByte() + x));
                zeroFlag = a == 0;
                negativeFlag = (a & 0x80) != 0;
                return 4;
            }
            case (byte) 0xAD -> { // LDA nnnn
                a = memory.readByte(fetchWord());
                zeroFlag = a == 0;
                negativeFlag = (a & 0x80) != 0;
                return 4;
            }
            case (byte) 0xBD -> { // LDA nnnn,X
                short absoluteAddress = fetchWord();
                short effectiveAddress = (short) (absoluteAddress + x);
                a = memory.readByte(effectiveAddress);
                zeroFlag = a == 0;
                negativeFlag = (a & 0x80) != 0;
                if ((effectiveAddress & 0xFF00) != (absoluteAddress & 0xFF00)) {
                    return 5;
                } else {
                    return 4;
                }
            }
            case (byte) 0xB9 -> { // LDA nnnn,Y
                short absoluteAddress = fetchWord();
                short effectiveAddress = (short) (absoluteAddress + y);
                a = memory.readByte(effectiveAddress);
                zeroFlag = a == 0;
                negativeFlag = (a & 0x80) != 0;
                if ((effectiveAddress & 0xFF00) != (absoluteAddress & 0xFF00)) {
                    return 5;
                } else {
                    return 4;
                }
            }
            case (byte) 0xA1 -> { // LDA (nn,X)
                short indirectAddress = (short) (fetchByte() + x);
                short effectiveAddress = memory.readWord(indirectAddress);
                a = memory.readByte(effectiveAddress);
                zeroFlag = a == 0;
                negativeFlag = (a & 0x80) != 0;
                return 6;
            }
            case (byte) 0xB1 -> { // LDA (nn),Y
                short indirectAddress = fetchByte();
                short absoluteAddress = memory.readWord(indirectAddress);
                short effectiveAddress = (short) (absoluteAddress + y);
                a = memory.readByte(effectiveAddress);
                zeroFlag = a == 0;
                negativeFlag = (a & 0x80) != 0;
                if ((effectiveAddress & 0xFF00) != (absoluteAddress & 0xFF00)) {
                    return 6;
                } else {
                    return 5;
                }
            }
            default -> throw new RuntimeException("Unknown opcode: " + opcode);
        }
    }
}
