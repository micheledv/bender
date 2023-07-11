package io.mundt.bender;

import io.mundt.bender.CPU.UnknownOpcodeException;
import junit.framework.TestCase;

public class CPUTest extends TestCase {
    private CPU cpu;

    private Memory memory;

    public void setUp() {
        memory = new Memory();
        cpu = new CPU(memory);
    }

    public void testReset() {
        memory.writeWord(0xFFFC, 0x1234);
        cpu.reset();

        assertEquals(0x1234, cpu.pc);
        assertEquals(0xFF, cpu.sp & 0xFF);

        assertEquals(0, cpu.a);
        assertEquals(0, cpu.x);
        assertEquals(0, cpu.y);

        assertFalse(cpu.carry);
        assertFalse(cpu.zero);
        assertFalse(cpu.interruptDisabled);
        assertFalse(cpu.decimalMode);
        assertFalse(cpu.breakCommand);
        assertFalse(cpu.overflow);
        assertFalse(cpu.negative);
    }

    public void testFetchByte() {
        memory.writeByte(0x1234, 0x56);
        cpu.pc = 0x1234;
        assertEquals(0x56, cpu.fetchByte());
        assertEquals(0x1235, cpu.pc);
    }

    public void testFetchWord() {
        memory.writeWord(0x1234, 0x5678);
        cpu.pc = 0x1234;
        assertEquals(0x5678, cpu.fetchWord());
        assertEquals(0x1236, cpu.pc);
    }

    public void testStackPush() {
        cpu.sp = (byte) 0xFF;
        cpu.stackPush(0x42);
        assertEquals(0xFE, cpu.sp & 0xFF);
        assertEquals(0x42, memory.readByte(0x01FF));
    }

    public void testStackPop() {
        cpu.sp = (byte) 0xFE;
        memory.writeByte(0x01FF, 0x42);
        assertEquals(0x42, cpu.stackPop());
        assertEquals(0xFF, cpu.sp & 0xFF);
    }

    public void testGetStatus() {
        cpu.carry = true;
        cpu.zero = true;
        cpu.interruptDisabled = true;
        cpu.decimalMode = true;
        cpu.breakCommand = true;
        cpu.overflow = true;
        cpu.negative = true;
        assertEquals(0xDF, cpu.getStatus());
    }

    public void testSetStatus() {
        cpu.setStatus(0xDF);
        assertTrue(cpu.carry);
        assertTrue(cpu.zero);
        assertTrue(cpu.interruptDisabled);
        assertTrue(cpu.decimalMode);
        assertTrue(cpu.breakCommand);
        assertTrue(cpu.overflow);
        assertTrue(cpu.negative);
    }

    public void testUnknownOpcode() {
        memory.writeByte(0x1234, 0x00);
        cpu.pc = 0x1234;
        try {
            cpu.step();
            fail("Expected UnknownOpcodeException");
        } catch (UnknownOpcodeException e) {
            // pass
        }
    }

    public void testLDAImmediate() throws UnknownOpcodeException {
        memory.writeByte(0x1234, 0xA9); // LDA #nn
        memory.writeByte(0x1235, 0x42); // #nn = 0x42

        cpu.pc = 0x1234;
        int cycles = cpu.step();

        assertEquals(0x42, cpu.a); // A = #nn
        assertFalse(cpu.zero); // Z = false
        assertFalse(cpu.negative); // N = false
        assertEquals(2, cycles); // 2 cycles
    }

    public void testLDAZeroPage() throws UnknownOpcodeException {
        memory.writeByte(0x1234, 0xA5); // LDA nn
        memory.writeByte(0x1235, 0x42); // nn = 0x42
        memory.writeByte(0x0042, 0x84); // 0x42 = 0x84

        cpu.pc = 0x1234;
        int cycles = cpu.step();

        assertEquals(0x84, cpu.a & 0xFF); // A = [nn]
        assertFalse(cpu.zero); // Z = false
        assertTrue(cpu.negative); // N = true
        assertEquals(3, cycles); // 3 cycles
    }

    public void testLDAZeroPageX() throws UnknownOpcodeException {
        memory.writeByte(0x1234, 0xB5); // LDA nn,X
        memory.writeByte(0x1235, 0x42); // nn = 0x42
        memory.writeByte(0x0052, 0x84); // 0x42 = 0x84
        cpu.x = 0x10;

        cpu.pc = 0x1234;
        int cycles = cpu.step();

        assertEquals(0x84, cpu.a & 0xFF); // A = [nn + X]
        assertFalse(cpu.zero); // Z = false
        assertTrue(cpu.negative); // N = true
        assertEquals(4, cycles); // 4 cycles
    }

    public void testLDAAbsolute() throws UnknownOpcodeException {
        memory.writeByte(0x1234, 0xAD); // LDA nnnn
        memory.writeWord(0x1235, 0x5678); // nnnn = 0x5678
        memory.writeByte(0x5678, 0x84); // 0x5678 = 0x84

        cpu.pc = 0x1234;
        int cycles = cpu.step();

        assertEquals(0x84, cpu.a & 0xFF); // A = [nnnn]
        assertFalse(cpu.zero); // Z = false
        assertTrue(cpu.negative); // N = true
        assertEquals(4, cycles); // 4 cycles
    }

    public void testLDAAbsoluteXWithinPage() throws UnknownOpcodeException {
        memory.writeByte(0x1234, 0xBD); // LDA nnnn,X
        memory.writeWord(0x1235, 0x5678); // nnnn = 0x5678
        memory.writeByte(0x5688, 0x84); // 0x5678 = 0x84
        cpu.x = 0x10;

        cpu.pc = 0x1234;
        int cycles = cpu.step();

        assertEquals(0x84, cpu.a & 0xFF); // A = [nnnn + X]
        assertFalse(cpu.zero); // Z = false
        assertTrue(cpu.negative); // N = true
        assertEquals(4, cycles); // 4 cycles
    }

    public void testLDAAbsoluteXCrossingPage() throws UnknownOpcodeException {
        memory.writeByte(0x1234, 0xBD); // LDA nnnn,X
        memory.writeWord(0x1235, 0x56F0); // nnnn = 0x56F0
        memory.writeByte(0x5700, 0x84); // 0x5700 = 0x84
        cpu.x = 0x10;

        cpu.pc = 0x1234;
        int cycles = cpu.step();

        assertEquals(0x84, cpu.a & 0xFF); // A = [nnnn + X]
        assertFalse(cpu.zero); // Z = false
        assertTrue(cpu.negative); // N = true
        assertEquals(5, cycles); // 5 cycles
    }

