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
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.cocinegocios.Clases.Negocios;
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

/**
 * Clase con de poder crear los negocios
 * <p>
 * Esta clase contiene la lógica necesaria para que los usuarios puedan crear su propio negocio, donde se ejecuta tambien la logica de poder coger una
 * fotografia de la galeria del dispositivo y asociarla al producto correspondiente.
 * <p>
 * Autor: [Juan Ramón de León Martín]
 * Fecha: [3/12/2024]
 */

public class RegistroDeNegocios extends AppCompatActivity implements View.OnClickListener {

    private static final int PERMISSION_REQ_CODE = 100;
    private static final int PICK_IMAGE = 1000;

    private EditText campoNombre;
    private TextView campoGmail;
    private Spinner campoLocalidad;
    private ImageView campoImagen;
    private Button botonRegresarNegocio, botonAñadirImagen, botonRegistrarNegocio;

    private String nombre, gmail, localidad, imagenCodificada;

    private StorageReference mStorage;
    private Uri imageUri;

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
     * con el setOnClickLitsener implementado de la clase.
     *
     * @param savedInstanceState Bundle que contiene la estancia del fragmento, en caso de que esté siendo recreado.
     *
     * @return view La vista ya inicializada y asociados sus elementos.
     */
    @SuppressLint("Range")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registro_de_negocios);

        botonRegresarNegocio = findViewById(R.id.BotonRegresarNegocios);
        botonRegresarNegocio.setOnClickListener(this);

        botonAñadirImagen = findViewById(R.id.botonAñadirImagenLocal);
        botonAñadirImagen.setOnClickListener(this);

        botonRegistrarNegocio = findViewById(R.id.botonRegistrarNegocio);
        botonRegistrarNegocio.setOnClickListener(this);

        campoNombre = findViewById(R.id.nombreNegocio);
        campoGmail = findViewById(R.id.gmailNegocio);
        campoLocalidad = findViewById(R.id.spinnerLocalidadNegocio);
        String[] provincias = {
                "Álava", "Albacete", "Alicante", "Almería", "Ávila", "Badajoz", "Baleares", "Barcelona",
                "Burgos", "Cáceres", "Cádiz", "Castellón", "Ciudad Real", "Córdoba", "La Coruña",
                "Cuenca", "Gerona", "Granada", "Guadalajara", "Guipúzcoa", "Huelva", "Huesca", "Jaén",
                "León", "Lérida", "Lugo", "Madrid", "Málaga", "Murcia", "Navarra", "Orense", "Asturias",
                "Palencia", "Las Palmas", "Pontevedra", "La Rioja", "Salamanca", "Santa Cruz de Tenerife",
                "Cantabria", "Segovia", "Sevilla", "Soria", "Tarragona", "Teruel", "Toledo", "Valencia",
                "Valladolid", "Vizcaya", "Zamora", "Zaragoza"
        };

        //Adaptador para que el spinner tengo contenido dentro
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_item, provincias);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        campoLocalidad.setAdapter(adapter);
        campoLocalidad.setSelection(1);

        //Buscamos el rol del usuario actual, para en el caso de necesitar gestionar algo dependiendo de su rol, poder hacerlo
        UsuariosSQLite baseDatosUsuarios = new UsuariosSQLite(this, "bbddUsuarios", null, 1);
        baseDatos = baseDatosUsuarios.getWritableDatabase();

        String consultaUsuario = "SELECT correo, contrasena FROM usuario LIMIT 1";
        Cursor cursor = baseDatos.rawQuery(consultaUsuario, null);

        String SinCambiargmail = "";

        if (cursor.moveToFirst()) {
            SinCambiargmail = cursor.getString(cursor.getColumnIndex("correo"));
        }

        if (SinCambiargmail.isEmpty()){
            SinCambiargmail = "correoNoRecibido";
        }

        gmail = SinCambiargmail.replace("_", ".");
        campoGmail.setText(gmail);
        campoGmail.setEnabled(false);

        campoImagen = findViewById(R.id.imagenLocalRegistro);

        //Se carga el repositorio o direccion donde se vayan a guardar las imagenes en el Storage de Firebase
        FirebaseApp.initializeApp(RegistroDeNegocios.this);
        mStorage = FirebaseStorage.getInstance("gs://negocios-de-cocinas.appspot.com").getReference();
    }

    /**
     * Metodo para dar funcionalidad a los SetOnClickLitseners aasociados arriba
     *
     * Esta clase coge da lógica a los botones con el SetOnClickLitseners. En este caso podemos crear el negocio asociado a nuestro usuario, tambien elegir una imagen de nuestro almacenamiento
     * para asociar el negocio y por ultimo volver a la pantalla para ver el resto de negocios.
     *
     * @param v La vista actual de donde estamos trabajando.
     */
    @Override
    public void onClick(View v) {
        /*
         * Se da lógica al boton que sirve para volver a la pagina de seleccion de negocios
         */
        if (v.getId() == R.id.BotonRegresarNegocios) {
            Intent botonRegresarNegocio = new Intent(RegistroDeNegocios.this, SeleccionDeNegocios.class);
            startActivity(botonRegresarNegocio);
        /*
         * Se da lógica al boton que sirve para añadir una imagen al negocio
         */
        } else if (v.getId() == R.id.botonAñadirImagenLocal) {
            requestRuntimePermission();
        /*
         * Se da lógica al boton que sirve para registrar un negocio en la base de datos
         */
        } else if (v.getId() == R.id.botonRegistrarNegocio) {
            nombre = campoNombre.getText().toString();
            localidad = campoLocalidad.getSelectedItem().toString();
            //Comprobar si estan todos los campos rellenos
            if (nombre.isEmpty() || gmail.isEmpty() || localidad.isEmpty()) {
                Snackbar.make(v, "Rellena todos los campos", Snackbar.LENGTH_SHORT).show();
            } else {
                //Los filtros para saber si toda la información esta bien metida
                    if(gmail.endsWith("@gmail.com")) {
                        if (campoImagen.getDrawable() != null) {
                            //Se comprueba que el usario ha seleccionado una imagen para el tema de asociarlo al negocio
                            if(imageUri != null){
                                //Se asocia la imagen y transforma en un String de donde se va a contener en Firebase Storage
                                String fileName = getFileNameFromUri(imageUri);
                                StorageReference reference = mStorage.child("ImagenesLocales/"+ fileName);
                                DatabaseReference databaseReferenceCreacion = FirebaseDatabase.getInstance("https://negocios-de-cocinas-default-rtdb.europe-west1.firebasedatabase.app/").getReference();
                                DatabaseReference negocioReferencia = databaseReferenceCreacion.child("negocios");
                                DatabaseReference usuarioAdministradorNegocioReferencia = databaseReferenceCreacion.child("usuariosAdministradores");

                                imagenCodificada = reference.toString();

                                String primaryKeyCorreo = gmail.replace(".", "_");

                                //Se añade el negocio a la base de datos
                                negocioReferencia.child(primaryKeyCorreo).addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        //En caso de ya existir uno
                                        if (snapshot.exists()) {
                                            Snackbar.make(findViewById(android.R.id.content), "El gmail '" + gmail + "' existe en la base de datos", Snackbar.LENGTH_SHORT).show();
                                        //En caso de no ser asi
                                        } else {
                                            Negocios nuevoNegocio = new Negocios(nombre, primaryKeyCorreo, localidad, imagenCodificada);
                                            negocioReferencia.child(primaryKeyCorreo).setValue(nuevoNegocio).addOnSuccessListener(aVoid -> {
                                                        //Mostrar mensaje de éxito
                                                        Snackbar.make(findViewById(android.R.id.content), "Usuario registrado", Snackbar.LENGTH_SHORT).show();

                                                        //Se comprueba si la imagen existe ya (cosa casi imposible)
                                                        reference.getMetadata().addOnSuccessListener(metadata -> {
                                                            // La imagen ya existe
                                                            Snackbar.make(findViewById(android.R.id.content), "La imagen ya existe en el almacenamiento", Snackbar.LENGTH_SHORT).show();
                                                        }).addOnFailureListener(exception -> {
                                                            //La imagen no existe, proceder a subirla
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

                                                    })
                                                    .addOnFailureListener(e -> {
                                                        //Mostrar mensaje de error
                                                        Snackbar.make(findViewById(android.R.id.content), "Usuario sin registrar", Snackbar.LENGTH_SHORT).show();
                                                    });
                                            Snackbar.make(findViewById(android.R.id.content), "Negocio registrado con éxito", Snackbar.LENGTH_SHORT).show();

                                            Intent botonRegresarNegocio = new Intent(RegistroDeNegocios.this, SeleccionDeNegocios.class);
                                            startActivity(botonRegresarNegocio);
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        Snackbar.make(findViewById(android.R.id.content), "Error de conexión con la base de datos", Snackbar.LENGTH_SHORT).show();
                                    }
                                });
                            } else {
                                Snackbar.make(v, "El uri de la imagen esta vacio", Snackbar.LENGTH_SHORT).show();
                            }
                        } else {
                            //No hay imagen en el ImageView
                            Snackbar.make(v, "El ImageView no contiene ninguna imagen", Snackbar.LENGTH_SHORT).show();
                        }
                    } else {
                        Snackbar.make(v, "El gmail debe terminar en 'gmail.com'", Snackbar.LENGTH_SHORT).show();
                    }
            }
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
                        ActivityCompat.requestPermissions(RegistroDeNegocios.this, new String[]{permission}, PERMISSION_REQ_CODE);
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
        //Aqui recogenmos la imagen selecionada por el usuario
        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            ImageView imageView = findViewById(R.id.imagenLocalRegistro);
            //Mostrar la imagen seleccionada en el ImageView
            imageView.setImageURI(imageUri);
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
}