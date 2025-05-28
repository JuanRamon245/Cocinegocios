package com.example.cocinegocios.Fragmentos;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.cocinegocios.Clases.Usuarios;
import com.example.cocinegocios.Clases.UsuariosSQLite;
import com.example.cocinegocios.R;
import com.example.cocinegocios.SeleccionDeNegocios;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * Clase con funcion de fragmento para que el usuario pueda registrarse en la aplicación
 * <p>
 * Esta clase o fragmento contiene la lógica necesaria para que el usuario pueda registrar en la aplicación y en el caso de que no se deslogue, podrá continuar sin tener que iniciar sesión
 * usando su cuenta.
 * <p>
 * Autor: [Juan Ramón de León Martín]
 * Fecha: [3/12/2024]
 */
public class Registrarse extends Fragment implements View.OnClickListener{

    private Button botonDeRegistro;
    private Button botonFecha;
    private EditText campoNombreRegistro, campoApellidosRegistro, campoDNIRegistro, campoGmailRegistro, campoTelefonoRegistro, campoNacimientoRegistro, ContraseñaRegistro, repetirContraseñaRegistro;

    private String nombre, apellidos, gmail, DNI, telefonoString, fecha, contraseña, repetirContraseña, primaryKey;
    private int telefono;

    SQLiteDatabase baseDatos;

