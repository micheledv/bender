package io.mundt.bender;

public class CPU {
    public static final byte CARRY_FLAG = 0x01;
    public static final byte ZERO_FLAG = 0x02;
    public static final byte INTERRUPT_DISABLE_FLAG = 0x04;
    public static final byte DECIMAL_MODE_FLAG = 0x08;
    public static final byte BREAK_COMMAND_FLAG = 0x10;
    public static final byte OVERFLOW_FLAG = 0x40;
    public static final byte NEGATIVE_FLAG = (byte) 0x80;

    public final Memory memory;

    public short pc;

    public byte sp;

    public byte a, x, y;

    public boolean carry;
    public boolean zero;
    public boolean interruptDisabled;
    public boolean decimalMode;
    public boolean breakCommand;
    public boolean overflow;
    public boolean negative;

    public CPU(Memory memory) {
        this.memory = memory;
    }

    public void reset() {
        pc = (short) memory.readWord(0xFFFC);
        sp = (byte) 0xFF;
        a = x = y = 0;
        carry = zero = interruptDisabled = decimalMode = breakCommand = overflow = negative = false;
    }

    public int fetchByte() {
        int data = memory.readByte(pc);
        pc++;
        return data;
    }

    public int fetchWord() {
        int data = memory.readWord(pc);
        pc += 2;
        return data;
    }

    public void stackPush(int value) {
        memory.writeByte((sp & 0xFF) + 0x100, value);
        sp--;
    }

    public int stackPop() {
        sp++;
        return memory.readByte((sp & 0xFF) + 0x100);

    }

    public int getStatus() {
        byte status = 0;
        if (carry) {
            status |= CARRY_FLAG;
        }
        if (zero) {
            status |= ZERO_FLAG;
        }
        if (interruptDisabled) {
            status |= INTERRUPT_DISABLE_FLAG;
        }
        if (decimalMode) {
            status |= DECIMAL_MODE_FLAG;
        }
        if (breakCommand) {
            status |= BREAK_COMMAND_FLAG;
        }
        if (overflow) {
            status |= OVERFLOW_FLAG;
        }
        if (negative) {
            status |= NEGATIVE_FLAG;
        }
        return status & 0xFF;
    }

    public void setStatus(int status) {
        carry = (status & CARRY_FLAG) != 0;
        zero = (status & ZERO_FLAG) != 0;
        interruptDisabled = (status & INTERRUPT_DISABLE_FLAG) != 0;
        decimalMode = (status & DECIMAL_MODE_FLAG) != 0;
        breakCommand = (status & BREAK_COMMAND_FLAG) != 0;
        overflow = (status & OVERFLOW_FLAG) != 0;
        negative = (status & NEGATIVE_FLAG) != 0;
    }

