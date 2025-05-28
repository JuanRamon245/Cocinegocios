package com.example.cocinegocios.AdaptadoresCarruseles;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.cocinegocios.Fragmentos.TrabajadoresDelNegocio;
import com.example.cocinegocios.Fragmentos.SolicitudesDelNegocio;

/**
 * Clase con funcion de adaptador para contener como carrusel los fragmentos de solicitudes y trabajadores
 * <p>
 * Esta clase o adaptador contiene la lógica necesaria para contener los fragmentos correspondientes a solicitudes y trabajadores
 * <p>
 * Autor: [Juan Ramón de León Martín]
 * Fecha: [3/12/2024]
 */

public class AdaptadorGestionTrabajadores extends FragmentStateAdapter {

    private final String correoNegocio;

    /**
     * Constructor del adaptador carrusel de trabajadores.
     * <p>
     * Este constructor inicializa el adaptador con el contexto y la gestion del carrusel de trabajadores del negocio en el que estamos actualmente.
     *
     * @param fragmentActivity El fragmento de la actividad donde estemos actualmente.
     * @param correoNegocio El ID del negocio donde nos encontremos actualmente.
     */
    public AdaptadorGestionTrabajadores(@NonNull FragmentActivity fragmentActivity, String correoNegocio) {
        super(fragmentActivity);

        this.correoNegocio = correoNegocio;
    }

    /**
     * Metodo para dar utilidad al carrusel de trabajadores
     * <p>
     * Este fragmento coge la posicion del carrusel en la que nos encontremos, y dependiendo de esto, nosotros visuaiaremos un fragmento u otro, y a su vez tendremos unas opciones u otras.
     * Tambien se encarga de llevar el correo del negocio por medio del bundle.
     *
     * @param position La posición del elemento en el carrusel que se está procesando.
     *
     * @return 'fragmentos' Dependiendo en donde nos encontremos del carrusel, esto se encarga de redirigirnos al fragmento correspondiente
     */
    @NonNull
    @Override
    public Fragment createFragment(int position) {
        Bundle args = new Bundle();
        args.putString("correoNegocio", correoNegocio);
        switch (position) {
            case 0:
                TrabajadoresDelNegocio fragmentTrabajadores = new TrabajadoresDelNegocio();
                //Pasar el correo al fragmento
                fragmentTrabajadores.setArguments(args);
                return fragmentTrabajadores;
            case 1:
                SolicitudesDelNegocio fragmentSolicitudes = new SolicitudesDelNegocio();
                //Pasar el correo al fragmento
                fragmentSolicitudes.setArguments(args);
                return fragmentSolicitudes;
            default:
                TrabajadoresDelNegocio fragmentTrabajadoresDF = new TrabajadoresDelNegocio();
                //Pasar el correo al fragmento
                fragmentTrabajadoresDF.setArguments(args);
                return fragmentTrabajadoresDF;
        }
    }

    //Número de paginas del carrusel
    @Override
    public int getItemCount() {
        return 2;
    }
}