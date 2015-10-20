package me.tomassetti.turin.typesystem;

import me.tomassetti.jvm.JvmType;
import me.tomassetti.turin.definitions.InternalInvokableDefinition;
import me.tomassetti.turin.parser.ast.expressions.ActualParam;
import me.tomassetti.turin.symbols.Symbol;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * A reference to something that has a first goal to be invoked.
 * This could represent a function for example.
 * It represents something not overloaded.
 */
public class InvokableReferenceTypeUsage implements TypeUsage, InvokableType {

    private InternalInvokableDefinition internalInvokableDefinition;

    @Override
    public <T extends TypeUsage> TypeUsage replaceTypeVariables(Map<String, T> typeParams) {
        throw new UnsupportedOperationException();
    }

    public InvokableReferenceTypeUsage(InternalInvokableDefinition internalInvokableDefinition) {
        this.internalInvokableDefinition = internalInvokableDefinition;
    }

    /**
     * As a type it should consider only: if it is overloaded and if given parameters
     * with the same type return the same type.
     */
    @Override
    public boolean sameType(TypeUsage other) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isInvokable() {
        return true;
    }

    @Override
    public InvokableType asInvokable() {
        return this;
    }

    @Override
    public JvmType jvmType() {
        throw new UnsupportedOperationException("It has not a direct correspondent on the JVM");
    }

    @Override
    public boolean hasInstanceField(String fieldName, Symbol instance) {
        return false;
    }

    @Override
    public Symbol getInstanceField(String fieldName, Symbol instance) {
        throw new IllegalArgumentException(describe() + " has no field named " + fieldName);
    }

    @Override
    public Optional<InvokableType> getMethod(String method, boolean staticContext) {
        return Optional.empty();
    }

    @Override
    public boolean canBeAssignedTo(TypeUsage type) {
        return false;
    }

    @Override
    public boolean isOverloaded() {
        return false;
    }

    @Override
    public Optional<InternalInvokableDefinition> internalInvokableDefinitionFor(List<ActualParam> actualParams) {
        return Optional.of(internalInvokableDefinition);
    }

    @Override
    public String describe() {
        if (internalInvokableDefinition.isMethod()) {
            return internalInvokableDefinition.asMethod().getMethodName() + "(" +
                    String.join(", " , internalInvokableDefinition.getFormalParameters().stream().map((fp)->fp.getType().describe()).collect(Collectors.toList())) + ") -> " +
                    internalInvokableDefinition.getReturnType().describe();
        } else if (internalInvokableDefinition.isConstructor()) {
            return internalInvokableDefinition.asConstructor().getReturnType().describe() + "(" +
                    String.join(", " , internalInvokableDefinition.getFormalParameters().stream().map((fp)->fp.getType().describe()).collect(Collectors.toList()));
        } else {
            throw new UnsupportedOperationException();
        }
    }
}
