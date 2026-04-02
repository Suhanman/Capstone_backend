package com.emailagent.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * MariaDB VECTOR(384) 바이너리 ↔ float[] 변환기
 * - float[] → ByteBuffer: little-endian, 4bytes per float
 * - ByteBuffer → float[]: little-endian 역변환
 *
 * @Component: RecommendService에서 직접 주입받아 byte[] 변환에도 활용
 */
@Component
@Converter(autoApply = false)
public class VectorConverter implements AttributeConverter<float[], byte[]> {

    @Override
    public byte[] convertToDatabaseColumn(float[] attribute) {
        if (attribute == null) {
            return null;
        }
        ByteBuffer buffer = ByteBuffer.allocate(Float.BYTES * attribute.length)
                .order(ByteOrder.LITTLE_ENDIAN);
        for (float v : attribute) {
            buffer.putFloat(v);
        }
        return buffer.array();
    }

    @Override
    public float[] convertToEntityAttribute(byte[] dbData) {
        if (dbData == null) {
            return null;
        }
        ByteBuffer buffer = ByteBuffer.wrap(dbData).order(ByteOrder.LITTLE_ENDIAN);
        float[] result = new float[dbData.length / Float.BYTES];
        for (int i = 0; i < result.length; i++) {
            result[i] = buffer.getFloat();
        }
        return result;
    }
}
