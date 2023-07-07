package io.mundt;

import junit.framework.TestCase;

public class MemoryTest extends TestCase {
    private Memory memory;

    public void setUp() {
        memory = new Memory();
    }

    public void testReadWriteByte() {
        memory.writeByte((short) 0x12, (byte) 0x34);
        assertEquals(0x34, memory.readByte((short) 0x12));
    }

    public void testReadWriteWord() {
        memory.writeWord((short) 0x1234, (short) 0x5678);
        assertEquals(0x5678, memory.readWord((short) 0x1234));
    }

    public void testLittleEndianness() {
        memory.writeWord((short) 0x1234, (short) 0x5678);
        assertEquals(0x78, memory.readByte((short) 0x1234));
        assertEquals(0x56, memory.readByte((short) 0x1235));
    }
}
