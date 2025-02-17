package com.jfrog.ide.common.gradle;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import com.jfrog.GradleDependencyTree;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.scan.DependencyTree;
import org.jfrog.build.extractor.scan.GeneralInfo;
import org.jfrog.build.extractor.scan.License;
import org.jfrog.build.extractor.scan.Scope;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Build Gradle dependency tree before the Xray scan.
 *
 * @author yahavi
 */
@SuppressWarnings({"unused"})
public class GradleTreeBuilder {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final GradleDriver gradleDriver;
    private final Path projectDir;
    private Path pluginLibDir;

    public GradleTreeBuilder(Path projectDir, Map<String, String> env, String gradleExe) {
        this.projectDir = projectDir;
        this.gradleDriver = new GradleDriver(gradleExe, env);
    }

    /**
     * Build the Gradle dependency tree.
     *
     * @param logger - The logger.
     * @return full dependency tree without Xray scan results.
     * @throws IOException in case of I/O error.
     */
    public DependencyTree buildTree(Log logger) throws IOException {
        gradleDriver.verifyGradleInstalled();
        List<File> gradleDependenciesFiles = gradleDriver.generateDependenciesGraphAsJson(projectDir.toFile(), logger);
        return createDependencyTrees(gradleDependenciesFiles);
    }

    /**
     * Create dependency trees from files generated by running the 'generateDependenciesGraphAsJson' task.
     *
     * @param gradleDependenciesFiles - The files containing the dependency trees
     * @return a dependency tree contain one or more Gradle projects.
     * @throws IOException in case of any I/O error.
     */
    private DependencyTree createDependencyTrees(List<File> gradleDependenciesFiles) throws IOException {
        DependencyTree rootNode = new DependencyTree(projectDir.getFileName().toString());
        rootNode.setMetadata(true);
        rootNode.setGeneralInfo(new GeneralInfo().componentId(projectDir.getFileName().toString()).path(projectDir.toString()));
        for (File projectFile : gradleDependenciesFiles) {
            String projectName = new String(Base64.getDecoder().decode(projectFile.getName()), StandardCharsets.UTF_8);
            GradleDependencyTree node = objectMapper.readValue(projectFile, GradleDependencyTree.class);
            GeneralInfo generalInfo = createGeneralInfo(projectName, node).path(projectDir.toString());
            DependencyTree projectNode = createNode(generalInfo, node);
            projectNode.setMetadata(true);
            populateDependencyTree(projectNode, node);
            rootNode.add(projectNode);
        }
        if (gradleDependenciesFiles.size() == 1) {
            rootNode = (DependencyTree) rootNode.getFirstChild();
        }
        return rootNode;
    }

    /**
     * Recursively populate a Gradle dependency node.
     *
     * @param node                 - The dependency node to populate
     * @param gradleDependencyNode - The Gradle dependency node created by 'generateDependenciesGraphAsJson'
     */
    private void populateDependencyTree(DependencyTree node, GradleDependencyTree gradleDependencyNode) {
        for (Map.Entry<String, GradleDependencyTree> gradleEntry : gradleDependencyNode.getChildren().entrySet()) {
            GeneralInfo generalInfo = createGeneralInfo(gradleEntry.getKey(), gradleEntry.getValue());
            DependencyTree child = createNode(generalInfo, gradleEntry.getValue());
            node.add(child);
            populateDependencyTree(child, gradleEntry.getValue());
        }
    }

    private GeneralInfo createGeneralInfo(String id, GradleDependencyTree node) {
        return new GeneralInfo().pkgType("gradle").componentId(id);
    }

    /**
     * Create a dependency tree node.
     *
     * @param generalInfo          - The dependency General info
     * @param gradleDependencyNode - The Gradle dependency node created by 'generateDependenciesGraphAsJson'
     * @return the dependency tree node.
     */
    private DependencyTree createNode(GeneralInfo generalInfo, GradleDependencyTree gradleDependencyNode) {
        DependencyTree node = new DependencyTree(generalInfo.getComponentId());
        node.setGeneralInfo(generalInfo);
        Set<Scope> scopes = gradleDependencyNode.getConfigurations().stream().map(Scope::new).collect(Collectors.toSet());
        if (scopes.isEmpty()) {
            scopes.add(new Scope());
        }
        node.setScopes(scopes);
        node.setLicenses(Sets.newHashSet(new License()));
        if (isBlank(generalInfo.getGroupId())) {
            node.setMetadata(true);
        }
        return node;
    }
}
