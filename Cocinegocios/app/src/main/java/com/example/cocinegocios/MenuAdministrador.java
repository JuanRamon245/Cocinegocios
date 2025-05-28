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
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.cocinegocios.Adaptadores.AdaptadorListaCategorias;
import com.example.cocinegocios.Adaptadores.AdaptadorListaProductos;
import com.example.cocinegocios.Clases.Categoria;
import com.example.cocinegocios.Clases.Producto;
import com.example.cocinegocios.Clases.UsuariosSQLite;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

/**
 * Clase con de poder visualizar el todos los productos y filtrarlos
 * <p>
 * Esta clase contiene la lógica necesaria para que los usuarios puedan ver todos los productos del negocio y poder tambien filtrarlos para una mejor visyalización. Dependiendo el rol, tambien
 * podran crear categorias o dirigirnos a crear y editar productos del propio negocio.
 * <p>
 * Autor: [Juan Ramón de León Martín]
 * Fecha: [3/12/2024]
 */

public class MenuAdministrador extends AppCompatActivity implements View.OnClickListener {

    private Button botonCategorias, botonCrearProductos;
    private TextView textoMenu;

    private ListView listaCategorias;

    private ArrayList<Categoria> listaCategoriasEntrantes = new ArrayList<>();
    private ArrayAdapter<Categoria> miAdaptador;

    private ListView listaProductos;
    private ArrayList<Producto> listaProductosEntrantes = new ArrayList<>();
    private AdaptadorListaProductos adaptadorProductos;

    private String correoUsuario, rolUsuario, correoNegocio;

    private Bundle negocioSeleccionado;

    private Boolean cierto = false;

    SQLiteDatabase baseDatos;

    private ArrayList<Categoria> listaVerificación;

    /**
     * Interfaz que sirve para el uso y funcionamiento del metodo 'existeCategoriaEnNegocio()' y comprobar que hay categorias creadas para poder crear productos
     */
    public interface CategoriaCallback {
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
     * con el setOnClickLitsener implementado de la clase. Tambien nos carga los productos del negocio en el que estemos actualmente.
     * Por ultimo se encarga de manejar el menú desplegable por el cual nosotros nos podremos mover entre las distintas actividades dependiendo de nuestro rol.
     *
     * @param savedInstanceState Bundle que contiene la estancia del fragmento, en caso de que esté siendo recreado.
     *
     * @return view La vista ya inicializada y asociados sus elementos.
     */
    @SuppressLint("Range")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu_administrador);

        botonCategorias = findViewById(R.id.botonCategorias);
        botonCategorias.setOnClickListener(this);

        botonCrearProductos = findViewById(R.id.BotonParaCrearProducto);
        botonCrearProductos.setOnClickListener(this);

        textoMenu = findViewById(R.id.textoMenu);

        FirebaseApp.initializeApp(MenuAdministrador.this);

        //Buscamos el rol y el correo del usuario actual, para en el caso de necesitar gestionar algo dependiendo de su rol, poder hacerlo
        UsuariosSQLite baseDatosUsuarios = new UsuariosSQLite(this, "bbddUsuarios", null, 1);
        baseDatos = baseDatosUsuarios.getWritableDatabase();

        String consultaUsuario = "SELECT correo, rol, contrasena FROM usuario LIMIT 1";
        Cursor cursor = baseDatos.rawQuery(consultaUsuario, null);

        if (cursor.moveToFirst()) {
            correoUsuario = cursor.getString(cursor.getColumnIndex("correo"));
            rolUsuario = cursor.getString(cursor.getColumnIndex("rol"));
        }

        //Conseguimos el negocio actual por medio del bundle
        negocioSeleccionado = getIntent().getExtras();
        correoNegocio = negocioSeleccionado.getString("negocioLoggued");
        obtenerProductosPorNegocio(correoNegocio);

