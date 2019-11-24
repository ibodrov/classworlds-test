package ca.ibodrov.classworldstest.runner;

import ca.ibodrov.classworldstest.api.Task;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.classworlds.launcher.Launcher;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.eclipse.aether.AbstractRepositoryListener;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositoryEvent;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transfer.AbstractTransferListener;
import org.eclipse.aether.transfer.TransferEvent;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class Runner {

    private static final Logger log = LoggerFactory.getLogger(Runner.class);

    private static final ClassLoader PARENT_CLASSLOADER = ClassWorld.class.getClassLoader();

    private final ClassWorld classWorld;

    public Runner(ClassWorld classWorld) {
        this.classWorld = classWorld;
    }

    public void execute() throws Exception {

        // Bits to steal from DefaultClassRealmManager and DefaultMavenPluginManager

        // setup classWorld from bootstrap process
        // setup coreRealm from configuration in classworlds.conf
        // setup exported packages from core realm
        // create the api realm by only letting through the exported packages
        // setup exported artifacts from core realm to block the resolver from allowing these to be added

        Collection<Artifact> artifacts = resolve(Collections.singletonList(new Dependency(new DefaultArtifact("ca.ibodrov:classworlds-test-plugin-blue:1.0-SNAPSHOT"), "compile")));

        ClassRealm coreRealm = classWorld.getClassRealm( "concord.core" );
        Class graphBuilder = coreRealm.loadClass("com.google.common.graph.GraphBuilder");
        System.out.println("graphBuilder = " + graphBuilder + " from " + graphBuilder.getProtectionDomain().getCodeSource().getLocation().toURI());

        ClassRealm pluginRealm = classWorld.newRealm("blue");
        if(pluginRealm.getParentClassLoader() == null) {
            pluginRealm.setParentClassLoader(PARENT_CLASSLOADER);
        }
        Thread.currentThread().setContextClassLoader(pluginRealm);
        pluginRealm.setParentRealm(classWorld.getClassRealm("concord.core"));

        for (Artifact a : artifacts) {
            URL url = a.getFile().toURL();
            pluginRealm.addURL(url);
        }

        pluginRealm.display();

        Class<Task> klass = (Class<Task>) pluginRealm.loadClass("ca.ibodrov.classworldstest.plugin.blue.BlueTask");
        System.out.println("klass.getClassLoader() = " + klass.getClassLoader());
        Task task = klass.newInstance();
        System.out.println("task.getClass().getClassLoader() = " + task.getClass().getClassLoader());
        task.run("hi!");
    }

    // Setup classloads to boot from the configuration
    public static void main(String[] args) throws Exception {
        System.setProperty("classworlds.bootstrapped", "true");
        String workingDirectory = new File("").getAbsolutePath();
        String concordHome = workingDirectory + "/src/concord";
        System.out.println("concordHome = " + concordHome);
        System.setProperty("concord.home", concordHome);
        Launcher.main(new String[]{});
    }

    // Classworlds knows how to call this special method
    public static void main(String[] args, ClassWorld classWorld) throws Exception {
        System.out.println("Hello! I am being executed by the ClassWorlds Launcher!");
        Runner runner = new Runner(classWorld);
        runner.execute();
    }

    private Collection<Artifact> resolve(List<Dependency> deps) throws IOException {
        RepositorySystem system = newMavenRepositorySystem();
        RepositorySystemSession session = newRepositorySystemSession(system);

        CollectRequest req = new CollectRequest();
        req.setDependencies(deps);


        req.setRepositories(Collections.singletonList(new RemoteRepository.Builder("central", "default", "https://repo.maven.apache.org/maven2/").build()));

        DependencyRequest dependencyRequest = new DependencyRequest(req, null);

        try {
            return system.resolveDependencies(session, dependencyRequest)
                    .getArtifactResults().stream()
                    .map(ArtifactResult::getArtifact)
                    .collect(Collectors.toSet());
        } catch (DependencyResolutionException e) {
            throw new IOException(e);
        }
    }

    private static RepositorySystem newMavenRepositorySystem() {
        DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
        locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
        locator.addService(TransporterFactory.class, FileTransporterFactory.class);
        locator.addService(TransporterFactory.class, HttpTransporterFactory.class);

        locator.setErrorHandler(new DefaultServiceLocator.ErrorHandler() {
            @Override
            public void serviceCreationFailed(Class<?> type, Class<?> impl, Throwable exception) {
                log.error("newMavenRepositorySystem -> service creation error: type={}, impl={}", type, impl, exception);
            }
        });

        return locator.getService(RepositorySystem.class);
    }


    private DefaultRepositorySystemSession newRepositorySystemSession(RepositorySystem system) {
        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
        session.setChecksumPolicy(RepositoryPolicy.CHECKSUM_POLICY_IGNORE);

        Path localCacheDir = Paths.get(System.getProperty("user.home")).resolve(".m2/repository");
        LocalRepository localRepo = new LocalRepository(localCacheDir.toFile());
        session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, localRepo));
        session.setTransferListener(new AbstractTransferListener() {
            @Override
            public void transferFailed(TransferEvent event) {
                log.error("transferFailed -> {}", event);
            }
        });

        session.setRepositoryListener(new AbstractRepositoryListener() {
            @Override
            public void artifactResolving(RepositoryEvent event) {
                log.debug("artifactResolving -> {}", event);
            }

            @Override
            public void artifactResolved(RepositoryEvent event) {
                log.debug("artifactResolved -> {}", event);
            }
        });

        return session;
    }
}