    /**
     * Metodo para crear la vista del fragmento
     * <p>
     * Esta clase coge y asocia los elementos de la vista, los inicializa, y luego los asocia con el layout correspondiente. Tambien settea los botónes
     * con el setOnClickLitsener implementado de la clase. Por ultimo carga la BBDD de SQLite en caso de necesitarla para guardar los datos del usuario con el que se registra.
     *
     * @param inflater Objeto que permite crear la vista usando el Layout asociado al fragmento.
     * @param container Vista padre que contiene los elementos del fragmento
     * @param savedInstanceState Bundle que contiene la estancia del fragmento, en caso de que esté siendo recreado.
     *
     * @return view La vista ya inicializada y asociados sus elementos.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_registrarse, container, false);

        botonDeRegistro = view.findViewById(R.id.botonDeRegistroDeSesion);
        botonDeRegistro.setOnClickListener(this);

        botonFecha = view.findViewById(R.id.botonFecha);
        botonFecha.setOnClickListener(this);

        campoNombreRegistro = view.findViewById(R.id.campoNombreRegistro);
        campoApellidosRegistro = view.findViewById(R.id.campoApellidosRegistro);
        campoDNIRegistro = view.findViewById(R.id.campoDNIRegistro);
        campoGmailRegistro = view.findViewById(R.id.campoGmailRegistro);
        campoTelefonoRegistro = view.findViewById(R.id.campoTelefonoRegistro);
        campoNacimientoRegistro = view.findViewById(R.id.campoNacimientoRegistro);
        ContraseñaRegistro =  (EditText) view.findViewById(R.id.ContraseñaRegistro);
        repetirContraseñaRegistro =  (EditText) view.findViewById(R.id.repetirContraseñaRegistro);

        UsuariosSQLite baseDatosUsuarios = new UsuariosSQLite(getContext(), "bbddUsuarios", null, 1);
        baseDatos = baseDatosUsuarios.getWritableDatabase();

        return view;
    }

    /**
     * Metodo para dar funcionalidad a los SetOnClickLitseners aasociados arriba
     * <p>
     * Esta clase coge da lógica a los botones con el SetOnClickLitseners. En este caso comprueba que el usuario introducido no esté en la BBDD, comprobando si su correo existe. En el caso
     * de ser verdadero, se crea un usuario con sus datos, dandole hasta una foto de perfil predeterminada y se guardan sus datos en SQLite para poder iniciar sesión proximamente sin tener
     * que introductir los datos. En el caso contrario, se le notificará al usuario cual ha sido su error en el intento de registrarse en la aplicación.
     *
     *
     * @param v La vista actual de donde estamos trabajando.
     */
    @Override
    public void onClick(View v) {
        /*
         * Se da lógica al boton que sirve para registrarse, y en el caso de ser asi, guardar los datos en SQLite para futuros incios de sesión poder no inicar sesión de nuevo hasta que te deslogue
         */
        if (v.getId() == R.id.botonDeRegistroDeSesion) {
            nombre = campoNombreRegistro.getText().toString().trim();
            apellidos = campoApellidosRegistro.getText().toString().trim();
            DNI = campoDNIRegistro.getText().toString().trim();
            gmail = campoGmailRegistro.getText().toString().trim();
            telefonoString = campoTelefonoRegistro.getText().toString().trim();
            fecha = campoNacimientoRegistro.getText().toString().trim();
            contraseña = ContraseñaRegistro.getText().toString();
            repetirContraseña = repetirContraseñaRegistro.getText().toString();

            //Se comprueba que los campos no estén vacios
            if (!nombre.isEmpty() && !apellidos.isEmpty() && !DNI.isEmpty() && !gmail.isEmpty() && !telefonoString.isEmpty() && !fecha.isEmpty() && !contraseña.isEmpty() && !repetirContraseña.isEmpty()) {
                String fotoPerfil = "gs://negocios-de-cocinas.appspot.com/fotosDePerfil/3135768.png";

                telefono = Integer.parseInt(telefonoString);

                //Se reemplaza los puntos por '_' porque firebase no es capaz de interpretar algunos simbolos, entonces el más comun es sustituido
                primaryKey = gmail.replace(".", "_");
                Usuarios usuarioNuevo = new Usuarios(nombre, apellidos, fotoPerfil, primaryKey, DNI, fecha, telefono, "sin", "sin", contraseña);
                DatabaseReference referenciaCreacionUsuarios = FirebaseDatabase.getInstance().getReference();
                DatabaseReference crearUsuario = referenciaCreacionUsuarios.child("Usuarios");

                //Se busca que se exista el usuario introducido por medio de su gmail, en caso de encontrarlo, notificara el error, pero si no se encuentra
                //pasa los filtros para verificar que los datos son correctos
                DatabaseReference referenciaUsuarios = FirebaseDatabase.getInstance().getReference("Usuarios");
                Query busquedaUsuarios = referenciaUsuarios.orderByChild("gmail").equalTo(primaryKey);

                busquedaUsuarios.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            Snackbar.make(v, "El correo '"+gmail + "' existe en la base de datos", Snackbar.LENGTH_SHORT).show();
                        } else {
                            if (gmail.endsWith("@gmail.com")) {
                                if (DNI.matches("\\d{8}[A-Za-z]")) {
                                    if (contraseña.equals(repetirContraseña)) {
                                        //En caso de pasar todos los filtros se inserta el usuario en SQLite y firebase, y se inicia sesió
                                        crearUsuario.child(primaryKey).setValue(usuarioNuevo);
                                        Intent botonParaRegistrarse = new Intent(getActivity(), SeleccionDeNegocios.class);
                                        startActivity(botonParaRegistrarse);

                                        String creacionUsuario = "INSERT INTO usuario (correo, contrasena) VALUES ('"+primaryKey+"','"+contraseña+"');";
                                        baseDatos.execSQL(creacionUsuario);
                                    } else {
                                        Snackbar.make(v, "Las contraseñas deben coincidir", Snackbar.LENGTH_SHORT).show();
                                    }
                                } else {
                                    Snackbar.make(v, "El DNI debe contener 8 digitos y 1 letra", Snackbar.LENGTH_SHORT).show();
                                }
                            } else {
                                Snackbar.make(v, "El correo '"+gmail + "' debe terminar en '@gmail.com'", Snackbar.LENGTH_SHORT).show();
                            }
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Snackbar.make(v, "Error de base de datos", Snackbar.LENGTH_SHORT).show();
                    }
                });
            } else {
                Snackbar.make(v, "Rellena todos los campos correctamente", Snackbar.LENGTH_SHORT).show();
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
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    getContext(),
                    new DatePickerDialog.OnDateSetListener() {
                        @Override
                        public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                            Calendar fechaSeleccionada = Calendar.getInstance();
                            fechaSeleccionada.set(Calendar.YEAR, year);
                            fechaSeleccionada.set(Calendar.MONTH, monthOfYear);
                            fechaSeleccionada.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                            SimpleDateFormat formato = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                            String fechaFormateada = formato.format(fechaSeleccionada.getTime());

                            campoNacimientoRegistro.setText(fechaFormateada);
                        }
                    }, anio, mes, dia);
            datePickerDialog.show();
        }
    }
}
