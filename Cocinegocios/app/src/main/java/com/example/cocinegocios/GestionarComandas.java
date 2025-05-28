package com.example.cocinegocios;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.cocinegocios.Adaptadores.AdaptadorListaComandas;
import com.example.cocinegocios.Adaptadores.AdaptadorListaEspacios;
import com.example.cocinegocios.Clases.Comanda;
import com.example.cocinegocios.Clases.Espacio;
import com.example.cocinegocios.Clases.Producto;
import com.example.cocinegocios.Clases.UsuariosSQLite;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;

/**
 * Clase con de poder gestionar las comandas
 * <p>
 * Esta clase contiene la lógica necesaria para que los administradores puedan ver las comandas creadas en el negocio.
 * Ademas de esto y poder gestionarlas, se crea en esta actividad el menu desplegable con todas las opciones del uusario dependiendo su rol dentro del negocio.
 * <p>
 * Autor: [Juan Ramón de León Martín]
 * Fecha: [3/12/2024]
 */

public class GestionarComandas extends AppCompatActivity implements View.OnClickListener {

    private Button botonParaCrearComanda;

    private ListView listaComandas;
    private ArrayList<Comanda> listaComandasEntrantes = new ArrayList<>();
    private AdaptadorListaComandas adaptadorComandas;

    private ArrayList<Espacio> listaEspaciosEntrantes = new ArrayList<>();
    private ArrayList<String> espacios = new ArrayList<>();
    private ArrayAdapter<Espacio> adaptadorEspacios;

    private String correoUsuario, rolUsuario, correoNegocio, guardarEspacio, id, espacio, negocio, indicador;
    private int mesa;
    private Double subtotal;

    SQLiteDatabase baseDatos;

    private Bundle negocioSeleccionado;

    private ArrayList<Producto> listaVerificaciónProductos;

    /**
     * Interfaz que sirve para el uso y funcionamiento del metodo 'existeProductoEnNegocio()' y comprobar que hay productos
     */
    public interface ProductosCallback {
        void onResult(String resultado);
    }

    private ArrayList<Espacio> listaVerificaciónEspacios;