    public void testLDAAbsoluteYWithinPage() throws UnknownOpcodeException {
        memory.writeByte(0x1234, 0xB9); // LDA nnnn,Y
        memory.writeWord(0x1235, 0x5678); // nnnn = 0x5678
        memory.writeByte(0x5688, 0x84); // 0x5678 = 0x84
        cpu.y = 0x10;

        cpu.pc = 0x1234;
        int cycles = cpu.step();

        assertEquals(0x84, cpu.a & 0xFF); // A = [nnnn + Y]
        assertFalse(cpu.zero); // Z = false
        assertTrue(cpu.negative); // N = true
        assertEquals(4, cycles); // 4 cycles
    }

    public void testLDAAbsoluteYCrossingPage() throws UnknownOpcodeException {
        memory.writeByte(0x1234, 0xB9); // LDA nnnn,Y
        memory.writeWord(0x1235, 0x56F0); // nnnn = 0x56F0
        memory.writeByte(0x5700, 0x84); // 0x5700 = 0x84
        cpu.y = 0x10;

        cpu.pc = 0x1234;
        int cycles = cpu.step();

        assertEquals(0x84, cpu.a & 0xFF); // A = [nnnn + Y]
        assertFalse(cpu.zero); // Z = false
        assertTrue(cpu.negative); // N = true
        assertEquals(5, cycles); // 5 cycles
    }

    public void testLDAIndirectX() throws UnknownOpcodeException {
        memory.writeByte(0x1234, 0xA1); // LDA (nn,X)
        memory.writeByte(0x1235, 0x42); // nn = 0x42
        memory.writeWord(0x0052, 0x5678); // 0x52 = 0x5678
        memory.writeByte(0x5678, 0x84); // 0x5678 = 0x84
        cpu.x = 0x10;

        cpu.pc = 0x1234;
        int cycles = cpu.step();

        assertEquals(0x84, cpu.a & 0xFF); // A = [[nn + X]]
        assertFalse(cpu.zero); // Z = false
        assertTrue(cpu.negative); // N = true
        assertEquals(6, cycles); // 6 cycles
    }

    public void testLDAIndirectYWithinPage() throws UnknownOpcodeException {
        memory.writeByte(0x1234, 0xB1); // LDA (nn),Y
        memory.writeByte(0x1235, 0x42); // nn = 0x42
        memory.writeWord(0x0042, 0x5678); // 0x42 = 0x5678
        memory.writeByte(0x5688, 0x84); // 0x5678 = 0x84
        cpu.y = 0x10;

        cpu.pc = 0x1234;
        int cycles = cpu.step();

        assertEquals(0x84, cpu.a & 0xFF); // A = [[nn] + Y]
        assertFalse(cpu.zero); // Z = false
        assertTrue(cpu.negative); // N = true
        assertEquals(5, cycles); // 5 cycles
    }

    public void testLDAIndirectYCrossingPage() throws UnknownOpcodeException {
        memory.writeByte(0x1234, 0xB1); // LDA (nn),Y
        memory.writeByte(0x1235, 0x42); // nn = 0x42
        memory.writeWord(0x0042, 0x56F0); // 0x42 = 0x56F0
        memory.writeByte(0x5700, 0x84); // 0x5700 = 0x84
        cpu.y = 0x10;

        cpu.pc = 0x1234;
        int cycles = cpu.step();

        assertEquals(0x84, cpu.a & 0xFF); // A = [[nn] + Y]
        assertFalse(cpu.zero); // Z = false
        assertTrue(cpu.negative); // N = true
        assertEquals(6, cycles); // 6 cycles
    }

    public void testLDXImmediate() throws UnknownOpcodeException {
        memory.writeByte(0x1234, 0xA2); // LDX nn
        memory.writeByte(0x1235, 0x84); // nn = 0x84

        cpu.pc = 0x1234;
        int cycles = cpu.step();

        assertEquals(0x84, cpu.x & 0xFF); // X = nn
        assertFalse(cpu.zero); // Z = false
        assertTrue(cpu.negative); // N = true
        assertEquals(2, cycles); // 2 cycles
    }

    public void testLDXZeroPage() throws UnknownOpcodeException {
        memory.writeByte(0x1234, 0xA6); // LDX nn
        memory.writeByte(0x1235, 0x42); // nn = 0x42
        memory.writeByte(0x0042, 0x84); // 0x42 = 0x84

        cpu.pc = 0x1234;
        int cycles = cpu.step();

        assertEquals(0x84, cpu.x & 0xFF); // X = [nn]
        assertFalse(cpu.zero); // Z = false
        assertTrue(cpu.negative); // N = true
        assertEquals(3, cycles); // 3 cycles
    }

    public void testLDXZeroPageY() throws UnknownOpcodeException {
        memory.writeByte(0x1234, 0xB6); // LDX nn,Y
        memory.writeByte(0x1235, 0x42); // nn = 0x42
        memory.writeByte(0x0052, 0x84); // 0x52 = 0x84
        cpu.y = 0x10;

        cpu.pc = 0x1234;
        int cycles = cpu.step();

        assertEquals(0x84, cpu.x & 0xFF); // X = [nn + Y]
        assertFalse(cpu.zero); // Z = false
        assertTrue(cpu.negative); // N = true
        assertEquals(4, cycles); // 4 cycles
    }

    public void testLDXAbsolute() throws UnknownOpcodeException {
        memory.writeByte(0x1234, 0xAE); // LDX nnnn
        memory.writeWord(0x1235, 0x5678); // nnnn = 0x5678
        memory.writeByte(0x5678, 0x84); // 0x5678 = 0x84

        cpu.pc = 0x1234;
        int cycles = cpu.step();

        assertEquals(0x84, cpu.x & 0xFF); // X = [nnnn]
        assertFalse(cpu.zero); // Z = false
        assertTrue(cpu.negative); // N = true
        assertEquals(4, cycles); // 4 cycles
    }

    public void testLDXAbsoluteYWithinPage() throws UnknownOpcodeException {
        memory.writeByte(0x1234, 0xBE); // LDX nnnn,Y
        memory.writeWord(0x1235, 0x5678); // nnnn = 0x5678
        memory.writeByte(0x5688, 0x84); // 0x5688 = 0x84
        cpu.y = 0x10;

        cpu.pc = 0x1234;
        int cycles = cpu.step();

        assertEquals(0x84, cpu.x & 0xFF); // X = [nnnn + Y]
        assertFalse(cpu.zero); // Z = false
        assertTrue(cpu.negative); // N = true
        assertEquals(4, cycles); // 4 cycles
    }

