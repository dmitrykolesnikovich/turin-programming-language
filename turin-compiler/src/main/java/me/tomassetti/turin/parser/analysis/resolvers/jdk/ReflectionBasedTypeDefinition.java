package me.tomassetti.turin.parser.analysis.resolvers.jdk;

import me.tomassetti.turin.compiler.errorhandling.SemanticErrorException;
import me.tomassetti.jvm.JvmConstructorDefinition;
import me.tomassetti.jvm.JvmMethodDefinition;
import me.tomassetti.jvm.JvmType;
import me.tomassetti.turin.definitions.InternalConstructorDefinition;
import me.tomassetti.turin.definitions.InternalMethodDefinition;
import me.tomassetti.turin.definitions.TypeDefinition;
import me.tomassetti.turin.parser.analysis.exceptions.UnsolvedSymbolException;
import me.tomassetti.turin.parser.analysis.resolvers.SymbolResolver;
import me.tomassetti.turin.parser.ast.Node;
import me.tomassetti.turin.parser.ast.TypeDefinitionNode;
import me.tomassetti.turin.parser.ast.expressions.ActualParam;
import me.tomassetti.turin.parser.ast.expressions.Invokable;
import me.tomassetti.turin.symbols.FormalParameter;
import me.tomassetti.turin.symbols.FormalParameterSymbol;
import me.tomassetti.turin.typesystem.FunctionReferenceTypeUsage;
import me.tomassetti.turin.typesystem.ReferenceTypeUsage;
import me.tomassetti.turin.typesystem.TypeUsage;

import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;

class ReflectionBasedTypeDefinition extends TypeDefinitionNode {

    private Class<?> clazz;
    private List<TypeUsage> typeParameters = new LinkedList<>();

    public ReflectionBasedTypeDefinition(Class<?> clazz) {
        super(clazz.getCanonicalName());
        this.clazz = clazz;
    }

    public void addTypeParameter(TypeUsage typeUsage) {
        typeParameters.add(typeUsage);
    }

    private static TypeUsage typeFor(List<Method> methods, Node parentToAssign) {
        if (methods.isEmpty()) {
            throw new IllegalArgumentException();
        }
        methods.forEach((m)-> {
            if (!Modifier.isStatic(m.getModifiers())) {
                throw new IllegalArgumentException("Non static method given: " + m);
            }
        });
        if (methods.size() != 1) {
            throw new UnsupportedOperationException();
        }
        return typeFor(methods.get(0), parentToAssign);
    }

    private static TypeUsage typeFor(Method method, Node parentToAssign) {
        List<TypeUsage> paramTypes = Arrays.stream(method.getGenericParameterTypes()).map((pt)->toTypeUsage(pt)).collect(Collectors.toList());
        FunctionReferenceTypeUsage functionReferenceTypeUsage = new FunctionReferenceTypeUsage(paramTypes, toTypeUsage(method.getGenericReturnType()));
        return functionReferenceTypeUsage;
    }

    private static TypeUsage toTypeUsage(Type type) {
        return ReflectionBasedMethodResolution.toTypeUsage(type, Collections.emptyMap());
    }

    private static TypeUsage toTypeUsage(TypeVariable typeVariable) {
        return ReflectionBasedMethodResolution.toTypeUsage(typeVariable, Collections.emptyMap());
    }

    @Override
    public Optional<List<? extends FormalParameter>> findFormalParametersFor(Invokable invokable, SymbolResolver resolver) {
        return super.findFormalParametersFor(invokable, resolver);
    }

    @Override
    public boolean hasField(String fieldName, boolean staticContext) {
        for (Field field : clazz.getFields()) {
            if (field.getName().equals(fieldName)) {
                if (Modifier.isStatic(field.getModifiers()) == staticContext) {
                    return true;
                }
            }
        }

        for (Method method : clazz.getMethods()) {
            if (method.getName().equals(fieldName)) {
                if (Modifier.isStatic(method.getModifiers()) == staticContext) {
                    return true;
                }
            }
        }

        // TODO consider inherited fields and methods
        return false;
    }

    @Override
    public List<InternalConstructorDefinition> getConstructors(SymbolResolver resolver) {
        return Arrays.stream(clazz.getConstructors())
                .map((c) -> toInternalConstructorDefinition(c))
                .collect(Collectors.toList());
    }

    @Override
    public boolean canFieldBeAssigned(String field, SymbolResolver resolver) {
        return true;
    }

