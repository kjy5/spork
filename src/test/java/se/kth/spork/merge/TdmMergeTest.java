package se.kth.spork.merge;

import com.github.gumtreediff.tree.ITree;
import gumtree.spoon.builder.SpoonGumTreeBuilder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import spoon.Launcher;
import spoon.reflect.declaration.CtClass;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class TdmMergeTest {
    private static final Path cleanMergeDirpath = Paths.get("src/test/resources/clean");
    private static final Path leftModifiedDirpath = cleanMergeDirpath.resolve("left_modified");
    private static final Path rightModifiedDirpath = cleanMergeDirpath.resolve("right_modified");
    private static final Path bothModifiedDirpath = cleanMergeDirpath.resolve("both_modified");

    @ParameterizedTest
    @ValueSource(strings = {"add_parameter"})
    void mergeToTree_shouldReturnExpectedTree_whenLeftVersionIsModified(String testName) throws IOException{
        File testDir = leftModifiedDirpath.resolve(testName).toFile();
        runTestMerge(testDir);
    }

    @ParameterizedTest
    @ValueSource(strings = {"add_parameter"})
    void mergeToTree_shouldReturnExpectedTree_whenRightVersionIsModified(String testName) throws IOException{
        File testDir = rightModifiedDirpath.resolve(testName).toFile();
        runTestMerge(testDir);
    }

    @ParameterizedTest
    @ValueSource(strings = {"move_if"})
    void mergeToTree_shouldReturnExpectedTree_whenBothVersionsAreModified(String testName) throws IOException {
        File testDir = bothModifiedDirpath.resolve(testName).toFile();
        runTestMerge(testDir);
    }

    private static void runTestMerge(File testDir) throws IOException {
        TestSources sources = getTestSources(testDir);
        ITree expected = toGumTree(sources.expected);

        CtClass<?> base = Launcher.parseClass(sources.base);
        CtClass<?> left = Launcher.parseClass(sources.left);
        CtClass<?> right = Launcher.parseClass(sources.right);

        ITree merged = TdmMerge.mergeToTree(base, left, right);

        assertTrue(merged.isIsomorphicTo(expected));
    }

    private static TestSources getTestSources(File testDir) throws IOException {
        Path path = testDir.toPath();
        return new TestSources(
                read(path.resolve("Base.java")),
                read(path.resolve("Left.java")),
                read(path.resolve("Right.java")),
                read(path.resolve("Expected.java"))
        );
    }

    private static String read(Path path) throws IOException {
        return String.join("\n", Files.readAllLines(path));
    }

    private static ITree toGumTree(String clazz) {
        CtClass<?> spoonTree = Launcher.parseClass(clazz);
        SpoonGumTreeBuilder builder = new SpoonGumTreeBuilder();
        return builder.getTree(spoonTree);
    }

    private static class TestSources {
        String base;
        String left;
        String right;
        String expected;

        TestSources(String base, String left, String right, String expected) {
            this.base = base;
            this.left = left;
            this.right = right;
            this.expected = expected;
        }
    }

}