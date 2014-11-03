package org.codemucker.cqrs.generator;

import java.io.File;

import org.codemucker.cqrs.client.gwt.GenerateCqrsGwtClient;
import org.codemucker.jfind.DirectoryRoot;
import org.codemucker.jfind.Root.RootContentType;
import org.codemucker.jfind.Root.RootType;
import org.codemucker.jfind.Roots;
import org.codemucker.jfind.matcher.ARoot;
import org.codemucker.jmutate.generate.GeneratorRunner;
import org.codemucker.jtest.MavenProjectLayout;
import org.codemucker.jtest.ProjectLayout;
import org.junit.Test;

public class CqrsRestServiceClientGeneratorTest {

    @Test
    //@Ignore("Not yet working on scanning for source request beans")
    public void smokeTest(){
        String pkg = CqrsRestServiceClientGeneratorTest.class.getPackage().getName();
        ProjectLayout project = new MavenProjectLayout();
        File generateTo = project.newTmpSubDir("genRootUnitTst");
        
        GeneratorRunner runner = GeneratorRunner.with()
                .defaults()
                .scanRoots(Roots.with().all().classpath(true).build())
                .scanRootMatching(ARoot.that().isDirectory().isSrc())
                .scanPackages(pkg)
                .scanSubTypes()
                .failOnParseError(true)
                .defaultGenerateTo(new DirectoryRoot(generateTo,RootType.GENERATED,RootContentType.SRC))
                .build();
        
        runner.run();;
    }
    
    @GenerateCqrsGwtClient(
            scanDependencies=false,
            serviceName="com.acne.mypackage.MyTestCqrsService",
            requestBeanPackages={"org.codemucker.cqrs.generator.*"}       
     )
    public static class ICauseGeneratorToBeInvoked {
        
    }
    
    
}
