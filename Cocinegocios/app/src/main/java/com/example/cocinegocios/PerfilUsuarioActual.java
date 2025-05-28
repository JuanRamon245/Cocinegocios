package com.example.cocinegocios;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.cocinegocios.Clases.Usuarios;
import com.example.cocinegocios.Clases.UsuariosSQLite;
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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * Clase con de poder visualizar el perfil actual y actualizarlo si es necesario
 * <p>
 * Esta clase contiene la lógica necesaria para que los usuarios puedan ver todos los datos de su propio perfil y si hay algo incorrecto o algo que actualizar, poder cambiarlo si es necesario.
 * <p>
 * Autor: [Juan Ramón de León Martín]
 * Fecha: [3/12/2024]
 */

public class PerfilUsuarioActual extends AppCompatActivity implements View.OnClickListener {

    private ImageView imagenUsuario;
    private TextView gmailUsuario, nombreUsuario, apellidosUsuario, DNIUsuario, telefonoUsuario, nacimientoUsuario, contraseñaUsuario;
    private Button botonActualizarUsuario, botonFecha;

    private String correo, nombre, apellidos, DNI, telefono, nacimiento, contraseña, URLImagenPerfil;

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
     * con el setOnClickLitsener implementado de la clase. Tambien nos carga el usuario con el que estemos logueados actualmente y rellena sus datos para que sea más facil la edición.
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
        setContentView(R.layout.activity_perfil_usuario_actual);

        botonFecha = findViewById(R.id.botonFecha);
        botonFecha.setOnClickListener(this);

        botonActualizarUsuario = findViewById(R.id.botonActualizarUsuario);
        botonActualizarUsuario.setOnClickListener(this);

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

        gmailUsuario = findViewById(R.id.correoUsuario);
        nombreUsuario = findViewById(R.id.nombreUsuario);
        apellidosUsuario = findViewById(R.id.apellidosUsuario);
        DNIUsuario = findViewById(R.id.DNIUsuario);
        telefonoUsuario = findViewById(R.id.telefonoUsuario);
        nacimientoUsuario = findViewById(R.id.nacimientoUsuario);
        contraseñaUsuario = findViewById(R.id.contraseñaUsuario);

        imagenUsuario =  findViewById(R.id.imagenUsuario);

        //Buscamos un usuario en la base de datos que coincida con el almacenado en SQLite para cargar sus datos en los campos
        DatabaseReference databaseReferenceCreacion = FirebaseDatabase.getInstance("https://negocios-de-cocinas-default-rtdb.europe-west1.firebasedatabase.app/").getReference();
        DatabaseReference usuariosReferencia = databaseReferenceCreacion.child("Usuarios");

