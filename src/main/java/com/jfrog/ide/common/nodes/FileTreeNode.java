package com.jfrog.ide.common.nodes;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jfrog.ide.common.nodes.subentities.Severity;

import java.io.File;

public class FileTreeNode extends SortableChildrenTreeNode implements SubtitledTreeNode, Comparable<FileTreeNode> {
    @JsonProperty()
    protected String fileName = "";
    @JsonProperty()
    protected String filePath = "";
    @JsonProperty()
    protected Severity topSeverity = Severity.Normal;

    // Empty constructor for deserialization
    protected FileTreeNode() {
    }

    public FileTreeNode(String filePath) {
        this.filePath = filePath;
        File f = new File(filePath);
        fileName = f.getName();
    }

    public Severity getSeverity() {
        return topSeverity;
    }

    @Override
    public String getTitle() {
        return fileName;
    }

    @Override
    public String getSubtitle() {
        return filePath;
    }

    @Override
    public String toString() {
        return fileName;
    }

    @Override
    public String getIcon() {
        return topSeverity.getIconName();
    }

    public String getFilePath() {
        return filePath;
    }

    public void addIssue(IssueNode issue) {
        add(issue);
        if (issue.getSeverity().isHigherThan(topSeverity)) {
            topSeverity = issue.getSeverity();
        }
    }

    @Override
    public int compareTo(FileTreeNode other) {
        return other.getSeverity().ordinal() - this.getSeverity().ordinal();
    }
}
