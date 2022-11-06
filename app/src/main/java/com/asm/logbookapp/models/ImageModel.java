package com.asm.logbookapp.models;

public class ImageModel {
    private String urlImage, uriCaptured;

    public String getUriCaptured() {
        return uriCaptured;
    }

    public void setUriCaptured(String uriCaptured) {
        this.uriCaptured = uriCaptured;
    }

    public String getUrlImage() {
        return urlImage;
    }

    public void setUrlImage(String urlImage) {
        this.urlImage = urlImage;
    }

    public ImageModel(String urlImage, String uriCaptured) {
        this.urlImage = urlImage;
        this.uriCaptured = uriCaptured;
    }

}