        usuariosReferencia.child(correoUsuario).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Usuarios usuarios = snapshot.getValue(Usuarios.class);
                    if (usuarios != null) {
                        //En el caso de encontrarlo podemos rellenar todos los campos con sus datos.
                        gmailUsuario.setText(usuarios.getGmail().replace("_","."));
                        gmailUsuario.setEnabled(false);
                        nombreUsuario.setText(usuarios.getNombre());
                        apellidosUsuario.setText(usuarios.getApellidos());
                        DNIUsuario.setText(usuarios.getDNI());
                        telefonoUsuario.setText(String.valueOf(usuarios.getTelefono()));
                        nacimientoUsuario.setText(usuarios.getFecha());
                        contraseñaUsuario.setText(usuarios.getContraseña());
                        URLImagenPerfil = usuarios.getFotoPerfil();
                        String direcciónReal = URLImagenPerfil.replace("gs://negocios-de-cocinas.appspot.com/", "");
                        StorageReference imageRef = mStorage.child(direcciónReal);

                        imagenUsuario.setImageDrawable(null);

                        //Cargar la imagen usando Glide
                        imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            imageUri = uri;
                            Glide.with(PerfilUsuarioActual.this)
                                    .load(uri)
                                    .into(imagenUsuario);
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
        FirebaseApp.initializeApp(PerfilUsuarioActual.this);
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
            Intent irMenu = new Intent(PerfilUsuarioActual.this, MenuAdministrador.class);
            irMenu.putExtras(negocioSeleccionado);
            startActivity(irMenu);
        } else if (item.getItemId() == R.id.CrearEspacios) {
            Intent irEspacios = new Intent(PerfilUsuarioActual.this, GestionarEspacios.class);
            irEspacios.putExtras(negocioSeleccionado);
            startActivity(irEspacios);
        } else if (item.getItemId() == R.id.GestionarTrabajadores) {
            Intent irEspacios = new Intent(PerfilUsuarioActual.this, GestionarTrabajadores.class);
            irEspacios.putExtras(negocioSeleccionado);
            startActivity(irEspacios);
        } else if (item.getItemId() == R.id.GestionarComandas) {
            Intent irEspacios = new Intent(PerfilUsuarioActual.this, GestionarComandas.class);
            irEspacios.putExtras(negocioSeleccionado);
            startActivity(irEspacios);
        } else if (item.getItemId() == R.id.GestionarNegocio) {
            Intent irNegocio = new Intent(PerfilUsuarioActual.this, GestionarNegocioUsuario.class);
            irNegocio.putExtras(negocioSeleccionado);
            startActivity(irNegocio);
        } else if (item.getItemId() == R.id.CerrarSesion) {
            Intent irEspacios = new Intent(PerfilUsuarioActual.this, SeleccionDeNegocios.class);

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
     *
     * Esta clase coge da lógica a los botones con el SetOnClickLitseners. En este caso podemos tras acceder a esta pagina, podemos cambiar los datos de nuestro uusario,
     * y tambien la fecha nuestra de nacimiento como podiamos en el registro de usuarios. Tambien sirve para manejar la logica de poder dirigirnos al perfil del usuario actual,
     * aunque es poco util si ya estamos en él.
     *
     * @param v La vista actual de donde estamos trabajando.
     */
    @Override
    public void onClick(View v) {
        /*
         * Se da lógica al boton que sirve para actualizar el usuario actual con los campos cambiados
         */
        if (v.getId() == R.id.botonActualizarUsuario) {
            nombre = nombreUsuario.getText().toString();
            apellidos = apellidosUsuario.getText().toString();
            DNI = DNIUsuario.getText().toString();
            telefono = telefonoUsuario.getText().toString();
            nacimiento = nacimientoUsuario.getText().toString();
            contraseña = contraseñaUsuario.getText().toString();

            DatabaseReference databaseReferenceCreacion = FirebaseDatabase.getInstance("https://negocios-de-cocinas-default-rtdb.europe-west1.firebasedatabase.app/").getReference();
            DatabaseReference usuariosReferencia = databaseReferenceCreacion.child("Usuarios");

            //Se comprueba que no todos los campos estén vacios y se filtran si estan correctos
            if (nombre.isEmpty() || apellidos.isEmpty() || DNI.isEmpty() || telefono.isEmpty() || nacimiento.isEmpty() || contraseña.isEmpty()) {
                Snackbar.make(v, "Rellena todos los campos", Snackbar.LENGTH_SHORT).show();
            } else {
                if (DNI.matches("\\d{8}[A-Za-z]")) {
                    //Se busca el usuario en la base de datos para cambiar sus atributos
                    usuariosReferencia.child(correoUsuario).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            Log.d("PRUEBA", "LLEGAMOS A LA PARTE 3");
                            if (snapshot.exists()) {
                                Usuarios usuarioExistente = snapshot.getValue(Usuarios.class);

                                usuarioExistente.setNombre(nombre);
                                usuarioExistente.setApellidos(apellidos);
                                usuarioExistente.setDNI(DNI);
                                usuarioExistente.setTelefono(Integer.parseInt(telefono));
                                usuarioExistente.setFecha(nacimiento);
                                usuarioExistente.setContraseña(contraseña);

                                //Tras haber recogido los datos nuevos dedl usuario se pone a actualizar los datos del mismo si es encontrado, con los nuevos valores
                                usuariosReferencia.child(correoUsuario).setValue(usuarioExistente).addOnSuccessListener(aVoid -> {
                                    Snackbar.make(findViewById(android.R.id.content), "Usuario actualizado", Snackbar.LENGTH_SHORT).show();

                                    //tambien necesitamos actualizar los datos del usuario en SQLite a la vez de en Firebase
                                    String actualizacionUsuario = "UPDATE usuario SET contrasena = ? WHERE correo = ?;";
                                    SQLiteStatement statement = baseDatos.compileStatement(actualizacionUsuario);
                                    statement.bindString(1, contraseña);
                                    statement.bindString(2, correoUsuario);
                                    statement.execute();
                                }).addOnFailureListener(e -> {
                                    Snackbar.make(findViewById(android.R.id.content), "Error al actualizar el usuario", Snackbar.LENGTH_SHORT).show();
                                });
                            } else {
                                Snackbar.make(findViewById(android.R.id.content), "No se encontró el usuario", Snackbar.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Snackbar.make(findViewById(android.R.id.content), "Error de conexión con la base de datos", Snackbar.LENGTH_SHORT).show();
                        }
                    });
                }
            }

        /*
         * Se da lógica al boton que sirve para asignarse la fecha de nacimiento del usuario abriendo un calendario con la fecha actual y oder elegir asi su fecha
         */
        } else if (v.getId() == R.id.botonFecha) {
            //Se crea y genera el calendario
            Calendar calendario = Calendar.getInstance();
            int anio = calendario.get(Calendar.YEAR);
            int mes = calendario.get(Calendar.MONTH);
            int dia = calendario.get(Calendar.DAY_OF_MONTH);

            //Se da uso con un datePickerDialog para poder elegir la fecha y formatearla a como queremos
            DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                    new DatePickerDialog.OnDateSetListener() {
                        @Override
                        public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                            Calendar fechaSeleccionada = Calendar.getInstance();
                            fechaSeleccionada.set(Calendar.YEAR, year);
                            fechaSeleccionada.set(Calendar.MONTH, monthOfYear);
                            fechaSeleccionada.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                            SimpleDateFormat formato = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                            String fechaFormateada = formato.format(fechaSeleccionada.getTime());

                            nacimientoUsuario.setText(fechaFormateada);
                        }
                    }, anio, mes, dia);
            datePickerDialog.show();
        /*
         * Se da lógica al boton que sirve para dirigirnos al perfil del usuario al que estamos logueado actualmente.
         */
        }  else if (v.getId() == R.id.botonParaIrAlPerfil) {
            Intent botonPerfil = new Intent(PerfilUsuarioActual.this, PerfilUsuarioActual.class);
            botonPerfil.putExtras(negocioSeleccionado);
            startActivity(botonPerfil);
        }
    }
}