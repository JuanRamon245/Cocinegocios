package com.example.cocinegocios;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.bumptech.glide.Glide;
import com.example.cocinegocios.Adaptadores.AdaptadorListaCategorias;
import com.example.cocinegocios.Clases.Categoria;
import com.example.cocinegocios.Clases.Producto;
import com.example.cocinegocios.Clases.UsuariosSQLite;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;

/**
 * Clase con de poder editar los productos
 * <p>
 * Esta clase contiene la lógica necesaria para que los administradores puedan editar productos al negocio, donde se ejecuta tambien la logica de poder coger una
 * fotografia de la galeria del dispositivo y asociarla al producto correspondiente, en el caso de querer cambiar la imagen
 * <p>
 * Autor: [Juan Ramón de León Martín]
 * Fecha: [3/12/2024]
 */

public class EditarProductos extends AppCompatActivity implements View.OnClickListener {

    private static final int PERMISSION_REQ_CODE = 100;
    private static final int PICK_IMAGE = 1000;

    private EditText campoDescripcionProducto, campoPasosASeguir, campoPrecioProducto, nombreProductoEditable;
    private Spinner campoCategoria;
    private Button botonEditarProducto, botonVolverCatalogo, botonAñadirImagenProducto;
    private ImageView imageView;

    private String correoNegocio, idProducto, categoriaProducto, nombre, negocio, categoria, fotoCodificada, descripcion, pasosSeguir, precioSinCambiar, enlaceImagen, rolUsuario;
    private Double precio;

    private ArrayList<Categoria> listaCategoriasEntrantes = new ArrayList<>();
    private ArrayList<String> categorias = new ArrayList<>();
    private ArrayAdapter<Categoria> miAdaptador;

    private StorageReference mStorage;
    private Uri imageUri;

    private Bundle negocioSeleccionado;