    /**
     * Interfaz que sirve para el uso y funcionamiento del metodo 'existeEspacioEnNegocio()' y comprobar que hay espacios
     */
    public interface EspaciosCallback {
        void onResult(String resultado);
    }

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
     * con el setOnClickLitsener implementado de la clase. Tambien hace uso del adaptador 'AdaptadorListaComandas' para poder cargar las comandas del negocio en el que estamos actualmente  mediante
     * el metodo 'obtenerComandasPorNegocio()'. Tambien carga el primer espacio del negocio ordenado alfabeticamente haciendo uso del adaptador 'AdaptadorListaEspacios' y el metodo
     * 'obtenerPrimerEspacioDesdeFirebase()'. Por ultimo se encarga de manejar el menú desplegable por el cual nosotros nos podremos mover entre las distintas actividades dependiendo de nuestro rol.
     *
     * @param savedInstanceState Bundle que contiene la estancia del fragmento, en caso de que esté siendo recreado.
     *
     * @return view La vista ya inicializada y asociados sus elementos.
     */
    @SuppressLint("Range")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gestionar_comandas);

        botonParaCrearComanda = findViewById(R.id.botonParaCrearComanda);
        botonParaCrearComanda.setOnClickListener(this);

        //Conseguimos el negocio actual por medio del bundle
        negocioSeleccionado = getIntent().getExtras();
        correoNegocio = negocioSeleccionado.getString("negocioLoggued");

        //Cargar y rellenar el listado de comandas con las comandas del negocio actual
        listaComandas = findViewById(R.id.listaComandas);
        adaptadorComandas = new AdaptadorListaComandas(this, listaComandasEntrantes, correoNegocio);
        listaComandas.setAdapter(adaptadorComandas);
        obtenerComandasPorNegocio(correoNegocio);

        //Buscamos el rol y el correo del usuario actual, para en el caso de necesitar gestionar algo dependiendo de su rol, poder hacerlo
        UsuariosSQLite baseDatosUsuarios = new UsuariosSQLite(this, "bbddUsuarios", null, 1);
        baseDatos = baseDatosUsuarios.getWritableDatabase();

        String consultaUsuario = "SELECT correo, rol, contrasena FROM usuario LIMIT 1";
        Cursor cursor = baseDatos.rawQuery(consultaUsuario, null);

        if (cursor.moveToFirst()) {
            correoUsuario = cursor.getString(cursor.getColumnIndex("correo"));
            rolUsuario = cursor.getString(cursor.getColumnIndex("rol"));
        }

        adaptadorEspacios = new AdaptadorListaEspacios(this, listaEspaciosEntrantes, rolUsuario);
        obtenerPrimerEspacioDesdeFirebase(correoNegocio);

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
            botonParaCrearComanda.setVisibility(View.GONE);
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
            Intent irMenu = new Intent(GestionarComandas.this, MenuAdministrador.class);
            irMenu.putExtras(negocioSeleccionado);
            startActivity(irMenu);
        } else if (item.getItemId() == R.id.CrearEspacios) {
            Intent irEspacios = new Intent(GestionarComandas.this, GestionarEspacios.class);
            irEspacios.putExtras(negocioSeleccionado);
            startActivity(irEspacios);
        } else if (item.getItemId() == R.id.GestionarTrabajadores) {
            Intent irEspacios = new Intent(GestionarComandas.this, GestionarTrabajadores.class);
            irEspacios.putExtras(negocioSeleccionado);
            startActivity(irEspacios);
        } else if (item.getItemId() == R.id.GestionarComandas) {
            Intent irEspacios = new Intent(GestionarComandas.this, GestionarComandas.class);
            irEspacios.putExtras(negocioSeleccionado);
            startActivity(irEspacios);
        } else if (item.getItemId() == R.id.GestionarNegocio) {
            Intent irNegocio = new Intent(GestionarComandas.this, GestionarNegocioUsuario.class);
            irNegocio.putExtras(negocioSeleccionado);
            startActivity(irNegocio);
        } else if (item.getItemId() == R.id.CerrarSesion) {
            Intent irEspacios = new Intent(GestionarComandas.this, SeleccionDeNegocios.class);

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
     * Esta clase coge da lógica a los botones con el SetOnClickLitseners. En este caso podemos generar la comanda primero para que luego la logica para añadir productosComanda no sea un caos
     * mediante el boton para dirigirnos a la pantalla para crear comandas. Tambien sirve para manejar la logica de poder dirigirnos al perfil del usuario actual.
     *
     * @param v La vista actual de donde estamos trabajando.
     */
    @Override
    public void onClick(View v) {
        /*
         * Se da lógica al boton que sirve para crear las comandas antes de poder dirigirnos a la pantalla para crear comandas y poder añadir productosComanda asociados a la comanda
         */
        if (v.getId() == R.id.botonParaCrearComanda) {
            //Se hace un pequeño filtro para verificar que el negocio actual tiene productos y espacios para poder generar comandas
            listaVerificaciónProductos = new ArrayList<>();
            existeProductoEnNegocio(correoNegocio, listaVerificaciónProductos, resultadoProducto -> {
                if ("hay producto".equals(resultadoProducto)) {
                    listaVerificaciónEspacios = new ArrayList<>();
                    existeEspacioEnNegocio(correoNegocio, listaVerificaciónEspacios, resultadoEspacio -> {
                    if ("hay espacio".equals(resultadoEspacio)) {
                        id = generarIdAleatorio(5) + "*" + correoNegocio;
                        mesa = 1;
                        indicador = "enProduccion";
                        subtotal = 1.11;

                        //En el caso de que haya un espacio guardado o no se crea de una forma u otra la comanda antes de dirigirnos a la pantalla
                        if (guardarEspacio != null) {
                            espacio = guardarEspacio;
                            Comanda nuevaComanda = new Comanda(id, correoNegocio, espacio, indicador, mesa, subtotal);
                            DatabaseReference databaseReferenceCreacion = FirebaseDatabase.getInstance("https://negocios-de-cocinas-default-rtdb.europe-west1.firebasedatabase.app/").getReference();
                            DatabaseReference referenciaComandas = databaseReferenceCreacion.child("comandas");

                            referenciaComandas.child(id).setValue(nuevaComanda).addOnSuccessListener(aVoid -> {
                                Snackbar.make(findViewById(android.R.id.content), "Comanda registrada", Snackbar.LENGTH_SHORT).show();
                                negocioSeleccionado.putString("idComanda", id);
                                Intent botonIrCrearProductos = new Intent(GestionarComandas.this, CrearComandas.class);
                                botonIrCrearProductos.putExtras(negocioSeleccionado);
                                startActivity(botonIrCrearProductos);
                            }).addOnFailureListener(e -> {
                                Snackbar.make(findViewById(android.R.id.content), "Comanda sin registrar", Snackbar.LENGTH_SHORT).show();
                            });
                        } else {
                            espacio = "";
                            Comanda nuevaComanda = new Comanda(id, correoNegocio, espacio, indicador, mesa, subtotal);
                            DatabaseReference databaseReferenceCreacion = FirebaseDatabase.getInstance("https://negocios-de-cocinas-default-rtdb.europe-west1.firebasedatabase.app/").getReference();
                            DatabaseReference referenciaComandas = databaseReferenceCreacion.child("comandas");

                            referenciaComandas.child(id).setValue(nuevaComanda).addOnSuccessListener(aVoid -> {
                                Snackbar.make(findViewById(android.R.id.content), "Comanda registrada", Snackbar.LENGTH_SHORT).show();
                                negocioSeleccionado.putString("idComanda", id);
                                Intent botonIrCrearProductos = new Intent(GestionarComandas.this, CrearComandas.class);
                                botonIrCrearProductos.putExtras(negocioSeleccionado);
                                startActivity(botonIrCrearProductos);
                            }).addOnFailureListener(e -> {
                                Snackbar.make(findViewById(android.R.id.content), "Comanda sin registrar", Snackbar.LENGTH_SHORT).show();
                            });
                        }
                    } else if ("no hay espacio".equals(resultadoEspacio)) {
                        Snackbar.make(v, "Crea minimo un espacio para crear una comanda", Snackbar.LENGTH_SHORT).show();
                    } else {
                        Log.e("Resultado", "Error al verificar espacios");
                    }
                    });
                } else if ("no hay producto".equals(resultadoProducto)) {
                    Snackbar.make(v, "Crea minimo un producto para crear una comanda", Snackbar.LENGTH_SHORT).show();
                } else {
                    Log.e("Resultado", "Error al verificar productos");
                }
            });
        /*
         * Se da lógica al boton que sirve para dirigirnos al perfil del usuario al que estamos logueado actualmente.
         */
        } else if (v.getId() == R.id.botonParaIrAlPerfil) {
            Intent botonPerfil = new Intent(GestionarComandas.this, PerfilUsuarioActual.class);
            botonPerfil.putExtras(negocioSeleccionado);
            startActivity(botonPerfil);
        }
    }

    /**
     * Método que sirve para obtener las comandas del negocio en el que estemos y ordenarlas segun su estado, para representar una jerarquia de prioridades
     *
     * @param correoNegocio Correo del negocio en el que estemos trabajando actualmente.
     */
    private void obtenerComandasPorNegocio(String correoNegocio) {
        DatabaseReference databaseReferenceComandas = FirebaseDatabase.getInstance().getReference().child("comandas");

        databaseReferenceComandas.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                listaComandasEntrantes.clear();
                for (DataSnapshot comandaSnapshot : dataSnapshot.getChildren()) {
                    Comanda comanda = comandaSnapshot.getValue(Comanda.class);
                    if (comanda != null && correoNegocio.equals(comanda.getNegocioAsociado())) {
                        //Agrega solo los productos que pertenecen al negocio actual
                        listaComandasEntrantes.add(comanda);
                    }
                }

                //Comparador que compara los estados de las comandas una vez añadidas al listado para mostrarse correctamente
                Collections.sort(listaComandasEntrantes, new Comparator<Comanda>() {
                    @Override
                    public int compare(Comanda comanda1, Comanda comanda2) {
                        //Define el orden de los estados
                        String estado1 = comanda1.getIndicador();
                        String estado2 = comanda2.getIndicador();

                        //1º "listoParaPagar", 2º "listoParaEntregar", 3º "enProduccion"
                        int prioridad1 = getEstadoPrioridad(estado1);
                        int prioridad2 = getEstadoPrioridad(estado2);

                        //Compara las prioridades
                        return Integer.compare(prioridad1, prioridad2);
                    }
                });

                adaptadorComandas.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    /**
     * Método que sirve para obtener las comandas del negocio en el que estemos y ordenarlas segun su estado, para representar una jerarquia de prioridades
     *
     * @param longitud Longitud que debe tener la codificación
     *
     * @return id.toString() Retornamos el codigo generado gracias a la clase Random
     */
    public static String generarIdAleatorio(int longitud) {
        String caracteresPermitidos = "abcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder id = new StringBuilder();
        Random random = new Random();

        for (int i = 0; i < longitud; i++) {
            int indiceAleatorio = random.nextInt(caracteresPermitidos.length());
            char caracterAleatorio = caracteresPermitidos.charAt(indiceAleatorio);
            id.append(caracterAleatorio);
        }

        return id.toString();
    }

    /**
     * Método que busca el primer espacio de firebase en el negocio donde estamos actualmente.
     *
     * @param nombreNegocio Correo del negocio en el que estemos trabajando actualmente.
     */
    private void obtenerPrimerEspacioDesdeFirebase(String nombreNegocio) {
        DatabaseReference databaseReferenceEspacios = FirebaseDatabase.getInstance().getReference().child("espacio");

        databaseReferenceEspacios.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String nombreEspacio = ""; // Variable dentro del alcance correcto
                for (DataSnapshot productoSnapshot : dataSnapshot.getChildren()) {
                    Espacio espacio = productoSnapshot.getValue(Espacio.class);
                    if (espacio != null && nombreNegocio.equals(espacio.getNegocio())) {
                        nombreEspacio = espacio.getNombre();
                        guardarEspacio = nombreEspacio;
                        break;
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    /**
     * Método que fuerza la recarga de la lista de comandas del negocio donde esta el usuario actualmente.
     */
    public void actualizarListaComandas() {
        obtenerComandasPorNegocio(correoNegocio);
        adaptadorComandas.notifyDataSetChanged();
    }

    /**
     * Método que sirve para verificar y cargar los nuevos productosComanda que hayan sido añadidos.
     *
     * @param nombreNegocio Correo del negocio en el que estamos actualmente para poder cargarlos
     * @param productosEncontrados Lista de productos para poder llenar y luego meter en el listado correspondiente.
     * @param callback Llamad a la interfaz para mostrar los productosComanda correctamente.
     */
    public void existeProductoEnNegocio(String nombreNegocio, ArrayList<Producto> productosEncontrados, GestionarComandas.ProductosCallback callback) {
        DatabaseReference databaseReferenceProductos = FirebaseDatabase.getInstance().getReference().child("productos");

        //Se buscan todos los productosComanda de la comanda en la que estemos actualmente y se añaden al listado
        databaseReferenceProductos.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                //Limpia la lista antes de agregar nuevos elementos
                productosEncontrados.clear();
                boolean cierto = false;

                for (DataSnapshot productoSnapshot : dataSnapshot.getChildren()) {
                    Producto producto = productoSnapshot.getValue(Producto.class);
                    if (producto != null && nombreNegocio.equals(producto.getNegocio())) {
                        //Agrega los productos encontrados a la lista proporcionada
                        productosEncontrados.add(producto);
                        cierto = true;
                        //El bucle se para cuando se encuentra una coincidencia
                        break;
                    }
                }

                //Dar un resultado al callback
                if (cierto) {
                    callback.onResult("hay producto");
                } else {
                    callback.onResult("no hay producto");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FirebaseError", "Error al obtener productos: " + error.getMessage());
                callback.onResult("error");
            }
        });
    }

    /**
     * Método que sirve para verificar y cargar los espacios del negocio
     *
     * @param nombreNegocio Correo del negocio en el que estamos actualmente.
     * @param espaciosEncontrados Lista de espacios para comprobar si hay espacios en el negocio actual y añadirlos.
     * @param callback Llamad a la interfaz para mostrar los espacios correctamente.
     */
    public void existeEspacioEnNegocio(String nombreNegocio, ArrayList<Espacio> espaciosEncontrados, GestionarComandas.EspaciosCallback callback) {
        DatabaseReference databaseReferenceEspacios = FirebaseDatabase.getInstance().getReference().child("espacio");

        //Se buscan los espacios de la base de datos y se filtra por medio del correo del negocio actual
        databaseReferenceEspacios.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                //Limpia la lista antes de agregar nuevos elementos
                espaciosEncontrados.clear();
                boolean cierto = false;

                for (DataSnapshot espacioSnapshot : dataSnapshot.getChildren()) {
                    Espacio espacio = espacioSnapshot.getValue(Espacio.class);
                    if (espacio != null && nombreNegocio.equals(espacio.getNegocio())) {
                        espaciosEncontrados.add(espacio);
                        cierto = true;
                        break;
                    }
                }

                //Dar un resultado al callback
                if (cierto) {
                    callback.onResult("hay espacio");
                } else {
                    callback.onResult("no hay espacio");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FirebaseError", "Error al obtener espacios: " + error.getMessage());
                callback.onResult("error");
            }
        });
    }

    //USAR ESTE METODO PARA DAR LOS ICONOS DE LAS COMANDAS

    /**
     * Método que sirve para ordenar las comandas de cada negocio dependiendo su estado.
     *
     * @param estado Estado de la comanda en la que estamos actualmente
     * @return int Retorna un numero que representa la prioridad en la lista para gestionar las comandas.
     */
    private int getEstadoPrioridad(String estado) {
        switch (estado) {
            case "listoParaPagar":
                return 1;
            case "listoParaEntregar":
                return 2;
            case "enProduccion":
                return 3;
            default:
                //Si el estado no es uno de los esperados, lo coloca al final
                return Integer.MAX_VALUE;
        }
    }
}