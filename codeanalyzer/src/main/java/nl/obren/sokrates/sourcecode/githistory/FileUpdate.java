package nl.obren.sokrates.sourcecode.githistory;

import nl.obren.sokrates.sourcecode.ExtensionGroupExtractor;

public class FileUpdate {
    private String date = "";
    private String authorEmail = "";
    private String commitId = "";
    private String path = "";

    public FileUpdate(String date, String authorEmail, String commitId, String path) {
        this.date = date;
        this.authorEmail = authorEmail;
        this.commitId = commitId;
        this.path = path;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getAuthorEmail() {
        return authorEmail;
    }

    public void setAuthorEmail(String authorEmail) {
        this.authorEmail = authorEmail;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getCommitId() {
        return commitId;
    }

    public void setCommitId(String commitId) {
        this.commitId = commitId;
    }

    public String getExtension() {
        return ExtensionGroupExtractor.getExtension(getPath());
    }
}
