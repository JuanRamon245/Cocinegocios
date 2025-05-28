package com.example.cocinegocios;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.cocinegocios.Adaptadores.AdaptadorListaNegocios;
import com.example.cocinegocios.Clases.Negocios;
import com.example.cocinegocios.Clases.UsuariosSQLite;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

/**
 * Clase con de poder introducirnos a un negocio
 * <p>
 * Esta clase contiene la lógica necesaria para que los usuarios puedan visualizar los negocios para luego independientemente de otros factores, poder acceder al mismo con un rol u otro.
 * Tambien da la posibilidad de poder crear 1 negocio por usuario, redirigiendonos a la pagina de crear negocios.
 * <p>
 * Autor: [Juan Ramón de León Martín]
 * Fecha: [3/12/2024]
 */

public class SeleccionDeNegocios extends AppCompatActivity implements View.OnClickListener {

    private Button botonIrCrearNegocio, usarFiltros, botonLogout;
    private ListView listViewNegocios;
    private ArrayList<Negocios> listaNegociosEntrantes = new ArrayList<>();
    AdaptadorListaNegocios miAdaptador;

    private String correoUsuario;

    SQLiteDatabase baseDatos;

    /**
     * Metodo que sirve para el uso del botón de hacia atras del telefono no haga nada
     */
    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {
    }

    /**
     * Metodo para crear la vista de la actividad
     * <p>
     * Esta clase coge y asocia los elementos de la vista, los inicializa, y luego los asocia con el layout correspondiente. Tambien se encarga de cargar los negocios de la base de datos mediante el
     * metodo 'cargarNegociosDesdeFirebase()'.
     *
     * @param savedInstanceState Bundle que contiene la estancia del fragmento, en caso de que esté siendo recreado.
     *
     * @return view La vista ya inicializada y asociados sus elementos.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seleccion_de_negocios);

        botonIrCrearNegocio = findViewById(R.id.BotonParaAñadirNegocios);
        botonIrCrearNegocio.setOnClickListener(this);

        botonLogout = findViewById(R.id.BotonLogOut);
        botonLogout.setOnClickListener(this);

        //cargo el listview de negocios de la base de datos mediante el metodo 'cargarNegociosDesdeFirebase()'
        listViewNegocios = findViewById(R.id.listaNegocios);
        miAdaptador = new AdaptadorListaNegocios(this, listaNegociosEntrantes);
        listViewNegocios.setAdapter(miAdaptador);
        cargarNegociosDesdeFirebase();

        //Buscamos la BBDD de SQLite para poder añadir un rol al usuario cuando se inice o en caso de desloguearse borrar el usuario actual de la BBDD de SQLite
        UsuariosSQLite baseDatosUsuarios = new UsuariosSQLite(this, "bbddUsuarios", null, 1);
        baseDatos = baseDatosUsuarios.getWritableDatabase();
    }

    /**
     * Metodo para dar funcionalidad a los SetOnClickLitseners aasociados arriba
     * <p>
     * Esta clase coge da lógica a los botones con el SetOnClickLitseners. En este caso podemos dirigirnos a la pagina para poder crear negocios o cerrar sesion, con lo que
     * borraremos nuestro usuario de SQLite.
     *
     * @param v La vista actual de donde estamos trabajando.
     */
    @Override
    public void onClick(View v) {
        /*
         * Se da lógica al boton que sirve para dirigirnos al donde podremos crear los negocios.
         */
        if (v.getId() == R.id.BotonParaAñadirNegocios) {
            Intent botonIrCrearNegocio = new Intent(SeleccionDeNegocios.this, RegistroDeNegocios.class);
            startActivity(botonIrCrearNegocio);
        /*
         * Se da lógica al boton que sirve para cerrar sesión y borrar el usuario de SQLite
         */
        } else if (v.getId() == R.id.BotonLogOut) {
            String sqlCreacion = "CREATE TABLE usuario(correo TEXT PRIMARY KEY, contrasena TEXT, rol TEXT)";
            String sqlBorrado = "DROP TABLE IF EXISTS usuario";
            baseDatos.execSQL(sqlBorrado);
            baseDatos.execSQL(sqlCreacion);

            Intent botonVolver = new Intent(SeleccionDeNegocios.this, MainActivity.class);
            startActivity(botonVolver);
        }
    }

    //Cargar los negocios de la base de datos
    private void cargarNegociosDesdeFirebase() {
        DatabaseReference databaseReferenceCreacion = FirebaseDatabase.getInstance("https://negocios-de-cocinas-default-rtdb.europe-west1.firebasedatabase.app/").getReference();
        DatabaseReference negocioReferencia = databaseReferenceCreacion.child("negocios");

        negocioReferencia.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                listaNegociosEntrantes.clear();
                for (DataSnapshot negociosSnapshot : dataSnapshot.getChildren()) {
                    Negocios negocio = negociosSnapshot.getValue(Negocios.class);
                    if (negocio != null) {
                        String nombre = negocio.getNombre();
                        String gmail = negocio.getGmail();
                        String localidad = negocio.getLocalidad();
                        String imagenCodificada = negocio.getImagenCodificada();
                        listaNegociosEntrantes.add(new Negocios(nombre, gmail, localidad, imagenCodificada));
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