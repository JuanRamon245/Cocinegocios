package com.example.cocinegocios;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.cocinegocios.Adaptadores.AdaptadorListaEspacios;
import com.example.cocinegocios.Clases.Espacio;
import com.example.cocinegocios.Clases.UsuariosSQLite;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

/**
 * Clase con de poder gestionar los espacios
 * <p>
 * Esta clase contiene la lógica necesaria para que los administradores puedan ver los espacios creados en los negocios y poder crearlos si es necesario.
 * Ademas de esto y poder gestionarlos, se crea en esta actividad el menu desplegable con todas las opciones del uusario dependiendo su rol dentro del negocio.
 * <p>
 * Autor: [Juan Ramón de León Martín]
 * Fecha: [3/12/2024]
 */

public class GestionarEspacios extends AppCompatActivity implements View.OnClickListener {

    private Button botonCrearEspacios;

    private ListView listaEspacios;
    private ArrayList<Espacio> listaEspaciosEntrantes = new ArrayList<>();
    private AdaptadorListaEspacios adaptadorEspacios;

    private String correoUsuario, rolUsuario, correoNegocio;

    SQLiteDatabase baseDatos;

    private Bundle negocioSeleccionado;

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
     * Esta clase coge y asocia los elementos de la vista, los inicializa, y luego los asocia con el layout correspondiente. Tambien settea los botónes
     * con el setOnClickLitsener implementado de la clase. Tambien hace uso del adaptador 'AdaptadorListaEspacios' para poder cargar los espacios del negocio en el que estamos actualmente mediante
     * el metodo 'obtenerEspaciosPorNegocio()'. Tambien nos permite crear espacios para el negocio sin tener que desplazarnos a otra actividad y poder editar el propio espacio pero solo el
     * número de mesas de las que dispone. Por ultimo se encarga de manejar el menú desplegable por el cual nosotros nos podremos mover entre las distintas actividades dependiendo de nuestro rol.
     *
     * @param savedInstanceState Bundle que contiene la estancia del fragmento, en caso de que esté siendo recreado.
     *
     * @return view La vista ya inicializada y asociados sus elementos.
     */
    @SuppressLint("Range")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gestionar_espacios);

        botonCrearEspacios = findViewById(R.id.botonParaCrearEspacio);
        botonCrearEspacios.setOnClickListener(this);

        //Conseguimos el negocio actual por medio del bundle
        negocioSeleccionado = getIntent().getExtras();
        correoNegocio = negocioSeleccionado.getString("negocioLoggued");

        //Cargar y rellenar el listado de espacios con los espacios del negocio actual
        listaEspacios = findViewById(R.id.listaEspacios);
        adaptadorEspacios = new AdaptadorListaEspacios(this, listaEspaciosEntrantes, correoNegocio);
        listaEspacios.setAdapter(adaptadorEspacios);
        obtenerEspaciosPorNegocio(correoNegocio);

        //Buscamos el rol y el correo del usuario actual, para en el caso de necesitar gestionar algo dependiendo de su rol, poder hacerlo
        UsuariosSQLite baseDatosUsuarios = new UsuariosSQLite(this, "bbddUsuarios", null, 1);
        baseDatos = baseDatosUsuarios.getWritableDatabase();

        String consultaUsuario = "SELECT correo, rol, contrasena FROM usuario LIMIT 1";
        Cursor cursor = baseDatos.rawQuery(consultaUsuario, null);

        if (cursor.moveToFirst()) {
            correoUsuario = cursor.getString(cursor.getColumnIndex("correo"));
            rolUsuario = cursor.getString(cursor.getColumnIndex("rol"));
        }

        //Configuramos el NavigationView para tener una cabecera donde poder acceder a nuestro perfil
        NavigationView navigationView = findViewById(R.id.navigationViewTT);

        View headerView = navigationView.getHeaderView(0);
        ImageButton botonPerfil = headerView.findViewById(R.id.botonParaIrAlPerfil);
        botonPerfil.setOnClickListener(this);

        //Manejamos segun el rol del usuario las posibilidades a las que puede viajar
        if (rolUsuario.equals("Administrador")) {
            navigationView.getMenu().findItem(R.id.GestionarComandas).setVisible(false);
        } else if (rolUsuario.equals("Camarero")) {
            navigationView.getMenu().findItem(R.id.CrearEspacios).setVisible(false);
            navigationView.getMenu().findItem(R.id.GestionarTrabajadores).setVisible(false);
            navigationView.getMenu().findItem(R.id.GestionarNegocio).setVisible(false);
        } else {
            navigationView.getMenu().findItem(R.id.CrearEspacios).setVisible(false);
            navigationView.getMenu().findItem(R.id.GestionarTrabajadores).setVisible(false);
            navigationView.getMenu().findItem(R.id.GestionarNegocio).setVisible(false);
        }

        //Añadimos un escuchador a cada uno de los elemento del menu para gestionar despues que hacer con ello
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                handleNavigationItemSelected(item);
                return true;
            }
        });
    }

    /**
     * Método que sirve para que el menu que tenemos a la izquierda al deslizar pueda llevarnos a la actividad correspondiente de la aplicación
     *
     * @param item Objeto del menu que previamente fue seleccionado.
     */
    private void handleNavigationItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.Menu) {
            Intent irMenu = new Intent(GestionarEspacios.this, MenuAdministrador.class);
            irMenu.putExtras(negocioSeleccionado);
            startActivity(irMenu);
        } else if (item.getItemId() == R.id.CrearEspacios) {
            Intent irEspacios = new Intent(GestionarEspacios.this, GestionarEspacios.class);
            irEspacios.putExtras(negocioSeleccionado);
            startActivity(irEspacios);
        } else if (item.getItemId() == R.id.GestionarTrabajadores) {
            Intent irEspacios = new Intent(GestionarEspacios.this, GestionarTrabajadores.class);
            irEspacios.putExtras(negocioSeleccionado);
            startActivity(irEspacios);
        } else if (item.getItemId() == R.id.GestionarComandas) {
            Intent irEspacios = new Intent(GestionarEspacios.this, GestionarComandas.class);
            irEspacios.putExtras(negocioSeleccionado);
            startActivity(irEspacios);
        } else if (item.getItemId() == R.id.GestionarNegocio) {
            Intent irNegocio = new Intent(GestionarEspacios.this, GestionarNegocioUsuario.class);
            irNegocio.putExtras(negocioSeleccionado);
            startActivity(irNegocio);
        } else if (item.getItemId() == R.id.CerrarSesion) {
            Intent irEspacios = new Intent(GestionarEspacios.this, SeleccionDeNegocios.class);

            String actualizacionRolUsuario = "UPDATE usuario SET rol = ? WHERE correo = ?";
            SQLiteStatement statement = baseDatos.compileStatement(actualizacionRolUsuario);
            statement.bindNull(1);
            statement.bindString(2, correoUsuario);
            statement.executeUpdateDelete();

            startActivity(irEspacios);
        }
    }

    /**
     * Metodo para dar funcionalidad a los SetOnClickLitseners aasociados arriba
     * <p>
     * Esta clase coge da lógica a los botones con el SetOnClickLitseners. En este caso podemos crear un espacio por medio del dialogo que se nos abre con un botón.
     * Tambien sirve para manejar la logica de poder dirigirnos al perfil del usuario actual.
     *
     * @param v La vista actual de donde estamos trabajando.
     */
    @Override
    public void onClick(View v) {
        /*
         * Se da lógica al boton que sirve para crear las espacios dentro del negocio que podremos usar más adelante para gestionar.
         */
        if (v.getId() == R.id.botonParaCrearEspacio) {
            //Crea el diálogo y establece el layout
            Dialog dialogoCrearEspacio = new Dialog(this);
            dialogoCrearEspacio.setContentView(R.layout.dialogo_crear_espacio);
            dialogoCrearEspacio.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

            //Obtiene referencias de los EditText y el botón del diálogo
            EditText nombreEspacioEditText = dialogoCrearEspacio.findViewById(R.id.nombrePropuestoEspacio);
            EditText nMesasEditText = dialogoCrearEspacio.findViewById(R.id.nMesasPropuestoEspacio);
            Button botonCrear = dialogoCrearEspacio.findViewById(R.id.botonCrearCategoriaPanel);

            /*
             * Se da lógica al boton que sirve para crear el espacio dentro del dialogo
             */
            botonCrear.setOnClickListener(view -> {
                String nombreEspacio = nombreEspacioEditText.getText().toString();
                if (!nombreEspacio.isEmpty()) {
                    String nMesas = nMesasEditText.getText().toString();
                    String id = correoNegocio+"*"+nombreEspacio;

                    DatabaseReference databaseReferenceCreacion = FirebaseDatabase.getInstance("https://negocios-de-cocinas-default-rtdb.europe-west1.firebasedatabase.app/").getReference();
                    DatabaseReference espacioReferencia = databaseReferenceCreacion.child("espacio");

                    //En el caso de que se introduzca un número int se continua
                    try {
                        int nMesasNumeral = Integer.valueOf(nMesas);

                        //Se busca si el espacio introducido existe en la base de datos para el  negocio y no se crea si es así
                        espacioReferencia.child(id).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                //En el caso de existir el espacio
                                if (snapshot.exists()) {
                                    Snackbar.make(findViewById(android.R.id.content), "El espacio '" + nombreEspacio + "' existe en la base de datos", Snackbar.LENGTH_SHORT).show();
                                } else {
                                    Espacio nuevoEspacio = new Espacio(id, nombreEspacio, correoNegocio, nMesasNumeral);
                                    espacioReferencia.child(id).setValue(nuevoEspacio).addOnSuccessListener(aVoid -> {
                                                //Mostrar mensaje de éxito
                                                Snackbar.make(findViewById(android.R.id.content), "Espacio registrado", Snackbar.LENGTH_SHORT).show();
                                                actualizarListaEspacios();
                                            })
                                            .addOnFailureListener(e -> {
                                                //Mostrar mensaje de error
                                                Snackbar.make(findViewById(android.R.id.content), "Usuario sin registrar", Snackbar.LENGTH_SHORT).show();
                                            });
                                    Snackbar.make(findViewById(android.R.id.content), "Negocio registrado con éxito", Snackbar.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Snackbar.make(findViewById(android.R.id.content), "Error de conexión con la base de datos", Snackbar.LENGTH_SHORT).show();
                            }
                        });

                        dialogoCrearEspacio.dismiss();
                    } catch (NumberFormatException e) {
                        Snackbar.make(v, "Introduce un número válido para las mesas", Snackbar.LENGTH_SHORT).show();
                    }
                } else {
                    Snackbar.make(v, "Introduce un nombre al espacio", Snackbar.LENGTH_SHORT).show();
                }

            });
            dialogoCrearEspacio.show();
        /*
         * Se da lógica al boton que sirve para dirigirnos al perfil del usuario al que estamos logueado actualmente.
         */
        } else if (v.getId() == R.id.botonParaIrAlPerfil) {
            Intent botonPerfil = new Intent(GestionarEspacios.this, PerfilUsuarioActual.class);
            botonPerfil.putExtras(negocioSeleccionado);
            startActivity(botonPerfil);
        }
    }

    /**
     * Método que sirve para obtener los espacios de firebase y mostrarlos en la actividad.
     *
     * @param nombreNegocio Correo del negocio en el que estemos trabajando actualmente.
     */
    private void obtenerEspaciosPorNegocio(String nombreNegocio) {
        //Se buscan los espacios de la base de datos y se filtra por medio del correo del negocio actual
        DatabaseReference databaseReferenceEspacios = FirebaseDatabase.getInstance().getReference().child("espacio");

        databaseReferenceEspacios.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                listaEspaciosEntrantes.clear();
                for (DataSnapshot productoSnapshot : dataSnapshot.getChildren()) {
                    Espacio espacio = productoSnapshot.getValue(Espacio.class);
                    if (espacio != null && nombreNegocio.equals(espacio.getNegocio())) {
                        //Agrega solo los productos que pertenecen al negocio actual
                        listaEspaciosEntrantes.add(espacio);
                    }
                }
                adaptadorEspacios.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    /**
     * Método que fuerza la recarga de la lista de espacios del negocio donde esta el usuario actualmente.
     */
    public void actualizarListaEspacios() {
        obtenerEspaciosPorNegocio(correoNegocio);
        adaptadorEspacios.notifyDataSetChanged();
    }

}