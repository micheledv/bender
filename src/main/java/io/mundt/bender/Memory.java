package io.mundt.bender;

public class Memory {
    private final byte[] data = new byte[64 * 1024];

    public int readByte(int address) {
        return data[address & 0xFFFF] & 0xFF;
    }

    public void writeByte(int address, int value) {
        data[address & 0xFFFF] = (byte) value;
    }

    public int readWord(int address) {
        int low = data[address & 0xFFFF] & 0xFF;
        int high = data[(address & 0xFFFF) + 1] & 0xFF;
        return ((high << 8) | low) & 0xFFFF;
    }

    public void writeWord(int address, int value) {
        byte low = (byte) value;
        byte high = (byte) (value >> 8);
        data[address & 0xFFFF] = low;
        data[(address & 0xFFFF) + 1] = high;
    }
}
