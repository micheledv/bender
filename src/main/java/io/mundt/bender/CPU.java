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
        pc = memory.readWord((short) 0xFFFC);
        sp = (byte) 0xFF;
        a = x = y = 0;
        carry = zero = interruptDisabled = decimalMode = breakCommand = overflow = negative = false;
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

    public void stackPush(byte value) {
        memory.writeByte((short) (0x100 + (sp & 0xFF)), value);
        sp--;
    }

    public byte stackPop() {
        sp++;
        return memory.readByte((short) (0x100 + (sp & 0xFF)));
    }

    public byte getStatus() {
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
        return status;
    }

    public void setStatus(byte status) {
        carry = (status & CARRY_FLAG) != 0;
        zero = (status & ZERO_FLAG) != 0;
        interruptDisabled = (status & INTERRUPT_DISABLE_FLAG) != 0;
        decimalMode = (status & DECIMAL_MODE_FLAG) != 0;
        breakCommand = (status & BREAK_COMMAND_FLAG) != 0;
        overflow = (status & OVERFLOW_FLAG) != 0;
        negative = (status & NEGATIVE_FLAG) != 0;
    }

    public int step() throws UnknownOpcodeException {
        byte opcode = fetchByte();
        switch (opcode) {
            case (byte) 0xA9 -> { // LDA #nn
                a = fetchByte();
                zero = a == 0;
                negative = (a & 0x80) != 0;
                return 2;
            }
            case (byte) 0xA5 -> { // LDA nn
                a = memory.readByte(fetchByte());
                zero = a == 0;
                negative = (a & 0x80) != 0;
                return 3;
            }
            case (byte) 0xB5 -> { // LDA nn,X
                a = memory.readByte((short) (fetchByte() + x));
                zero = a == 0;
                negative = (a & 0x80) != 0;
                return 4;
            }
            case (byte) 0xAD -> { // LDA nnnn
                a = memory.readByte(fetchWord());
                zero = a == 0;
                negative = (a & 0x80) != 0;
                return 4;
            }
            case (byte) 0xBD -> { // LDA nnnn,X
                short absoluteAddress = fetchWord();
                short effectiveAddress = (short) (absoluteAddress + x);
                a = memory.readByte(effectiveAddress);
                zero = a == 0;
                negative = (a & 0x80) != 0;
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
                zero = a == 0;
                negative = (a & 0x80) != 0;
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
                zero = a == 0;
                negative = (a & 0x80) != 0;
                return 6;
            }
            case (byte) 0xB1 -> { // LDA (nn),Y
                short indirectAddress = fetchByte();
                short absoluteAddress = memory.readWord(indirectAddress);
                short effectiveAddress = (short) (absoluteAddress + y);
                a = memory.readByte(effectiveAddress);
                zero = a == 0;
                negative = (a & 0x80) != 0;
                if ((effectiveAddress & 0xFF00) != (absoluteAddress & 0xFF00)) {
                    return 6;
                } else {
                    return 5;
                }
            }
            case (byte) 0xA2 -> { // LDX #nn
                x = fetchByte();
                zero = x == 0;
                negative = (x & 0x80) != 0;
                return 2;
            }
            case (byte) 0xA6 -> { // LDX nn
                x = memory.readByte(fetchByte());
                zero = x == 0;
                negative = (x & 0x80) != 0;
                return 3;
            }
            case (byte) 0xB6 -> { // LDX nn,Y
                x = memory.readByte((short) (fetchByte() + y));
                zero = x == 0;
                negative = (x & 0x80) != 0;
                return 4;
            }
            case (byte) 0xAE -> { // LDX nnnn
                x = memory.readByte(fetchWord());
                zero = x == 0;
                negative = (x & 0x80) != 0;
                return 4;
            }
            case (byte) 0xBE -> { // LDX nnnn,Y
                short absoluteAddress = fetchWord();
                short effectiveAddress = (short) (absoluteAddress + y);
                x = memory.readByte(effectiveAddress);
                zero = x == 0;
                negative = (x & 0x80) != 0;
                if ((effectiveAddress & 0xFF00) != (absoluteAddress & 0xFF00)) {
                    return 5;
                } else {
                    return 4;
                }
            }
            case (byte) 0xA0 -> { // LDY #nn
                y = fetchByte();
                zero = y == 0;
                negative = (y & 0x80) != 0;
                return 2;
            }
            case (byte) 0xA4 -> { // LDY nn
                y = memory.readByte(fetchByte());
                zero = y == 0;
                negative = (y & 0x80) != 0;
                return 3;
            }
            case (byte) 0xB4 -> { // LDY nn,X
                y = memory.readByte((short) (fetchByte() + x));
                zero = y == 0;
                negative = (y & 0x80) != 0;
                return 4;
            }
            case (byte) 0xAC -> { // LDY nnnn
                y = memory.readByte(fetchWord());
                zero = y == 0;
                negative = (y & 0x80) != 0;
                return 4;
            }
            case (byte) 0xBC -> { // LDY nnnn,X
                short absoluteAddress = fetchWord();
                short effectiveAddress = (short) (absoluteAddress + x);
                y = memory.readByte(effectiveAddress);
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
            default -> throw new UnknownOpcodeException(opcode);
        }
    }

    public static class UnknownOpcodeException extends Throwable {
        public UnknownOpcodeException(byte opcode) {
            super(String.format("Unknown opcode: %02X", opcode));
        }
    }
}
