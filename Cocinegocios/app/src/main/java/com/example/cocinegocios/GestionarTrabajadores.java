package com.example.cocinegocios;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.example.cocinegocios.AdaptadoresCarruseles.AdaptadorGestionTrabajadores;
import com.example.cocinegocios.Clases.UsuariosSQLite;
import com.example.cocinegocios.Fragmentos.TrabajadoresDelNegocio;
import com.google.android.material.navigation.NavigationView;

/**
 * Clase con de poder gestionar los trabajadores como contenedor del carrusel
 * <p>
 * Esta clase contiene la lógica necesaria para que los administradores puedan ver las solicitudes del negocio y los trabajadores del mismo por medio de un carrusel
 * <p>
 * Autor: [Juan Ramón de León Martín]
 * Fecha: [3/12/2024]
 */

public class GestionarTrabajadores extends AppCompatActivity implements View.OnClickListener {

    private ViewPager2 viewPager;
    private View indicator;

    private Bundle negocioSeleccionado;

    private String correoUsuario, rolUsuario, correoNegocio;

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
     * Esta clase coge y asocia los elementos de la vista, los inicializa, y luego los asocia con el layout correspondiente. Tambien se encarga de contener los fragmentos 'SolicitudesDelNegocio' u
     * 'TrabajadoresDelNegocio' mediante el adaptador 'AdaptadorGestionTrabajadores'. Por ultimo se encarga de manejar el menú desplegable por el cual nosotros nos podremos mover entre las
     * distintas actividades dependiendo de nuestro rol.
     *
     * @param savedInstanceState Bundle que contiene la estancia del fragmento, en caso de que esté siendo recreado.
     *
     * @return view La vista ya inicializada y asociados sus elementos.
     */
    @SuppressLint("Range")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gestionar_trabajadores);

        //Conseguimos el negocio actual por medio del bundle
        negocioSeleccionado = getIntent().getExtras();
        correoNegocio = negocioSeleccionado.getString("negocioLoggued");

        //Cargamos el adaptador encargado de los fragmentos 'SolicitudesDelNegocio' y 'TrabajadoresDelNegocio'
        viewPager = findViewById(R.id.ViewPager);
        indicator = findViewById(R.id.indicator);
        AdaptadorGestionTrabajadores adapter = new AdaptadorGestionTrabajadores(this, correoNegocio);
        viewPager.setAdapter(adapter);

        //Revisamos en que fragmento estamos actualmente
        findViewById(R.id.textViewTrabajadores).setOnClickListener(v -> {
            viewPager.setCurrentItem(0);
            updateIndicator(0);
        });

        findViewById(R.id.textViewSolicitudes).setOnClickListener(v -> {
            viewPager.setCurrentItem(1);
            updateIndicator(1);
        });

        //Lógica para hacer que si estamos en el fragmento1 y volvemos al fragmento2 recargamos el listado
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateIndicator(position);
                if (position == 0) {
                    TrabajadoresDelNegocio fragmentTrabajadores = (TrabajadoresDelNegocio) getSupportFragmentManager()
                            .findFragmentByTag("f" + viewPager.getCurrentItem());
                    if (fragmentTrabajadores != null) {
                        fragmentTrabajadores.actualizarListaUsuariosTrabajadores();
                    }
                }
            }
        });

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
            Intent irMenu = new Intent(GestionarTrabajadores.this, MenuAdministrador.class);
            irMenu.putExtras(negocioSeleccionado);
            startActivity(irMenu);
        } else if (item.getItemId() == R.id.CrearEspacios) {
            Intent irEspacios = new Intent(GestionarTrabajadores.this, GestionarEspacios.class);
            irEspacios.putExtras(negocioSeleccionado);
            startActivity(irEspacios);
        } else if (item.getItemId() == R.id.GestionarTrabajadores) {
            Intent irEspacios = new Intent(GestionarTrabajadores.this, GestionarTrabajadores.class);
            irEspacios.putExtras(negocioSeleccionado);
            startActivity(irEspacios);
        } else if (item.getItemId() == R.id.GestionarComandas) {
            Intent irEspacios = new Intent(GestionarTrabajadores.this, GestionarComandas.class);
            irEspacios.putExtras(negocioSeleccionado);
            startActivity(irEspacios);
        } else if (item.getItemId() == R.id.GestionarNegocio) {
            Intent irNegocio = new Intent(GestionarTrabajadores.this, GestionarNegocioUsuario.class);
            irNegocio.putExtras(negocioSeleccionado);
            startActivity(irNegocio);
        } else if (item.getItemId() == R.id.CerrarSesion) {
            Intent irEspacios = new Intent(GestionarTrabajadores.this, SeleccionDeNegocios.class);

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
     * Esta clase coge da lógica a los botones con el SetOnClickLitseners. En este caso podemos tras acceder a esta pagina, dirigirnos al perfil del usuario actual.
     *
     * @param v La vista actual de donde estamos trabajando.
     */
    @Override
    public void onClick(View v) {
        /*
         * Se da lógica al boton que sirve para dirigirnos al perfil del usuario al que estamos logueado actualmente.
         */
        if (v.getId() == R.id.botonParaIrAlPerfil) {
            Intent botonPerfil = new Intent(GestionarTrabajadores.this, PerfilUsuarioActual.class);
            botonPerfil.putExtras(negocioSeleccionado);
            startActivity(botonPerfil);
        }
    }

    /**
     * Metodo para dar funcionalidad donde estamos del carrusel
     * <p>
     * Esta clase coge da lógica a la posicion en la que estemos del carrusel apra mostrarlo en un indicador, arriba de la actividad
     *
     * @param position Posición en la que estemos actualmente en el carruse
     */
    private void updateIndicator(int position) {
        float targetX;
        if (position == 0) {
            //Posición para "Solicitudes"
            targetX = findViewById(R.id.textViewTrabajadores).getX() - findViewById(R.id.textViewSolicitudes).getX();
        } else {
            //Posición para "Trabajadores"
            targetX = findViewById(R.id.textViewSolicitudes).getX() - findViewById(R.id.textViewTrabajadores).getX();
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
            ((TextView) findViewById(R.id.textViewTrabajadores)).setTextColor(getResources().getColor(R.color.AzulClaro));
            ((TextView) findViewById(R.id.textViewSolicitudes)).setTextColor(getResources().getColor(R.color.BlancoGrisaceo));
        } else {
            ((TextView) findViewById(R.id.textViewSolicitudes)).setTextColor(getResources().getColor(R.color.AzulClaro));
            ((TextView) findViewById(R.id.textViewTrabajadores)).setTextColor(getResources().getColor(R.color.BlancoGrisaceo));
        }
    }
}