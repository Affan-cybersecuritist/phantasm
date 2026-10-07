package com.phantasm.tools;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Utility for parsing Java `.class` binary files (JVM bytecode)
 * to automatically extract internal fully qualified class names.
 */
public class ClassFileHelper {

    /**
     * Extracts the fully qualified class name (e.g. "com.example.MyClass") from a .class file.
     *
     * @param classFilePath Path to the .class file
     * @return Fully qualified class name
     */
    public static String extractClassName(Path classFilePath) throws IOException {
        byte[] bytes = Files.readAllBytes(classFilePath);
        return extractClassName(bytes);
    }

    /**
     * Extracts the fully qualified class name from raw bytecode.
     *
     * @param classBytes raw byte array of compiled .class file
     * @return Fully qualified class name
     */
    public static String extractClassName(byte[] classBytes) throws IOException {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(classBytes))) {
            int magic = in.readInt();
            if (magic != 0xCAFEBABE) {
                throw new IllegalArgumentException("Invalid Java .class file magic header: 0x" + Integer.toHexString(magic));
            }

            in.readUnsignedShort(); // minor_version
            in.readUnsignedShort(); // major_version

            int constantPoolCount = in.readUnsignedShort();
            Object[] constantPool = new Object[constantPoolCount];
            int[] classInfoNameIndex = new int[constantPoolCount];

            // 1-indexed constant pool
            for (int i = 1; i < constantPoolCount; i++) {
                int tag = in.readUnsignedByte();
                switch (tag) {
                    case 1: // CONSTANT_Utf8
                        int length = in.readUnsignedShort();
                        byte[] utf8Bytes = new byte[length];
                        in.readFully(utf8Bytes);
                        constantPool[i] = new String(utf8Bytes, "UTF-8");
                        break;
                    case 3: // CONSTANT_Integer
                    case 4: // CONSTANT_Float
                        in.skipBytes(4);
                        break;
                    case 5: // CONSTANT_Long
                    case 6: // CONSTANT_Double
                        in.skipBytes(8);
                        i++; // Long and Double take 2 constant pool entries
                        break;
                    case 7: // CONSTANT_Class
                        classInfoNameIndex[i] = in.readUnsignedShort();
                        break;
                    case 8: // CONSTANT_String
                    case 16: // CONSTANT_MethodType
                    case 19: // CONSTANT_Module
                    case 20: // CONSTANT_Package
                        in.skipBytes(2);
                        break;
                    case 9: // CONSTANT_Fieldref
                    case 10: // CONSTANT_Methodref
                    case 11: // CONSTANT_InterfaceMethodref
                    case 12: // CONSTANT_NameAndType
                    case 17: // CONSTANT_Dynamic
                    case 18: // CONSTANT_InvokeDynamic
                        in.skipBytes(4);
                        break;
                    case 15: // CONSTANT_MethodHandle
                        in.skipBytes(3);
                        break;
                    default:
                        throw new IOException("Unsupported constant pool tag: " + tag + " at index " + i);
                }
            }

            in.readUnsignedShort(); // access_flags
            int thisClassIndex = in.readUnsignedShort();

            int nameIndex = classInfoNameIndex[thisClassIndex];
            String internalName = (String) constantPool[nameIndex];
            if (internalName == null) {
                throw new IOException("Failed to resolve class name from constant pool index " + nameIndex);
            }

            return internalName.replace('/', '.');
        }
    }
}
