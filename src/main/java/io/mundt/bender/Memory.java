package io.mundt.bender;

public class Memory {
    private final byte[] data = new byte[64 * 1024];

    public byte readByte(short address) {
        return data[upscale(address)];
    }

    public void writeByte(short address, byte value) {
        data[upscale(address)] = value;
    }

    public short readWord(short address) {
        int upscaledAddress = upscale(address);
        int low = data[upscaledAddress] & 0xFF;
        int high = data[upscaledAddress + 1] & 0xFF;
        return (short) ((high << 8) | low);
    }

    public void writeWord(short address, short value) {
        int upscaledAddress = upscale(address);
        int low = value & 0xFF;
        int high = (value >> 8) & 0xFF;
        data[upscaledAddress] = (byte) low;
        data[upscaledAddress + 1] = (byte) high;
    }

    private int upscale(short address) {
        return address & 0xFFFF;
    }
}
