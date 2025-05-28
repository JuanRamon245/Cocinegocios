package com.example.cocinegocios;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.cocinegocios.Adaptadores.AdaptadorListaCategorias;
import com.example.cocinegocios.Adaptadores.AdaptadorListaEspacios;
import com.example.cocinegocios.Adaptadores.AdaptadorListaProductosComandaCreacion;
import com.example.cocinegocios.Adaptadores.AdaptadorListaProductosDialogo;
import com.example.cocinegocios.Clases.Categoria;
import com.example.cocinegocios.Clases.Comanda;
import com.example.cocinegocios.Clases.Espacio;
import com.example.cocinegocios.Clases.Producto;
import com.example.cocinegocios.Clases.ProductoComanda;
import com.example.cocinegocios.Clases.UsuariosSQLite;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Clase con de poder crear las comandas
 * <p>
 * Esta clase contiene la lógica necesaria para que los camareros puedan añadir productos a las comandas que esten creando, mediante un dialogo con todos los productos del negocio,
 * pudiendolos filtrar por categorias. Tambien puede eliminar lso prodcutos de las comandas si ha habido un error, elegir el espacio del negocio donde se ha realizado la comanda, y en conjunto la mesa.
 * Por ultimo debe permitir cancelarla comanda, porque al crearla se edita con el restod e datos nuevos, ya que la logica para poder añadir los productos a la comanda era necesario crear la comanda
 * previamente a acceder a esta pantalla, porque sino los productos negocio no se podrian añadir.
 * <p>
 * Autor: [Juan Ramón de León Martín]
 * Fecha: [3/12/2024]
 */

public class CrearComandas extends AppCompatActivity implements View.OnClickListener {

    private ArrayList<Espacio> listaEspaciosEntrantes = new ArrayList<>();
    private Map<String, String> espaciosMap = new HashMap<>();
    private ArrayList<String> espacios = new ArrayList<>();
    private ArrayAdapter<Espacio> adaptadorEspacios;

    private ArrayList<Categoria> listaCategoriasEntrantes = new ArrayList<>();
    private ListView listaCategorias;
    private ArrayAdapter<Categoria> miAdaptador;

    private ListView listaProductos;
    private ArrayList<Producto> listaProductosEntrantes = new ArrayList<>();
    private AdaptadorListaProductosDialogo adaptadorProductos;

    private ListView listaProductosComanda;
    private ArrayList<ProductoComanda> listaProductosComandaEntrantes = new ArrayList<>();
    private AdaptadorListaProductosComandaCreacion adaptadorProductosComanda;

    private EditText campoNumeroMesas;
    private Spinner espacioSpinner;

    private String rolUsuario, correoNegocio, idComanda, espacio;
    private int mesa;

    private Button botonCancelarComanda, botonCrearComanda, BotonParaAñadirProductos;

    private Bundle negocioSeleccionado;

    SQLiteDatabase baseDatos;

    private ArrayList<ProductoComanda> listaVerificación;

    /**
     * Interfaz que sirve para el uso y funcionamiento del metodo 'existeProductoComandaEnNegocio()' y comprobar que hay productoComandas y poder crear la comanda con minimo 1 productoComanda
     */
    public interface ProductoComandaCallback {
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
     * con el setOnClickLitsener implementado de la clase. Tambien hace uso del adaptador 'AdaptadorListaEspacios' para poder cargar los espacios del negocio en el que estamos actualmente  mediante
     * el metodo 'obtenerEspaciosDesdeFirebase()'. Tambien carga los productosComanda haciendo uso del adaptador 'AdaptadorListaProductosComandaCreacion' y el metodo 'obtenerProductosComandaPorNegocio()'
     *
     * @param savedInstanceState Bundle que contiene la estancia del fragmento, en caso de que esté siendo recreado.
     *
     * @return view La vista ya inicializada y asociados sus elementos.
     */
    @SuppressLint("Range")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crear_comandas);

        botonCancelarComanda = findViewById(R.id.botonCancelarComanda);
        botonCancelarComanda.setOnClickListener(this);

        botonCrearComanda = findViewById(R.id.botonCrearComanda);
        botonCrearComanda.setOnClickListener(this);

        BotonParaAñadirProductos = findViewById(R.id.BotonParaAñadirProductos);
        BotonParaAñadirProductos.setOnClickListener(this);

        campoNumeroMesas = findViewById(R.id.campoNumeroMesas);

