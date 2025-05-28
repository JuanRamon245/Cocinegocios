package com.example.cocinegocios;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.bumptech.glide.Glide;
import com.example.cocinegocios.Clases.Negocios;
import com.example.cocinegocios.Clases.UsuariosSQLite;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.navigation.NavigationView;
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

/**
 * Clase con de poder gestionar el negocio del administrador
 * <p>
 * Esta clase contiene la lógica necesaria para que los administradores puedan ver los su propio negocio y puedan editarlo si es necesario cambiar algún campo.
 * <p>
 * Autor: [Juan Ramón de León Martín]
 * Fecha: [3/12/2024]
 */

public class GestionarNegocioUsuario extends AppCompatActivity implements View.OnClickListener {

    private static final int PERMISSION_REQ_CODE = 100;
    private static final int PICK_IMAGE = 1000;

    private ImageView imagenNegocio;
    private TextView gmailNegocio, nombreNegocio;
    private Spinner spinnerLocalidadNegocio;
    private Button botonCambiarImagenNegocio, botonActualizarNegocio;

    private String gmail, nombre, localidad, URLImagenNegocioActual, URLImagenNegocioNueva;

    private boolean activo = false;

    private StorageReference mStorage;
    private Uri imageUri;

    private Bundle negocioSeleccionado;

    private String rolUsuario, correoUsuario, correoNegocio;

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
     * con el setOnClickLitsener implementado de la clase. Tambien nos carga el negocio donde estemos actualmente y rellena sus datos paar que sea más facil la edición.
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
        setContentView(R.layout.activity_gestionar_negocio_usuario);

        botonCambiarImagenNegocio = findViewById(R.id.botonCambiarImagenNegocio);
        botonCambiarImagenNegocio.setOnClickListener(this);

        botonActualizarNegocio = findViewById(R.id.botonActualizarNegocio);
        botonActualizarNegocio.setOnClickListener(this);

        //Conseguimos el negocio actual por medio del bundle
        negocioSeleccionado = getIntent().getExtras();
        correoNegocio = negocioSeleccionado.getString("negocioLoggued");

        //Buscamos el rol y el correo del usuario actual, para en el caso de necesitar gestionar algo dependiendo de su rol, poder hacerlo
        UsuariosSQLite baseDatosUsuarios = new UsuariosSQLite(this, "bbddUsuarios", null, 1);
        baseDatos = baseDatosUsuarios.getWritableDatabase();

        String consultaUsuario = "SELECT correo, rol, contrasena FROM usuario LIMIT 1";
        Cursor cursor = baseDatos.rawQuery(consultaUsuario, null);

        if (cursor.moveToFirst()) {
            correoUsuario = cursor.getString(cursor.getColumnIndex("correo"));
            rolUsuario = cursor.getString(cursor.getColumnIndex("rol"));
        }

        gmailNegocio = findViewById(R.id.correoNegocio);
        nombreNegocio = findViewById(R.id.nombreNegocio);

        imagenNegocio =  findViewById(R.id.imagenNegocio);

        spinnerLocalidadNegocio = findViewById(R.id.spinnerLocalidadNegocio);
        String[] provincias = {
                "Álava", "Albacete", "Alicante", "Almería", "Ávila", "Badajoz", "Baleares", "Barcelona",
                "Burgos", "Cáceres", "Cádiz", "Castellón", "Ciudad Real", "Córdoba", "La Coruña",
                "Cuenca", "Gerona", "Granada", "Guadalajara", "Guipúzcoa", "Huelva", "Huesca", "Jaén",
                "León", "Lérida", "Lugo", "Madrid", "Málaga", "Murcia", "Navarra", "Orense", "Asturias",
                "Palencia", "Las Palmas", "Pontevedra", "La Rioja", "Salamanca", "Santa Cruz de Tenerife",
                "Cantabria", "Segovia", "Sevilla", "Soria", "Tarragona", "Teruel", "Toledo", "Valencia",
                "Valladolid", "Vizcaya", "Zamora", "Zaragoza"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_item, provincias);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinnerLocalidadNegocio.setAdapter(adapter);

