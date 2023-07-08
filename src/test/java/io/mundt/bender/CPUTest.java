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
        memory.writeWord((short) 0xFFFC, (short) 0x1234);
        cpu.reset();

        assertEquals(0x1234, cpu.pc);
        assertEquals((byte) 0xFF, cpu.sp);

        assertEquals(0, cpu.a);
        assertEquals(0, cpu.x);
        assertEquals(0, cpu.y);

        assertFalse(cpu.carryFlag);
        assertFalse(cpu.zeroFlag);
        assertFalse(cpu.interruptDisableFlag);
        assertFalse(cpu.decimalModeFlag);
        assertFalse(cpu.breakCommandFlag);
        assertFalse(cpu.overflowFlag);
        assertFalse(cpu.negativeFlag);
    }

    public void testFetchByte() {
        memory.writeByte((short) 0x1234, (byte) 0x56);
        cpu.pc = (short) 0x1234;
        assertEquals(0x56, cpu.fetchByte());
        assertEquals(0x1235, cpu.pc);
    }

    public void testFetchWord() {
        memory.writeWord((short) 0x1234, (short) 0x5678);
        cpu.pc = (short) 0x1234;
        assertEquals(0x5678, cpu.fetchWord());
        assertEquals(0x1236, cpu.pc);
    }

    public void testStackPush() {
        cpu.sp = (byte) 0xFF;
        cpu.stackPush((byte) 0x42);
        assertEquals((byte) 0xFE, cpu.sp);
        assertEquals((byte) 0x42, memory.readByte((short) 0x01FF));
    }

    public void testStackPop() {
        cpu.sp = (byte) 0xFE;
        memory.writeByte((short) 0x01FF, (byte) 0x42);
        assertEquals((byte) 0x42, cpu.stackPop());
        assertEquals((byte) 0xFF, cpu.sp);
    }

    public void testUnknownOpcode() {
        memory.writeByte((short) 0x1234, (byte) 0x00);
        cpu.pc = (short) 0x1234;
        try {
            cpu.step();
            fail("Expected UnknownOpcodeException");
        } catch (UnknownOpcodeException e) {
            // pass
        }
    }

    public void testLDAImmediate() throws UnknownOpcodeException {
        memory.writeByte((short) 0x1234, (byte) 0xA9); // LDA #nn
        memory.writeByte((short) 0x1235, (byte) 0x42); // #nn = 0x42

        cpu.pc = (short) 0x1234;
        int cycles = cpu.step();

        assertEquals(0x42, cpu.a); // A = #nn
        assertFalse(cpu.zeroFlag); // Z = false
        assertFalse(cpu.negativeFlag); // N = false
        assertEquals(2, cycles); // 2 cycles
    }

    public void testLDAZeroPage() throws UnknownOpcodeException {
        memory.writeByte((short) 0x1234, (byte) 0xA5); // LDA nn
        memory.writeByte((short) 0x1235, (byte) 0x42); // nn = 0x42
        memory.writeByte((short) 0x0042, (byte) 0x84); // 0x42 = 0x84

        cpu.pc = (short) 0x1234;
        int cycles = cpu.step();

        assertEquals((byte) 0x84, cpu.a); // A = [nn]
        assertFalse(cpu.zeroFlag); // Z = false
        assertTrue(cpu.negativeFlag); // N = true
        assertEquals(3, cycles); // 3 cycles
    }

    public void testLDAZeroPageX() throws UnknownOpcodeException {
        memory.writeByte((short) 0x1234, (byte) 0xB5); // LDA nn,X
        memory.writeByte((short) 0x1235, (byte) 0x42); // nn = 0x42
        memory.writeByte((short) 0x0052, (byte) 0x84); // 0x42 = 0x84
        cpu.x = 0x10;

        cpu.pc = (short) 0x1234;
        int cycles = cpu.step();

        assertEquals((byte) 0x84, cpu.a); // A = [nn + X]
        assertFalse(cpu.zeroFlag); // Z = false
        assertTrue(cpu.negativeFlag); // N = true
        assertEquals(4, cycles); // 4 cycles
    }

    public void testLDAAbsolute() throws UnknownOpcodeException {
        memory.writeByte((short) 0x1234, (byte) 0xAD); // LDA nnnn
        memory.writeWord((short) 0x1235, (short) 0x5678); // nnnn = 0x5678
        memory.writeByte((short) 0x5678, (byte) 0x84); // 0x5678 = 0x84

        cpu.pc = (short) 0x1234;
        int cycles = cpu.step();

        assertEquals((byte) 0x84, cpu.a); // A = [nnnn]
        assertFalse(cpu.zeroFlag); // Z = false
        assertTrue(cpu.negativeFlag); // N = true
        assertEquals(4, cycles); // 4 cycles
    }

    public void testLDAAbsoluteXWithinPage() throws UnknownOpcodeException {
        memory.writeByte((short) 0x1234, (byte) 0xBD); // LDA nnnn,X
        memory.writeWord((short) 0x1235, (short) 0x5678); // nnnn = 0x5678
        memory.writeByte((short) 0x5688, (byte) 0x84); // 0x5678 = 0x84
        cpu.x = 0x10;

        cpu.pc = (short) 0x1234;
        int cycles = cpu.step();

        assertEquals((byte) 0x84, cpu.a); // A = [nnnn + X]
        assertFalse(cpu.zeroFlag); // Z = false
        assertTrue(cpu.negativeFlag); // N = true
        assertEquals(4, cycles); // 4 cycles
    }

    public void testLDAAbsoluteXCrossingPage() throws UnknownOpcodeException {
        memory.writeByte((short) 0x1234, (byte) 0xBD); // LDA nnnn,X
        memory.writeWord((short) 0x1235, (short) 0x56F0); // nnnn = 0x56F0
        memory.writeByte((short) 0x5700, (byte) 0x84); // 0x5700 = 0x84
        cpu.x = 0x10;

        cpu.pc = (short) 0x1234;
        int cycles = cpu.step();

        assertEquals((byte) 0x84, cpu.a); // A = [nnnn + X]
        assertFalse(cpu.zeroFlag); // Z = false
        assertTrue(cpu.negativeFlag); // N = true
        assertEquals(5, cycles); // 5 cycles
    }

    public void testLDAAbsoluteYWithinPage() throws UnknownOpcodeException {
        memory.writeByte((short) 0x1234, (byte) 0xB9); // LDA nnnn,Y
        memory.writeWord((short) 0x1235, (short) 0x5678); // nnnn = 0x5678
        memory.writeByte((short) 0x5688, (byte) 0x84); // 0x5678 = 0x84
        cpu.y = 0x10;

        cpu.pc = (short) 0x1234;
        int cycles = cpu.step();

        assertEquals((byte) 0x84, cpu.a); // A = [nnnn + Y]
        assertFalse(cpu.zeroFlag); // Z = false
        assertTrue(cpu.negativeFlag); // N = true
        assertEquals(4, cycles); // 4 cycles
    }

    public void testLDAAbsoluteYCrossingPage() throws UnknownOpcodeException {
        memory.writeByte((short) 0x1234, (byte) 0xB9); // LDA nnnn,Y
        memory.writeWord((short) 0x1235, (short) 0x56F0); // nnnn = 0x56F0
        memory.writeByte((short) 0x5700, (byte) 0x84); // 0x5700 = 0x84
        cpu.y = 0x10;

        cpu.pc = (short) 0x1234;
        int cycles = cpu.step();

        assertEquals((byte) 0x84, cpu.a); // A = [nnnn + Y]
        assertFalse(cpu.zeroFlag); // Z = false
        assertTrue(cpu.negativeFlag); // N = true
        assertEquals(5, cycles); // 5 cycles
    }

    public void testLDAIndirectX() throws UnknownOpcodeException {
        memory.writeByte((short) 0x1234, (byte) 0xA1); // LDA (nn,X)
        memory.writeByte((short) 0x1235, (byte) 0x42); // nn = 0x42
        memory.writeWord((short) 0x0052, (short) 0x5678); // 0x52 = 0x5678
        memory.writeByte((short) 0x5678, (byte) 0x84); // 0x5678 = 0x84
        cpu.x = 0x10;

        cpu.pc = (short) 0x1234;
        int cycles = cpu.step();

        assertEquals((byte) 0x84, cpu.a); // A = [[nn + X]]
        assertFalse(cpu.zeroFlag); // Z = false
        assertTrue(cpu.negativeFlag); // N = true
        assertEquals(6, cycles); // 6 cycles
    }

    public void testLDAIndirectYWithinPage() throws UnknownOpcodeException {
        memory.writeByte((short) 0x1234, (byte) 0xB1); // LDA (nn),Y
        memory.writeByte((short) 0x1235, (byte) 0x42); // nn = 0x42
        memory.writeWord((short) 0x0042, (short) 0x5678); // 0x42 = 0x5678
        memory.writeByte((short) 0x5688, (byte) 0x84); // 0x5678 = 0x84
        cpu.y = 0x10;

        cpu.pc = (short) 0x1234;
        int cycles = cpu.step();

        assertEquals((byte) 0x84, cpu.a); // A = [[nn] + Y]
        assertFalse(cpu.zeroFlag); // Z = false
        assertTrue(cpu.negativeFlag); // N = true
        assertEquals(5, cycles); // 5 cycles
    }

    public void testLDAIndirectYCrossingPage() throws UnknownOpcodeException {
        memory.writeByte((short) 0x1234, (byte) 0xB1); // LDA (nn),Y
        memory.writeByte((short) 0x1235, (byte) 0x42); // nn = 0x42
        memory.writeWord((short) 0x0042, (short) 0x56F0); // 0x42 = 0x56F0
        memory.writeByte((short) 0x5700, (byte) 0x84); // 0x5700 = 0x84
        cpu.y = 0x10;

        cpu.pc = (short) 0x1234;
        int cycles = cpu.step();

        assertEquals((byte) 0x84, cpu.a); // A = [[nn] + Y]
        assertFalse(cpu.zeroFlag); // Z = false
        assertTrue(cpu.negativeFlag); // N = true
        assertEquals(6, cycles); // 6 cycles
    }

    public void testLDXImmediate() throws UnknownOpcodeException {
        memory.writeByte((short) 0x1234, (byte) 0xA2); // LDX nn
        memory.writeByte((short) 0x1235, (byte) 0x84); // nn = 0x84

        cpu.pc = (short) 0x1234;
        int cycles = cpu.step();

        assertEquals((byte) 0x84, cpu.x); // X = nn
        assertFalse(cpu.zeroFlag); // Z = false
        assertTrue(cpu.negativeFlag); // N = true
        assertEquals(2, cycles); // 2 cycles
    }

    public void testLDXZeroPage() throws UnknownOpcodeException {
        memory.writeByte((short) 0x1234, (byte) 0xA6); // LDX nn
        memory.writeByte((short) 0x1235, (byte) 0x42); // nn = 0x42
        memory.writeByte((short) 0x0042, (byte) 0x84); // 0x42 = 0x84

        cpu.pc = (short) 0x1234;
        int cycles = cpu.step();

        assertEquals((byte) 0x84, cpu.x); // X = [nn]
        assertFalse(cpu.zeroFlag); // Z = false
        assertTrue(cpu.negativeFlag); // N = true
        assertEquals(3, cycles); // 3 cycles
    }

    public void testLDXZeroPageY() throws UnknownOpcodeException {
        memory.writeByte((short) 0x1234, (byte) 0xB6); // LDX nn,Y
        memory.writeByte((short) 0x1235, (byte) 0x42); // nn = 0x42
        memory.writeByte((short) 0x0052, (byte) 0x84); // 0x52 = 0x84
        cpu.y = 0x10;

        cpu.pc = (short) 0x1234;
        int cycles = cpu.step();

        assertEquals((byte) 0x84, cpu.x); // X = [nn + Y]
        assertFalse(cpu.zeroFlag); // Z = false
        assertTrue(cpu.negativeFlag); // N = true
        assertEquals(4, cycles); // 4 cycles
    }

    public void testLDXAbsolute() throws UnknownOpcodeException {
        memory.writeByte((short) 0x1234, (byte) 0xAE); // LDX nnnn
        memory.writeWord((short) 0x1235, (short) 0x5678); // nnnn = 0x5678
        memory.writeByte((short) 0x5678, (byte) 0x84); // 0x5678 = 0x84

        cpu.pc = (short) 0x1234;
        int cycles = cpu.step();

        assertEquals((byte) 0x84, cpu.x); // X = [nnnn]
        assertFalse(cpu.zeroFlag); // Z = false
        assertTrue(cpu.negativeFlag); // N = true
        assertEquals(4, cycles); // 4 cycles
    }

    public void testLDXAbsoluteYWithinPage() throws UnknownOpcodeException {
        memory.writeByte((short) 0x1234, (byte) 0xBE); // LDX nnnn,Y
        memory.writeWord((short) 0x1235, (short) 0x5678); // nnnn = 0x5678
        memory.writeByte((short) 0x5688, (byte) 0x84); // 0x5688 = 0x84
        cpu.y = 0x10;

        cpu.pc = (short) 0x1234;
        int cycles = cpu.step();

        assertEquals((byte) 0x84, cpu.x); // X = [nnnn + Y]
        assertFalse(cpu.zeroFlag); // Z = false
        assertTrue(cpu.negativeFlag); // N = true
        assertEquals(4, cycles); // 4 cycles
    }

    public void testLDXAbsoluteYCrossingPage() throws UnknownOpcodeException {
        memory.writeByte((short) 0x1234, (byte) 0xBE); // LDX nnnn,Y
        memory.writeWord((short) 0x1235, (short) 0x56F0); // nnnn = 0x56F0
        memory.writeByte((short) 0x5700, (byte) 0x84); // 0x5700 = 0x84
        cpu.y = 0x10;

        cpu.pc = (short) 0x1234;
        int cycles = cpu.step();

        assertEquals((byte) 0x84, cpu.x); // X = [nnnn + Y]
        assertFalse(cpu.zeroFlag); // Z = false
        assertTrue(cpu.negativeFlag); // N = true
        assertEquals(5, cycles); // 5 cycles
    }

    public void testLDYImmediate() throws UnknownOpcodeException {
        memory.writeByte((short) 0x1234, (byte) 0xA0); // LDY nn
        memory.writeByte((short) 0x1235, (byte) 0x84); // nn = 0x84

        cpu.pc = (short) 0x1234;
        int cycles = cpu.step();

        assertEquals((byte) 0x84, cpu.y); // Y = nn
        assertFalse(cpu.zeroFlag); // Z = false
        assertTrue(cpu.negativeFlag); // N = true
        assertEquals(2, cycles); // 2 cycles
    }

    public void testLDYZeroPage() throws UnknownOpcodeException {
        memory.writeByte((short) 0x1234, (byte) 0xA4); // LDY nn
        memory.writeByte((short) 0x1235, (byte) 0x42); // nn = 0x42
        memory.writeByte((short) 0x0042, (byte) 0x84); // 0x42 = 0x84

        cpu.pc = (short) 0x1234;
        int cycles = cpu.step();

        assertEquals((byte) 0x84, cpu.y); // Y = [nn]
        assertFalse(cpu.zeroFlag); // Z = false
        assertTrue(cpu.negativeFlag); // N = true
        assertEquals(3, cycles); // 3 cycles
    }

    public void testLDYZeroPageX() throws UnknownOpcodeException {
        memory.writeByte((short) 0x1234, (byte) 0xB4); // LDY nn,X
        memory.writeByte((short) 0x1235, (byte) 0x42); // nn = 0x42
        memory.writeByte((short) 0x0052, (byte) 0x84); // 0x52 = 0x84
        cpu.x = 0x10;

        cpu.pc = (short) 0x1234;
        int cycles = cpu.step();

        assertEquals((byte) 0x84, cpu.y); // Y = [nn + X]
        assertFalse(cpu.zeroFlag); // Z = false
        assertTrue(cpu.negativeFlag); // N = true
        assertEquals(4, cycles); // 4 cycles
    }

    public void testLDYAbsolute() throws UnknownOpcodeException {
        memory.writeByte((short) 0x1234, (byte) 0xAC); // LDY nnnn
        memory.writeWord((short) 0x1235, (short) 0x5678); // nnnn = 0x5678
        memory.writeByte((short) 0x5678, (byte) 0x84); // 0x5678 = 0x84

        cpu.pc = (short) 0x1234;
        int cycles = cpu.step();

        assertEquals((byte) 0x84, cpu.y); // Y = [nnnn]
        assertFalse(cpu.zeroFlag); // Z = false
        assertTrue(cpu.negativeFlag); // N = true
        assertEquals(4, cycles); // 4 cycles
    }

    public void testLDYAbsoluteXWithinPage() throws UnknownOpcodeException {
        memory.writeByte((short) 0x1234, (byte) 0xBC); // LDY nnnn,X
        memory.writeWord((short) 0x1235, (short) 0x5678); // nnnn = 0x5678
        memory.writeByte((short) 0x5688, (byte) 0x84); // 0x5688 = 0x84
        cpu.x = 0x10;

        cpu.pc = (short) 0x1234;
        int cycles = cpu.step();

        assertEquals((byte) 0x84, cpu.y); // Y = [nnnn + X]
        assertFalse(cpu.zeroFlag); // Z = false
        assertTrue(cpu.negativeFlag); // N = true
        assertEquals(4, cycles); // 4 cycles
    }

    public void testLDYAbsoluteXCrossingPage() throws UnknownOpcodeException {
        memory.writeByte((short) 0x1234, (byte) 0xBC); // LDY nnnn,X
        memory.writeWord((short) 0x1235, (short) 0x56F0); // nnnn = 0x56F0
        memory.writeByte((short) 0x5700, (byte) 0x84); // 0x5700 = 0x84
        cpu.x = 0x10;

        cpu.pc = (short) 0x1234;
        int cycles = cpu.step();

        assertEquals((byte) 0x84, cpu.y); // Y = [nnnn + X]
        assertFalse(cpu.zeroFlag); // Z = false
        assertTrue(cpu.negativeFlag); // N = true
        assertEquals(5, cycles); // 5 cycles
    }

    public void testSTAZeroPage() throws UnknownOpcodeException {
        memory.writeByte((short) 0x1234, (byte) 0x85); // STA nn
        memory.writeByte((short) 0x1235, (byte) 0x42); // nn = 0x42
        cpu.a = (byte) 0x84;

        cpu.pc = (short) 0x1234;
        int cycles = cpu.step();

        assertEquals((byte) 0x84, memory.readByte((short) 0x0042)); // [nn] = A
        assertEquals(3, cycles); // 3 cycles
    }

    public void testSTAZeroPageX() throws UnknownOpcodeException {
        memory.writeByte((short) 0x1234, (byte) 0x95); // STA nn,X
        memory.writeByte((short) 0x1235, (byte) 0x42); // nn = 0x42
        cpu.a = (byte) 0x84;
        cpu.x = 0x10;

        cpu.pc = (short) 0x1234;
        int cycles = cpu.step();

        assertEquals((byte) 0x84, memory.readByte((short) 0x0052)); // [nn + X] = A
        assertEquals(4, cycles); // 4 cycles
    }

    public void testSTAAbsolute() throws UnknownOpcodeException {
        memory.writeByte((short) 0x1234, (byte) 0x8D); // STA nnnn
        memory.writeWord((short) 0x1235, (short) 0x5678); // nnnn = 0x5678
        cpu.a = (byte) 0x84;

        cpu.pc = (short) 0x1234;
        int cycles = cpu.step();

        assertEquals((byte) 0x84, memory.readByte((short) 0x5678)); // [nnnn] = A
        assertEquals(4, cycles); // 4 cycles
    }

    public void testSTAAbsoluteX() throws UnknownOpcodeException {
        memory.writeByte((short) 0x1234, (byte) 0x9D); // STA nnnn,X
        memory.writeWord((short) 0x1235, (short) 0x5678); // nnnn = 0x5678
        cpu.a = (byte) 0x84;
        cpu.x = 0x10;

        cpu.pc = (short) 0x1234;
        int cycles = cpu.step();

        assertEquals((byte) 0x84, memory.readByte((short) 0x5688)); // [nnnn + X] = A
        assertEquals(5, cycles); // 5 cycles
    }

    public void testSTAAbsoluteY() throws UnknownOpcodeException {
        memory.writeByte((short) 0x1234, (byte) 0x99); // STA nnnn,Y
        memory.writeWord((short) 0x1235, (short) 0x5678); // nnnn = 0x5678
        cpu.a = (byte) 0x84;
        cpu.y = 0x10;

        cpu.pc = (short) 0x1234;
        int cycles = cpu.step();

        assertEquals((byte) 0x84, memory.readByte((short) 0x5688)); // [nnnn + Y] = A
        assertEquals(5, cycles); // 5 cycles
    }

    public void testSTAIndirectX() throws UnknownOpcodeException {
        memory.writeByte((short) 0x1234, (byte) 0x81); // STA (nn,X)
        memory.writeByte((short) 0x1235, (byte) 0x42); // nn = 0x42
        memory.writeWord((short) 0x0052, (short) 0x5678); // 0x42 = 0x5678
        cpu.a = (byte) 0x84;
        cpu.x = 0x10;

        cpu.pc = (short) 0x1234;
        int cycles = cpu.step();

        assertEquals((byte) 0x84, memory.readByte((short) 0x5678)); // [[nn + X]] = A
        assertEquals(6, cycles); // 6 cycles
    }

    public void testSTAIndirectY() throws UnknownOpcodeException {
        memory.writeByte((short) 0x1234, (byte) 0x91); // STA (nn),Y
        memory.writeByte((short) 0x1235, (byte) 0x42); // nn = 0x42
        memory.writeWord((short) 0x42, (short) 0x5678); // 0x42 = 0x5678
        cpu.a = (byte) 0x84;
        cpu.y = 0x10;

        cpu.pc = (short) 0x1234;
        int cycles = cpu.step();

        assertEquals((byte) 0x84, memory.readByte((short) 0x5688)); // [[nn] + Y] = A
        assertEquals(6, cycles); // 6 cycles
    }

    public void testSTXZeroPage() throws UnknownOpcodeException {
        memory.writeByte((short) 0x1234, (byte) 0x86); // STX nn
        memory.writeByte((short) 0x1235, (byte) 0x42); // nn = 0x42
        cpu.x = (byte) 0x84;

        cpu.pc = (short) 0x1234;
        int cycles = cpu.step();

        assertEquals((byte) 0x84, memory.readByte((short) 0x0042)); // [nn] = X
        assertEquals(3, cycles); // 3 cycles
    }

    public void testSTXZeroPageY() throws UnknownOpcodeException {
        memory.writeByte((short) 0x1234, (byte) 0x96); // STX nn,Y
        memory.writeByte((short) 0x1235, (byte) 0x42); // nn = 0x42
        cpu.x = (byte) 0x84;
        cpu.y = 0x10;

        cpu.pc = (short) 0x1234;
        int cycles = cpu.step();

        assertEquals((byte) 0x84, memory.readByte((short) 0x0052)); // [nn + Y] = X
        assertEquals(4, cycles); // 4 cycles
    }

    public void testSTXAbsolute() throws UnknownOpcodeException {
        memory.writeByte((short) 0x1234, (byte) 0x8E); // STX nnnn
        memory.writeWord((short) 0x1235, (short) 0x5678); // nnnn = 0x5678
        cpu.x = (byte) 0x84;

        cpu.pc = (short) 0x1234;
        int cycles = cpu.step();

        assertEquals((byte) 0x84, memory.readByte((short) 0x5678)); // [nnnn] = X
        assertEquals(4, cycles); // 4 cycles
    }

    public void testSTYZeroPage() throws UnknownOpcodeException {
        memory.writeByte((short) 0x1234, (byte) 0x84); // STY nn
        memory.writeByte((short) 0x1235, (byte) 0x42); // nn = 0x42
        cpu.y = (byte) 0x84;

        cpu.pc = (short) 0x1234;
        int cycles = cpu.step();

        assertEquals((byte) 0x84, memory.readByte((short) 0x0042)); // [nn] = Y
        assertEquals(3, cycles); // 3 cycles
    }

    public void testSTYZeroPageX() throws UnknownOpcodeException {
        memory.writeByte((short) 0x1234, (byte) 0x94); // STY nn,X
        memory.writeByte((short) 0x1235, (byte) 0x42); // nn = 0x42
        cpu.y = (byte) 0x84;
        cpu.x = 0x10;

        cpu.pc = (short) 0x1234;
        int cycles = cpu.step();

        assertEquals((byte) 0x84, memory.readByte((short) 0x0052)); // [nn + X] = Y
        assertEquals(4, cycles); // 4 cycles
    }

    public void testSTYAbsolute() throws UnknownOpcodeException {
        memory.writeByte((short) 0x1234, (byte) 0x8C); // STY nnnn
        memory.writeWord((short) 0x1235, (short) 0x5678); // nnnn = 0x5678
        cpu.y = (byte) 0x84;

        cpu.pc = (short) 0x1234;
        int cycles = cpu.step();

        assertEquals((byte) 0x84, memory.readByte((short) 0x5678)); // [nnnn] = Y
        assertEquals(4, cycles); // 4 cycles
    }

    public void testTAX() throws UnknownOpcodeException {
        memory.writeByte((short) 0x1234, (byte) 0xAA); // TAX
        cpu.a = (byte) 0x84;

        cpu.pc = (short) 0x1234;
        int cycles = cpu.step();

        assertEquals((byte) 0x84, cpu.x); // X = A
        assertEquals(2, cycles); // 2 cycles
    }

    public void testTAY() throws UnknownOpcodeException {
        memory.writeByte((short) 0x1234, (byte) 0xA8); // TAY
        cpu.a = (byte) 0x84;

        cpu.pc = (short) 0x1234;
        int cycles = cpu.step();

        assertEquals((byte) 0x84, cpu.y); // Y = A
        assertEquals(2, cycles); // 2 cycles
    }

    public void testTXA() throws UnknownOpcodeException {
        memory.writeByte((short) 0x1234, (byte) 0x8A); // TXA
        cpu.x = (byte) 0x84;

        cpu.pc = (short) 0x1234;
        int cycles = cpu.step();

        assertEquals((byte) 0x84, cpu.a); // A = X
        assertEquals(2, cycles); // 2 cycles
    }

    public void testTYA() throws UnknownOpcodeException {
        memory.writeByte((short) 0x1234, (byte) 0x98); // TYA
        cpu.y = (byte) 0x84;

        cpu.pc = (short) 0x1234;
        int cycles = cpu.step();

        assertEquals((byte) 0x84, cpu.a); // A = Y
        assertEquals(2, cycles); // 2 cycles
    }
}