    public void testLDXAbsoluteYCrossingPage() throws UnknownOpcodeException {
        memory.writeByte(0x1234, 0xBE); // LDX nnnn,Y
        memory.writeWord(0x1235, 0x56F0); // nnnn = 0x56F0
        memory.writeByte(0x5700, 0x84); // 0x5700 = 0x84
        cpu.y = 0x10;

        cpu.pc = 0x1234;
        int cycles = cpu.step();

        assertEquals(0x84, cpu.x & 0xFF); // X = [nnnn + Y]
        assertFalse(cpu.zero); // Z = false
        assertTrue(cpu.negative); // N = true
        assertEquals(5, cycles); // 5 cycles
    }

    public void testLDYImmediate() throws UnknownOpcodeException {
        memory.writeByte(0x1234, 0xA0); // LDY nn
        memory.writeByte(0x1235, 0x84); // nn = 0x84

        cpu.pc = 0x1234;
        int cycles = cpu.step();

        assertEquals(0x84, cpu.y & 0xFF); // Y = nn
        assertFalse(cpu.zero); // Z = false
        assertTrue(cpu.negative); // N = true
        assertEquals(2, cycles); // 2 cycles
    }

    public void testLDYZeroPage() throws UnknownOpcodeException {
        memory.writeByte(0x1234, 0xA4); // LDY nn
        memory.writeByte(0x1235, 0x42); // nn = 0x42
        memory.writeByte(0x0042, 0x84); // 0x42 = 0x84

        cpu.pc = 0x1234;
        int cycles = cpu.step();

        assertEquals(0x84, cpu.y & 0xFF); // Y = [nn]
        assertFalse(cpu.zero); // Z = false
        assertTrue(cpu.negative); // N = true
        assertEquals(3, cycles); // 3 cycles
    }

    public void testLDYZeroPageX() throws UnknownOpcodeException {
        memory.writeByte(0x1234, 0xB4); // LDY nn,X
        memory.writeByte(0x1235, 0x42); // nn = 0x42
        memory.writeByte(0x0052, 0x84); // 0x52 = 0x84
        cpu.x = 0x10;

        cpu.pc = 0x1234;
        int cycles = cpu.step();

        assertEquals(0x84, cpu.y & 0xFF); // Y = [nn + X]
        assertFalse(cpu.zero); // Z = false
        assertTrue(cpu.negative); // N = true
        assertEquals(4, cycles); // 4 cycles
    }

    public void testLDYAbsolute() throws UnknownOpcodeException {
        memory.writeByte(0x1234, 0xAC); // LDY nnnn
        memory.writeWord(0x1235, 0x5678); // nnnn = 0x5678
        memory.writeByte(0x5678, 0x84); // 0x5678 = 0x84

        cpu.pc = 0x1234;
        int cycles = cpu.step();

        assertEquals(0x84, cpu.y & 0xFF); // Y = [nnnn]
        assertFalse(cpu.zero); // Z = false
        assertTrue(cpu.negative); // N = true
        assertEquals(4, cycles); // 4 cycles
    }

    public void testLDYAbsoluteXWithinPage() throws UnknownOpcodeException {
        memory.writeByte(0x1234, 0xBC); // LDY nnnn,X
        memory.writeWord(0x1235, 0x5678); // nnnn = 0x5678
        memory.writeByte(0x5688, 0x84); // 0x5688 = 0x84
        cpu.x = 0x10;

        cpu.pc = 0x1234;
        int cycles = cpu.step();

        assertEquals(0x84, cpu.y & 0xFF); // Y = [nnnn + X]
        assertFalse(cpu.zero); // Z = false
        assertTrue(cpu.negative); // N = true
        assertEquals(4, cycles); // 4 cycles
    }

    public void testLDYAbsoluteXCrossingPage() throws UnknownOpcodeException {
        memory.writeByte(0x1234, 0xBC); // LDY nnnn,X
        memory.writeWord(0x1235, 0x56F0); // nnnn = 0x56F0
        memory.writeByte(0x5700, 0x84); // 0x5700 = 0x84
        cpu.x = 0x10;

        cpu.pc = 0x1234;
        int cycles = cpu.step();

        assertEquals(0x84, cpu.y & 0xFF); // Y = [nnnn + X]
        assertFalse(cpu.zero); // Z = false
        assertTrue(cpu.negative); // N = true
        assertEquals(5, cycles); // 5 cycles
    }

    public void testSTAZeroPage() throws UnknownOpcodeException {
        memory.writeByte(0x1234, 0x85); // STA nn
        memory.writeByte(0x1235, 0x42); // nn = 0x42
        cpu.a = (byte) 0x84;

        cpu.pc = 0x1234;
        int cycles = cpu.step();

        assertEquals(0x84, memory.readByte(0x0042)); // [nn] = A
        assertEquals(3, cycles); // 3 cycles
    }

    public void testSTAZeroPageX() throws UnknownOpcodeException {
        memory.writeByte(0x1234, 0x95); // STA nn,X
        memory.writeByte(0x1235, 0x42); // nn = 0x42
        cpu.a = (byte) 0x84;
        cpu.x = 0x10;

        cpu.pc = 0x1234;
        int cycles = cpu.step();

        assertEquals(0x84, memory.readByte(0x0052)); // [nn + X] = A
        assertEquals(4, cycles); // 4 cycles
    }

    public void testSTAAbsolute() throws UnknownOpcodeException {
        memory.writeByte(0x1234, 0x8D); // STA nnnn
        memory.writeWord(0x1235, 0x5678); // nnnn = 0x5678
        cpu.a = (byte) 0x84;

        cpu.pc = 0x1234;
        int cycles = cpu.step();

        assertEquals(0x84, memory.readByte(0x5678)); // [nnnn] = A
        assertEquals(4, cycles); // 4 cycles
    }

    public void testSTAAbsoluteX() throws UnknownOpcodeException {
        memory.writeByte(0x1234, 0x9D); // STA nnnn,X
        memory.writeWord(0x1235, 0x5678); // nnnn = 0x5678
        cpu.a = (byte) 0x84;
        cpu.x = 0x10;

        cpu.pc = 0x1234;
        int cycles = cpu.step();

        assertEquals(0x84, memory.readByte(0x5688)); // [nnnn + X] = A
        assertEquals(5, cycles); // 5 cycles
    }

