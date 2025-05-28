package com.example.cocinegocios.AdaptadoresCarruseles;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.cocinegocios.Fragmentos.IniciarSesion;
import com.example.cocinegocios.Fragmentos.Registrarse;

/**
 * Clase con funcion de adaptador para contener como carrusel los fragmentos de registrarse e iniciar sesión
 * <p>
 * Esta clase o adaptador contiene la lógica necesaria para contener los fragmentos correspondientes a solicitudes y trabajadores
 * <p>
 * Autor: [Juan Ramón de León Martín]
 * Fecha: [3/12/2024]
 */

public class AdaptadorLoggin extends FragmentStateAdapter {

    /**
     * Constructor del adaptador del carrusel de registro.
     * <p>
     * Este constructor inicializa el adaptador con el contexto y el carrusel del registrode la aplicación.
     *
     * @param fragmentActivity El fragmento de la actividad donde estemos actualmente.
     */
    public AdaptadorLoggin(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    /**
     * Metodo para dar utilidad al carrusel de trabajadores
     * <p>
     * Este fragmento coge la posicion del carrusel en la que nos encontremos, y dependiendo de esto, nosotros visuaiaremos un fragmento u otro, y a su vez tendremos unas opciones u otras.
     *
     * @param position La posición del elemento en el carrusel que se está procesando.
     *
     * @return 'fragmentos' Dependiendo en donde nos encontremos del carrusel, esto se encarga de redirigirnos al fragmento correspondiente
     */
    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                // Fragmento de inicio de sesión
                return new IniciarSesion();
            case 1:
                // Fragmento de registro
                return new Registrarse();
            default:
                return new IniciarSesion();
        }
    }

    //Número de paginas del carrusel
    @Override
    public int getItemCount() {
        return 2;
    }
}
