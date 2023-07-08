package io.mundt.bender;

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

    public int step() throws UnknownOpcodeException {
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
            case (byte) 0xA2 -> { // LDX #nn
                x = fetchByte();
                zeroFlag = x == 0;
                negativeFlag = (x & 0x80) != 0;
                return 2;
            }
            case (byte) 0xA6 -> { // LDX nn
                x = memory.readByte(fetchByte());
                zeroFlag = x == 0;
                negativeFlag = (x & 0x80) != 0;
                return 3;
            }
            case (byte) 0xB6 -> { // LDX nn,Y
                x = memory.readByte((short) (fetchByte() + y));
                zeroFlag = x == 0;
                negativeFlag = (x & 0x80) != 0;
                return 4;
            }
            case (byte) 0xAE -> { // LDX nnnn
                x = memory.readByte(fetchWord());
                zeroFlag = x == 0;
                negativeFlag = (x & 0x80) != 0;
                return 4;
            }
            case (byte) 0xBE -> { // LDX nnnn,Y
                short absoluteAddress = fetchWord();
                short effectiveAddress = (short) (absoluteAddress + y);
                x = memory.readByte(effectiveAddress);
                zeroFlag = x == 0;
                negativeFlag = (x & 0x80) != 0;
                if ((effectiveAddress & 0xFF00) != (absoluteAddress & 0xFF00)) {
                    return 5;
                } else {
                    return 4;
                }
            }
            case (byte) 0xA0 -> { // LDY #nn
                y = fetchByte();
                zeroFlag = y == 0;
                negativeFlag = (y & 0x80) != 0;
                return 2;
            }
            case (byte) 0xA4 -> { // LDY nn
                y = memory.readByte(fetchByte());
                zeroFlag = y == 0;
                negativeFlag = (y & 0x80) != 0;
                return 3;
            }
            case (byte) 0xB4 -> { // LDY nn,X
                y = memory.readByte((short) (fetchByte() + x));
                zeroFlag = y == 0;
                negativeFlag = (y & 0x80) != 0;
                return 4;
            }
            case (byte) 0xAC -> { // LDY nnnn
                y = memory.readByte(fetchWord());
                zeroFlag = y == 0;
                negativeFlag = (y & 0x80) != 0;
                return 4;
            }
            case (byte) 0xBC -> { // LDY nnnn,X
                short absoluteAddress = fetchWord();
                short effectiveAddress = (short) (absoluteAddress + x);
                y = memory.readByte(effectiveAddress);
                zeroFlag = y == 0;
                negativeFlag = (y & 0x80) != 0;
                if ((effectiveAddress & 0xFF00) != (absoluteAddress & 0xFF00)) {
                    return 5;
                } else {
                    return 4;
                }
            }
            case (byte) 0x85 -> { // STA nn
                memory.writeByte(fetchByte(), a);
                return 3;
            }
            case (byte) 0x95 -> { // STA nn,X
                memory.writeByte((short) (fetchByte() + x), a);
                return 4;
            }
            case (byte) 0x8D -> { // STA nnnn
                memory.writeByte(fetchWord(), a);
                return 4;
            }
            case (byte) 0x9D -> { // STA nnnn,X
                memory.writeByte((short) (fetchWord() + x), a);
                return 5;
            }
            case (byte) 0x99 -> { // STA nnnn,Y
                memory.writeByte((short) (fetchWord() + y), a);
                return 5;
            }
            case (byte) 0x81 -> { // STA (nn,X)
                short indirectAddress = (short) (fetchByte() + x);
                short effectiveAddress = memory.readWord(indirectAddress);
                memory.writeByte(effectiveAddress, a);
                return 6;
            }
            case (byte) 0x91 -> { // STA (nn),Y
                short indirectAddress = fetchByte();
                short absoluteAddress = memory.readWord(indirectAddress);
                short effectiveAddress = (short) (absoluteAddress + y);
                memory.writeByte(effectiveAddress, a);
                return 6;
            }
            case (byte) 0x86 -> { // STX nn
                memory.writeByte(fetchByte(), x);
                return 3;
            }
            case (byte) 0x96 -> { // STX nn,Y
                memory.writeByte((short) (fetchByte() + y), x);
                return 4;
            }
            case (byte) 0x8E -> { // STX nnnn
                memory.writeByte(fetchWord(), x);
                return 4;
            }
            case (byte) 0x84 -> { // STY nn
                memory.writeByte(fetchByte(), y);
                return 3;
            }
            case (byte) 0x94 -> { // STY nn,X
                memory.writeByte((short) (fetchByte() + x), y);
                return 4;
            }
            case (byte) 0x8C -> { // STY nnnn
                memory.writeByte(fetchWord(), y);
                return 4;
            }
            case (byte) 0xAA -> { // TAX
                x = a;
                zeroFlag = x == 0;
                negativeFlag = (x & 0x80) != 0;
                return 2;
            }
            case (byte) 0xA8 -> { // TAY
                y = a;
                zeroFlag = y == 0;
                negativeFlag = (y & 0x80) != 0;
                return 2;
            }
            case (byte) 0x8A -> { // TXA
                a = x;
                zeroFlag = a == 0;
                negativeFlag = (a & 0x80) != 0;
                return 2;
            }
            case (byte) 0x98 -> { // TYA
                a = y;
                zeroFlag = a == 0;
                negativeFlag = (a & 0x80) != 0;
                return 2;
            }
            default -> throw new UnknownOpcodeException(opcode);
        }
    }

    public static class UnknownOpcodeException extends Throwable {
        public UnknownOpcodeException(byte opcode) {
            super(String.format("Unknown opcode: %02X", opcode));
        }
    }
}
