package org.skhuconnect.petition.similarity.service;

import java.nio.ByteBuffer;

public final class EmbeddingVectorCodec {

    private static final int FLOAT_BYTES = Float.BYTES;

    private EmbeddingVectorCodec() {
    }

    public static byte[] encode(float[] vector) {
        ByteBuffer buffer = ByteBuffer.allocate(vector.length * FLOAT_BYTES);
        for (float value : vector) {
            buffer.putFloat(value);
        }
        return buffer.array();
    }

    public static float[] decode(byte[] bytes, int dimensions) {
        if (bytes.length != dimensions * FLOAT_BYTES) {
            throw new IllegalArgumentException("embedding byte length does not match dimensions");
        }
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        float[] vector = new float[dimensions];
        for (int index = 0; index < dimensions; index++) {
            vector[index] = buffer.getFloat();
        }
        return vector;
    }

    public static float[] decode(byte[] bytes) {
        if (bytes.length % FLOAT_BYTES != 0) {
            throw new IllegalArgumentException("embedding byte length is invalid");
        }
        return decode(bytes, bytes.length / FLOAT_BYTES);
    }

    public static double cosineSimilarity(float[] left, float[] right) {
        if (left.length != right.length) {
            throw new IllegalArgumentException("vector dimensions do not match");
        }
        double dot = 0.0;
        double leftNorm = 0.0;
        double rightNorm = 0.0;
        for (int index = 0; index < left.length; index++) {
            dot += left[index] * right[index];
            leftNorm += left[index] * left[index];
            rightNorm += right[index] * right[index];
        }
        if (leftNorm == 0.0 || rightNorm == 0.0) {
            return 0.0;
        }
        return dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }
}