    public int step() throws UnknownOpcodeException {
        byte opcode = (byte) fetchByte();
        switch (opcode) {
            case (byte) 0xA9 -> { // LDA #nn
                a = (byte) fetchByte();
                zero = a == 0;
                negative = (a & 0x80) != 0;
                return 2;
            }
            case (byte) 0xA5 -> { // LDA nn
                a = (byte) memory.readByte(fetchByte());
                zero = a == 0;
                negative = (a & 0x80) != 0;
                return 3;
            }
            case (byte) 0xB5 -> { // LDA nn,X
                a = (byte) memory.readByte(fetchByte() + x);
                zero = a == 0;
                negative = (a & 0x80) != 0;
                return 4;
            }
            case (byte) 0xAD -> { // LDA nnnn
                a = (byte) memory.readByte(fetchWord());
                zero = a == 0;
                negative = (a & 0x80) != 0;
                return 4;
            }
            case (byte) 0xBD -> { // LDA nnnn,X
                int absoluteAddress = fetchWord();
                int effectiveAddress = absoluteAddress + x;
                a = (byte) memory.readByte(effectiveAddress);
                zero = a == 0;
                negative = (a & 0x80) != 0;
                if ((effectiveAddress & 0xFF00) != (absoluteAddress & 0xFF00)) {
                    return 5;
                } else {
                    return 4;
                }
            }
            case (byte) 0xB9 -> { // LDA nnnn,Y
                int absoluteAddress = fetchWord();
                int effectiveAddress = absoluteAddress + y;
                a = (byte) memory.readByte(effectiveAddress);
                zero = a == 0;
                negative = (a & 0x80) != 0;
                if ((effectiveAddress & 0xFF00) != (absoluteAddress & 0xFF00)) {
                    return 5;
                } else {
                    return 4;
                }
            }
            case (byte) 0xA1 -> { // LDA (nn,X)
                int indirectAddress = fetchByte() + x;
                int effectiveAddress = memory.readWord(indirectAddress);
                a = (byte) memory.readByte(effectiveAddress);
                zero = a == 0;
                negative = (a & 0x80) != 0;
                return 6;
            }
            case (byte) 0xB1 -> { // LDA (nn),Y
                int indirectAddress = fetchByte();
                int absoluteAddress = memory.readWord(indirectAddress);
                int effectiveAddress = absoluteAddress + y;
                a = (byte) memory.readByte(effectiveAddress);
                zero = a == 0;
                negative = (a & 0x80) != 0;
                if ((effectiveAddress & 0xFF00) != (absoluteAddress & 0xFF00)) {
                    return 6;
                } else {
                    return 5;
                }
            }
            case (byte) 0xA2 -> { // LDX #nn
                x = (byte) fetchByte();
                zero = x == 0;
                negative = (x & 0x80) != 0;
                return 2;
            }
            case (byte) 0xA6 -> { // LDX nn
                x = (byte) memory.readByte(fetchByte());
                zero = x == 0;
                negative = (x & 0x80) != 0;
                return 3;
            }
            case (byte) 0xB6 -> { // LDX nn,Y
                x = (byte) memory.readByte(fetchByte() + y);
                zero = x == 0;
                negative = (x & 0x80) != 0;
                return 4;
            }
            case (byte) 0xAE -> { // LDX nnnn
                x = (byte) memory.readByte(fetchWord());
                zero = x == 0;
                negative = (x & 0x80) != 0;
                return 4;
            }
            case (byte) 0xBE -> { // LDX nnnn,Y
                int absoluteAddress = fetchWord();
                int effectiveAddress = absoluteAddress + y;
                x = (byte) memory.readByte(effectiveAddress);
                zero = x == 0;
                negative = (x & 0x80) != 0;
                if ((effectiveAddress & 0xFF00) != (absoluteAddress & 0xFF00)) {
                    return 5;
                } else {
                    return 4;
                }
            }
            case (byte) 0xA0 -> { // LDY #nn
                y = (byte) fetchByte();
                zero = y == 0;
                negative = (y & 0x80) != 0;
                return 2;
            }
            case (byte) 0xA4 -> { // LDY nn
                y = (byte) memory.readByte(fetchByte());
                zero = y == 0;
                negative = (y & 0x80) != 0;
                return 3;
            }
            case (byte) 0xB4 -> { // LDY nn,X
                y = (byte) memory.readByte(fetchByte() + x);
                zero = y == 0;
                negative = (y & 0x80) != 0;
                return 4;
            }
            case (byte) 0xAC -> { // LDY nnnn
                y = (byte) memory.readByte(fetchWord());
                zero = y == 0;
                negative = (y & 0x80) != 0;
                return 4;
            }
            case (byte) 0xBC -> { // LDY nnnn,X
                int absoluteAddress = fetchWord();
                int effectiveAddress = absoluteAddress + x;
                y = (byte) memory.readByte(effectiveAddress);
                zero = y == 0;
                negative = (y & 0x80) != 0;
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
                memory.writeByte(fetchByte() + x, a);
                return 4;
            }
            case (byte) 0x8D -> { // STA nnnn
                memory.writeByte(fetchWord(), a);
                return 4;
            }
            case (byte) 0x9D -> { // STA nnnn,X
                memory.writeByte(fetchWord() + x, a);
                return 5;
            }
            case (byte) 0x99 -> { // STA nnnn,Y
                memory.writeByte(fetchWord() + y, a);
                return 5;
            }
            case (byte) 0x81 -> { // STA (nn,X)
                int indirectAddress = fetchByte() + x;
                int effectiveAddress = memory.readWord(indirectAddress);
                memory.writeByte(effectiveAddress, a);
                return 6;
            }
            case (byte) 0x91 -> { // STA (nn),Y
                int indirectAddress = fetchByte();
                int absoluteAddress = memory.readWord(indirectAddress);
                int effectiveAddress = absoluteAddress + y;
                memory.writeByte(effectiveAddress, a);
                return 6;
            }
            case (byte) 0x86 -> { // STX nn
                memory.writeByte(fetchByte(), x);
                return 3;
            }
            case (byte) 0x96 -> { // STX nn,Y
                memory.writeByte(fetchByte() + y, x);
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
                memory.writeByte(fetchByte() + x, y);
                return 4;
            }
            case (byte) 0x8C -> { // STY nnnn
                memory.writeByte(fetchWord(), y);
                return 4;
            }
            case (byte) 0xAA -> { // TAX
                x = a;
                zero = x == 0;
                negative = (x & 0x80) != 0;
                return 2;
            }
            case (byte) 0xA8 -> { // TAY
                y = a;
                zero = y == 0;
                negative = (y & 0x80) != 0;
                return 2;
            }
            case (byte) 0x8A -> { // TXA
                a = x;
                zero = a == 0;
                negative = (a & 0x80) != 0;
                return 2;
            }
            case (byte) 0x98 -> { // TYA
                a = y;
                zero = a == 0;
                negative = (a & 0x80) != 0;
                return 2;
            }
            case (byte) 0xBA -> { // TSX
                x = sp;
                zero = x == 0;
                negative = (x & 0x80) != 0;
                return 2;
            }
            case (byte) 0x9A -> { // TXS
                sp = x;
                return 2;
            }
            case (byte) 0x48 -> { // PHA
                stackPush(a);
                return 3;
            }
            case (byte) 0x08 -> { // PHP
                stackPush(getStatus());
                return 3;
            }
            case (byte) 0x68 -> { // PLA
                a = (byte) stackPop();
                zero = a == 0;
                negative = (a & 0x80) != 0;
                return 4;
            }
            case (byte) 0x28 -> { // PLP
                setStatus(stackPop());
                return 4;
            }
            case (byte) 0x29 -> { // AND #nn
                a &= fetchByte();
                zero = a == 0;
                negative = (a & 0x80) != 0;
                return 2;
            }
            case (byte) 0x25 -> { // AND nn
                a &= memory.readByte(fetchByte());
                zero = a == 0;
                negative = (a & 0x80) != 0;
                return 3;
            }
            case (byte) 0x35 -> { // AND nn,X
                a &= memory.readByte(fetchByte() + x);
                zero = a == 0;
                negative = (a & 0x80) != 0;
                return 4;
            }
            case (byte) 0x2D -> { // AND nnnn
                a &= memory.readByte(fetchWord());
                zero = a == 0;
                negative = (a & 0x80) != 0;
                return 4;
            }
            case (byte) 0x3D -> { // AND nnnn,X
                int absoluteAddress = fetchWord();
                int effectiveAddress = absoluteAddress + x;
                a &= memory.readByte(effectiveAddress);
                zero = a == 0;
                negative = (a & 0x80) != 0;
                if ((effectiveAddress & 0xFF00) != (absoluteAddress & 0xFF00)) {
                    return 5;
                } else {
                    return 4;
                }
            }
            case (byte) 0x39 -> { // AND nnnn,Y
                int absoluteAddress = fetchWord();
                int effectiveAddress = absoluteAddress + y;
                a &= memory.readByte(effectiveAddress);
                zero = a == 0;
                negative = (a & 0x80) != 0;
                if ((effectiveAddress & 0xFF00) != (absoluteAddress & 0xFF00)) {
                    return 5;
                } else {
                    return 4;
                }
            }
            case (byte) 0x21 -> { // AND (nn,X)
                int indirectAddress = fetchByte() + x;
                int effectiveAddress = memory.readWord(indirectAddress);
                a &= memory.readByte(effectiveAddress);
                zero = a == 0;
                negative = (a & 0x80) != 0;
                return 6;
            }
            case (byte) 0x31 -> { // AND (nn),Y
                int indirectAddress = fetchByte();
                int absoluteAddress = memory.readWord(indirectAddress);
                int effectiveAddress = absoluteAddress + y;
                a &= memory.readByte(effectiveAddress);
                zero = a == 0;
                negative = (a & 0x80) != 0;
                if ((effectiveAddress & 0xFF00) != (absoluteAddress & 0xFF00)) {
                    return 6;
                } else {
                    return 5;
                }
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
