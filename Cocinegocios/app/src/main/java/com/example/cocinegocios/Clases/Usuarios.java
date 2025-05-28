package com.example.cocinegocios.Clases;

public class Usuarios {
    private String nombre;
    private String apellidos;
    private String fotoPerfil;
    private String DNI;
    private String gmail;
    private int telefono;
    private String fecha;
    private String negocioOficio;
    private String oficio;
    private String contraseña;

    public Usuarios() {
    }

    public Usuarios(String nombre, String apellidos, String fotoPerfil, String gmail, String DNI, String fecha, int telefono,String negocioOficio, String oficio, String contraseña) {
        this.nombre = nombre;
        this.apellidos = apellidos;
        this.fotoPerfil = fotoPerfil;
        this.gmail = gmail;
        this.DNI = DNI;
        this.fecha = fecha;
        this.telefono = telefono;
        this.negocioOficio = negocioOficio;
        this.oficio = oficio;
        this.contraseña = contraseña;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellidos() {
        return apellidos;
    }

    public void setApellidos(String apellidos) {
        this.apellidos = apellidos;
    }

    public String getFotoPerfil() {
        return fotoPerfil;
    }

    public void setFotoPerfil(String fotoPerfil) {
        this.fotoPerfil = fotoPerfil;
    }

    public String getGmail() {
        return gmail;
    }

    public void setGmail(String gmail) {
        this.gmail = gmail;
    }

    public String getDNI() {
        return DNI;
    }

    public void setDNI(String DNI) {
        this.DNI = DNI;
    }

    public int getTelefono() {
        return telefono;
    }

    public void setTelefono(int telefono) {
        this.telefono = telefono;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    public String getNegocioOficio() {
        return negocioOficio;
    }

    public void setNegocioOficio(String negocioOficio) {
        this.negocioOficio = negocioOficio;
    }

    public String getOficio() {
        return oficio;
    }

    public void setOficio(String oficio) {
        this.oficio = oficio;
    }

    public String getContraseña() {
        return contraseña;
    }

    public void setContraseña(String contraseña) {
        this.contraseña = contraseña;
    }
}
