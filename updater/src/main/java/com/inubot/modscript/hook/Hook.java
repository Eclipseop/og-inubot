package com.inubot.modscript.hook;

import java.io.DataOutputStream;
import java.io.IOException;

public abstract class Hook {

    protected interface Type {
        byte FIELD = 0;
        byte INVOKE = 1;
    }

    public final String name;

    public Hook(String name) {
        this.name = name;
    }

    public abstract byte getType();

    public abstract String getOutput();

    protected abstract void writeData(DataOutputStream out) throws IOException;

    protected abstract void writeEncryptedData(DataOutputStream out) throws IOException;

    public void writeToStream(DataOutputStream out) throws IOException {
        out.writeByte(getType());
        writeData(out);
    }

    public void writeToEncryptedStream(DataOutputStream out) throws IOException {
        out.writeByte(getType());
        writeEncryptedData(out);
    }
}
