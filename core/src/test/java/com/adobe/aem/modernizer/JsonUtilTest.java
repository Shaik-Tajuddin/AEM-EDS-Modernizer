package com.adobe.aem.modernizer;

import com.adobe.aem.modernizer.util.JsonSerializationException;
import com.adobe.aem.modernizer.util.JsonUtil;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JsonUtilTest {

    @Test
    void testSerializationAndDeserialization() {
        Map<String, String> data = Collections.singletonMap("key", "value");
        String json = JsonUtil.toJson(data);
        assertThat(json).contains("\"key\":\"value\"");

        String pretty = JsonUtil.toPrettyJson(data);
        assertThat(pretty).contains("\"key\" : \"value\"");

        Map<?, ?> parsed = JsonUtil.fromJson(json, Map.class);
        assertThat(parsed).isNotNull();
        assertThat(parsed.get("key")).isEqualTo("value");

        InputStream is = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        Map<?, ?> fromStream = JsonUtil.fromJson(is, Map.class);
        assertThat(fromStream).isNotNull();
        assertThat(fromStream.get("key")).isEqualTo("value");
    }

    @Test
    void testNullAndEmptyInputs() {
        assertThat(JsonUtil.toJson(null)).isEqualTo("null");
        assertThat(JsonUtil.toPrettyJson(null)).isEqualTo("null");
        assertThat(JsonUtil.fromJson("", Map.class)).isNull();
        assertThat(JsonUtil.fromJson((String) null, Map.class)).isNull();
        assertThat(JsonUtil.fromJson((InputStream) null, Map.class)).isNull();
        assertThat(JsonUtil.mapper()).isNotNull();
    }

    @Test
    void testSerializationExceptions() {
        Object selfReferencing = new Object() {
            public Object getSelf() { return this; }
        };
        assertThatThrownBy(() -> JsonUtil.toJson(selfReferencing))
                .isInstanceOf(JsonSerializationException.class);
        assertThatThrownBy(() -> JsonUtil.toPrettyJson(selfReferencing))
                .isInstanceOf(JsonSerializationException.class);
        assertThatThrownBy(() -> JsonUtil.fromJson("invalid json", Map.class))
                .isInstanceOf(JsonSerializationException.class);
        assertThatThrownBy(() -> JsonUtil.fromJson(new ByteArrayInputStream("invalid".getBytes()), Map.class))
                .isInstanceOf(JsonSerializationException.class);

        JsonSerializationException ex1 = new JsonSerializationException("msg");
        assertThat(ex1.getMessage()).isEqualTo("msg");
        JsonSerializationException ex2 = new JsonSerializationException("msg2", new RuntimeException("cause"));
        assertThat(ex2.getCause()).isNotNull();
    }
}
