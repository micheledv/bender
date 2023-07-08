package io.mundt.bender;

public class Memory {
    private final byte[] data = new byte[64 * 1024];

    public byte readByte(short address) {
        return data[address & 0xFFFF];
    }

    public void writeByte(short address, byte value) {
        data[address & 0xFFFF] = value;
    }

    public short readWord(short address) {
        int low = data[address & 0xFFFF] & 0xFF;
        int high = data[(address & 0xFFFF) + 1] & 0xFF;
        return (short) ((high << 8) | low);
    }

    public void writeWord(short address, short value) {
        int low = value & 0xFF;
        int high = (value >> 8) & 0xFF;
        data[address & 0xFFFF] = (byte) low;
        data[(address & 0xFFFF) + 1] = (byte) high;
    }
}
