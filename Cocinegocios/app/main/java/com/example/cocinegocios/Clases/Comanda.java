package com.example.cocinegocios.Clases;

public class Comanda {

    private String id;
    private String negocioAsociado;
    private String espacio;
    private int numeroMesa;
    private String indicador;
    private Double subtotal;

    public Comanda() {
    }

    public Comanda(String id, String negocioAsociado, String espacio, String indicador, int numeroMesa, Double subtotal) {
        this.id = id;
        this.negocioAsociado = negocioAsociado;
        this.espacio = espacio;
        this.indicador = indicador;
        this.numeroMesa = numeroMesa;
        this.subtotal = subtotal;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNegocioAsociado() {
        return negocioAsociado;
    }

    public void setNegocioAsociado(String negocioAsociado) {
        this.negocioAsociado = negocioAsociado;
    }

    public int getNumeroMesa() {
        return numeroMesa;
    }

    public void setNumeroMesa(int numeroMesa) {
        this.numeroMesa = numeroMesa;
    }

    public String getEspacio() {
        return espacio;
    }

    public void setEspacio(String espacio) {
        this.espacio = espacio;
    }

    public Double getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(Double subtotal) {
        this.subtotal = subtotal;
    }

    public String getIndicador() {
        return indicador;
    }

    public void setIndicador(String indicador) {
        this.indicador = indicador;
    }
}
