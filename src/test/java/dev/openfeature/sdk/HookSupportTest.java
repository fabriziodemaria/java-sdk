package dev.openfeature.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import dev.openfeature.sdk.fixtures.HookFixtures;

class HookSupportTest implements HookFixtures {

    @Test
    @DisplayName("should merge EvaluationContexts on before hooks correctly")
    void shouldMergeEvaluationContextsOnBeforeHooksCorrectly() {
        Map<String, Value> attributes = new HashMap<>();
        attributes.put("baseKey", new Value("baseValue"));
        EvaluationContext baseContext = new ImmutableContext(attributes);
        HookContext<String> hookContext = new HookContext<>("flagKey", FlagValueType.STRING, "defaultValue", baseContext, () -> "client", () -> "provider");
        Hook<String> stringHook = new TestStringHook();
        Hook<Boolean> boolHook = new TestBoolHook();
        HookSupport hookSupport = new HookSupport();

        EvaluationContext result = hookSupport.beforeHooks(FlagValueType.STRING, hookContext, Arrays.asList(stringHook, boolHook), Collections.emptyMap());

        assertThat(result.getValue("bla").asString()).isEqualTo("defaultValueTest");
//        assertTrue(result.getValue("foo").asBoolean()); ???
        assertThat(result.getValue("baseKey").asString()).isEqualTo("baseValue");
    }

    static class TestStringHook implements Hook<String> {
        @Override
        public Optional<EvaluationContext> before(HookContext<String> ctx,
            Map<String, Object> hints) {
            String dv = ctx.getDefaultValue();
            Map<String, Value> attributes = new HashMap<>();
            attributes.put("bla", new Value(dv + "Test"));
            EvaluationContext baseContext = new ImmutableContext(attributes);
            return Optional.of(baseContext);
        }
    }

    static class TestBoolHook implements Hook<Boolean> {
        @Override
        public Optional<EvaluationContext> before(HookContext<Boolean> ctx,
            Map<String, Object> hints) {
            Boolean dv = ctx.getDefaultValue();
            Map<String, Value> attributes = new HashMap<>();
            attributes.put("foo", new Value(dv));
            EvaluationContext baseContext = new ImmutableContext(attributes);
            return Optional.of(baseContext);
        }

    }

    @ParameterizedTest
    @EnumSource(value = FlagValueType.class)
    @DisplayName("should always call generic hook")
    void shouldAlwaysCallGenericHook(FlagValueType flagValueType) {
        Hook<?> genericHook = mockGenericHook();
        HookSupport hookSupport = new HookSupport();
        EvaluationContext baseContext = new ImmutableContext();
        IllegalStateException expectedException = new IllegalStateException("All fine, just a test");
        HookContext<Object> hookContext = new HookContext<>("flagKey", flagValueType, createDefaultValue(flagValueType), baseContext, () -> "client", () -> "provider");

        hookSupport.beforeHooks(flagValueType, hookContext, Collections.singletonList(genericHook), Collections.emptyMap());
        hookSupport.afterHooks(flagValueType, hookContext, FlagEvaluationDetails.builder().build(), Collections.singletonList(genericHook), Collections.emptyMap());
        hookSupport.afterAllHooks(flagValueType, hookContext, Collections.singletonList(genericHook), Collections.emptyMap());
        hookSupport.errorHooks(flagValueType, hookContext, expectedException, Collections.singletonList(genericHook), Collections.emptyMap());

        verify(genericHook).before(any(), any());
        verify(genericHook).after(any(), any(), any());
        verify(genericHook).finallyAfter(any(), any());
        verify(genericHook).error(any(), any(), any());
    }

    private Object createDefaultValue(FlagValueType flagValueType) {
        switch (flagValueType) {
            case INTEGER:
                return 1;
            case BOOLEAN:
                return true;
            case STRING:
                return "defaultValue";
            case OBJECT:
                return "object";
            case DOUBLE:
                return "double";
            default:
                throw new IllegalArgumentException();
        }
    }

    private EvaluationContext evaluationContextWithValue(String key, String value) {
        Map<String, Value> attributes = new HashMap<>();
        attributes.put(key, new Value(value));
        EvaluationContext baseContext = new ImmutableContext(attributes);
        return baseContext;
    }

}
