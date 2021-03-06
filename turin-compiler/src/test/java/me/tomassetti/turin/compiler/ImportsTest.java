package me.tomassetti.turin.compiler;

import me.tomassetti.turin.compiler.errorhandling.ErrorCollector;
import me.tomassetti.turin.parser.ast.Position;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedList;

import static org.junit.Assert.*;
import static org.easymock.EasyMock.*;

public class ImportsTest extends AbstractCompilerTest {

    @Before
    public void setup() {
        errorCollector = new MyErrorCollector();
    }

    @Test
    public void importOfTypeWithoutAlias() throws NoSuchMethodException, IOException, InvocationTargetException, IllegalAccessException {
        Method method = compileFunction("importOfTypeWithoutAlias", new Class[]{});
        Object res = method.invoke(null);
        assertTrue(res instanceof LinkedList);
    }

    @Test
    public void importOfTypeWithAlias() throws NoSuchMethodException, IOException, InvocationTargetException, IllegalAccessException {
        Method method = compileFunction("importOfTypeWithAlias", new Class[]{});
        Object res = method.invoke(null);
        assertTrue(res instanceof LinkedList);
    }

    @Test
    public void importOfTypesInPackage() throws NoSuchMethodException, IOException, InvocationTargetException, IllegalAccessException {
        Method method = compileFunction("importOfTypesInPackage", new Class[]{});
        Object res = method.invoke(null);
        assertTrue(res instanceof LinkedList);
    }

    @Test
    public void importOfFieldsInTypeWithoutAlias() throws NoSuchMethodException, IOException, InvocationTargetException, IllegalAccessException {
        Method method = compileFunction("importOfFieldsInTypeWithoutAlias", new Class[]{});
        Object res = method.invoke(null);
        assertTrue(res instanceof PrintStream);
        assertEquals(System.out, res);
    }

    @Test
    public void importOfFieldsInTypeWithAlias() throws NoSuchMethodException, IOException, InvocationTargetException, IllegalAccessException {
        Method method = compileFunction("importOfFieldsInTypeWithAlias", new Class[]{});
        Object res = method.invoke(null);
        assertTrue(res instanceof PrintStream);
        assertEquals(System.out, res);
    }

    @Test
    public void importOfAllFieldsInType() throws NoSuchMethodException, IOException, InvocationTargetException, IllegalAccessException {
        Method method = compileFunction("importOfAllFieldsInType", new Class[]{});
        Object res = method.invoke(null);
        assertTrue(res instanceof PrintStream);
        assertEquals(System.out, res);
    }

    private ErrorCollector errorCollector;

    @Override
    protected ErrorCollector getErrorCollector() {
        return errorCollector;
    }

    @Test
    public void importOfAllFieldsInUnexistingType() throws NoSuchMethodException, IOException, InvocationTargetException, IllegalAccessException {
        errorCollector = createMock(ErrorCollector.class);
        errorCollector.recordSemanticError(Position.create(3, 0, 3, 36), "Import not resolved: java.lang.SystemUnexisting");
        replay(errorCollector);
        attemptToCompile("importOfAllFieldsInUnexistingType", Collections.emptyList());
        verify(errorCollector);
    }

    @Test
    public void importOfTypesInPackageInUnexistingPackage() throws NoSuchMethodException, IOException, InvocationTargetException, IllegalAccessException {
        errorCollector = createMock(ErrorCollector.class);
        errorCollector.recordSemanticError(Position.create(3, 0, 3, 24), "Import not resolved: foo.unexisting");
        replay(errorCollector);
        attemptToCompile("importOfTypesFromUnexistingPackage", Collections.emptyList());
        verify(errorCollector);
    }

    @Test
    public void importOfUnexistingField() throws NoSuchMethodException, IOException, InvocationTargetException, IllegalAccessException {
        errorCollector = createMock(ErrorCollector.class);
        errorCollector.recordSemanticError(Position.create(3, 0, 3, 28), "Import not resolved: java.lang.System.baz");
        replay(errorCollector);
        attemptToCompile("importOfUnexistingField", Collections.emptyList());
        verify(errorCollector);
    }

    @Test
    public void importOfUnexistingType() throws NoSuchMethodException, IOException, InvocationTargetException, IllegalAccessException {
        errorCollector = createMock(ErrorCollector.class);
        errorCollector.recordSemanticError(Position.create(3, 0, 3, 27), "Import not resolved: java.lang.SystemBaz");
        replay(errorCollector);
        attemptToCompile("importOfUnexistingType", Collections.emptyList());
        verify(errorCollector);
    }

}
