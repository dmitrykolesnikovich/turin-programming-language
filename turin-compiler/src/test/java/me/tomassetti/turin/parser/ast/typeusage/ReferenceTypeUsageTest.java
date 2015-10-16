package me.tomassetti.turin.parser.ast.typeusage;

import me.tomassetti.turin.implicit.BasicTypeUsage;
import me.tomassetti.turin.parser.analysis.resolvers.InFileSymbolResolver;
import me.tomassetti.turin.parser.analysis.resolvers.jdk.JdkTypeResolver;
import me.tomassetti.turin.parser.analysis.resolvers.SymbolResolver;
import me.tomassetti.turin.parser.ast.*;
import me.tomassetti.turin.parser.analysis.resolvers.jdk.ReflectionTypeDefinitionFactory;
import me.tomassetti.turin.parser.ast.properties.PropertyDefinition;
import me.tomassetti.turin.parser.ast.properties.PropertyReference;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;

public class ReferenceTypeUsageTest {

    private PropertyReference nameRef;
    private PropertyDefinition ageProperty;

    @Before
    public void setup() {
        // define AST
        TurinFile turinFile = new TurinFile();

        NamespaceDefinition namespaceDefinition = new NamespaceDefinition("manga");

        turinFile.setNameSpace(namespaceDefinition);

        ReferenceTypeUsageNode stringType = new ReferenceTypeUsageNode("String");
        BasicTypeUsage intType = BasicTypeUsage.UINT;

        PropertyDefinition nameProperty = new PropertyDefinition("name", stringType, Optional.empty(), Optional.empty(), Collections.emptyList());

        turinFile.add(nameProperty);

        TurinTypeDefinition mangaCharacter = new TurinTypeDefinition("MangaCharacter");
        ageProperty = new PropertyDefinition("age", intType, Optional.empty(), Optional.empty(), Collections.emptyList());
        nameRef = new PropertyReference("name");
        mangaCharacter.add(nameRef);
        mangaCharacter.add(ageProperty);

        turinFile.add(mangaCharacter);
    }

    @Test
    public void javaType() {
        SymbolResolver resolver = new InFileSymbolResolver(JdkTypeResolver.getInstance());
        assertEquals("Ljava/lang/String;", nameRef.getType(resolver).jvmType(resolver).getSignature());
        assertEquals("I", ageProperty.getType().jvmType(resolver).getSignature());
    }

    @Test
    public void isInterfaceNegativeCase() {
        TypeDefinition typeDefinition = ReflectionTypeDefinitionFactory.getInstance().getTypeDefinition(String.class);
        ReferenceTypeUsageNode typeUsage = new ReferenceTypeUsageNode(typeDefinition);
        assertEquals(false, typeUsage.isInterface(new InFileSymbolResolver(JdkTypeResolver.getInstance())));
    }

    @Test
    public void isInterfacePositiveCase() {
        TypeDefinition typeDefinition = ReflectionTypeDefinitionFactory.getInstance().getTypeDefinition(List.class);
        ReferenceTypeUsageNode typeUsage = new ReferenceTypeUsageNode(typeDefinition);
        assertEquals(true, typeUsage.isInterface(new InFileSymbolResolver(JdkTypeResolver.getInstance())));
    }

}
