package btm.sword;

import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;
import org.jetbrains.annotations.NotNull;

import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver;

/**
 * Paper {@link PluginLoader} that resolves runtime dependencies via Maven before the plugin
 * class is loaded.
 * <p>
 * Currently adds the InvUI library ({@code xyz.xenondevs.invui:invui:1.47}) from the
 * XenonDevs repository to the plugin classloader so that menu classes are available at runtime
 * without bundling the JAR.
 * </p>
 */
public class SwordPluginLoader implements PluginLoader {
    @Override
    public void classloader(@NotNull PluginClasspathBuilder pluginClasspathBuilder) {
        MavenLibraryResolver resolver = new MavenLibraryResolver();
        resolver.addRepository(new RemoteRepository.Builder("xenondevs", "default", "https://repo.xenondevs.xyz/releases/").build());
        resolver.addDependency(new Dependency(new DefaultArtifact("xyz.xenondevs.invui:invui:pom:1.47"), null));
        pluginClasspathBuilder.addLibrary(resolver);
    }
}