    public void testSTAAbsoluteY() throws UnknownOpcodeException {
        memory.writeByte(0x1234, 0x99); // STA nnnn,Y
        memory.writeWord(0x1235, 0x5678); // nnnn = 0x5678
        cpu.a = (byte) 0x84;
        cpu.y = 0x10;

        cpu.pc = 0x1234;
        int cycles = cpu.step();

        assertEquals(0x84, memory.readByte(0x5688)); // [nnnn + Y] = A
        assertEquals(5, cycles); // 5 cycles
    }

    public void testSTAIndirectX() throws UnknownOpcodeException {
        memory.writeByte(0x1234, 0x81); // STA (nn,X)
        memory.writeByte(0x1235, 0x42); // nn = 0x42
        memory.writeWord(0x0052, 0x5678); // 0x42 = 0x5678
        cpu.a = (byte) 0x84;
        cpu.x = 0x10;

        cpu.pc = 0x1234;
        int cycles = cpu.step();

        assertEquals(0x84, memory.readByte(0x5678)); // [[nn + X]] = A
        assertEquals(6, cycles); // 6 cycles
    }

    public void testSTAIndirectY() throws UnknownOpcodeException {
        memory.writeByte(0x1234, 0x91); // STA (nn),Y
        memory.writeByte(0x1235, 0x42); // nn = 0x42
        memory.writeWord(0x42, 0x5678); // 0x42 = 0x5678
        cpu.a = (byte) 0x84;
        cpu.y = 0x10;

        cpu.pc = 0x1234;
        int cycles = cpu.step();

        assertEquals(0x84, memory.readByte(0x5688)); // [[nn] + Y] = A
        assertEquals(6, cycles); // 6 cycles
    }

    public void testSTXZeroPage() throws UnknownOpcodeException {
        memory.writeByte(0x1234, 0x86); // STX nn
        memory.writeByte(0x1235, 0x42); // nn = 0x42
        cpu.x = (byte) 0x84;

        cpu.pc = 0x1234;
        int cycles = cpu.step();

        assertEquals(0x84, memory.readByte(0x0042)); // [nn] = X
        assertEquals(3, cycles); // 3 cycles
    }

    public void testSTXZeroPageY() throws UnknownOpcodeException {
        memory.writeByte(0x1234, 0x96); // STX nn,Y
        memory.writeByte(0x1235, 0x42); // nn = 0x42
        cpu.x = (byte) 0x84;
        cpu.y = 0x10;

        cpu.pc = 0x1234;
        int cycles = cpu.step();

        assertEquals(0x84, memory.readByte(0x0052)); // [nn + Y] = X
        assertEquals(4, cycles); // 4 cycles
    }

    public void testSTXAbsolute() throws UnknownOpcodeException {
        memory.writeByte(0x1234, 0x8E); // STX nnnn
        memory.writeWord(0x1235, 0x5678); // nnnn = 0x5678
        cpu.x = (byte) 0x84;

        cpu.pc = 0x1234;
        int cycles = cpu.step();

        assertEquals(0x84, memory.readByte(0x5678)); // [nnnn] = X
        assertEquals(4, cycles); // 4 cycles
    }

    public void testSTYZeroPage() throws UnknownOpcodeException {
        memory.writeByte(0x1234, 0x84); // STY nn
        memory.writeByte(0x1235, 0x42); // nn = 0x42
        cpu.y = (byte) 0x84;

        cpu.pc = 0x1234;
        int cycles = cpu.step();

        assertEquals(0x84, memory.readByte(0x0042)); // [nn] = Y
        assertEquals(3, cycles); // 3 cycles
    }

    public void testSTYZeroPageX() throws UnknownOpcodeException {
        memory.writeByte(0x1234, 0x94); // STY nn,X
        memory.writeByte(0x1235, 0x42); // nn = 0x42
        cpu.y = (byte) 0x84;
        cpu.x = 0x10;

        cpu.pc = 0x1234;
        int cycles = cpu.step();

        assertEquals(0x84, memory.readByte(0x0052)); // [nn + X] = Y
        assertEquals(4, cycles); // 4 cycles
    }

    public void testSTYAbsolute() throws UnknownOpcodeException {
        memory.writeByte(0x1234, 0x8C); // STY nnnn
        memory.writeWord(0x1235, 0x5678); // nnnn = 0x5678
        cpu.y = (byte) 0x84;

        cpu.pc = 0x1234;
        int cycles = cpu.step();

        assertEquals(0x84, memory.readByte(0x5678)); // [nnnn] = Y
        assertEquals(4, cycles); // 4 cycles
    }

    public void testTAX() throws UnknownOpcodeException {
        memory.writeByte(0x1234, 0xAA); // TAX
        cpu.a = (byte) 0x84;

        cpu.pc = 0x1234;
        int cycles = cpu.step();

        assertEquals(0x84, cpu.x & 0xFF); // X = A
        assertEquals(2, cycles); // 2 cycles
    }

    public void testTAY() throws UnknownOpcodeException {
        memory.writeByte(0x1234, 0xA8); // TAY
        cpu.a = (byte) 0x84;

        cpu.pc = 0x1234;
        int cycles = cpu.step();

        assertEquals(0x84, cpu.y & 0xFF); // Y = A
        assertEquals(2, cycles); // 2 cycles
    }

    public void testTXA() throws UnknownOpcodeException {
        memory.writeByte(0x1234, 0x8A); // TXA
        cpu.x = (byte) 0x84;

        cpu.pc = 0x1234;
        int cycles = cpu.step();

        assertEquals(0x84, cpu.a & 0xFF); // A = X
        assertEquals(2, cycles); // 2 cycles
    }

    public void testTYA() throws UnknownOpcodeException {
        memory.writeByte(0x1234, 0x98); // TYA
        cpu.y = (byte) 0x84;

        cpu.pc = 0x1234;
        int cycles = cpu.step();

        assertEquals(0x84, cpu.a & 0xFF); // A = Y
        assertEquals(2, cycles); // 2 cycles
    }

    public void testTSX() throws UnknownOpcodeException {
        memory.writeByte(0x1234, 0xBA); // TSX
        cpu.sp = (byte) 0x84;

        cpu.pc = 0x1234;
        int cycles = cpu.step();

        assertEquals(0x84, cpu.x & 0xFF); // X = SP
        assertEquals(2, cycles); // 2 cycles
    }

    public void testTXS() throws UnknownOpcodeException {
        memory.writeByte(0x1234, 0x9A); // TXS
        cpu.x = (byte) 0x84;

        cpu.pc = 0x1234;
        int cycles = cpu.step();

        assertEquals(0x84, cpu.sp & 0xFF); // SP = X
        assertEquals(2, cycles); // 2 cycles
    }