        DatabaseReference databaseReferenceCreacion = FirebaseDatabase.getInstance("https://negocios-de-cocinas-default-rtdb.europe-west1.firebasedatabase.app/").getReference();
        DatabaseReference negociosReferencia = databaseReferenceCreacion.child("negocios");

        //buscamos un negocio que coincida con el correo del usuario, porque los negocios están asociados con el correo del usuario que lo crea y aqui somos administradores.
        negociosReferencia.child(correoUsuario).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Negocios negocios = snapshot.getValue(Negocios.class);
                    if (negocios != null) {
                        //En el caso de encontrarlo podemos rellenar todos los campos con sus datos.
                        gmailNegocio.setText(negocios.getGmail().replace("_","."));
                        gmailNegocio.setEnabled(false);
                        nombreNegocio.setText(negocios.getNombre());
                        localidad = negocios.getLocalidad();
                        int posicion = buscarProvincia(provincias, localidad);
                        spinnerLocalidadNegocio.setSelection(posicion);
                        URLImagenNegocioActual = negocios.getImagenCodificada();
                        String direcciónReal = URLImagenNegocioActual.replace("gs://negocios-de-cocinas.appspot.com/", "");
                        StorageReference imageRef = mStorage.child(direcciónReal);

                        imagenNegocio.setImageDrawable(null);

                        // Cargar la imagen usando Glide
                        imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            imageUri = uri;
                            Glide.with(GestionarNegocioUsuario.this)
                                    .load(uri)
                                    .into(imagenNegocio);
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

        //Se carga el repositorio o direccion donde se vayan a guardar las imagenes en el Storage de Firebase
        FirebaseApp.initializeApp(GestionarNegocioUsuario.this);
        mStorage = FirebaseStorage.getInstance("gs://negocios-de-cocinas.appspot.com").getReference();

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
            Intent irMenu = new Intent(GestionarNegocioUsuario.this, MenuAdministrador.class);
            irMenu.putExtras(negocioSeleccionado);
            startActivity(irMenu);
        } else if (item.getItemId() == R.id.CrearEspacios) {
            Intent irEspacios = new Intent(GestionarNegocioUsuario.this, GestionarEspacios.class);
            irEspacios.putExtras(negocioSeleccionado);
            startActivity(irEspacios);
        } else if (item.getItemId() == R.id.GestionarTrabajadores) {
            Intent irEspacios = new Intent(GestionarNegocioUsuario.this, GestionarTrabajadores.class);
            irEspacios.putExtras(negocioSeleccionado);
            startActivity(irEspacios);
        } else if (item.getItemId() == R.id.GestionarComandas) {
            Intent irEspacios = new Intent(GestionarNegocioUsuario.this, GestionarComandas.class);
            irEspacios.putExtras(negocioSeleccionado);
            startActivity(irEspacios);
        } else if (item.getItemId() == R.id.GestionarNegocio) {
            Intent irNegocio = new Intent(GestionarNegocioUsuario.this, GestionarNegocioUsuario.class);
            irNegocio.putExtras(negocioSeleccionado);
            startActivity(irNegocio);
        } else if (item.getItemId() == R.id.CerrarSesion) {
            Intent irEspacios = new Intent(GestionarNegocioUsuario.this, SeleccionDeNegocios.class);

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
     * Esta clase coge da lógica a los botones con el SetOnClickLitseners. En este caso podemos tras acceder a esta pagina, tenemos la opcion a cambiar la imagen del negocio y poder cambiar
     * todos sus campos a excepcion del correo. Tambien sirve para manejar la logica de poder dirigirnos al perfil del usuario actual.
     *
     * @param v La vista actual de donde estamos trabajando.
     */
    @Override
    public void onClick(View v) {
        /*
         * Se da lógica al boton que sirve para añadir una nueva imagen al negocio
         */
        if (v.getId() == R.id.botonCambiarImagenNegocio) {
            requestRuntimePermission();
        /*
         * Se da lógica al boton que sirve para actualizar el negocio actual con los campos cambiados
         */
        } else if (v.getId() == R.id.botonActualizarNegocio) {
            nombre = nombreNegocio.getText().toString();
            localidad = spinnerLocalidadNegocio.getSelectedItem().toString();

            //Se comprueba que no todos los campos estén vacios
            if (nombre.isEmpty()) {
                Snackbar.make(v, "Rellena todos los campos", Snackbar.LENGTH_SHORT).show();
            }

            //Se comprueba que haya una imagen minimo en el uri para poder asociarlo al negocio
            if (imageUri != null) {
                //Se asocia la imagen y transforma en un String de donde se va a contener en Firebase Storage
                String fileName = getFileNameFromUri(imageUri);
                StorageReference reference = mStorage.child("ImagenesLocales/"+ fileName);

                DatabaseReference databaseReferenceCreacion = FirebaseDatabase.getInstance("https://negocios-de-cocinas-default-rtdb.europe-west1.firebasedatabase.app/").getReference();
                DatabaseReference negocioReferencia = databaseReferenceCreacion.child("negocios");

                URLImagenNegocioNueva = reference.toString();

                //Se busca el negocio en la base de datos para cambiar sus atributos
                negocioReferencia.child(correoNegocio).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            Snackbar.make(findViewById(android.R.id.content), "El negocio '" + nombre + "' existe en la base de datos", Snackbar.LENGTH_SHORT).show();
                            Negocios negocioExistente = snapshot.getValue(Negocios.class);

                            negocioExistente.setNombre(nombre);
                            negocioExistente.setLocalidad(localidad);

                            //Esto es una logica para coger la uri de la imagen si no se ha cambiado o si se ha cambiado coger la nueva uri
                            if (activo) {
                                negocioExistente.setImagenCodificada(URLImagenNegocioNueva);
                            } else {
                                negocioExistente.setImagenCodificada(URLImagenNegocioActual);
                            }

                            //Metodo para subir la imagen o no subirla en el caso de existir
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

                            //Tras lo anterior se pone a actualizar los datos del negocio encontrado con los nuevos valores
                            negocioReferencia.child(correoNegocio).setValue(negocioExistente).addOnSuccessListener(aVoid -> {
                                Snackbar.make(findViewById(android.R.id.content), "Negocio actualizado", Snackbar.LENGTH_SHORT).show();
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
            }
        /*
         * Se da lógica al boton que sirve para dirigirnos al perfil del usuario al que estamos logueado actualmente.
         */
        } else if (v.getId() == R.id.botonParaIrAlPerfil) {
            Intent botonPerfil = new Intent(GestionarNegocioUsuario.this, PerfilUsuarioActual.class);
            botonPerfil.putExtras(negocioSeleccionado);
            startActivity(botonPerfil);
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
                        ActivityCompat.requestPermissions(GestionarNegocioUsuario.this, new String[]{permission}, PERMISSION_REQ_CODE);
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

            imagenNegocio = findViewById(R.id.imagenNegocio);
            imagenNegocio.setImageURI(imageUri);
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
     * Método que sirve para obtener los espacios de firebase y mostrarlos en la actividad.
     *
     * @param provincias Correo del negocio en el que estemos trabajando actualmente.
     * @param nombre Correo del negocio en el que estemos trabajando actualmente.
     */
    public static int buscarProvincia(String[] provincias, String nombre) {
        for (int i = 0; i < provincias.length; i++) {
            if (provincias[i].equalsIgnoreCase(nombre)) {
                return i;
            }
        }
        //Si no se encuentra la provincia
        return -1;
    }
}