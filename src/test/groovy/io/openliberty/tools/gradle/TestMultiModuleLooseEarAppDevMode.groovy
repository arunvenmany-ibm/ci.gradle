package io.openliberty.tools.gradle

import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test

import static org.junit.Assert.assertTrue;

class TestMultiModuleLooseEarAppDevMode extends BaseDevTest {
    static File resourceDir = new File("build/resources/test/multi-module-loose-ear-pages-test")
    static File buildDir = new File(integTestDir, "/multi-module-loose-ear-pages-test")
    static String buildFilename = "build.gradle"

    @BeforeClass
    public static void setup() throws IOException, InterruptedException, FileNotFoundException {
        createDir(buildDir);
        createTestProject(buildDir, resourceDir, buildFilename);
        new File(buildDir, "build").createNewFile();
        runDevMode("--skipTests",buildDir)
    }

    @Test
    public void modifyJavaFileTest() throws Exception {

        // modify a java file
        File srcHelloWorld = new File(buildDir, "/jar/src/main/java/io/openliberty/guides/multimodules/lib/Converter.java");
        File targetHelloWorld = new File(buildDir, "/jar/build/classes/java/main/io/openliberty/guides/multimodules/lib/Converter.class");
        assertTrue(srcHelloWorld.exists());
        assertTrue(targetHelloWorld.exists());

        long lastModified = targetHelloWorld.lastModified();
        waitLongEnough();
        String str = "// testing";
        BufferedWriter javaWriter = new BufferedWriter(new FileWriter(srcHelloWorld, true));
        javaWriter.append(' ');
        javaWriter.append(str);
        javaWriter.close();

        assertTrue(waitForCompilation(targetHelloWorld, lastModified, 6000));
    }

    @Test
    public void manualTestsInvocationTest() throws Exception {

        writer.write("\n");
        writer.flush();
        if (!verifyLogMessage(2000,  "Tests will not run on demand for ear because skipTests is set to true")) {
            assertTrue(verifyLogMessage(2000,  "Tests will not run on demand for ear because skipTests is set to true"));
        }
        if (!verifyLogMessage(2000,  "Tests will not run on demand for jar because skipTests is set to true")) {
            assertTrue(verifyLogMessage(2000,  "Tests will not run on demand for jar because skipTests is set to true"));
        }
        if (!verifyLogMessage(2000,  "Tests will not run on demand for war because skipTests is set to true")) {
            assertTrue(verifyLogMessage(2000,  "Tests will not run on demand for war because skipTests is set to true"));
        }

    }

    @AfterClass
    public static void cleanUpAfterClass() throws Exception {
        String stdout = getContents(logFile, "Dev mode std output");
        System.out.println(stdout);
        String stderr = getContents(errFile, "Dev mode std error");
        System.out.println(stderr);
        cleanUpAfterClass(true);
    }
}