    private boolean activo = false;

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
     * Esta clase coge y asocia los elementos de la vista, los inicializa, y luego los asocia con el layout correspondiente. Tambien settea los botónes
     * con el setOnClickLitsener implementado de la clase. Tambien hace uso del adaptador 'AdaptadorListaCategorias' para poder cargar las categorias del negocio, haciendose uso del metodo
     * 'obtenerCategoriasDesdeFirebase()'.
     *
     * @param savedInstanceState Bundle que contiene la estancia del fragmento, en caso de que esté siendo recreado.
     *
     * @return view La vista ya inicializada y asociados sus elementos.
     */
    @SuppressLint("Range")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editar_productos);

        botonEditarProducto = findViewById(R.id.botonEditarProducto);
        botonEditarProducto.setOnClickListener(this);

        botonVolverCatalogo = findViewById(R.id.BotonRegresarCatalogo);
        botonVolverCatalogo.setOnClickListener(this);

        botonAñadirImagenProducto = findViewById(R.id.botonAñadirImagenProducto);
        botonAñadirImagenProducto.setOnClickListener(this);

        negocioSeleccionado = getIntent().getExtras();
        correoNegocio = negocioSeleccionado.getString("negocioLoggued");
        idProducto = negocioSeleccionado.getString("productoSeleccionado");

        nombreProductoEditable = findViewById(R.id.nombreProductoEditable);
        campoDescripcionProducto = findViewById(R.id.descripcionProducto);
        campoPasosASeguir = findViewById(R.id.pasosASeguir);
        campoPrecioProducto = findViewById(R.id.precioProducto);
        imageView = findViewById(R.id.imagenProducto);

        //Buscamos el rol del usuario actual, para en el caso de necesitar gestionar algo dependiendo de su rol, poder hacerlo
        UsuariosSQLite baseDatosUsuarios = new UsuariosSQLite(this, "bbddUsuarios", null, 1);
        baseDatos = baseDatosUsuarios.getWritableDatabase();

        String consultaUsuario = "SELECT correo, rol, contrasena FROM usuario LIMIT 1";
        Cursor cursor = baseDatos.rawQuery(consultaUsuario, null);

        if (cursor.moveToFirst()) {
            rolUsuario = cursor.getString(cursor.getColumnIndex("rol"));
        }

        //Se cargan las categorias del negocio actual
        campoCategoria = findViewById(R.id.categoriaSpinner);
        miAdaptador = new AdaptadorListaCategorias(this, listaCategoriasEntrantes, rolUsuario);

        DatabaseReference databaseReferenceCreacion = FirebaseDatabase.getInstance("https://negocios-de-cocinas-default-rtdb.europe-west1.firebasedatabase.app/").getReference();
        DatabaseReference productosReferencia = databaseReferenceCreacion.child("productos");

        //Se buscan todos los datos del producto asociado que recibimos para editar
        productosReferencia.child(idProducto).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Producto producto = snapshot.getValue(Producto.class);
                    if (producto != null) {
                        nombreProductoEditable.setText(producto.getNombre());
                        nombreProductoEditable.setEnabled(false);
                        campoDescripcionProducto.setText(producto.getDescripcion());
                        campoPasosASeguir.setText(producto.getPasosSeguir());
                        DecimalFormat formato = new DecimalFormat("0.00");
                        campoPrecioProducto.setText(formato.format(producto.getPrecio()).replace(",","."));

                        categoriaProducto = producto.getCategoria();
                        obtenerCategoriasDesdeFirebase();

                        enlaceImagen = producto.getFotoCodificada();
                        String direcciónReal = enlaceImagen.replace("gs://negocios-de-cocinas.appspot.com/", "");
                        StorageReference imageRef = mStorage.child(direcciónReal);

                        //Limpiar la imagen anterior mientras se carga la nueva
                        imageView.setImageDrawable(null);

                        //Cargar la imagen usando Glide
                        imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            imageUri = uri;
                            Glide.with(EditarProductos.this)
                                    .load(uri)
                                    .into(imageView);
                        }).addOnFailureListener(exception -> {
                            Log.e("STORAGE", "Error al obtener la URL de la imagen", exception);
                        });
                    }
                } else {
                    Snackbar.make(findViewById(android.R.id.content), "No existe ningun producto con ese ID", Snackbar.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Snackbar.make(findViewById(android.R.id.content), "Error de conexión con la base de datos", Snackbar.LENGTH_SHORT).show();
            }
        });

        FirebaseApp.initializeApp(EditarProductos.this);
        mStorage = FirebaseStorage.getInstance("gs://negocios-de-cocinas.appspot.com").getReference();

    }

    /**
     * Metodo para dar funcionalidad a los SetOnClickLitseners aasociados arriba
     * <p>
     * Esta clase coge da lógica a los botones con el SetOnClickLitseners. En este caso podemos editar el producto cumpliendo todos los requisitos de la creacion de productos. Tambien si queremos
     * cambiar la foto del producto, tenemos la posibilidad de añadir una foto para asociarla al producto mediante metodos donde se comprueba si el usuario ha dado permiso para que la aplicación
     * acceda al almacenamiento del dispositivo, luego coger la imagen y guardarla en un uri, que es la direccion de la imagen, como la URL de una web, y tras eso ponerla en el ImageView. Por
     * último está el botón para regresar al catalogo.
     *
     * @param v La vista actual de donde estamos trabajando.
     */
    @Override
    public void onClick(View v) {
        /*
         * Se da lógica al boton que sirve para crear el producto si se cumplen con los requisitos descritos abajo y los filtros
         */
        if (v.getId() == R.id.botonEditarProducto) {
            nombre = nombreProductoEditable.getText().toString();
            descripcion = campoDescripcionProducto.getText().toString();
            pasosSeguir = campoPasosASeguir.getText().toString();
            precioSinCambiar = campoPrecioProducto.getText().toString();

            //Se comprueba que todos los campos no esten vacios
            if (nombre.isEmpty() || descripcion.isEmpty() || pasosSeguir.isEmpty() || precioSinCambiar.isEmpty()) {
                Snackbar.make(v, "Rellena todos los campos", Snackbar.LENGTH_SHORT).show();
            }

            //Se comprueba que el usario ha seleccionado una imagen para el tema de asociarlo al producto
            if(imageUri != null) {
                //Se asocia la imagen y transforma en un String de donde se va a contener en Firebase Storage
                String fileName = getFileNameFromUri(imageUri);
                StorageReference reference = mStorage.child("ProductosGuardados/" + fileName);

                DatabaseReference databaseReferenceCreacion = FirebaseDatabase.getInstance("https://negocios-de-cocinas-default-rtdb.europe-west1.firebasedatabase.app/").getReference();
                DatabaseReference productosReferencia = databaseReferenceCreacion.child("productos");

                negocio = correoNegocio;
                categoria = campoCategoria.getSelectedItem().toString();
                fotoCodificada = reference.toString();

                //Se cambia el precio de String a Double
                try {
                    precio = Double.valueOf(precioSinCambiar);

                    //Se cambia el producto a la base de datos, si ya existe uno con el mismo ID
                    productosReferencia.child(idProducto).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                Snackbar.make(findViewById(android.R.id.content), "El producto '" + nombre + "' existe en la base de datos", Snackbar.LENGTH_SHORT).show();
                                Producto productoExistente = snapshot.getValue(Producto.class);

                                //Se consigue los campos para actualizar el producto
                                productoExistente.setNombre(nombre);
                                productoExistente.setDescripcion(descripcion);
                                productoExistente.setPasosSeguir(pasosSeguir);
                                if (activo) {
                                    productoExistente.setFotoCodificada(fotoCodificada);
                                } else {
                                    productoExistente.setFotoCodificada(enlaceImagen);
                                }
                                productoExistente.setCategoria(categoria);
                                productoExistente.setPrecio(precio);


                                //Se sube la imagen a firebase y actualiza la del producto
                                if (imageUri != null) {
                                    reference.getMetadata().addOnSuccessListener(metadata -> {
                                        // La imagen ya existe
                                        Snackbar.make(findViewById(android.R.id.content), "La imagen ya existe en el almacenamiento", Snackbar.LENGTH_SHORT).show();
                                    }).addOnFailureListener(exception -> {
                                        // La imagen no existe, proceder a subirla
                                        reference.putFile(imageUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                            @Override
                                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                                Snackbar.make(findViewById(android.R.id.content), "Imagen subida correctamente", Snackbar.LENGTH_SHORT).show();
                                            }
                                        }).addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                Snackbar.make(findViewById(android.R.id.content), "Imagen no se subio correctamente", Snackbar.LENGTH_SHORT).show();
                                            }
                                        });
                                    });
                                }

                                //Se actualiza el producto existente
                                productosReferencia.child(idProducto).setValue(productoExistente).addOnSuccessListener(aVoid -> {
                                    Snackbar.make(findViewById(android.R.id.content), "Producto actualizado", Snackbar.LENGTH_SHORT).show();
                                    //Regresar al menú de administrador
                                    Intent botonRegresarAdmin = new Intent(EditarProductos.this, MenuAdministrador.class);
                                    negocioSeleccionado.putString("productoSeleccionado", null);
                                    botonRegresarAdmin.putExtras(negocioSeleccionado);
                                    startActivity(botonRegresarAdmin);
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
                } catch (NumberFormatException e) {
                    Snackbar.make(v, "Introduce un número válido para el precio", Snackbar.LENGTH_SHORT).show();
                    Log.d("FALTA", "EL DOUBLE: "+precioSinCambiar);
                }
            }
        /*
         * Se da lógica al boton que sirve para regresar a la pantalla donde estan todos los productos
         */
        } else if (v.getId() == R.id.BotonRegresarCatalogo) {
            Intent botonregresarAdmin = new Intent(EditarProductos.this, MenuAdministrador.class);
            negocioSeleccionado.putString("productoSeleccionado", null);
            botonregresarAdmin.putExtras(negocioSeleccionado);
            startActivity(botonregresarAdmin);
        /*
         * Se da lógica al boton que sirve para añadir una imagen al producto
         */
        } else if (v.getId() == R.id.botonAñadirImagenProducto) {
            requestRuntimePermission();
        }
    }

    /**
     * Método que sirve para comprobar que la aplicación tiene los permisos necesarios para poder leer el almacenamiento del dispositivo y poder escoger una imagen más adelante.
     * En el caso de no tener permisos, esto es solicitado por medio de un AlertDialog.
     */
    private void requestRuntimePermission() {
        String permission;
        //Segun si android es una version nueva o antigua manejamos un tipo de permiso o otro
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permission = android.Manifest.permission.READ_MEDIA_IMAGES;
        } else {
            permission = Manifest.permission.READ_EXTERNAL_STORAGE;
        }
        //Se acepto anteriormente el permiso para acceder a la galeria
        if (checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED) {
            Snackbar.make(findViewById(android.R.id.content), "Permiso aceptado para acceder a la galería", Snackbar.LENGTH_SHORT).show();
            abrirGaleria();
        } else if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
            //No hay permisos para acceder a la galeria, se solicitan con un alertDialog
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Esta aplicación necesita permisos de almacenamiento para seguir con la siguiente acción")
                    .setTitle("Permiso requerido")
                    .setCancelable(false)
                    .setPositiveButton("ok", (dialog, which) -> {
                        ActivityCompat.requestPermissions(EditarProductos.this, new String[]{permission}, PERMISSION_REQ_CODE);
                        dialog.dismiss();
                    })
                    .setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss());
            builder.show();
        } else {
            //En el caso de que os permisos solicitados se rechazen
            Snackbar.make(findViewById(android.R.id.content), "Permiso denegado para acceder a la galería, solicitando permisos", Snackbar.LENGTH_SHORT).show();
            ActivityCompat.requestPermissions(this, new String[]{permission}, PERMISSION_REQ_CODE);
        }
    }

    /**
     * Método que sirve para verificar la solicitud de permisos en la aplicacion y en el caso contrario avisar que pasará al rechazarlo
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQ_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //Se acepta  el permiso para acceder a la galeria
                Snackbar.make(findViewById(android.R.id.content), "Permiso aceptado para acceder a la galería", Snackbar.LENGTH_SHORT).show();
                abrirGaleria();
            } else if (!ActivityCompat.shouldShowRequestPermissionRationale(this, permissions[0])) {
                //Se rechaza el permiso para acceder a la galeria
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("No podrá acceder al uso de imágenes de galería.")
                        .setTitle("Permiso requerido")
                        .setCancelable(false)
                        .setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss())
                        .setPositiveButton("Ajustes", (dialog, which) -> {
                            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            Uri uri = Uri.fromParts("package", getPackageName(), null);
                            intent.setData(uri);
                            startActivity(intent);
                            dialog.dismiss();
                        });
                builder.show();
            } else {
                requestRuntimePermission();
            }
        }
    }

    /**
     * Método para abrir la galería
     */
    private void abrirGaleria() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE);
    }

    /**
     * Método para recibir el resultado de la galería y mostrar la imagen en el imageView de la actividad. A su vez guardamos el uri de la imagen en el necesario para poder crear el producto.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //Aqui recogenmos la imagen sellecionada por el usuario
        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();

            imageView = findViewById(R.id.imagenProducto);
            imageView.setImageURI(imageUri);
            activo = true;
        }
    }

    /**
     * Método para recibir la direccion de la imagen por medio del uri guardado previamente.
     */
    private String getFileNameFromUri(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    result = cursor.getString(nameIndex);
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        } else if (uri.getScheme().equals("file")) {
            result = new File(uri.getPath()).getName();
        }
        return result;
    }

    /**
     * Método que sirve para obtener las categorias de firebase y mostrarlas en el spinner de categorias, dependiendo del negocio en el que estamos
     */
    private void obtenerCategoriasDesdeFirebase() {
        DatabaseReference databaseReferenceCreacion = FirebaseDatabase.getInstance("https://negocios-de-cocinas-default-rtdb.europe-west1.firebasedatabase.app/").getReference();
        DatabaseReference negocioReferencia = databaseReferenceCreacion.child("categorias");

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
                        if (correoNegocio.equals(gmail)) {
                            // Solo agrega la categoría si el correo coincide
                            categorias.add(nombre);
                        }
                    }
                }

                String[] categoria = new String[categorias.size()];
                for (int i = 0; i < categorias.size(); i++) {
                    categoria[i] = categorias.get(i);
                }

                ArrayAdapter<String> adaptadorCategorias = new ArrayAdapter<>(EditarProductos.this, R.layout.spinner_item, categorias);
                adaptadorCategorias.setDropDownViewResource(R.layout.spinner_dropdown_item);
                campoCategoria.setAdapter(adaptadorCategorias);

                int position = categorias.indexOf(categoriaProducto);
                if (position != -1) {
                    campoCategoria.setSelection(position);
                }

                miAdaptador.notifyDataSetChanged();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

}