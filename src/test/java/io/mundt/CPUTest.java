package io.mundt;

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
        assertEquals(0x01FF, cpu.sp);

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

    public void testUnknownOpcode() {
        memory.writeByte((short) 0x1234, (byte) 0x00);
        cpu.pc = (short) 0x1234;
        try {
            cpu.step();
            fail("Expected RuntimeException");
        } catch (RuntimeException e) {
            assertEquals("Unknown opcode: 0", e.getMessage());
        }
    }

    public void testLDAImmediate() {
        memory.writeByte((short) 0x1234, (byte) 0xA9); // LDA #nn
        memory.writeByte((short) 0x1235, (byte) 0x42); // #nn = 0x42

        cpu.pc = (short) 0x1234;
        int cycles = cpu.step();

        assertEquals(0x42, cpu.a); // A = #nn
        assertFalse(cpu.zeroFlag); // Z = false
        assertFalse(cpu.negativeFlag); // N = false
        assertEquals(2, cycles); // 2 cycles
    }

    public void testLDAZeroPage() {
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

    public void testLDAZeroPageX() {
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

    public void testLDAAbsolute() {
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

    public void testLDAAbsoluteXWithinPage() {
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

    public void testLDAAbsoluteXCrossingPage() {
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

    public void testLDAAbsoluteYWithinPage() {
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

    public void testLDAAbsoluteYCrossingPage() {
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

    public void testLDAIndirectX() {
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

    public void testLDAIndirectYWithinPage() {
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

    public void testLDAIndirectYCrossingPage() {
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
}
