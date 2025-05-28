package com.example.cocinegocios.Fragmentos;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.cocinegocios.Adaptadores.AdaptadorListaSolicitudes;
import com.example.cocinegocios.Clases.Solicitudes;
import com.example.cocinegocios.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

/**
 * Clase con funcion de fragmento para que el usuario pueda gestionar las solicitudes del negocio
 * <p>
 * Esta clase o fragmento contiene la lógica necesaria para que el usuario pueda gestionar las solicitudes de entrada de trabajadores a la plantilla a su negocio.
 * <p>
 * Autor: [Juan Ramón de León Martín]
 * Fecha: [3/12/2024]
 */
public class SolicitudesDelNegocio extends Fragment {

    private ListView listViewSolicitudes;
    private ArrayList<Solicitudes> listaSolicitudesEntrantes = new ArrayList<>();
    AdaptadorListaSolicitudes miAdaptador;
    private String correoNegocio;

    /**
     * Metodo para crear la vista del fragmento
     * <p>
     * Esta clase coge y asocia los elementos de la vista, los inicializa, y luego los asocia con el layout correspondiente. Tambien settea los botónes
     * con el setOnClickLitsener implementado de la clase. Tambien hace uso del adaptador 'AdaptadorListaSolicitudes' para poder cargar las solicitudes del negocio mediante tambien el uso del
     * metodo 'cargarSolicitudesDesdeFirebase()'
     *
     * @param inflater Objeto que permite crear la vista usando el Layout asociado al fragmento.
     * @param container Vista padre que contiene los elementos del fragmento
     * @param savedInstanceState Bundle que contiene la estancia del fragmento, en caso de que esté siendo recreado.
     *
     * @return view La vista ya inicializada y asociados sus elementos.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_solicitudes_del_negocio, container, false);

        //Se recupera el correoNegocio dde la vista padre para ayudar al filtrado de solicitudes
        if (getArguments() != null) {
            correoNegocio = getArguments().getString("correoNegocio");
        }

        //Se hace uso del adaptador de solicitudes para gestionar el ListView de la vista y poder rellenarlo con las solicitudes adecuadas mediante el metodo 'cargarSolicitudesDesdeFirebase()'
        listViewSolicitudes = view.findViewById(R.id.listaSolicitudes);
        miAdaptador = new AdaptadorListaSolicitudes(getActivity(), listaSolicitudesEntrantes);
        listViewSolicitudes.setAdapter(miAdaptador);
        cargarSolicitudesDesdeFirebase();

        return view;
    }

    /**
     * Método que filtra de entre todos las solicitudes cuyo negocio asociado coincida con el negocio actual.
     */
    private void cargarSolicitudesDesdeFirebase() {
        DatabaseReference databaseReferenceCreacion = FirebaseDatabase.getInstance("https://negocios-de-cocinas-default-rtdb.europe-west1.firebasedatabase.app/").getReference();
        DatabaseReference negocioReferencia = databaseReferenceCreacion.child("Solicitudes");

        //Se busca las solicitudes, se filtran y si pasan ese filtro se añaden al listado, que se recarga cada vez que empieze este proceso
        negocioReferencia.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                listaSolicitudesEntrantes.clear();
                for (DataSnapshot solicitudesSnapshot : dataSnapshot.getChildren()) {
                    Solicitudes solicitud = solicitudesSnapshot.getValue(Solicitudes.class);
                    if (solicitud != null && correoNegocio.equals(solicitud.getCorreoNegocio())) {
                        String id = solicitud.getId();
                        String correoNegocio = solicitud.getCorreoNegocio();
                        String correoUsuario = solicitud.getCorreoUsuario();
                        String nombre = solicitud.getNombre();
                        String apellidos = solicitud.getApellidos();
                        String fecha = solicitud.getFecha();
                        String DNI = solicitud.getDNI();
                        listaSolicitudesEntrantes.add(new Solicitudes(id, correoNegocio, correoUsuario, nombre, apellidos, fecha, DNI));
                    }
                }
                miAdaptador.notifyDataSetChanged();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
}