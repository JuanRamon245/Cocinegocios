package com.example.cocinegocios;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.example.cocinegocios.AdaptadoresCarruseles.AdaptadorLoggin;
import com.example.cocinegocios.Clases.Usuarios;
import com.example.cocinegocios.Clases.UsuariosSQLite;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

/**
 * Clase con de poder gestionar el inicio como contenedor del carrusel
 * <p>
 * Esta clase contiene la lógica necesaria para que los usuarios nada más encender la aplicación puedan iniciar sesion, registrarse y si ya se registraron previamnete y no se desloguearon
 * poder iniciar sesión sin rellenar campos
 * <p>
 * Autor: [Juan Ramón de León Martín]
 * Fecha: [3/12/2024]
 */

public class MainActivity extends AppCompatActivity {

    private String contrasenaUsuario, correoUsuario;
    private ViewPager2 viewPager;
    private View indicator;

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
     * Esta clase coge y asocia los elementos de la vista, los inicializa, y luego los asocia con el layout correspondiente. Tambien se encarga de contener los fragmentos 'IniciarSesion' y
     * 'Registrarse' mediante el adaptador 'AdaptadorLoggin'. Por ultimo tambien se encarga de crear la BBDD de SQLite e iniciar sesión si te iniciaste sesión previamente y no cerraste la sesión.
     *
     * @param savedInstanceState Bundle que contiene la estancia del fragmento, en caso de que esté siendo recreado.
     *
     * @return view La vista ya inicializada y asociados sus elementos.
     */
    @SuppressLint("Range")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Crear la BBDD de SQLite si no esta creada
        UsuariosSQLite baseDatosUsuarios = new UsuariosSQLite(this, "bbddUsuarios", null, 1);
        baseDatos = baseDatosUsuarios.getWritableDatabase();

        //Verificar si la tabla 'usuario' ya existe
        String consultaTabla = "SELECT name FROM sqlite_master WHERE type='table' AND name='usuario'";
        Cursor cursorTabla = baseDatos.rawQuery(consultaTabla, null);

        if (!cursorTabla.moveToFirst()) {
            //Si no existe, crea la tabla
            String sqlCreacion = "CREATE TABLE usuario(correo TEXT PRIMARY KEY, contrasena TEXT, rol TEXT)";
            baseDatos.execSQL(sqlCreacion);
        }

        //Cerrar el cursor después de usarlo
        cursorTabla.close();

        String consultaUsuario = "SELECT correo, contrasena FROM usuario LIMIT 1";
        Cursor cursor = baseDatos.rawQuery(consultaUsuario, null);

        if (cursor.moveToFirst()) {
            correoUsuario = cursor.getString(cursor.getColumnIndex("correo"));
            contrasenaUsuario = cursor.getString(cursor.getColumnIndex("contrasena"));
        }

        //Logica para poder inciar sesión nada más iniciar la aplicación si ya iniciaste sesión previamente y no te habias deslogueado
        if (correoUsuario != null && contrasenaUsuario != null) {
            DatabaseReference referenciaUsuarios = FirebaseDatabase.getInstance().getReference("Usuarios");
            Query busquedaUsuarios = referenciaUsuarios.orderByChild("gmail").equalTo(correoUsuario);

            //En el caso de encontrar un usario que exista en la base de datos, iniciar sesión directamente
            busquedaUsuarios.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                            Usuarios usuario = userSnapshot.getValue(Usuarios.class);
                            if (usuario.getContraseña().equals(contrasenaUsuario)) {
                                Intent botonParaIniciarSesion = new Intent(MainActivity.this, SeleccionDeNegocios.class);
                                startActivity(botonParaIniciarSesion);
                            }
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                }
            });
        }

        //Cargamos el adaptador encargado de los fragmentos 'Registrarse' y 'IniciarSesion'
        viewPager = findViewById(R.id.ViewPager);
        indicator = findViewById(R.id.indicator);
        AdaptadorLoggin adapter = new AdaptadorLoggin(this);
        viewPager.setAdapter(adapter);

        //Revisamos en que fragmento estamos actualmente
        findViewById(R.id.textViewLogin).setOnClickListener(v -> {
            viewPager.setCurrentItem(0);
            updateIndicator(0);
        });

        findViewById(R.id.textViewRegister).setOnClickListener(v -> {
            viewPager.setCurrentItem(1);
            updateIndicator(1);
        });

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateIndicator(position);
            }
        });
    }

    /**
     * Metodo para dar funcionalidad donde estamos del carrusel
     *
     * Esta clase coge da lógica a la posicion en la que estemos del carrusel apra mostrarlo en un indicador, arriba de la actividad
     *
     * @param position Posición en la que estemos actualmente en el carruse
     */
    private void updateIndicator(int position) {
        float targetX;
        if (position == 0) {
            // Posición para "Inicio de sesión"
            targetX = findViewById(R.id.textViewLogin).getX() - findViewById(R.id.textViewRegister).getX();
        } else {
            //Posición para "Registrarse"
            targetX = findViewById(R.id.textViewRegister).getX() - findViewById(R.id.textViewLogin).getX();
        }

        //Animar la barra de indicador
        ObjectAnimator animator = ObjectAnimator.ofFloat(indicator, "translationX", targetX);
        //Duración de la animación
        animator.setDuration(300);
        //Interpolador para suavizar la animación
        animator.setInterpolator(new DecelerateInterpolator());
        animator.start();

        //Cambiar colores de texto
        if (position == 0) {
            ((TextView) findViewById(R.id.textViewLogin)).setTextColor(getResources().getColor(R.color.AzulClaro));
            ((TextView) findViewById(R.id.textViewRegister)).setTextColor(getResources().getColor(R.color.BlancoGrisaceo));
        } else {
            ((TextView) findViewById(R.id.textViewRegister)).setTextColor(getResources().getColor(R.color.AzulClaro));
            ((TextView) findViewById(R.id.textViewLogin)).setTextColor(getResources().getColor(R.color.BlancoGrisaceo));
        }
    }
}