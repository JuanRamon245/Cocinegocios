package com.example.cocinegocios.Clases;

public class Producto {
    private String id;
    private String nombre;
    private String negocio;
    private String categoria;
    private String fotoCodificada;
    private String descripcion;
    private String pasosSeguir;
    private double precio;

    public Producto() {
    }

    public Producto(String id, String nombre, String negocio, String categoria, String fotoCodificada, String descripcion, String pasosSeguir, double precio) {
        this.id = id;
        this.nombre = nombre;
        this.negocio = negocio;
        this.categoria = categoria;
        this.fotoCodificada = fotoCodificada;
        this.descripcion = descripcion;
        this.pasosSeguir = pasosSeguir;
        this.precio = precio;
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

    public String getNegocio() {
        return negocio;
    }

    public void setNegocio(String negocio) {
        this.negocio = negocio;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public String getFotoCodificada() {
        return fotoCodificada;
    }

    public void setFotoCodificada(String fotoCodificada) {
        this.fotoCodificada = fotoCodificada;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getPasosSeguir() {
        return pasosSeguir;
    }

    public void setPasosSeguir(String pasosSeguir) {
        this.pasosSeguir = pasosSeguir;
    }

    public double getPrecio() {
        return precio;
    }

    public void setPrecio(double precio) {
        this.precio = precio;
    }
}
