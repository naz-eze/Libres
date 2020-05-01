package app.libres.mobile.model;

public class InfoModel {

    private String infoHeader;
    private String infoMeaning;
    private String infoSource;
    private String infoUrl;

    public InfoModel(String infoHeader, String infoMeaning, String infoSource, String infoUrl) {
        this.infoHeader = infoHeader;
        this.infoMeaning = infoMeaning;
        this.infoSource = infoSource;
        this.infoUrl = infoUrl;
    }

    public String getInfoHeader() {
        return infoHeader;
    }

    public String getInfoMeaning() {
        return infoMeaning;
    }

    public String getInfoSource() {
        return infoSource;
    }

    public String getInfoUrl() {
        return infoUrl;
    }
}