        //Buscamos el rol del usuario actual, para en el caso de necesitar gestionar algo dependiendo de su rol, poder hacerlo
        UsuariosSQLite baseDatosUsuarios = new UsuariosSQLite(this, "bbddUsuarios", null, 1);
        baseDatos = baseDatosUsuarios.getWritableDatabase();

        String consultaUsuario = "SELECT correo, rol, contrasena FROM usuario LIMIT 1";
        Cursor cursor = baseDatos.rawQuery(consultaUsuario, null);

        if (cursor.moveToFirst()) {
            rolUsuario = cursor.getString(cursor.getColumnIndex("rol"));
        }

        negocioSeleccionado = getIntent().getExtras();
        correoNegocio = negocioSeleccionado.getString("negocioLoggued");
        idComanda = negocioSeleccionado.getString("idComanda");
        Log.d("PRUEBAID", idComanda);

        //Se cargan los espacios del negocio actual
        espacioSpinner = findViewById(R.id.espacioSpinner);
        adaptadorEspacios = new AdaptadorListaEspacios(this, listaEspaciosEntrantes, correoNegocio);
        obtenerEspaciosDesdeFirebase();

        //Se cargan los productosComanda de la comanda que estemos creando
        listaProductosComanda = findViewById(R.id.listaProductos);
        adaptadorProductosComanda = new AdaptadorListaProductosComandaCreacion(this, listaProductosComandaEntrantes, correoNegocio);
        listaProductosComanda.setAdapter(adaptadorProductosComanda);
        obtenerProductosComandaPorNegocio(idComanda);
    }

    /**
     * Metodo para dar funcionalidad a los SetOnClickLitseners aasociados arriba
     * <p>
     * Esta clase coge da lógica a los botones con el SetOnClickLitseners. En este caso podemos eliminar la comanda actual si nos hemos equivocado o se canlcela por X o Y razon, eliminando
     * consigo los productosComanda que contenga, para no dejarlos en la BBDD sin hacer nada. La otra posibilidad que tenemos es la de crear la comanda, que como la comanda es creada justo
     * antes de acceder a esta pantalla, pues se editan los ddatos con los nuevos introducidos, los productos comanda son creados mediante otro dialogo asi que no afecta a este apartado.
     * Por último tenemos el boton para añadir productos a la comanda, mediante el dialogo que nos muestra los productos contenidos en el negocio, aunque se pueden filtrar medinate el boton
     * de categorias para que sea más facil.
     *
     * @param v La vista actual de donde estamos trabajando.
     */
    @Override
    public void onClick(View v) {
        /*
         * Se da lógica al boton que sirve para cancelar la comanda, borrandola de la base de datos y consigo los productosComanda que estuvieran asociados.
         */
        if (v.getId() == R.id.botonCancelarComanda) {
            DatabaseReference databaseReferenceProductos = FirebaseDatabase.getInstance().getReference("comandas");
            Query busquedaComandas = databaseReferenceProductos.orderByChild("id").equalTo(idComanda);

            //Se busca la comanda actual para poder eliminarla
            busquedaComandas.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            snapshot.getRef().removeValue();
                        }

                        //En el caso de haberla encontrador, tambien se buscan los productosComanda asociados a la comanda para ser eliminados, tras esto se vuelve a la actividad de las comandas
                        DatabaseReference databaseReferenceProductosComanda = FirebaseDatabase.getInstance().getReference("productosComanda");
                        Query busquedaProductosComanda = databaseReferenceProductosComanda.orderByChild("pedidoAsociado").equalTo(idComanda);

                        busquedaProductosComanda.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                if (dataSnapshot.exists()) {
                                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                        snapshot.getRef().removeValue();
                                    }
                                }
                                Intent botonIrCrearProductos = new Intent(CrearComandas.this, GestionarComandas.class);
                                negocioSeleccionado.remove("idComanda");
                                botonIrCrearProductos.putExtras(negocioSeleccionado);
                                startActivity(botonIrCrearProductos);
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {
                                Snackbar.make(v, "Error al eliminar productos asociados", Snackbar.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Snackbar.make(v, "Error de base de datos", Snackbar.LENGTH_SHORT).show();
                }
            });
            /*
             * Se da lógica al boton que sirve para crear la comanda, editando sus datos predefinidos por los nuevos introducidos por el usuario
             */
        } else if (v.getId() == R.id.botonCrearComanda) {
            listaVerificación = new ArrayList<>();
            existeProductoComandaEnNegocio(idComanda, listaVerificación, resultado -> {
                if ("hay producto en pedido".equals(resultado)) {
                    mesa =  Integer.parseInt(campoNumeroMesas.getText().toString());
                    espacio = espacioSpinner.getSelectedItem().toString();
                    String keyEncontrada = null;
                    // Deja de buscar los espacios cuando encuentre la clave del espacio del negocio
                    for (Map.Entry<String, String> entrada : espaciosMap.entrySet()) {
                        if (entrada.getValue().equals(espacio)) {
                            keyEncontrada = entrada.getKey();
                            break;
                        }
                    }

                    //Si el espacio elegido, contiene menos mesas que la mesa elegida para la comanda se notificara y no se seguira el proceso de fitrado
                    DatabaseReference databaseReferenceCreacion = FirebaseDatabase.getInstance("https://negocios-de-cocinas-default-rtdb.europe-west1.firebasedatabase.app/").getReference();
                    DatabaseReference espacioReferencia = databaseReferenceCreacion.child("espacio");

                    espacioReferencia.child(keyEncontrada).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                Espacio espacioEncontrado = snapshot.getValue(Espacio.class);
                                if (espacioEncontrado != null) {
                                    int mesaMaxima = espacioEncontrado.getnMesas();
                                    if (mesa <= mesaMaxima && mesa > 0) {
                                        //En el caso de pasar el proceso de filtrado se actualizara la comanda, cambiando el espacio, el nº de la mesa y poniendo un subtotal acorde a los productos contenidos
                                        DatabaseReference databaseReferenceCreacion = FirebaseDatabase.getInstance("https://negocios-de-cocinas-default-rtdb.europe-west1.firebasedatabase.app/").getReference();
                                        DatabaseReference comandasReferencia = databaseReferenceCreacion.child("comandas");

                                        comandasReferencia.child(idComanda).addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                if (snapshot.exists()) {
                                                    Comanda comandaExistente = snapshot.getValue(Comanda.class);

                                                    double subtotalGlobal = 0.0;
                                                    for (ProductoComanda producto : listaProductosComandaEntrantes) {
                                                        subtotalGlobal += producto.getCantidad() * producto.getPrecioUnidad();
                                                    }

                                                    comandaExistente.setEspacio(espacio);
                                                    comandaExistente.setNumeroMesa(mesa);
                                                    comandaExistente.setSubtotal(subtotalGlobal);

                                                    //Se verifica y actualiza la comanda actual
                                                    comandasReferencia.child(idComanda).setValue(comandaExistente).addOnSuccessListener(aVoid -> {
                                                        Snackbar.make(findViewById(android.R.id.content), "Comanda actualizado", Snackbar.LENGTH_SHORT).show();
                                                        Intent botonIrCrearProductos = new Intent(CrearComandas.this, GestionarComandas.class);
                                                        negocioSeleccionado.remove("idComanda");
                                                        botonIrCrearProductos.putExtras(negocioSeleccionado);
                                                        startActivity(botonIrCrearProductos);
                                                    }).addOnFailureListener(e -> {
                                                        Snackbar.make(findViewById(android.R.id.content), "Error al actualizar el producto", Snackbar.LENGTH_SHORT).show();
                                                    });
                                                } else {
                                                    Snackbar.make(findViewById(android.R.id.content), "No se encontró el producto", Snackbar.LENGTH_SHORT).show();
                                                }
                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError error) {
                                                Snackbar.make(findViewById(android.R.id.content), "Error de conexión con la base de datos", Snackbar.LENGTH_SHORT).show();
                                            }
                                        });
                                    } else {
                                        Snackbar.make(v, "Las mesas tienen que ser del 1 al "+mesaMaxima, Snackbar.LENGTH_SHORT).show();
                                    }
                                }
                            }
                        } @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Snackbar.make(findViewById(android.R.id.content), "Error de conexión con la base de datos", Snackbar.LENGTH_SHORT).show();
                        }
                    });
                } else if ("no hay producto en pedido".equals(resultado)) {
                    Snackbar.make(v, "Añade minimo un producto a la comanda para poderla crear", Snackbar.LENGTH_SHORT).show();
                } else {
                    Log.e("Resultado", "Error al verificar comandas");
                }
            });
            /*
             * Se da lógica al boton que sirve para añadir productos a la comanda, abriendo un dialogo con la lista de los productos del negocio y pudiendolos filtrar con el boton de categorias
             */
        } else if (v.getId() == R.id.BotonParaAñadirProductos) {
            Dialog dialogoMenuParaAñadirProductos = new Dialog(this);
            dialogoMenuParaAñadirProductos.setContentView(R.layout.dialogo_anadir_producto_comanda);
            dialogoMenuParaAñadirProductos.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialogoMenuParaAñadirProductos.setCanceledOnTouchOutside(false);
            dialogoMenuParaAñadirProductos.setCancelable(false);

            //Se cargan los productos del negocio en el que estemos actualmente
            listaProductos = dialogoMenuParaAñadirProductos.findViewById(R.id.listaProductos);
            adaptadorProductos = new AdaptadorListaProductosDialogo(this, listaProductosEntrantes, correoNegocio, idComanda);
            listaProductos.setAdapter(adaptadorProductos);
            obtenerProductosPorNegocio(correoNegocio);

            TextView textoMenu = dialogoMenuParaAñadirProductos.findViewById(R.id.textoMenu);

            Button botonCategorias = dialogoMenuParaAñadirProductos.findViewById(R.id.botonCategorias);
            Button botonCerrarProductosDisponibles = dialogoMenuParaAñadirProductos.findViewById(R.id.botonCerrarProductosDisponibles);

            /*
             * Se da lógica al boton que sirve para filtrar por categorias los productos del negocio
             */
            botonCategorias.setOnClickListener(categoriaView  -> {
                BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
                View bottomSheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_categorias, null);
                bottomSheetDialog.setContentView(bottomSheetView);

                listaCategorias = bottomSheetView.findViewById(R.id.listaCategorias);

                //Se cargan las categorias del negocio en el que estemos actualmente
                miAdaptador = new AdaptadorListaCategorias(this, listaCategoriasEntrantes, rolUsuario);
                listaCategorias.setAdapter(miAdaptador);
                obtenerCategoriasDesdeFirebase(correoNegocio);

                //Se cargan los productos del negocio en el que estemos actualmente, filtrado por la categoria selecionada actualmente
                listaCategorias.setOnItemClickListener((adapterView, view, position, id) -> {
                    Categoria categoriaSeleccionada = listaCategoriasEntrantes.get(position);
                    textoMenu.setText(categoriaSeleccionada.getNombreCategoria());
                    obtenerProductosPorNegocioCategoria(correoNegocio, categoriaSeleccionada.getNombreCategoria());
                    bottomSheetDialog.dismiss();
                });

                Button botonCrearCategoria = bottomSheetView.findViewById(R.id.botonCrearCategoria);
                bottomSheetDialog.show();
            });

            /*
             * Se da lógica al boton que sirve para cerrar el dialogo que muestra los productso del negocio apra ser añadidos a la comanda
             */
            botonCerrarProductosDisponibles.setOnClickListener(view -> {
                dialogoMenuParaAñadirProductos.dismiss();
                obtenerProductosComandaPorNegocio(idComanda);
            });

            dialogoMenuParaAñadirProductos.show();
        }
    }

    /**
     * Método que sirve para obtener los espacios de firebase y mostrarlos en la comanda. Tambien los mete en un Map con el id y el nombre del espacio, para poder arriba completar
     * la logica para obtener el numero de mesas que contiene el espacio.
     */
    private void obtenerEspaciosDesdeFirebase() {
        //Se buscan los espacios de la base de datos y se filtra por medio del correo del negocio actual
        DatabaseReference databaseReferenceCreacion = FirebaseDatabase.getInstance("https://negocios-de-cocinas-default-rtdb.europe-west1.firebasedatabase.app/").getReference();
        DatabaseReference negocioReferencia = databaseReferenceCreacion.child("espacio");

        negocioReferencia.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                listaEspaciosEntrantes.clear();

                for (DataSnapshot espaciosSnapshot : dataSnapshot.getChildren()) {
                    Espacio espacio = espaciosSnapshot.getValue(Espacio.class);
                    if (espacio != null) {
                        String claveEspacio = espacio.getId();
                        String nombre = espacio.getNombre();
                        String negocio = espacio.getNegocio();
                        if (correoNegocio.equals(negocio)) {
                            // Solo agrega el espacio si el correo coincide con el del mismo
                            espaciosMap.put(claveEspacio, nombre);
                            espacios.add(nombre);
                        }
                    }
                }

                //Se calcula el numero de espacios que contiene el negocio y se introduce los espacios dentro
                String[] espacio = new String[espacios.size()];
                for (int i = 0; i < espacios.size(); i++) {
                    espacio[i] = espacios.get(i);
                }

                //Se cargan los espacios conseguidos previamente en el spinner de espacios de la comanda
                ArrayAdapter<String> adaptadorEspacios2 = new ArrayAdapter<>(CrearComandas.this, R.layout.spinner_item, espacios);
                adaptadorEspacios2.setDropDownViewResource(R.layout.spinner_dropdown_item);
                espacioSpinner.setAdapter(adaptadorEspacios2);

                adaptadorEspacios.notifyDataSetChanged();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    /**
     * Método que sirve para obtener las categorias contenidas en el negocio actual para el dialogo que contiene los productos para añadirse a la comanda
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
                    if (producto != null && nombreNegocio.equals(producto.getNegocio()) && categoriaSeleccionada.equals(producto.getCategoria())) {
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


    private void obtenerProductosPorNegocio(String nombreNegocio) {
        DatabaseReference databaseReferenceProductos = FirebaseDatabase.getInstance().getReference().child("productos");

        //Se buscan todos los productos del negocio en el que estemos actualmente y se cargan en la lista
        databaseReferenceProductos.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                listaProductosEntrantes.clear();
                for (DataSnapshot productoSnapshot : dataSnapshot.getChildren()) {
                    Producto producto = productoSnapshot.getValue(Producto.class);
                    if (producto != null && nombreNegocio.equals(producto.getNegocio())) {
                        //Agrega solo los productos que pertenecen al negocio actual
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
     * Método que sirve para obtener los productos de la comanda en la que estemos actualmnete y hayan sido añadidos
     *
     * @param idComanda ID de la comanda en la que estemos actualmente para cargar los productosComanda asociados a la comanda.
     */
    private void obtenerProductosComandaPorNegocio(String idComanda) {
        DatabaseReference databaseReferenceProductos = FirebaseDatabase.getInstance().getReference().child("productosComanda");

        //Se buscan todos los productosComanda de la comanda en la que estemos actualmente y se añaden al listado
        databaseReferenceProductos.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                listaProductosComandaEntrantes.clear();
                for (DataSnapshot productoSnapshot : dataSnapshot.getChildren()) {
                    ProductoComanda productoComanda = productoSnapshot.getValue(ProductoComanda.class);
                    if (productoComanda != null && idComanda.equals(productoComanda.getPedidoAsociado())) {
                        listaProductosComandaEntrantes.add(productoComanda);
                    }
                }
                adaptadorProductosComanda.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    /**
     * Método que sirve para verificar y cargar los nuevos productosComanda que hayan sido añadidos.
     *
     * @param pedidoAsociado ID de la comanda en la que estemos actualmente para cargar los productosComanda asociados a la comanda.
     * @param productoComandaEncontradas Lista de productosComanda para poder llenar y luego meter en el listado correspondiente.
     * @param callback Llamad a la interfaz para mostrar los productosComanda correctamente.
     */
    public void existeProductoComandaEnNegocio(String pedidoAsociado, ArrayList<ProductoComanda> productoComandaEncontradas, CrearComandas.ProductoComandaCallback callback) {
        DatabaseReference databaseReferenceProductoComanda = FirebaseDatabase.getInstance().getReference().child("productosComanda");

        //Se buscan todos los productosComanda de la comanda en la que estemos actualmente y se añaden al listado
        databaseReferenceProductoComanda.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                //Limpia la lista antes de agregar nuevos elementos
                productoComandaEncontradas.clear();
                boolean cierto = false;

                for (DataSnapshot productoComandaSnapshot : dataSnapshot.getChildren()) {
                    ProductoComanda productoComanda = productoComandaSnapshot.getValue(ProductoComanda.class);
                    if (productoComanda != null && pedidoAsociado.equals(productoComanda.getPedidoAsociado())) {
                        //Agrega los productos encontrados a la lista proporcionada
                        productoComandaEncontradas.add(productoComanda);
                        cierto = true;
                        //El bucle se para cuando se encuentra una coincidencia
                        break;
                    }
                }

                //Dar un resultado al callback
                if (cierto) {
                    callback.onResult("hay producto en pedido");
                } else {
                    callback.onResult("no hay producto en pedido");
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