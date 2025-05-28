package com.example.cocinegocios.Clases;

public class Espacio {

    private String id;
    private String nombre;
    private String negocio;
    private int nMesas;

    public Espacio() {
    }

    public Espacio(String id, String nombre, String negocio, int nMesas) {
        this.id = id;
        this.nombre = nombre;
        this.negocio = negocio;
        this.nMesas = nMesas;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public int getnMesas() {
        return nMesas;
    }

    public void setnMesas(int nMesas) {
        this.nMesas = nMesas;
    }

    public String getNegocio() {
        return negocio;
    }

    public void setNegocio(String negocio) {
        this.negocio = negocio;
    }
}
