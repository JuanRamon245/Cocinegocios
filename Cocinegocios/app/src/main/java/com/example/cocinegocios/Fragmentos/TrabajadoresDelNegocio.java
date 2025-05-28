package com.example.cocinegocios.Fragmentos;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.cocinegocios.Adaptadores.AdaptadorListaTrabajadores;
import com.example.cocinegocios.Clases.Usuarios;
import com.example.cocinegocios.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

/**
 * Clase con funcion de fragmento para que el usuario pueda gestionar los trabajadores del negocio
 * <p>
 * Esta clase o fragmento contiene la lógica necesaria para que el usuario pueda gestionar los usuarios pertenecientes al negocio en el que estemos actualmente.
 * <p>
 * Autor: [Juan Ramón de León Martín]
 * Fecha: [3/12/2024]
 */

public class TrabajadoresDelNegocio extends Fragment {

    private ListView listViewUsuarios;
    private ArrayList<Usuarios> listaUsuariosEntrantes = new ArrayList<>();
    AdaptadorListaTrabajadores miAdaptador;
    private String correoNegocio;

    /**
     * Metodo para crear la vista del fragmento
     * <p>
     * Esta clase coge y asocia los elementos de la vista, los inicializa, y luego los asocia con el layout correspondiente. Tambien settea los botónes
     * con el setOnClickLitsener implementado de la clase. Tambien hace uso del adaptador 'AdaptadorListaTrabajadores' para poder cargar los trabajadores del negocio mediante tambien el uso del
     * metodo 'cargarUsuariosDesdeFirebase()'
     *
     * @param inflater Objeto que permite crear la vista usando el Layout asociado al fragmento.
     * @param container Vista padre que contiene los elementos del fragmento
     * @param savedInstanceState Bundle que contiene la estancia del fragmento, en caso de que esté siendo recreado.
     *
     * @return view La vista ya inicializada y asociados sus elementos.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_trabajadores_del_negocio, container, false);

        //Se recupera el correoNegocio dde la vista padre para ayudar al filtrado de trabajadores
        if (getArguments() != null) {
            correoNegocio = getArguments().getString("correoNegocio");
        }

        /*
         * Se hace uso del adaptador de trabajadores para gestionar el ListView de la vista y poder rellenarlo con los trabajadores adecuadas mediante el metodo 'cargarUsuariosDesdeFirebase()'
         * pasandole como parametro el correo del negocio.
         */
        listViewUsuarios = view.findViewById(R.id.listaTrabajadores);
        miAdaptador = new AdaptadorListaTrabajadores(getActivity(), listaUsuariosEntrantes);
        listViewUsuarios.setAdapter(miAdaptador);
        cargarUsuariosDesdeFirebase(correoNegocio);

        return view;
    }

    /**
     * Método que filtra de entre todos las solicitudes cuyo negocio asociado coincida con el negocio actual.
     *
     * @param correoNegocioActual String que contiene el correo del negocio en el que estemos actualmente, para poder filtrar los trabajadores independienetemente del engocio en el que trabajen
     */
    private void cargarUsuariosDesdeFirebase(String correoNegocioActual) {
        DatabaseReference databaseReferenceCreacion = FirebaseDatabase.getInstance("https://negocios-de-cocinas-default-rtdb.europe-west1.firebasedatabase.app/").getReference();
        DatabaseReference usuariosReferencia = databaseReferenceCreacion.child("Usuarios");

        /*
         * Se busca los trabajadores, se filtran usando el negocio actual para verificar que trabajan en ese negocio y si pasan ese filtro se añaden al listado, que se recarga cada vez que
         * empieze este proceso.
         */
        usuariosReferencia.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                listaUsuariosEntrantes.clear();
                for (DataSnapshot usuariosSnapshot : dataSnapshot.getChildren()) {
                    Usuarios usuarios = usuariosSnapshot.getValue(Usuarios.class);
                    if (usuarios != null && correoNegocioActual.equals(usuarios.getNegocioOficio())) {
                        String nombre = usuarios.getNombre();
                        String apellidos = usuarios.getApellidos();
                        String fotoPerfil = usuarios.getFotoPerfil();
                        String DNI = usuarios.getDNI();
                        String gmail = usuarios.getGmail();
                        int telefono = usuarios.getTelefono();
                        String fecha = usuarios.getFecha();
                        String negocioOficio = usuarios.getNegocioOficio();
                        String oficio = usuarios.getOficio();
                        String contraseña = usuarios.getContraseña();
                        listaUsuariosEntrantes.add(new Usuarios(nombre, apellidos, fotoPerfil, gmail, DNI, fecha, telefono, negocioOficio, oficio, contraseña));
                    }
                }
                miAdaptador.notifyDataSetChanged();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    /**
     * Método que fuerza la recarga de la lista de usuarios que trabajan en el negocio actual
     */
    public void actualizarListaUsuariosTrabajadores() {
        cargarUsuariosDesdeFirebase(correoNegocio);
        miAdaptador.notifyDataSetChanged();
    }
}