    public void testPHA() throws UnknownOpcodeException {
        memory.writeByte(0x1234, 0x48); // PHA
        cpu.a = (byte) 0x84;
        cpu.sp = 0x10;

        cpu.pc = 0x1234;
        int cycles = cpu.step();

        assertEquals(0x84, memory.readByte(0x0110)); // [SP] = A
        assertEquals(0x0F, cpu.sp); // SP--
        assertEquals(3, cycles); // 3 cycles
    }

    public void testPHP() throws UnknownOpcodeException {
        memory.writeByte(0x1234, 0x08); // PHP
        cpu.setStatus(0x84);
        cpu.sp = 0x10;

        cpu.pc = 0x1234;
        int cycles = cpu.step();

        assertEquals(0x84, memory.readByte(0x0110)); // [SP] = P
        assertEquals(0x0F, cpu.sp); // SP--
        assertEquals(3, cycles); // 3 cycles
    }

    public void testPLA() throws UnknownOpcodeException {
        memory.writeByte(0x1234, 0x68); // PLA
        memory.writeByte(0x0110, 0x84); // [SP] = A
        cpu.sp = 0x0F;

        cpu.pc = 0x1234;
        int cycles = cpu.step();

        assertEquals(0x84, cpu.a & 0xFF); // A = [SP]
        assertEquals(0x10, cpu.sp); // SP++
        assertEquals(4, cycles); // 4 cycles
    }

    public void testPLP() throws UnknownOpcodeException {
        memory.writeByte(0x1234, 0x28); // PLP
        memory.writeByte(0x0110, 0x84); // [SP] = P
        cpu.sp = 0x0F;

        cpu.pc = 0x1234;
        int cycles = cpu.step();

        assertEquals(0x84, cpu.getStatus()); // P = [SP]
        assertEquals(0x10, cpu.sp); // SP++
        assertEquals(4, cycles); // 4 cycles
    }

    public void testANDImmediate() throws UnknownOpcodeException {
        memory.writeByte(0x1234, 0x29); // AND nn
        memory.writeByte(0x1235, 0x3C); // nn = 0x3C
        cpu.a = (byte) 0xF0;
        cpu.negative = true;
        cpu.zero = true;

        cpu.pc = 0x1234;
        int cycles = cpu.step();

        assertEquals(0x30, cpu.a); // A = A & nn
        assertFalse(cpu.negative); // N = 0
        assertFalse(cpu.zero); // Z = 0
        assertEquals(2, cycles); // 2 cycles
    }

    public void testANDZeroPage() throws UnknownOpcodeException {
        memory.writeByte(0x1234, 0x25); // AND nn
        memory.writeByte(0x1235, 0xFF); // nn = 0xFF
        memory.writeByte(0x00FF, 0x3C); // [nn] = 0x3C
        cpu.a = (byte) 0xF0;
        cpu.negative = true;
        cpu.zero = true;

        cpu.pc = 0x1234;
        int cycles = cpu.step();

        assertEquals(0x30, cpu.a); // A = A & [nn]
        assertFalse(cpu.negative); // N = 0
        assertFalse(cpu.zero); // Z = 0
        assertEquals(3, cycles); // 3 cycles
    }

    public void testANDZeroPageX() throws UnknownOpcodeException {
        memory.writeByte(0x1234, 0x35); // AND nn,X
        memory.writeByte(0x1235, 0x80); // nn = 0x80
        memory.writeByte(0x0084, 0x3C); // [nn+X] = 0x3C
        cpu.a = (byte) 0xF0;
        cpu.x = 0x04;
        cpu.negative = true;
        cpu.zero = true;

        cpu.pc = 0x1234;
        int cycles = cpu.step();

        assertEquals(0x30, cpu.a); // A = A & [nn+X]
        assertFalse(cpu.negative); // N = 0
        assertFalse(cpu.zero); // Z = 0
        assertEquals(4, cycles); // 4 cycles
    }

    public void testANDAbsolute() throws UnknownOpcodeException {
        memory.writeByte(0x1234, 0x2D); // AND nnnn
        memory.writeWord(0x1235, 0x5678); // nnnn = 0x5678
        memory.writeByte(0x5678, 0x3C); // [nnnn] = 0x3C
        cpu.a = (byte) 0xF0;
        cpu.negative = true;
        cpu.zero = true;

        cpu.pc = 0x1234;
        int cycles = cpu.step();

        assertEquals(0x30, cpu.a); // A = A & [nnnn]
        assertFalse(cpu.negative); // N = 0
        assertFalse(cpu.zero); // Z = 0
        assertEquals(4, cycles); // 4 cycles
    }

    public void testANDAbsoluteXWithinPage() throws UnknownOpcodeException {
        memory.writeByte(0x1234, 0x3D); // AND nnnn,X
        memory.writeWord(0x1235, 0x5678); // nnnn = 0x5678
        memory.writeByte(0x5678 + 0x04, 0x3C); // [nnnn+X] = 0x3C
        cpu.a = (byte) 0xF0;
        cpu.x = 0x04;
        cpu.negative = true;
        cpu.zero = true;

        cpu.pc = 0x1234;
        int cycles = cpu.step();

        assertEquals(0x30, cpu.a); // A = A & [nnnn+X]
        assertFalse(cpu.negative); // N = 0
        assertFalse(cpu.zero); // Z = 0
        assertEquals(4, cycles); // 4 cycles
    }

    public void testANDAbsoluteXCrossingPage() throws UnknownOpcodeException {
        memory.writeByte(0x1234, 0x3D); // AND nnnn,X
        memory.writeWord(0x1235, 0x56FF); // nnnn = 0x56FF
        memory.writeByte(0x5703, 0x3C); // [nnnn+X] = 0x3C
        cpu.a = (byte) 0xF0;
        cpu.x = 0x04;
        cpu.negative = true;
        cpu.zero = true;

        cpu.pc = 0x1234;
        int cycles = cpu.step();

        assertEquals(0x30, cpu.a); // A = A & [nnnn+X]
        assertFalse(cpu.negative); // N = 0
        assertFalse(cpu.zero); // Z = 0
        assertEquals(5, cycles); // 5 cycles
    }

