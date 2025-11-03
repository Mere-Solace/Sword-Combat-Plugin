package btm.sword;

import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import io.papermc.paper.plugin.loader.library.impl.JarLibrary;
import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver;
import java.nio.file.Path;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;
import org.jetbrains.annotations.NotNull;

public class SwordPluginLoader implements PluginLoader {
    @Override
    public void classloader(@NotNull PluginClasspathBuilder pluginClasspathBuilder) {
        MavenLibraryResolver resolver = new MavenLibraryResolver();
        resolver.addRepository(new RemoteRepository.Builder("xenondevs", "default", "https://repo.xenondevs.xyz/releases/").build());
        resolver.addDependency(new Dependency(new DefaultArtifact("xyz.xenondevs.invui:invui:pom:1.47"), null));
        pluginClasspathBuilder.addLibrary(resolver);
    }

//    @Override
//    public void classloader(PluginClasspathBuilder classpathBuilder) {
//        classpathBuilder.addLibrary(new JarLibrary(Path.of("dependency.jar")));
//
//        MavenLibraryResolver resolver = new MavenLibraryResolver();
//
//        resolver.addRepository(new RemoteRepository.Builder(
//                "xenondevs",
//                "default",
//                "https://repo.xenondevs.xyz/releases/")
//                .build());
//
//        resolver.addDependency(new Dependency(
//                new DefaultArtifact("xyz.xenondevs.invui:invui:1.0"),
//                null
//        ));
//
//        classpathBuilder.addLibrary(resolver);
//    }
}
