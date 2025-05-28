package com.example.cocinegocios.Clases;

public class ProductoComanda {

    private String id;
    private String pedidoAsociado;
    private String idProducto;
    private String nombre;
    private int cantidad;
    private Double precioUnidad;
    private String estado;

    public ProductoComanda() {
    }

    public ProductoComanda(String id, String pedidoAsociado, String idProducto, int cantidad, String nombre, Double precioUnidad, String estado) {
        this.id = id;
        this.pedidoAsociado = pedidoAsociado;
        this.idProducto = idProducto;
        this.cantidad = cantidad;
        this.nombre = nombre;
        this.precioUnidad = precioUnidad;
        this.estado = estado;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPedidoAsociado() {
        return pedidoAsociado;
    }

    public void setPedidoAsociado(String pedidoAsociado) {
        this.pedidoAsociado = pedidoAsociado;
    }

    public String getIdProducto() {
        return idProducto;
    }

    public void setIdProducto(String idProducto) {
        this.idProducto = idProducto;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

    public Double getPrecioUnidad() {
        return precioUnidad;
    }

    public void setPrecioUnidad(Double precioUnidad) {
        this.precioUnidad = precioUnidad;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

}