    public void testANDAbsoluteYWithinPage() throws UnknownOpcodeException {
        memory.writeByte(0x1234, 0x39); // AND nnnn,Y
        memory.writeWord(0x1235, 0x5678); // nnnn = 0x5678
        memory.writeByte(0x5678 + 0x04, 0x3C); // [nnnn+Y] = 0x3C
        cpu.a = (byte) 0xF0;
        cpu.y = 0x04;
        cpu.negative = true;
        cpu.zero = true;

        cpu.pc = 0x1234;
        int cycles = cpu.step();

        assertEquals(0x30, cpu.a); // A = A & [nnnn+Y]
        assertFalse(cpu.negative); // N = 0
        assertFalse(cpu.zero); // Z = 0
        assertEquals(4, cycles); // 4 cycles
    }

    public void testANDAbsoluteYCrossingPage() throws UnknownOpcodeException {
        memory.writeByte(0x1234, 0x39); // AND nnnn,Y
        memory.writeWord(0x1235, 0x56FF); // nnnn = 0x56FF
        memory.writeByte(0x5703, 0x3C); // [nnnn+Y] = 0x3C
        cpu.a = (byte) 0xF0;
        cpu.y = 0x04;
        cpu.negative = true;
        cpu.zero = true;

        cpu.pc = 0x1234;
        int cycles = cpu.step();

        assertEquals(0x30, cpu.a); // A = A & [nnnn+Y]
        assertFalse(cpu.negative); // N = 0
        assertFalse(cpu.zero); // Z = 0
        assertEquals(5, cycles); // 5 cycles
    }

    public void testANDIndirectX() throws UnknownOpcodeException {
        memory.writeByte(0x1234, 0x21); // AND (nn,X)
        memory.writeByte(0x1235, 0x80); // nn = 0x80
        memory.writeByte(0x0084, 0x78); // [nn+X] = 0x78
        memory.writeByte(0x0085, 0x56); // [nn+X+1] = 0x56
        memory.writeByte(0x5678, 0x3C); // [0x5678] = 0x3C
        cpu.a = (byte) 0xF0;
        cpu.x = 0x04;
        cpu.negative = true;
        cpu.zero = true;

        cpu.pc = 0x1234;
        int cycles = cpu.step();

        assertEquals(0x30, cpu.a); // A = A & [nn+X]
        assertFalse(cpu.negative); // N = 0
        assertFalse(cpu.zero); // Z = 0
        assertEquals(6, cycles); // 6 cycles
    }

    public void testANDIndirectYWithinPage() throws UnknownOpcodeException {
        cpu.y = 0x04;
        memory.writeByte(0x1234, 0x31); // AND (nn),Y
        memory.writeByte(0x1235, 0x80); // nn = 0x80
        memory.writeWord(0x0080, 0x5678); // [nn] = 0x5678
        memory.writeByte(0x5678 + cpu.y, 0x3C); // [nn+Y] = 0x3C
        cpu.a = (byte) 0xF0;
        cpu.negative = true;
        cpu.zero = true;

        cpu.pc = 0x1234;
        int cycles = cpu.step();

        assertEquals(0x30, cpu.a); // A = A & [nn+Y]
        assertFalse(cpu.negative); // N = 0
        assertFalse(cpu.zero); // Z = 0
        assertEquals(5, cycles); // 5 cycles
    }

    public void testANDIndirectYCrossingPage() throws UnknownOpcodeException {
        cpu.y = 0x04;
        memory.writeByte(0x1234, 0x31); // AND (nn),Y
        memory.writeByte(0x1235, 0x80); // nn = 0x80
        memory.writeWord(0x0080, 0x56FF); // [nn] = 0x56FF
        memory.writeByte(0x56FF + cpu.y, 0x3C); // [nn+Y] = 0x3C
        cpu.a = (byte) 0xF0;
        cpu.negative = true;
        cpu.zero = true;

        cpu.pc = 0x1234;
        int cycles = cpu.step();

        assertEquals(0x30, cpu.a); // A = A & [nn+Y]
        assertFalse(cpu.negative); // N = 0
        assertFalse(cpu.zero); // Z = 0
        assertEquals(6, cycles); // 6 cycles
    }

    public void testEORImmediate() throws UnknownOpcodeException {
        memory.writeByte(0x1234, 0x49); // EOR #nn
        memory.writeByte(0x1235, 0x3C); // nn = 0x3C
        cpu.a = (byte) 0xF0;
        cpu.negative = false;
        cpu.zero = true;

        cpu.pc = 0x1234;
        int cycles = cpu.step();

        assertEquals(0xCC, cpu.a & 0xFF); // A = A ^ nn
        assertTrue(cpu.negative); // N = 1
        assertFalse(cpu.zero); // Z = 0
        assertEquals(2, cycles); // 2 cycles
    }

    public void testEORZeroPage() throws UnknownOpcodeException {
        memory.writeByte(0x1234, 0x45); // EOR nn
        memory.writeByte(0x1235, 0x3C); // nn = 0x3C
        memory.writeByte(0x003C, 0x78); // [nn] = 0x78
        cpu.a = (byte) 0xF0;
        cpu.negative = false;
        cpu.zero = true;

        cpu.pc = 0x1234;
        int cycles = cpu.step();

        assertEquals(0x88, cpu.a & 0xFF); // A = A ^ [nn]
        assertTrue(cpu.negative); // N = 1
        assertFalse(cpu.zero); // Z = 0
        assertEquals(3, cycles); // 3 cycles
    }

    public void testEORZeroPageX() throws UnknownOpcodeException {
        cpu.x = 0x04;
        memory.writeByte(0x1234, 0x55); // EOR nn,X
        memory.writeByte(0x1235, 0x3C); // nn = 0x3C
        memory.writeByte(0x003C + cpu.x, 0x78); // [nn+X] = 0x78
        cpu.a = (byte) 0xF0;
        cpu.negative = false;
        cpu.zero = true;

        cpu.pc = 0x1234;
        int cycles = cpu.step();

        assertEquals(0x88, cpu.a & 0xFF); // A = A ^ [nn+X]
        assertTrue(cpu.negative); // N = 1
        assertFalse(cpu.zero); // Z = 0
        assertEquals(4, cycles); // 4 cycles
    }

    public void testEORAbsolute() throws UnknownOpcodeException {
        memory.writeByte(0x1234, 0x4D); // EOR nnnn
        memory.writeWord(0x1235, 0x5678); // nnnn = 0x5678
        memory.writeByte(0x5678, 0x78); // [nnnn] = 0x78
        cpu.a = (byte) 0xF0;
        cpu.negative = false;
        cpu.zero = true;

        cpu.pc = 0x1234;
        int cycles = cpu.step();

        assertEquals(0x88, cpu.a & 0xFF); // A = A ^ [nnnn]
        assertTrue(cpu.negative); // N = 1
        assertFalse(cpu.zero); // Z = 0
        assertEquals(4, cycles); // 4 cycles
    }