        //Configuramos el adaptador para mostrar los productos del negocio
        listaProductos = findViewById(R.id.listaProductos);
        adaptadorProductos = new AdaptadorListaProductos(this, listaProductosEntrantes, correoNegocio);
        listaProductos.setAdapter(adaptadorProductos);

        //Configuramos el NavigationView para tener una cabecera donde poder acceder a nuestro perfil
        NavigationView navigationView = findViewById(R.id.navigationViewTT);

        View headerView = navigationView.getHeaderView(0);
        ImageButton botonPerfil = headerView.findViewById(R.id.botonParaIrAlPerfil);
        botonPerfil.setOnClickListener(this);

        //Se carga el repositorio o direccion donde se vayan a guardar las imagenes en el Storage de Firebase
        if (rolUsuario.equals("Administrador")) {
            navigationView.getMenu().findItem(R.id.GestionarComandas).setVisible(false);
        } else if (rolUsuario.equals("Camarero")) {
            navigationView.getMenu().findItem(R.id.CrearEspacios).setVisible(false);
            navigationView.getMenu().findItem(R.id.GestionarTrabajadores).setVisible(false);
            navigationView.getMenu().findItem(R.id.GestionarNegocio).setVisible(false);
            botonCrearProductos.setVisibility(View.GONE);
        } else {
            navigationView.getMenu().findItem(R.id.CrearEspacios).setVisible(false);
            navigationView.getMenu().findItem(R.id.GestionarTrabajadores).setVisible(false);
            navigationView.getMenu().findItem(R.id.GestionarNegocio).setVisible(false);
            botonCrearProductos.setVisibility(View.GONE);
        }

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
            Intent irMenu = new Intent(MenuAdministrador.this, MenuAdministrador.class);
            irMenu.putExtras(negocioSeleccionado);
            startActivity(irMenu);
        } else if (item.getItemId() == R.id.CrearEspacios) {
            Intent irEspacios = new Intent(MenuAdministrador.this, GestionarEspacios.class);
            irEspacios.putExtras(negocioSeleccionado);
            startActivity(irEspacios);
        } else if (item.getItemId() == R.id.GestionarTrabajadores) {
            Intent irEspacios = new Intent(MenuAdministrador.this, GestionarTrabajadores.class);
            irEspacios.putExtras(negocioSeleccionado);
            startActivity(irEspacios);
        } else if (item.getItemId() == R.id.GestionarComandas) {
            Intent irEspacios = new Intent(MenuAdministrador.this, GestionarComandas.class);
            irEspacios.putExtras(negocioSeleccionado);
            startActivity(irEspacios);
        } else if (item.getItemId() == R.id.GestionarNegocio) {
            Intent irNegocio = new Intent(MenuAdministrador.this, GestionarNegocioUsuario.class);
            irNegocio.putExtras(negocioSeleccionado);
            startActivity(irNegocio);
        } else if (item.getItemId() == R.id.CerrarSesion) {
            Intent irEspacios = new Intent(MenuAdministrador.this, SeleccionDeNegocios.class);

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
     * Esta clase coge da lógica a los botones con el SetOnClickLitseners. En este caso podemos tras acceder a esta pagina, podemos dirigirnos a podder crear productso, en el caso
     * de existir categorias.Tambien nos permite crear categorias y eliminarlas en el caso de ser administradores Tambien sirve para manejar la logica de poder dirigirnos al perfil
     * del usuario actual.
     *
     * @param v La vista actual de donde estamos trabajando.
     */
    @Override
    public void onClick(View v) {
        /*
         * Se da lógica al boton que sirve para mostrar las categorias del negocio por medio del botomsheet view, donde al seleccionar una filtraremos los productos por medio de esa categoria.
         * Tambien si nuestro rol dentro del negocio es el de administrador, podremos crear y eliminar categorias, y en si si eliminamos una categoria eliminamos los productos dentro de la categoria.
         */
        if (v.getId() == R.id.botonCategorias) {
            BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
            View bottomSheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_categorias, null);
            bottomSheetDialog.setContentView(bottomSheetView);

            listaCategorias = bottomSheetView.findViewById(R.id.listaCategorias);

            miAdaptador = new AdaptadorListaCategorias(this, listaCategoriasEntrantes, rolUsuario);
            listaCategorias.setAdapter(miAdaptador);
            obtenerCategoriasDesdeFirebase(correoNegocio);

            listaCategorias.setOnItemClickListener((adapterView, view, position, id) -> {
                Categoria categoriaSeleccionada = listaCategoriasEntrantes.get(position);
                textoMenu.setText(categoriaSeleccionada.getNombreCategoria());
                obtenerProductosPorNegocioCategoria(correoNegocio, categoriaSeleccionada.getNombreCategoria());
                bottomSheetDialog.dismiss();
            });

            Button botonCrearCategoria = bottomSheetView.findViewById(R.id.botonCrearCategoria);

            if (!rolUsuario.equals("Administrador")) {
                botonCrearCategoria.setVisibility(View.GONE);
            }

            /*
             * Se da lógica al boton que sirve para crear las categorias del negocio actual
             */
            botonCrearCategoria.setOnClickListener(view -> mostrarDialogoCrearCategoria());
            bottomSheetDialog.show();
        /*
         * Se da lógica al boton que sirve para dirigirnos a la pagina para poder crear productos, en el caso de que minimo exista una sola categoria en el negocio.
         */
        } else if (v.getId() == R.id.BotonParaCrearProducto) {
            listaVerificación = new ArrayList<>();
            existeCategoriaEnNegocio(correoNegocio, listaVerificación, resultado -> {
                if ("hay categoria".equals(resultado)) {
                    Intent botonIrCrearProductos = new Intent(MenuAdministrador.this, CrearProductos.class);
                    botonIrCrearProductos.putExtras(negocioSeleccionado);
                    startActivity(botonIrCrearProductos);
                } else if ("no hay categoria".equals(resultado)) {
                    Snackbar.make(v, "Crea minimo una categoria para crear un producto", Snackbar.LENGTH_SHORT).show();
                } else {
                    Log.e("Resultado", "Error al verificar categorías");
                }
            });
        /*
         * Se da lógica al boton que sirve para dirigirnos al perfil del usuario al que estamos logueado actualmente.
         */
        } else if (v.getId() == R.id.botonParaIrAlPerfil) {
            Intent botonPerfil = new Intent(MenuAdministrador.this, PerfilUsuarioActual.class);
            botonPerfil.putExtras(negocioSeleccionado);
            startActivity(botonPerfil);
        }
    }

    /**
     * Método que sirve para obtener las categorias contenidas en el negocio actual para el dialogo que sirve para filtrar los productos por categoria
     *
     * @param nombreNegocio Correo del negocio en el que estemos trabajando actualmente.
     */
    private void obtenerCategoriasDesdeFirebase(String nombreNegocio) {
        DatabaseReference databaseReferenceCreacion = FirebaseDatabase.getInstance("https://negocios-de-cocinas-default-rtdb.europe-west1.firebasedatabase.app/").getReference();
        DatabaseReference negocioReferencia = databaseReferenceCreacion.child("categorias");

        //Se buscan todas las categorias y se filtra por el gmail del negocio en el que estemos actualmente para añadirse a la lista
        negocioReferencia.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                listaCategoriasEntrantes.clear();
                for (DataSnapshot categoriaSnapshot : dataSnapshot.getChildren()) {
                    Categoria categoria = categoriaSnapshot.getValue(Categoria.class);
                    if (categoria != null) {
                        String claveCategoria = categoria.getClaveCategoria();
                        String gmail = categoria.getGmailNegocio();
                        String nombre = categoria.getNombreCategoria();
                        if (nombreNegocio.equals(gmail)) {
                            // Solo agrega la categoría si el correo coincide
                            listaCategoriasEntrantes.add(new Categoria(claveCategoria, gmail, nombre));
                        }
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
     * Método que sirve para obtener los productos del negocio en el que estemos
     *
     * @param nombreNegocio Correo del negocio en el que estemos trabajando actualmente.
     */
    private void obtenerProductosPorNegocio(String nombreNegocio) {
        DatabaseReference databaseReferenceProductos = FirebaseDatabase.getInstance().getReference().child("productos");

        //Se buscan todas las categorias y se filtra por el gmail del negocio en el que estemos actualmente para añadirse a la lista
        databaseReferenceProductos.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                listaProductosEntrantes.clear();
                for (DataSnapshot productoSnapshot : dataSnapshot.getChildren()) {
                    Producto producto = productoSnapshot.getValue(Producto.class);
                    if (producto != null && nombreNegocio.equals(producto.getNegocio())) {
                        // Agrega solo los productos que pertenecen al negocio actual
                        listaProductosEntrantes.add(producto);
                    }
                }
                adaptadorProductos.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    /**
     * Método que sirve para obtener los productos del negocio en el que estemos, filtrado por la categoria seleccionada en el dialogo
     *
     * @param nombreNegocio Correo del negocio en el que estemos trabajando actualmente.
     * @param categoriaSeleccionada Categoria seleccionada del bottomSheetDialog por la cual se quieren filtrar los productos.
     */
    private void obtenerProductosPorNegocioCategoria(String nombreNegocio, String categoriaSeleccionada) {
        DatabaseReference databaseReferenceProductos = FirebaseDatabase.getInstance().getReference().child("productos");

        //Se buscan todos los productos del negocio en el que estemos actualmente y se filtran despues por la categoria a la que estan asociada, tomando de referencia la categoria recibida previamente.
        databaseReferenceProductos.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                listaProductosEntrantes.clear();
                for (DataSnapshot productoSnapshot : dataSnapshot.getChildren()) {
                    Producto producto = productoSnapshot.getValue(Producto.class);
                    if (producto != null && nombreNegocio.equals(producto.getNegocio())) {
                        if (producto != null && categoriaSeleccionada.equals(producto.getCategoria())) {
                            listaProductosEntrantes.add(producto);
                        }
                    }
                }
                adaptadorProductos.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }


    /**
     * Método que sirve para visualizar un dialogo para poder crear las categorias para el negocio en el que estamos actualmente.
     */
    private void mostrarDialogoCrearCategoria() {
        //Crear el diálogo personalizado
        Dialog dialogoCrearCategoria = new Dialog(this);
        dialogoCrearCategoria.setContentView(R.layout.dialogo_crear_categoria);
        dialogoCrearCategoria.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        //Referencias a los elementos del layout del diálogo
        TextView titulo = dialogoCrearCategoria.findViewById(R.id.nombreTituloCategoria);
        EditText editTextNombreCategoria = dialogoCrearCategoria.findViewById(R.id.nombrePropuestoCategoria);
        Button botonCrear = dialogoCrearCategoria.findViewById(R.id.botonCrearCategoriaPanel);

        //Dependiendo de un filtrado del rol del usuario poder mostrar o  no la capacidad de crear o eliminar las categorias
        if (rolUsuario.equals("Administrador")) {
            botonCrear.setOnClickListener(v -> {
                String nombreCategoria = editTextNombreCategoria.getText().toString();
                //Comprobamos que todos los campos esten rellenos
                if (nombreCategoria.isEmpty()) {
                    Snackbar.make(v, "Rellena todos los campos", Snackbar.LENGTH_SHORT).show();
                } else {
                    //Creamos el ID de la categoria
                    String claveCategoria = correoUsuario + "_" + nombreCategoria;
                    Categoria nuevaCategoria = new Categoria(claveCategoria, correoUsuario, nombreCategoria);
                    //Hacemos una busqueda a ver si existe una categoria ya igual en Firebase
                    DatabaseReference databaseReferenceCreacion = FirebaseDatabase.getInstance("https://negocios-de-cocinas-default-rtdb.europe-west1.firebasedatabase.app/").getReference();
                    DatabaseReference categoriaReferencia = databaseReferenceCreacion.child("categorias");
                    categoriaReferencia.child(claveCategoria).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            //En caso de existir
                            if (snapshot.exists()) {
                                Snackbar.make(v, "La categoria '" + nombreCategoria + "' existe en la base de datos", Snackbar.LENGTH_SHORT).show();
                            //En caso de no existir
                            } else {
                                categoriaReferencia.child(claveCategoria).setValue(nuevaCategoria).addOnSuccessListener(aVoid -> {
                                            //Mostrar mensaje de éxito
                                            Snackbar.make(findViewById(android.R.id.content), "Categoria registrada", Snackbar.LENGTH_SHORT).show();
                                            //Cierra el diálogo de creación
                                            dialogoCrearCategoria.dismiss();
                                            //Actualiza la lista de categorías en el BottomSheetDialog
                                            obtenerCategoriasDesdeFirebase(correoNegocio);
                                        })
                                        .addOnFailureListener(e -> {
                                            //Mostrar mensaje de error
                                            Snackbar.make(findViewById(android.R.id.content), "Categoria sin registrar", Snackbar.LENGTH_SHORT).show();
                                        });
                                Snackbar.make(findViewById(android.R.id.content), "Categoria registrado con éxito", Snackbar.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Snackbar.make(findViewById(android.R.id.content), "Error de conexión con la base de datos", Snackbar.LENGTH_SHORT).show();
                        }
                    });
                }
            });
        //En el cao de que el rol del usuario actual no sea el de Administrador no se muestra el boton para crear categorias en el negocio
        } else if (rolUsuario.equals("Camarero")) {
            botonCrear.setVisibility(View.GONE);
        } else {
            botonCrear.setVisibility(View.GONE);
        }
        dialogoCrearCategoria.show();
    }

    /**
     * Método que fuerza la recarga de la lista de productos del negocio donde esta el usuario actualmente.
     */
    public void actualizarListaProductos() {
        obtenerProductosPorNegocio(correoNegocio);
        adaptadorProductos.notifyDataSetChanged();
    }

    /**
     * Método que sirve para verificar y cargar los nuevos productosComanda que hayan sido añadidos.
     *
     * @param nombreNegocio Correo del negocio en el que estemos actualmente.
     * @param categoriasEncontradas Lista de categorias para poder llenar y luego meter en el listado correspondiente.
     * @param callback Llamad a la interfaz para mostrar las categorias correctamente.
     */
    public void existeCategoriaEnNegocio(String nombreNegocio, ArrayList<Categoria> categoriasEncontradas, CategoriaCallback callback) {
        DatabaseReference databaseReferenceProductos = FirebaseDatabase.getInstance().getReference().child("categorias");

        //Hacemos una busqueda de todas las categorias y las filtramos por el negocio del usuario
        databaseReferenceProductos.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                //Limpia la lista antes de agregar nuevos elementos
                categoriasEncontradas.clear();
                boolean cierto = false;

                for (DataSnapshot categoriaSnapshot : dataSnapshot.getChildren()) {
                    Categoria categoria = categoriaSnapshot.getValue(Categoria.class);
                    if (categoria != null && nombreNegocio.equals(categoria.getGmailNegocio())) {
                        //Agrega los productos encontrados a la lista proporcionada
                        categoriasEncontradas.add(categoria);
                        cierto = true;
                        //Detén el bucle cuando encuentres una coincidencia
                        break;
                    }
                }

                //Dar un resultado al callback
                if (cierto) {
                    callback.onResult("hay categoria");
                } else {
                    callback.onResult("no hay categoria");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FirebaseError", "Error al obtener productos: " + error.getMessage());
                callback.onResult("error");
            }
        });
    }


}