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
}