    public void testEORAbsoluteXWithinPage() throws UnknownOpcodeException {
        cpu.x = 0x04;
        memory.writeByte(0x1234, 0x5D); // EOR nnnn,X
        memory.writeWord(0x1235, 0x5678); // nnnn = 0x5678
        memory.writeByte(0x5678 + cpu.x, 0x78); // [nnnn+X] = 0x78
        cpu.a = (byte) 0xF0;
        cpu.negative = false;
        cpu.zero = true;

        cpu.pc = 0x1234;
        int cycles = cpu.step();

        assertEquals(0x88, cpu.a & 0xFF); // A = A ^ [nnnn+X]
        assertTrue(cpu.negative); // N = 1
        assertFalse(cpu.zero); // Z = 0
        assertEquals(4, cycles); // 4 cycles
    }

    public void testEORAbsoluteXCrossingPage() throws UnknownOpcodeException {
        cpu.x = 0x04;
        memory.writeByte(0x1234, 0x5D); // EOR nnnn,X
        memory.writeWord(0x1235, 0x56FF); // nnnn = 0x56FF
        memory.writeByte(0x56FF + cpu.x, 0x78); // [nnnn+X] = 0x78
        cpu.a = (byte) 0xF0;
        cpu.negative = false;
        cpu.zero = true;

        cpu.pc = 0x1234;
        int cycles = cpu.step();

        assertEquals(0x88, cpu.a & 0xFF); // A = A ^ [nnnn+X]
        assertTrue(cpu.negative); // N = 1
        assertFalse(cpu.zero); // Z = 0
        assertEquals(5, cycles); // 5 cycles
    }

    public void testEORAbsoluteYWithinPage() throws UnknownOpcodeException {
        cpu.y = 0x04;
        memory.writeByte(0x1234, 0x59); // EOR nnnn,Y
        memory.writeWord(0x1235, 0x5678); // nnnn = 0x5678
        memory.writeByte(0x5678 + cpu.y, 0x78); // [nnnn+Y] = 0x78
        cpu.a = (byte) 0xF0;
        cpu.negative = false;
        cpu.zero = true;

        cpu.pc = 0x1234;
        int cycles = cpu.step();

        assertEquals(0x88, cpu.a & 0xFF); // A = A ^ [nnnn+Y]
        assertTrue(cpu.negative); // N = 1
        assertFalse(cpu.zero); // Z = 0
        assertEquals(4, cycles); // 4 cycles
    }

    public void testEORAbsoluteYCrossingPage() throws UnknownOpcodeException {
        cpu.y = 0x04;
        memory.writeByte(0x1234, 0x59); // EOR nnnn,Y
        memory.writeWord(0x1235, 0x56FF); // nnnn = 0x56FF
        memory.writeByte(0x56FF + cpu.y, 0x78); // [nnnn+Y] = 0x78
        cpu.a = (byte) 0xF0;
        cpu.negative = false;
        cpu.zero = true;

        cpu.pc = 0x1234;
        int cycles = cpu.step();

        assertEquals(0x88, cpu.a & 0xFF); // A = A ^ [nnnn+Y]
        assertTrue(cpu.negative); // N = 1
        assertFalse(cpu.zero); // Z = 0
        assertEquals(5, cycles); // 5 cycles
    }

    public void testEORIndirectX() throws UnknownOpcodeException {
        cpu.x = 0x04;
        memory.writeByte(0x1234, 0x41); // EOR (nn,X)
        memory.writeByte(0x1235, 0x38); // nn = 0x38
        memory.writeByte(0x0038 + cpu.x, 0x78); // [nn+X] = 0x78
        memory.writeByte(0x0078, 0x99); // [0x78] = 0x99
        cpu.a = (byte) 0xF0;
        cpu.negative = true;
        cpu.zero = true;

        cpu.pc = 0x1234;
        int cycles = cpu.step();

        assertEquals(0x69, cpu.a & 0xFF); // A = A ^ [nn+X]
        assertFalse(cpu.negative); // N = 0
        assertFalse(cpu.zero); // Z = 0
        assertEquals(6, cycles); // 6 cycles
    }

    public void testEORIndirectYWithinPage() throws UnknownOpcodeException {
        cpu.y = 0x04;
        memory.writeByte(0x1234, 0x51); // EOR (nn),Y
        memory.writeByte(0x1235, 0x38); // nn = 0x38
        memory.writeByte(0x0038, 0x78); // [nn] = 0x78
        memory.writeByte(0x0078 + cpu.y, 0x99); // [nn+Y] = 0x99
        cpu.a = (byte) 0xF0;
        cpu.negative = true;
        cpu.zero = true;

        cpu.pc = 0x1234;
        int cycles = cpu.step();

        assertEquals(0x69, cpu.a & 0xFF); // A = A ^ [nn+Y]
        assertFalse(cpu.negative); // N = 0
        assertFalse(cpu.zero); // Z = 0
        assertEquals(5, cycles); // 5 cycles
    }

    public void testEORIndirectYCrossingPage() throws UnknownOpcodeException {
        cpu.y = (byte) 0xFF;
        memory.writeByte(0x1234, 0x51); // EOR (nn),Y
        memory.writeByte(0x1235, 0x38); // nn = 0x38
        memory.writeWord(0x0038, 0x5678); // [nn] = 0x5678
        memory.writeByte(0x5678 + (cpu.y & 0xFF), 0x99); // [nn+Y] = 0x99
        cpu.a = (byte) 0xF0;
        cpu.negative = true;
        cpu.zero = true;

        cpu.pc = 0x1234;
        int cycles = cpu.step();

        assertEquals(0x69, cpu.a & 0xFF); // A = A ^ [nn+Y]
        assertFalse(cpu.negative); // N = 0
        assertFalse(cpu.zero); // Z = 0
        assertEquals(6, cycles); // 6 cycles
    }

    public void testORAImmediate() throws UnknownOpcodeException {
        memory.writeByte(0x1234, 0x09); // ORA #nn
        memory.writeByte(0x1235, 0x88); // nn = 0x88
        cpu.a = (byte) 0x77;
        cpu.negative = false;
        cpu.zero = false;

        cpu.pc = 0x1234;
        int cycles = cpu.step();

        assertEquals(0xFF, cpu.a & 0xFF); // A = A | nn
        assertTrue(cpu.negative); // N = 1
        assertFalse(cpu.zero); // Z = 0
        assertEquals(2, cycles); // 2 cycles
    }

