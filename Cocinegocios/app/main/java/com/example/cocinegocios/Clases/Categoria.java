package com.example.cocinegocios.Clases;

public class Categoria {

    private String claveCategoria;
    private String gmailNegocio;
    private String nombreCategoria;

    public Categoria() {
    }

    public Categoria(String claveCategoria, String gmailNegocio, String nombreCategoria) {
        this.claveCategoria = claveCategoria;
        this.gmailNegocio = gmailNegocio;
        this.nombreCategoria = nombreCategoria;
    }

    public String getClaveCategoria() {
        return claveCategoria;
    }

    public void setClaveCategoria(String claveCategoria) {
        this.claveCategoria = claveCategoria;
    }

    public String getGmailNegocio() {
        return gmailNegocio;
    }

    public void setGmailNegocio(String gmailNegocio) {
        this.gmailNegocio = gmailNegocio;
    }

    public String getNombreCategoria() {
        return nombreCategoria;
    }

    public void setNombreCategoria(String nombreCategoria) {
        this.nombreCategoria = nombreCategoria;
    }
}
