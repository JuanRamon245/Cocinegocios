package com.example.cocinegocios.Clases;

public class Negocios {
    private String nombre;
    private String gmail;
    private String localidad;
    private String imagenCodificada;

    public Negocios() {
    }

    public Negocios(String nombre, String gmail, String localidad, String imagenCodificada) {
        this.nombre = nombre;
        this.gmail = gmail;
        this.localidad = localidad;
        this.imagenCodificada = imagenCodificada;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getGmail() {
        return gmail;
    }

    public void setGmail(String gmail) {
        this.gmail = gmail;
    }

    public String getLocalidad() {
        return localidad;
    }

    public void setLocalidad(String localidad) {
        this.localidad = localidad;
    }

    public String getImagenCodificada() {
        return imagenCodificada;
    }

    public void setImagenCodificada(String imagenCodificada) {
        this.imagenCodificada = imagenCodificada;
    }

}