    public void testORAZeroPage() throws UnknownOpcodeException {
        memory.writeByte(0x1234, 0x05); // ORA nn
        memory.writeByte(0x1235, 0x88); // nn = 0x88
        memory.writeByte(0x0088, 0x77); // [nn] = 0x77
        cpu.a = (byte) 0x77;
        cpu.negative = true;
        cpu.zero = false;

        cpu.pc = 0x1234;
        int cycles = cpu.step();

        assertEquals(0x77, cpu.a & 0xFF); // A = A | [nn]
        assertFalse(cpu.negative); // N = 0
        assertFalse(cpu.zero); // Z = 0
        assertEquals(3, cycles); // 3 cycles
    }

    public void testORAZeroPageX() throws UnknownOpcodeException {
        cpu.x = 0x04;
        memory.writeByte(0x1234, 0x15); // ORA nn,X
        memory.writeByte(0x1235, 0x88); // nn = 0x88
        memory.writeByte(0x0088 + cpu.x, 0x77); // [nn+X] = 0x77
        cpu.a = (byte) 0x77;
        cpu.negative = true;
        cpu.zero = false;

        cpu.pc = 0x1234;
        int cycles = cpu.step();

        assertEquals(0x77, cpu.a & 0xFF); // A = A | [nn+X]
        assertFalse(cpu.negative); // N = 0
        assertFalse(cpu.zero); // Z = 0
        assertEquals(4, cycles); // 4 cycles
    }

    public void testORAAbsolute() throws UnknownOpcodeException {
        memory.writeByte(0x1234, 0x0D); // ORA nnnn
        memory.writeWord(0x1235, 0x5678); // nnnn = 0x5678
        memory.writeByte(0x5678, 0x77); // [nnnn] = 0x77
        cpu.a = (byte) 0x77;
        cpu.negative = true;
        cpu.zero = false;

        cpu.pc = 0x1234;
        int cycles = cpu.step();

        assertEquals(0x77, cpu.a & 0xFF); // A = A | [nnnn]
        assertFalse(cpu.negative); // N = 0
        assertFalse(cpu.zero); // Z = 0
        assertEquals(4, cycles); // 4 cycles
    }

    public void testORAAbsoluteXWithinPage() throws UnknownOpcodeException {
        cpu.x = 0x04;
        memory.writeByte(0x1234, 0x1D); // ORA nnnn,X
        memory.writeWord(0x1235, 0x5678); // nnnn = 0x5678
        memory.writeByte(0x5678 + cpu.x, 0x77); // [nnnn+X] = 0x77
        cpu.a = (byte) 0x77;
        cpu.negative = true;
        cpu.zero = false;

        cpu.pc = 0x1234;
        int cycles = cpu.step();

        assertEquals(0x77, cpu.a & 0xFF); // A = A | [nnnn+X]
        assertFalse(cpu.negative); // N = 0
        assertFalse(cpu.zero); // Z = 0
        assertEquals(4, cycles); // 4 cycles
    }

    public void testORAAbsoluteXCrossingPage() throws UnknownOpcodeException {
        cpu.x = (byte) 0xFF;
        memory.writeByte(0x1234, 0x1D); // ORA nnnn,X
        memory.writeWord(0x1235, 0x56FF); // nnnn = 0x56FF
        memory.writeByte(0x56FF + cpu.x, 0x77); // [nnnn+X] = 0x77
        cpu.a = (byte) 0x77;
        cpu.negative = true;
        cpu.zero = false;

        cpu.pc = 0x1234;
        int cycles = cpu.step();

        assertEquals(0x77, cpu.a & 0xFF); // A = A | [nnnn+X]
        assertFalse(cpu.negative); // N = 0
        assertFalse(cpu.zero); // Z = 0
        assertEquals(5, cycles); // 5 cycles
    }

    public void testORAIndirectX() throws UnknownOpcodeException {
        cpu.x = 0x04;
        memory.writeByte(0x1234, 0x01); // ORA (nn,X)
        memory.writeByte(0x1235, 0x88); // nn = 0x88
        memory.writeWord(0x0088 + cpu.x, 0x5678); // [nn+X] = 0x5678
        memory.writeByte(0x5678, 0x77); // [0x5678] = 0x77
        cpu.a = (byte) 0x77;
        cpu.negative = true;
        cpu.zero = false;

        cpu.pc = 0x1234;
        int cycles = cpu.step();

        assertEquals(0x77, cpu.a & 0xFF); // A = A | [nn+X]
        assertFalse(cpu.negative); // N = 0
        assertFalse(cpu.zero); // Z = 0
        assertEquals(6, cycles); // 6 cycles
    }

    public void testORAIndirectYWithinPage() throws UnknownOpcodeException {
        cpu.y = 0x04;
        memory.writeByte(0x1234, 0x11); // ORA (nn),Y
        memory.writeByte(0x1235, 0x88); // nn = 0x88
        memory.writeWord(0x0088, 0x5678); // [nn] = 0x5678
        memory.writeByte(0x5678 + cpu.y, 0x77); // [0x5678+Y] = 0x77
        cpu.a = (byte) 0x77;
        cpu.negative = true;
        cpu.zero = false;

        cpu.pc = 0x1234;
        int cycles = cpu.step();

        assertEquals(0x77, cpu.a & 0xFF); // A = A | [nn]+Y
        assertFalse(cpu.negative); // N = 0
        assertFalse(cpu.zero); // Z = 0
        assertEquals(5, cycles); // 5 cycles
    }

    public void testORAIndirectYCrossingPage() throws UnknownOpcodeException {
        cpu.y = (byte) 0xFF;
        memory.writeByte(0x1234, 0x11); // ORA (nn),Y
        memory.writeByte(0x1235, 0x88); // nn = 0x88
        memory.writeWord(0x0088, 0x56FF); // [nn] = 0x56FF
        memory.writeByte(0x56FF + cpu.y, 0x77); // [0x56FF+Y] = 0x77
        cpu.a = (byte) 0x77;
        cpu.negative = true;
        cpu.zero = false;

        cpu.pc = 0x1234;
        int cycles = cpu.step();

        assertEquals(0x77, cpu.a & 0xFF); // A = A | [nn]+Y
        assertFalse(cpu.negative); // N = 0
        assertFalse(cpu.zero); // Z = 0
        assertEquals(6, cycles); // 6 cycles
    }
}
