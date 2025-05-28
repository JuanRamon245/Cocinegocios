package com.example.cocinegocios.Clases;

public class Solicitudes {

    private String id;
    private String correoNegocio;
    private String correoUsuario;
    private String nombre;
    private String apellidos;
    private String fecha;
    private String DNI;

    public Solicitudes() {
    }

    public Solicitudes(String id, String correoNegocio, String correoUsuario, String nombre, String apellidos, String fecha, String DNI) {
        this.id = id;
        this.correoNegocio = correoNegocio;
        this.correoUsuario = correoUsuario;
        this.nombre = nombre;
        this.apellidos = apellidos;
        this.fecha = fecha;
        this.DNI = DNI;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCorreoNegocio() {
        return correoNegocio;
    }

    public void setCorreoNegocio(String correoNegocio) {
        this.correoNegocio = correoNegocio;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getCorreoUsuario() {
        return correoUsuario;
    }

    public void setCorreoUsuario(String correoUsuario) {
        this.correoUsuario = correoUsuario;
    }

    public String getApellidos() {
        return apellidos;
    }

    public void setApellidos(String apellidos) {
        this.apellidos = apellidos;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    public String getDNI() {
        return DNI;
    }

    public void setDNI(String DNI) {
        this.DNI = DNI;
    }
}