    @Override
    public TypeDefinition getSuperclass(SymbolResolver resolver) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T extends TypeUsage> Map<String, TypeUsage> associatedTypeParametersToName(SymbolResolver resolver, List<T> typeParams) {
        if (typeParams.isEmpty()) {
            return Collections.emptyMap();
        }
        if (clazz.getTypeParameters().length != typeParams.size()) {
            throw new IllegalStateException("It should have " + clazz.getTypeParameters().length + " and it has " + typeParams.size());
        }
        Map<String, TypeUsage> map = new HashMap<>();
        int i=0;
        for (TypeVariable tv : clazz.getTypeParameters()) {
            map.put(tv.getName(), typeParams.get(i));
            i++;
        }
        return map;
    }

    @Override
    public Optional<InternalConstructorDefinition> findConstructor(List<ActualParam> actualParams, SymbolResolver resolver) {
        Constructor constructor = ReflectionBasedMethodResolution.findConstructorAmongActualParams(
                actualParams, resolver, Arrays.asList(clazz.getConstructors()), this);
        return Optional.of(toInternalConstructorDefinition(constructor));
    }

    private InternalConstructorDefinition toInternalConstructorDefinition(Constructor<?> constructor) {
        JvmConstructorDefinition jvmConstructorDefinition = ReflectionTypeDefinitionFactory.toConstructorDefinition(constructor);
        return new InternalConstructorDefinition(formalParameters(constructor), jvmConstructorDefinition);
    }

    @Override
    public String toString() {
        return "ReflectionBasedTypeDefinition{" +
                "clazz=" + clazz +
                '}';
    }

    @Override
    public String getQualifiedName() {
        return clazz.getCanonicalName();
    }

    @Override
    public JvmMethodDefinition findMethodFor(String name, List<JvmType> argsTypes, SymbolResolver resolver, boolean staticContext) {
        return ReflectionTypeDefinitionFactory.toMethodDefinition(ReflectionBasedMethodResolution.findMethodAmong(name, argsTypes, resolver, staticContext, Arrays.asList(clazz.getMethods()), this));
    }

    @Override
    public JvmConstructorDefinition resolveConstructorCall(SymbolResolver resolver, List<ActualParam> actualParams) {
        try {
            List<JvmType> argsTypes = new ArrayList<>();
            for (ActualParam actualParam : actualParams) {
                if (actualParam.isNamed()) {
                    throw new SemanticErrorException(actualParam, "It is not possible to use named parameters on Java classes");
                } else {
                    argsTypes.add(actualParam.getValue().calcType().jvmType(resolver));
                }
            }
            return ReflectionBasedMethodResolution.findConstructorAmong(argsTypes, resolver, Arrays.asList(clazz.getConstructors()), this);
        } catch (RuntimeException e){
            throw new RuntimeException("Resolving constructor call on " + clazz.getCanonicalName(), e);
        }
    }

    @Override
    public boolean isMethodOverloaded(String methodName, SymbolResolver resolver) {
        return Arrays.stream(clazz.getMethods()).filter((m)->m.getName().equals(methodName)).count() > 1;
    }

    @Override
    public Optional<InternalMethodDefinition> findMethod(String methodName, List<ActualParam> actualParams, SymbolResolver resolver, boolean staticContext) {
        Optional<Method> res = ReflectionBasedMethodResolution.findMethodAmongActualParams(methodName, actualParams, resolver, staticContext, Arrays.asList(clazz.getMethods()), this);
        if (res.isPresent()) {
            return Optional.of(toInternalMethodDefinition(res.get()));
        } else {
            return Optional.empty();
        }
    }

    private InternalMethodDefinition toInternalMethodDefinition(Method method) {
        return new InternalMethodDefinition(method.getName(), formalParameters(method), toTypeUsage(method.getGenericReturnType()),
                ReflectionTypeDefinitionFactory.toMethodDefinition(method));
    }

    private List<FormalParameterSymbol> formalParameters(Constructor constructor) {
        return ReflectionBasedMethodResolution.formalParameters(constructor);
    }

    private List<FormalParameterSymbol> formalParameters(Method method) {
        return ReflectionBasedMethodResolution.formalParameters(method, getTypeVariables());
    }

    // Type parameters should be part of the usage
    @Deprecated
    private Map<String, TypeUsage> getTypeVariables() {
        Map<String, TypeUsage> map = new HashMap<>();
        if (clazz.getTypeParameters().length != typeParameters.size()) {
            throw new IllegalStateException("It should have " + clazz.getTypeParameters().length + " and it has " + typeParameters.size());
        }
        int i=0;
        for (TypeVariable tv : clazz.getTypeParameters()) {
            map.put(tv.getName(), typeParameters.get(i));
            i++;
        }
        return map;
    }

