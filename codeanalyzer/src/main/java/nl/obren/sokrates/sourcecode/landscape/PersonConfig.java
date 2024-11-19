package nl.obren.sokrates.sourcecode.landscape;

import java.util.ArrayList;
import java.util.List;

public class PersonConfig {
    private String name = "";
    private String link = "";
    private String image = "";
    private List<String> emailPatterns = new ArrayList<>();

    public PersonConfig() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public List<String> getEmailPatterns() {
        return emailPatterns;
    }

    public void setEmailPatterns(List<String> emailPatterns) {
        this.emailPatterns = emailPatterns;
    }
}