    @Override
    public TypeUsage getFieldType(String fieldName, boolean staticContext, SymbolResolver resolver) {
        for (Field field : clazz.getFields()) {
            if (field.getName().equals(fieldName)) {
                if (Modifier.isStatic(field.getModifiers()) == staticContext) {
                    return ReflectionTypeDefinitionFactory.toTypeUsage(field.getType());
                }
            }
        }

        List<Method> methods = new LinkedList<>();
        for (Method method : clazz.getMethods()) {
            if (method.getName().equals(fieldName)) {
                if (Modifier.isStatic(method.getModifiers()) == staticContext) {
                    methods.add(method);
                }
            }
        }
        if (!methods.isEmpty()) {
            return ReflectionBasedTypeDefinition.typeFor(methods, this);
        }

        // TODO consider inherited fields and methods
        throw new UnsupportedOperationException(fieldName);
    }

    @Override
    public List<ReferenceTypeUsage> getAllAncestors(SymbolResolver resolver) {
        List<ReferenceTypeUsage> ancestors = new ArrayList<>();
        if (clazz.getSuperclass() != null) {
            ReferenceTypeUsage superTypeDefinition = toReferenceTypeUsage(clazz.getSuperclass(), clazz.getGenericSuperclass());
            ancestors.add(superTypeDefinition);
            ancestors.addAll(superTypeDefinition.getAllAncestors(resolver));
        }
        int i = 0;
        for (Class<?> interfaze : clazz.getInterfaces()) {
            Type genericInterfaze = clazz.getGenericInterfaces()[i];
            ReferenceTypeUsage superTypeDefinition = toReferenceTypeUsage(interfaze, genericInterfaze);
            ancestors.add(superTypeDefinition);
            ancestors.addAll(superTypeDefinition.getAllAncestors(resolver));
            i++;
        }
        return ancestors;
    }

    @Override
    public boolean isInterface() {
        return clazz.isInterface();
    }

    @Override
    public boolean isClass() {
        return !clazz.isInterface() && !clazz.isEnum() && !clazz.isAnnotation() && !clazz.isArray() && !clazz.isPrimitive();
    }

    private ReferenceTypeUsage toReferenceTypeUsage(Class<?> clazz, Type type) {
        TypeDefinition typeDefinition = new ReflectionBasedTypeDefinition(clazz);
        ReferenceTypeUsage referenceTypeUsage = new ReferenceTypeUsage(typeDefinition);
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType)type;
            for (int tp=0;tp<clazz.getTypeParameters().length;tp++) {
                TypeVariable<? extends Class<?>> typeVariable = clazz.getTypeParameters()[tp];
                Type parameterType = parameterizedType.getActualTypeArguments()[tp];
                referenceTypeUsage.getTypeParameterValues().add(typeVariable.getName(), toTypeUsage(parameterType));
            }
        }
        return referenceTypeUsage;
    }

    @Override
    public Iterable<Node> getChildren() {
        return Collections.emptyList();
    }

    @Override
    public Node getField(String fieldName, SymbolResolver resolver) {
        return internalGetField(fieldName, null);
    }

    @Override
    public Node getFieldOnInstance(String fieldName, Node instance, SymbolResolver resolver) {
        return internalGetField(fieldName, instance);
    }

    /**
     * Instance null means get static fields.
     */
    private Node internalGetField(String fieldName, Node instance) {
        boolean isStatic = instance == null;
        for (Field field : clazz.getFields()) {
            if (field.getName().equals(fieldName) && Modifier.isStatic(field.getModifiers()) == isStatic) {
                ReflectionBasedField rbf = new ReflectionBasedField(field);
                rbf.setParent(instance);
                return rbf;
            }
        }
        List<Method> matchingMethods = new ArrayList<>();
        for (Method method : clazz.getMethods()) {
            if (method.getName().equals(fieldName) && Modifier.isStatic(method.getModifiers()) == isStatic){
                matchingMethods.add(method);
            }
        }
        if (matchingMethods.isEmpty()) {
            // TODO improve the error returned
            throw new UnsolvedSymbolException(fieldName);
        } else {
            ReflectionBasedSetOfOverloadedMethods rbsoom = new ReflectionBasedSetOfOverloadedMethods(matchingMethods, instance);
            rbsoom.setParent(instance);
            return rbsoom;
        }
    }
}
